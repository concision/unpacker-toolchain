package me.concision.unnamed.unpacker.cli.logging;

import me.concision.unnamed.unpacker.cli.UnpackerCmd;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * {@link UnpackerCmd} formatted logger.
 *
 * @author Concision
 */
public class UnpackerLoggerFormatter extends SimpleFormatter {
    /**
     * Default logging format (e.g. "[2020-09-06 20:18:04] INFO    Logged message content")
     */
    private static final String FORMAT = "[%1$tF %1$tT] %2$-7s %3$s %n";

    /**
     * Internal synchronously modified buffer for serializing {@link Throwable}s.
     */
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    /**
     * Wrapper around underlying byte {@link #buffer}.
     */
    private final PrintStream printStream = new PrintStream(buffer);

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String format(LogRecord record) {
        StringBuilder builder = new StringBuilder();

        // log record
        builder.append(String.format(
                FORMAT,
                new Date(record.getMillis()),
                record.getLevel().getLocalizedName(),
                record.getMessage()
        ));

        // log throwable
        Throwable thrown = record.getThrown();
        if (thrown != null) {
            // print stack trace to print stream
            thrown.printStackTrace(printStream);
            // serialize print stream to lines
            for (String line : new String(buffer.toByteArray(), StandardCharsets.ISO_8859_1).split("[\r\n]+")) {
                builder.append(String.format(
                        FORMAT,
                        new Date(record.getMillis()),
                        record.getLevel().getLocalizedName(),
                        line
                ));
            }
            // reset buffer
            buffer.reset();
        }

        // serialize in single message
        return builder.toString();
    }
}
