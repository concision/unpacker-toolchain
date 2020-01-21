package me.concision.unpacker.output.writers.single;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import lombok.NonNull;
import me.concision.unpacker.CommandArguments;
import me.concision.unpacker.Unpacker;
import me.concision.unpacker.output.OutputFormatWriter;
import org.apache.commons.compress.utils.IOUtils;

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
     * Opens an output stream to the unpacker destination. If no {@link CommandArguments#outputPath} is specified, defaults to {@link System#out}
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