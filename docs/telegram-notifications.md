# Telegram notification handoff

## Connect it to the app

Vedant, register `TelegramChannel` alongside the other channels in `AppConfig`. Load the bot token and chat ID from private runtime configuration and pass them to the constructor. If you use environment variables, the setup looks like this.

```java
String botToken = System.getenv("TELEGRAM_BOT_TOKEN");
String chatId = System.getenv("TELEGRAM_CHAT_ID");
TelegramChannel telegramChannel = new TelegramChannel(botToken, chatId);
notifications.addChannel(telegramChannel);
```

For now, all Telegram notifications go to the chat ID you configure. `NotificationObserver` calls each channel once per recipient, so an incident with multiple recipients can show up more than once in the same Telegram chat. If you want one Telegram message per incident event, call the channel once per event.

## How delivery works

The channel reuses one Java `HttpClient` and waits for each send to finish. It posts `chat_id` and `text` as a UTF-8 form and counts a chunk as delivered when it gets both a 2xx HTTP status and a JSON `ok` value of `true`. Tests use a fake `Transport` to inspect requests and simulate responses offline. [Telegram Bot API](https://core.telegram.org/bots/api#sendmessage)

- The constructor throws `IllegalArgumentException` for a missing or invalid token or a blank chat ID. A token can contain ASCII letters, digits, underscores, hyphens, and colons. The constructor trims surrounding whitespace from the chat ID, and the test constructor also requires a transport.
- The text is the subject, a blank line, then the body. Either field can be blank on its own. If either field is null or both are blank, the channel logs a warning and skips the send.
- The connection timeout is 5 seconds. Each request has up to 10 seconds, including time to read the response. Long messages go out in order in chunks of up to 4096 UTF-16 code units, with surrogate pairs kept together.
- The channel tries each chunk once. A failure, including a rate limit (HTTP 429), stops the rest of the message. The warning gives the failure category, any safe status or error code, and how many chunks Telegram confirmed. Logs omit URLs, tokens, message text, raw responses, and exception details. If the send is interrupted, the channel restores the thread's interrupt flag.

## Try it in your own Telegram chat

If you shared your bot token, revoke it in BotFather and get a replacement. Then open your bot's private chat and send `/start`. Run this from the repository root in PowerShell with Java 21 and internet access.

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File ".\docs\telegram-smoke-test.ps1"
```

1. Paste the token into the masked prompt. The helper keeps it out of files and command-line arguments.
2. Find your username or first name in the table, then enter the number in that row's **Chat ID** column. The helper prints the value to use for `TELEGRAM_CHAT_ID`.
3. Type `SEND` and check your bot chat for **ITSM test notification**. You can run this check with the app and PostgreSQL stopped.

## Test results from 19 November 2026

The sandbox blocked Maven's usual cache lock, so these runs used cached dependencies offline with an in-process lock.

| Command | Result |
| --- | --- |
| `.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' '-Dtest=TelegramChannelTest' test` | All 39 Telegram tests passed. |
| `.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' test` | 454 of 455 tests passed. `ItsmApplicationTests.contextLoads` was the only error. The sandbox blocked its PostgreSQL connection (SQL state `08001`). |
| `git diff --check` and per-file `git diff --no-index --check` for the new files | No whitespace issues. |

With Telegram and the Java command stubbed out, declining skipped the send, typing `SEND` called Java once, and entering a group chat ID stopped the script. The Java helper also compiled and rejected an invalid token from standard input without displaying it.

## Test cases

| ID | Area | Input | Expected result | Actual result |
| --- | --- | --- | --- | --- |
| TG-01 | Delivery | Valid subject and body, HTTP 200 with `ok: true` | Post the combined text to `sendMessage` over HTTPS | Passed. The request method, endpoint, form header, text, and `TELEGRAM` channel name matched. |
| TG-02 | Routing | An email recipient and a configured negative chat ID | Address the request to the configured chat | Passed. The `chat_id` in the request matched the configured value. |
| TG-03 | Encoding | Quotes, `&`, `+`, CRLF, non-Latin text, and emoji | Preserve every character | Passed. The decoded subject, blank line, and body matched exactly. |
| TG-04 | HTTP errors | HTTP 400, 401, 403, and 500 with `ok: true` | Log the HTTP status and return | Passed. Each send logged its status and stopped after one request. |
| TG-05 | Rate limiting | HTTP 429 | Log the rate limit after one attempt | Passed. The warning included 429, and the channel made one request. |
| TG-06 | API rejection | HTTP 200 with `ok: false` and error code 403 | Log the safe error code and omit the response description | Passed. The warning included code 403 and omitted the description. |
| TG-07 | Response parsing | Eight variants, including empty or malformed JSON, invalid `ok` fields, and trailing data | Warn and return | Passed. The channel reported an invalid API response for all eight. |
| TG-08 | Network errors and logging | An I/O exception that includes the URL, token, and notification text | Report the network error while keeping the sensitive values private | Passed. The warning omitted all three values. |
| TG-09 | Request timeout | The transport throws `HttpTimeoutException` | Log the timeout and return | Passed. The channel logged the timeout after one request. |
| TG-10 | Interruption | The transport throws `InterruptedException` | Warn and restore the calling thread's interrupt flag | Passed. The flag was set when the send returned. |
| TG-11 | Configuration | A missing or unsafe token, missing chat ID, or missing test transport | Reject the configuration | Passed. Every invalid constructor call threw `IllegalArgumentException`. |
| TG-12 | Invalid text | Null subject, null body, or two blank fields | Warn before making a request | Passed. The three sends logged three warnings and made zero requests. |
| TG-13 | Blank field | A blank subject with a populated body | Preserve the blank subject and separator | Passed. The decoded text retained the whitespace. |
| TG-14 | Exact limit | 4096 UTF-16 code units including the separator | Send one chunk | Passed. One chunk carried the full text. |
| TG-15 | Above limit | 4097 UTF-16 code units | Send two chunks that reproduce the original text when joined | Passed. The joined text matched exactly. |
| TG-16 | Unicode boundary | An emoji lands across the 4096-unit boundary | Keep the surrogate pair together | Passed. The second chunk began with the complete emoji. |
| TG-17 | Partial delivery | The second chunk of a three-chunk message gets HTTP 429 | Log one confirmed chunk and stop | Passed. The channel made two requests and reported partial delivery. |
| TG-18 | Recovery | One send fails, then a separate send succeeds | Send the second notification normally | Passed. The second request carried the expected text, and the first send logged a warning. |
| TG-19 | Observer | Telegram fails before the observer calls a second channel | Deliver the original notification to the second channel | Passed. The next channel got the recipient, subject, and body. |
| TG-20 | HTTP adapter | The response future fails with `IOException` | Pass the original I/O error back to the channel | Passed. The adapter threw the original exception. |
| TG-21 | HTTP adapter | A pending response times out or the caller is interrupted | Cancel the pending request and report the failure | Passed. The adapter cancelled the request in both cases and passed the failure back. |
| TG-22 | Live delivery | Look up the private chat and approve one test send | See the test subject and body in Telegram | Chat lookup succeeded. Message receipt is still unconfirmed. |
