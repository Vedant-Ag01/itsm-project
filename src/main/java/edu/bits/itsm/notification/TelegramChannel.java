package edu.bits.itsm.notification;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class TelegramChannel implements NotificationChannel {
    private static final Logger LOGGER = Logger.getLogger(TelegramChannel.class.getName());
    private static final Pattern TOKEN_PATH_CHARACTERS = Pattern.compile("[a-zA-Z0-9:_-]+");
    private static final ObjectReader RESPONSE_READER = JsonMapper.builder().build().reader()
            .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                    DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_TEXT_LENGTH = 4096;

    private final URI endpoint;
    private final String encodedChatId;
    private final Transport transport;

    public TelegramChannel(String botToken, String chatId) {
        this(buildEndpoint(botToken), encodeChatId(chatId), new HttpTransport(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build()));
    }

    TelegramChannel(String botToken, String chatId, Transport transport) {
        this(buildEndpoint(botToken), encodeChatId(chatId), transport);
    }

    private TelegramChannel(URI endpoint, String encodedChatId, Transport transport) {
        if (transport == null) {
            throw new IllegalArgumentException("Telegram transport is required");
        }
        this.endpoint = endpoint;
        this.encodedChatId = encodedChatId;
        this.transport = transport;
    }

    private static URI buildEndpoint(String botToken) {
        if (botToken == null || !TOKEN_PATH_CHARACTERS.matcher(botToken).matches()) {
            throw new IllegalArgumentException("Telegram bot token is missing or contains invalid characters");
        }
        return URI.create("https://api.telegram.org/bot" + botToken + "/sendMessage");
    }

    private static String encodeChatId(String chatId) {
        if (chatId == null || chatId.isBlank()) {
            throw new IllegalArgumentException("Telegram chat ID is required");
        }
        return URLEncoder.encode(chatId.strip(), StandardCharsets.UTF_8);
    }

    /** Sends to the configured Telegram chat. The caller's recipient does not select the chat. */
    @Override
    public void send(String recipient, String subject, String message) {
        if (subject == null || message == null || (subject.isBlank() && message.isBlank())) {
            logFailure("invalid notification text", 0);
            return;
        }

        String text = subject + "\n\n" + message;
        int confirmedChunks = 0;
        for (int start = 0; start < text.length();) {
            int end = start + Math.min(MAX_TEXT_LENGTH, text.length() - start);
            if (end < text.length() && Character.isSurrogatePair(text.charAt(end - 1), text.charAt(end))) {
                end--;
            }
            if (!sendChunk(text.substring(start, end), confirmedChunks)) {
                return;
            }
            confirmedChunks++;
            start = end;
        }
    }

    @Override
    public String getChannelName() {
        return "TELEGRAM";
    }

    private boolean sendChunk(String text, int confirmedChunks) {
        String form = "chat_id=" + encodedChatId + "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();

        try {
            Response response = transport.send(request);
            if (response.status() < 200 || response.status() >= 300) {
                logFailure("HTTP status " + response.status(), confirmedChunks);
                return false;
            }
            return checkApiResponse(response.body(), confirmedChunks);
        } catch (HttpTimeoutException e) {
            logFailure("request timed out", confirmedChunks);
        } catch (IOException e) {
            logFailure("network error", confirmedChunks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logFailure("interrupted", confirmedChunks);
        }
        return false;
    }

    private boolean checkApiResponse(String body, int confirmedChunks) {
        if (body != null) {
            try {
                JsonNode result = RESPONSE_READER.readTree(body);
                JsonNode ok = result.path("ok");
                if (ok.isBoolean()) {
                    if (ok.booleanValue()) {
                        return true;
                    }
                    int code = result.path("error_code").intValueOpt().orElse(0);
                    String detail = code >= 400 && code <= 599 ? " (code " + code + ")" : "";
                    logFailure("Telegram API rejected the request" + detail, confirmedChunks);
                    return false;
                }
            } catch (JacksonException e) {
                
            }
        }
        logFailure("invalid Telegram API response", confirmedChunks);
        return false;
    }

    private static void logFailure(String reason, int confirmedChunks) {
        String partial = confirmedChunks > 0 ? " with partial delivery" : "";
        LOGGER.warning("Telegram delivery failed" + partial + " (" + reason + ", confirmedChunks=" + confirmedChunks + ")");
    }

    @FunctionalInterface
    interface Transport {
        Response send(HttpRequest request) throws IOException, InterruptedException;
    }

    record Response(int status, String body) { }

    record HttpTransport(Function<HttpRequest, CompletableFuture<HttpResponse<String>>> sendAsync) implements Transport {
        HttpTransport(HttpClient client) {
            this(request -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)));
        }

        @Override
        public Response send(HttpRequest request) throws IOException, InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            CompletableFuture<HttpResponse<String>> pending = sendAsync.apply(request);
            try {
                // Bound receipt of the entire body, including on Java 21 where the request timer covers headers.
                HttpResponse<String> response = pending.get(request.timeout().orElseThrow().toMillis(), TimeUnit.MILLISECONDS);
                return new Response(response.statusCode(), response.body());
            } catch (TimeoutException e) {
                pending.cancel(true);
                throw new HttpTimeoutException("Telegram response timed out");
            } catch (InterruptedException e) {
                pending.cancel(true);
                throw e;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof IOException ioException) {
                    throw ioException;
                }
                throw new IOException("Telegram HTTP request failed", e.getCause());
            } catch (CancellationException e) {
                throw new IOException("Telegram HTTP request was cancelled", e);
            }
        }
    }
}
