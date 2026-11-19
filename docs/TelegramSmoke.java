import edu.bits.itsm.notification.NotificationChannel;
import edu.bits.itsm.notification.TelegramChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

class TelegramSmoke {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Run telegram-smoke-test.ps1 to choose a private chat and test delivery.");
            System.exit(2);
            return;
        }

        String token = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
        NotificationChannel channel;
        try {
            channel = new TelegramChannel(token, args[0]);
        } catch (IllegalArgumentException e) {
            System.err.println("The bot token or chat ID is missing or invalid.");
            System.exit(2);
            return;
        } catch (UncheckedIOException e) {
            System.err.println("Java could not open a network connection. Run the script from your local PowerShell window.");
            System.exit(2);
            return;
        }

        Logger logger = Logger.getLogger(TelegramChannel.class.getName());
        DeliveryWarnings warnings = new DeliveryWarnings();
        logger.setUseParentHandlers(false);
        logger.addHandler(warnings);
        channel.send("", "ITSM test notification", "Hi! This is a test from the ITSM Java notification channel.");

        if (warnings.failed) {
            System.exit(1);
        }
        System.out.println("Telegram accepted the test message. Check your chat to confirm it arrived.");
    }

    private static final class DeliveryWarnings extends Handler {
        private boolean failed;

        @Override
        public void publish(LogRecord record) {
            if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                failed = true;
                System.err.println(record.getMessage());
            }
        }

        @Override
        public void flush() { }

        @Override
        public void close() { }
    }
}
