package me.concision.warframe.decacher.output;

import java.io.IOException;
import java.io.InputStream;
import lombok.NonNull;
import me.concision.warframe.decacher.Decacher;

/**
 * Processed packages input stream and formats the result to the output destination
 *
 * @author Concision
 * @date 10/21/2019
 */
public interface OutputFormatWriter {
    /**
     * Writes packages input stream to an output using an implemented format
     *
     * @param decacher       associated {@link Decacher} parameter instance
     * @param packagesStream Packages.bin input stream
     * @throws IOException if an underlying IO exception is thrown
     */
    void format(@NonNull Decacher decacher, @NonNull InputStream packagesStream) throws IOException;
}