# Fills a fresh I-ITSM database with realistic demo incidents using the real API.
# Run ONCE on an empty database, with the app running.
$ErrorActionPreference = "Stop"
$base = "http://localhost:8080/api"

function Login($email) {
    $body = @{ email = $email; password = "password123" } | ConvertTo-Json
    $r = Invoke-RestMethod -Method Post -Uri "$base/auth/login" -ContentType "application/json" -Body $body
    return @{ Authorization = "Bearer $($r.token)" }
}

function Send-Api($path, $headers, $body) {
    if ($body) {
        return Invoke-RestMethod -Method Post -Uri "$base$path" -Headers $headers `
            -ContentType "application/json" -Body ($body | ConvertTo-Json)
    }
    return Invoke-RestMethod -Method Post -Uri "$base$path" -Headers $headers
}

$admin = Login "admin@bits.edu"
$asha  = Login "asha@bits.edu"
$techs = @{ "Ravi" = (Login "ravi@bits.edu"); "Meera" = (Login "meera@bits.edu") }

# plan: close | resolved | progress | assigned | open
$incidents = @(
    @{ t="VPN drops every few minutes for remote staff";
       d="Remote employees are disconnected from the VPN gateway roughly every five minutes during the working day.";
       s="VPN Gateway"; c="HIGH"; i="HIGH"; u="HIGH"; n=85; cat="NETWORK"; plan="close" },
    @{ t="Campus WiFi unreachable in the main library";
       d="Students cannot join the library WiFi since this morning and the access point lights are red.";
       s="Campus WiFi"; c="HIGH"; i="HIGH"; u="MEDIUM"; n=120; cat="NETWORK"; plan="assigned" },
    @{ t="Campus WiFi unreachable in the library";
       d="Students cannot join the library WiFi since this morning, access points show red lights.";
       s="Campus WiFi"; c="HIGH"; i="HIGH"; u="MEDIUM"; n=30; cat="NETWORK"; plan="open" },
    @{ t="Payroll database connection timeouts";
       d="The payroll application times out when querying the finance database during month-end processing.";
       s="Payroll System"; c="MEDIUM"; i="HIGH"; u="MEDIUM"; n=30; cat="DATABASE"; plan="resolved" },
    @{ t="Email attachments over 5 MB are rejected";
       d="Staff receive a size limit error when sending documents that are larger than five megabytes.";
       s="Staff Email"; c="MEDIUM"; i="MEDIUM"; u="MEDIUM"; n=60; cat="APPLICATION"; plan="progress" },
    @{ t="Lab printer in block C jams on every job";
       d="The shared printer in computer lab C stops with a paper jam after each print job.";
       s="Lab Printing"; c="MEDIUM"; i="LOW"; u="LOW"; n=8; cat="HARDWARE"; plan="close" },
    @{ t="Student portal shows a blank page after login";
       d="After signing in, the student portal displays an empty white page on every browser we tried.";
       s="Student Portal"; c="HIGH"; i="MEDIUM"; u="MEDIUM"; n=45; cat="APPLICATION"; plan="resolved" },
    @{ t="Suspicious login attempts on the HR account";
       d="The identity provider logged more than two hundred failed sign-ins on a single HR mailbox overnight.";
       s="Identity Provider"; c="HIGH"; i="HIGH"; u="HIGH"; n=15; cat="SECURITY"; plan="progress" },
    @{ t="Backup job failing on the file server";
       d="The nightly backup of the departmental file server has ended with an error for three nights running.";
       s="File Server"; c="MEDIUM"; i="LOW"; u="MEDIUM"; n=15; cat="STORAGE"; plan="open" },
    @{ t="Cannot reset password through self-service";
       d="The reset link in the self-service page expires immediately, so users cannot choose a new password.";
       s="SSO Login"; c="MEDIUM"; i="LOW"; u="MEDIUM"; n=25; cat="AUTHENTICATION"; plan="close" },
    @{ t="Cloud storage quota reached for research group";
       d="The robotics research group has used its full cloud storage allowance and uploads now fail.";
       s="Cloud Storage"; c="MEDIUM"; i="LOW"; u="LOW"; n=12; cat="CLOUD"; plan="open" },
    @{ t="Classroom projector flickers during lectures";
       d="The ceiling projector in room 204 flickers and cuts out every ten minutes during teaching hours.";
       s="Classroom AV"; c="LOW"; i="MEDIUM"; u="LOW"; n=30; cat="HARDWARE"; plan="open" }
)

foreach ($x in $incidents) {
    $req = @{ title = $x.t; description = $x.d; serviceName = $x.s; criticality = $x.c;
              impact = $x.i; urgency = $x.u; affectedUsers = $x.n; category = $x.cat }
    $created = Send-Api "/incidents" $asha $req
    $id = $created.incident.id
    $line = "$id [$($created.incident.priority)] $($x.t)"

    if ($x.plan -ne "open") {
        $assigned = Send-Api "/incidents/$id/auto-assign" $admin $null
        $tech = $techs[$assigned.assignedTechnician]
        $line += "  -> $($assigned.assignedTechnician)"

        if ($x.plan -ne "assigned") {
            Send-Api "/incidents/$id/acknowledge" $tech $null | Out-Null
            Send-Api "/incidents/$id/start-work" $tech $null | Out-Null
            Send-Api "/incidents/$id/notes" $tech @{ text = "Checked logs and reproduced the problem; working on a fix." } | Out-Null
            $line += ", in progress"
        }
        if ($x.plan -eq "resolved" -or $x.plan -eq "close") {
            Send-Api "/incidents/$id/resolve" $tech @{ note = "Applied the fix and confirmed the service is working again." } | Out-Null
            $line += ", resolved"
        }
        if ($x.plan -eq "close") {
            Send-Api "/incidents/$id/verify" $asha $null | Out-Null
            Send-Api "/incidents/$id/close" $admin @{ note = "Verified by the reporter. Closing." } | Out-Null
            $line += ", verified, closed"
            try {
                Send-Api "/incidents/$id/feedback" $asha @{ rating = 5; comment = "Quick and clear fix, thank you." } | Out-Null
                $line += ", feedback given"
            } catch {
                $line += " (feedback not accepted: $($_.Exception.Message))"
            }
        }
    }
    Write-Host $line
}

Write-Host ""
Write-Host "Overview:"
Invoke-RestMethod -Uri "$base/admin/reports/overview" -Headers $admin | ConvertTo-Json