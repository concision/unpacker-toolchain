package me.concision.warframe.decacher.output.writers.single;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import lombok.NonNull;
import me.concision.warframe.decacher.CommandArguments;
import me.concision.warframe.decacher.Decacher;
import me.concision.warframe.decacher.output.OutputFormatWriter;
import org.apache.commons.compress.utils.IOUtils;

/**
 * An abstract format writer for writing to a single output destination
 *
 * @author Concision
 * @date 10/24/2019
 */
public abstract class SingleFormatWriter implements OutputFormatWriter, Closeable {
    /**
     * Destination output stream
     */
    protected PrintStream outputStream;

    /**
     * Opens an output stream to the decacher destination. If no {@link CommandArguments#outputPath} is specified, defaults to {@link System#out}
     *
     * @param decacher associated {@link Decacher} instance
     */
    public void open(@NonNull Decacher decacher) {
        File outputPath = decacher.args().outputPath;
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