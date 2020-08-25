package me.concision.extractor.output;

import lombok.NonNull;
import me.concision.extractor.Extractor;

import java.io.IOException;
import java.io.InputStream;

/**
 * Processed packages input stream and formats the result to the output destination
 *
 * @author Concision
 */
public interface OutputFormatWriter {
    /**
     * Writes packages input stream to an output using an implemented format
     *
     * @param extractor      associated {@link Extractor} parameter instance
     * @param packagesStream Packages.bin input stream
     * @throws IOException if an underlying IO exception is thrown
     */
    void format(@NonNull Extractor extractor, @NonNull InputStream packagesStream) throws IOException;
}