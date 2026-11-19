package edu.bits.itsm.notification;

import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.event.IncidentEvent;
import edu.bits.itsm.event.IncidentEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelegramChannelTest {
    private static final String TOKEN = "123456:fake-token";
    private static final String CHAT_ID = "-1001234567890";
    private static final String ENDPOINT = "https://api.telegram.org/bot" + TOKEN + "/sendMessage";
    private static final TelegramChannel.Response OK = new TelegramChannel.Response(200, "{\"ok\":true}");
    private static final Logger LOGGER = Logger.getLogger(TelegramChannel.class.getName());

    private final List<HttpRequest> requests = new ArrayList<>();
    private final List<LogRecord> logs = new ArrayList<>();
    private final Handler logCapture = new Handler() {
        @Override
        public void publish(LogRecord record) {
            logs.add(record);
        }

        @Override
        public void flush() { }

        @Override
        public void close() { }
    };
    private boolean useParentHandlers;

    @BeforeEach
    void captureLogs() {
        useParentHandlers = LOGGER.getUseParentHandlers();
        LOGGER.setUseParentHandlers(false);
        LOGGER.addHandler(logCapture);
    }

    @AfterEach
    void restoreLogger() {
        LOGGER.removeHandler(logCapture);
        LOGGER.setUseParentHandlers(useParentHandlers);
    }

    @Test
    void sendsSubjectAndBodyToConfiguredChat() throws Exception {
        NotificationChannel channel = channelReturning(OK);

        channel.send("reporter@example.test", "Incident opened", "A technician will follow up.");

        assertEquals("TELEGRAM", channel.getChannelName());
        assertEquals(1, requests.size());
        HttpRequest request = requests.getFirst();
        assertEquals(URI.create(ENDPOINT), request.uri());
        assertEquals("POST", request.method());
        assertEquals(Duration.ofSeconds(10), request.timeout().orElseThrow());
        assertEquals("application/x-www-form-urlencoded; charset=UTF-8",
                request.headers().firstValue("Content-Type").orElseThrow());
        assertEquals(Map.of("chat_id", CHAT_ID, "text", "Incident opened\n\nA technician will follow up."),
                decodedForm(request));
        assertTrue(logs.isEmpty());
    }

    @Test
    void preservesSpecialCharactersAndUnicode() throws Exception {
        String subject = "<Incident> + & \"सर्वर\"";
        String message = "第一行\r\nsecond = 100% & + '🛠️'\n";

        channelReturning(OK).send("ignored", subject, message);

        assertEquals(subject + "\n\n" + message, decodedForm(requests.getFirst()).get("text"));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 429, 500})
    void logsHttpRejectionAndReturns(int status) {
        channelReturning(new TelegramChannel.Response(status, OK.body())).send("ignored", "Subject", "Body");

        assertEquals(1, requests.size());
        assertFailure("HTTP status " + status);
    }

    @Test
    void logsAnApiRejectionWithoutLoggingItsDescription() {
        String response = "{\"ok\":false,\"error_code\":403,\"description\":\"" + ENDPOINT + "\"}";

        channelReturning(new TelegramChannel.Response(200, response)).send("ignored", "Subject", "Body");

        assertFailure("Telegram API rejected the request (code 403)");
        assertFalse(logs.getFirst().getMessage().contains(TOKEN));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"{not JSON}", "{}", "null", "{\"ok\":\"true\"}",
            "{\"ok\":true} {}", "{\"ok\":false,\"ok\":true}"})
    void logsInvalidApiResponseAndReturns(String body) {
        channelReturning(new TelegramChannel.Response(200, body)).send("ignored", "Subject", "Body");

        assertFailure("invalid Telegram API response");
    }

    @Test
    void logsNetworkFailureWithoutLeakingCredentialsOrContent() {
        String message = "private incident information";
        NotificationChannel channel = recordingChannel(request -> {
            throw new IOException("Failed to send " + message + " to " + request.uri());
        });

        channel.send("ignored", "Subject", message);

        assertEquals(1, requests.size());
        assertFailure("network error");
        String loggedMessage = logs.getFirst().getMessage();
        assertFalse(loggedMessage.contains(ENDPOINT));
        assertFalse(loggedMessage.contains(TOKEN));
        assertFalse(loggedMessage.contains(message));
    }

    @Test
    void logsRequestTimeoutAndReturns() {
        recordingChannel(request -> {
            throw new HttpTimeoutException("fake timeout");
        }).send("ignored", "Subject", "Body");

        assertEquals(1, requests.size());
        assertFailure("request timed out");
    }

    @Test
    void restoresInterruptStatus() {
        NotificationChannel channel = recordingChannel(request -> {
            throw new InterruptedException("fake interruption");
        });

        try {
            channel.send("ignored", "Subject", "Body");
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        assertFailure("interrupted");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "123:bad/token", "123:bad?token"})
    void rejectsMissingOrUnsafeToken(String token) {
        assertThrows(IllegalArgumentException.class, () -> new TelegramChannel(token, CHAT_ID));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t"})
    void rejectsMissingChatId(String chatId) {
        assertThrows(IllegalArgumentException.class, () -> new TelegramChannel(TOKEN, chatId));
    }

    @Test
    void rejectsMissingTransport() {
        assertThrows(IllegalArgumentException.class, () -> new TelegramChannel(TOKEN, CHAT_ID, null));
    }

    @Test
    void rejectsNullAndFullyBlankText() {
        NotificationChannel channel = channelReturning(OK);

        channel.send("ignored", null, "Body");
        channel.send("ignored", "Subject", null);
        channel.send("ignored", "  ", "\t\n");

        assertTrue(requests.isEmpty());
        assertEquals(3, logs.size());
        assertTrue(logs.stream().allMatch(log -> log.getMessage().contains("invalid notification text")));
    }

    @Test
    void permitsBlankSubjectWhenBodyHasContent() throws Exception {
        channelReturning(OK).send("ignored", " ", "Body");

        assertEquals(" \n\nBody", decodedForm(requests.getFirst()).get("text"));
    }

    @ParameterizedTest
    @ValueSource(ints = {4096, 4097})
    void preservesTextAtAndJustAboveTheLimit(int totalLength) throws Exception {
        String body = "x".repeat(totalLength - 3);

        channelReturning(OK).send("ignored", "S", body);

        assertEquals(totalLength == 4096 ? 1 : 2, requests.size());
        assertEquals(4096, decodedForm(requests.getFirst()).get("text").length());
        assertEquals("S\n\n" + body, reconstructedText());
    }

    @Test
    void keepsEmojiIntactAtTheChunkBoundary() throws Exception {
        String prefix = "S\n\n" + "x".repeat(4092);

        channelReturning(OK).send("ignored", "S", "x".repeat(4092) + "🚨end");

        assertEquals(2, requests.size());
        assertEquals(prefix, decodedForm(requests.getFirst()).get("text"));
        assertEquals("🚨end", decodedForm(requests.get(1)).get("text"));
    }

    @Test
    void stopsAfterAChunkFailsAndReportsPartialDelivery() {
        NotificationChannel channel = recordingChannel(request -> requests.size() == 1
                ? OK : new TelegramChannel.Response(429, "{}"));

        channel.send("ignored", "S", "x".repeat(8192));

        assertEquals(2, requests.size());
        assertFailure("HTTP status 429");
        assertTrue(logs.getFirst().getMessage().contains("partial delivery"));
        assertTrue(logs.getFirst().getMessage().contains("confirmedChunks=1"));
    }

    @Test
    void laterNotificationsCanSucceedAfterFailure() throws Exception {
        NotificationChannel channel = recordingChannel(request -> requests.size() == 1
                ? new TelegramChannel.Response(500, "{}") : OK);

        channel.send("ignored", "First subject", "First body");
        channel.send("ignored", "Second subject", "Second body");

        assertEquals(2, requests.size());
        assertEquals("Second subject\n\nSecond body", decodedForm(requests.get(1)).get("text"));
        assertFailure("HTTP status 500");
    }

    @Test
    void observerStillNotifiesTheNextChannelAfterTelegramFails() {
        String recipient = "reporter@example.test";
        NotificationObserver observer = new NotificationObserver(event -> List.of(recipient));
        List<String> delivered = new ArrayList<>();
        observer.addChannel(channelReturning(new TelegramChannel.Response(500, "{}")));
        observer.addChannel(new NotificationChannel() {
            @Override
            public void send(String target, String subject, String message) {
                delivered.addAll(List.of(target, subject, message));
            }

            @Override
            public String getChannelName() {
                return "FAKE";
            }
        });
        IncidentEvent event = new IncidentEvent("INC-42", IncidentEventType.CREATED, "reporter", null,
                IncidentStatus.NEW, "Database connection unavailable", LocalDateTime.of(2026, 1, 1, 9, 0));
        NotificationFactory.NotificationContent content = NotificationFactory.createMessage(event);

        observer.onEvent(event);

        assertEquals(1, requests.size());
        assertEquals(List.of(recipient, content.subject(), content.body()), delivered);
        assertFailure("HTTP status 500");
    }

    @Test
    void httpTransportPreservesNetworkFailureFromClient() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT)).timeout(Duration.ofSeconds(10)).build();
        IOException networkFailure = new IOException("fake failure");
        TelegramChannel.Transport transport = new TelegramChannel.HttpTransport(
                ignored -> CompletableFuture.failedFuture(networkFailure));

        assertSame(networkFailure, assertThrows(IOException.class, () -> transport.send(request)));
    }

    @Test
    void httpTransportCancelsWhenTheResponseDoesNotCompleteInTime() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT)).timeout(Duration.ofMillis(1)).build();
        CompletableFuture<HttpResponse<String>> pending = new CompletableFuture<>();
        TelegramChannel.Transport transport = new TelegramChannel.HttpTransport(ignored -> pending);

        assertThrows(HttpTimeoutException.class, () -> transport.send(request));
        assertTrue(pending.isCancelled());
    }

    @Test
    void httpTransportCancelsWhenInterruptedWhileWaitingForResponse() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT)).timeout(Duration.ofSeconds(10)).build();
        CompletableFuture<HttpResponse<String>> pending = new CompletableFuture<>();
        TelegramChannel.Transport transport = new TelegramChannel.HttpTransport(ignored -> {
            Thread.currentThread().interrupt();
            return pending;
        });

        try {
            assertThrows(InterruptedException.class, () -> transport.send(request));
            assertTrue(pending.isCancelled());
        } finally {
            Thread.interrupted();
        }
    }

    private NotificationChannel channelReturning(TelegramChannel.Response response) {
        return recordingChannel(request -> response);
    }

    private NotificationChannel recordingChannel(TelegramChannel.Transport transport) {
        return new TelegramChannel(TOKEN, CHAT_ID, request -> {
            requests.add(request);
            return transport.send(request);
        });
    }

    private void assertFailure(String category) {
        assertEquals(1, logs.size());
        LogRecord failure = logs.getFirst();
        assertEquals(Level.WARNING, failure.getLevel());
        assertTrue(failure.getMessage().contains(category), failure::getMessage);
        assertNull(failure.getThrown());
    }

    private String reconstructedText() throws Exception {
        StringBuilder text = new StringBuilder();
        for (HttpRequest request : requests) {
            text.append(decodedForm(request).get("text"));
        }
        return text.toString();
    }

    private static Map<String, String> decodedForm(HttpRequest request) throws Exception {
        HttpResponse.BodySubscriber<String> subscriber = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscriber.onSubscribe(subscription);
            }

            @Override
            public void onNext(ByteBuffer buffer) {
                subscriber.onNext(List.of(buffer));
            }

            @Override
            public void onError(Throwable failure) {
                subscriber.onError(failure);
            }

            @Override
            public void onComplete() {
                subscriber.onComplete();
            }
        });
        String form = subscriber.getBody().toCompletableFuture().get(1, TimeUnit.SECONDS);
        Map<String, String> fields = new HashMap<>();
        for (String field : form.split("&")) {
            String[] pair = field.split("=", 2);
            fields.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
        }
        return fields;
    }
}
