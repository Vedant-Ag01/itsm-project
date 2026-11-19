$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$classPathFile = Join-Path $repoRoot 'target/telegram-smoke-classpath.txt'
$smokeSource = Join-Path $PSScriptRoot 'TelegramSmoke.java'

$java = 'java'
if ($env:JAVA_HOME) {
    $java = Join-Path $env:JAVA_HOME 'bin/java.exe'
}

Write-Host 'Preparing the Java notification channel. Maven may take a moment.'
Push-Location -LiteralPath $repoRoot
try {
    & .\mvnw.cmd -q compile dependency:build-classpath '-DincludeScope=runtime' '-Dmdep.outputFile=target/telegram-smoke-classpath.txt'
    if ($LASTEXITCODE -ne 0) {
        throw 'Maven could not prepare the test. Check the build output above and rerun this script.'
    }
} finally {
    Pop-Location
}

$dependencies = (Get-Content -LiteralPath $classPathFile -Raw).Trim()
$appClasses = Join-Path $repoRoot 'target/classes'
$classPath = $appClasses + [IO.Path]::PathSeparator + $dependencies

$maskedToken = Read-Host 'Paste your replacement BotFather token here' -AsSecureString
try {
    $token = [System.Net.NetworkCredential]::new('', $maskedToken).Password
    if ($token -notmatch '\A[0-9]+:[A-Za-z0-9_-]+\z') {
        throw 'That does not look like a BotFather token. Rerun the script with the replacement token.'
    }

    try {
        $updates = Invoke-RestMethod -Method Post -Uri ('https://api.telegram.org/bot' + $token + '/getUpdates') `
            -Body @{ timeout = 0 } -TimeoutSec 15 -MaximumRedirection 0 -ErrorAction Stop
    } catch {
        $status = $_.Exception.Response.StatusCode
        if ($status) {
            throw "Telegram returned HTTP $([int]$status) while looking up your chat. Check the token and bot setup."
        }
        throw 'Could not reach Telegram. Check your internet connection and try again.'
    }
    if ($updates.ok -ne $true) {
        throw 'Telegram did not accept the chat lookup. Check the replacement token and try again.'
    }

    $chats = @($updates.result | ForEach-Object { $_.message.chat } |
        Where-Object { $_.type -eq 'private' } | Sort-Object -Property id -Unique)
    if ($chats.Count -eq 0) {
        throw 'No private chats found. Send /start again in your private chat with the bot and rerun the script.'
    }

    Write-Host 'Choose the row for your Telegram account.'
    $chats | Format-Table @{ Label = 'Chat ID'; Expression = { $_.id } }, username, first_name | Out-Host
    $chatId = (Read-Host 'Enter your chat ID, or press Enter to stop').Trim()
    if (!$chatId) {
        return
    }
    if ($chatId -notin @($chats | ForEach-Object { [string]$_.id })) {
        throw 'That chat ID is outside the list above. Rerun the script and choose your own chat.'
    }
    Write-Host "Use $chatId as TELEGRAM_CHAT_ID in your runtime configuration."

    if ((Read-Host "Type SEND to send one test message to chat $chatId") -cne 'SEND') {
        return
    }

    $token | & $java --class-path $classPath $smokeSource $chatId
    if ($LASTEXITCODE -ne 0) {
        throw 'The Java channel could not confirm delivery. See the message above for the reason.'
    }
} finally {
    $maskedToken.Dispose()
    Remove-Variable -Name token -ErrorAction SilentlyContinue
}
