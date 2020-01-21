package me.concision.unpacker.output;

import java.io.IOException;
import java.io.InputStream;
import lombok.NonNull;
import me.concision.unpacker.Unpacker;

/**
 * Processed packages input stream and formats the result to the output destination
 *
 * @author Concision
*/
public interface OutputFormatWriter {
    /**
     * Writes packages input stream to an output using an implemented format
     *
     * @param unpacker       associated {@link Unpacker} parameter instance
     * @param packagesStream Packages.bin input stream
     * @throws IOException if an underlying IO exception is thrown
     */
    void format(@NonNull Unpacker unpacker, @NonNull InputStream packagesStream) throws IOException;
}