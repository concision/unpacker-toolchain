package me.concision.unnamed.unpacker.cli.output.writers.single;

import lombok.NonNull;
import me.concision.unnamed.unpacker.cli.CommandArguments;
import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.output.OutputFormatWriter;
import me.concision.unnamed.unpacker.cli.output.OutputType.OutputMode;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * An abstract {@link OutputFormatWriter} for writing to a single output destination {@link OutputMode}.
 *
 * @author Concision
 */
public abstract class SingleFormatWriter implements OutputFormatWriter, Closeable {
    /**
     * Destination {@link OutputStream}
     */
    protected PrintStream outputStream;

    /**
     * Opens an output stream to the unpacker destination. If no {@link CommandArguments#outputPath} is specified,
     * defaults to {@link System#out}.
     *
     * @param unpacker associated {@link Unpacker} instance
     */
    public void open(@NonNull Unpacker unpacker) {
        File outputPath = unpacker.args().outputPath;
        if (outputPath != null) {
            try {
                outputStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputPath)));
            } catch (FileNotFoundException exception) {
                throw new RuntimeException("failed to create single file output: " + outputPath, exception);
            }
        } else {
            outputStream = System.out;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        try {
            outputStream.flush();
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }
}
