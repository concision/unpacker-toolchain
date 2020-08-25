package me.concision.unnamed.packages.cli.output.writers.single;

import lombok.NonNull;
import me.concision.unnamed.packages.cli.CommandArguments;
import me.concision.unnamed.packages.cli.Extractor;
import me.concision.unnamed.packages.cli.output.OutputFormatWriter;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * An abstract format writer for writing to a single output destination
 *
 * @author Concision
 */
public abstract class SingleFormatWriter implements OutputFormatWriter, Closeable {
    /**
     * Destination output stream
     */
    protected PrintStream outputStream;

    /**
     * Opens an output stream to the extractor destination. If no {@link CommandArguments#outputPath} is specified, defaults to {@link System#out}
     *
     * @param extractor associated {@link Extractor} instance
     */
    public void open(@NonNull Extractor extractor) {
        File outputPath = extractor.args().outputPath;
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
     * Closes destination stream
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