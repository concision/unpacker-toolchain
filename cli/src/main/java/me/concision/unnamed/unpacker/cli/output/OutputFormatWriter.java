package me.concision.unnamed.unpacker.cli.output;

import lombok.NonNull;
import me.concision.unnamed.unpacker.cli.Unpacker;

import java.io.IOException;
import java.io.InputStream;

/**
 * Processes Packages.bin {@link InputStream} and writes the output according to the {@link Unpacker} instance.
 *
 * @author Concision
 */
public interface OutputFormatWriter {
    /**
     * Writes Packages.bin {@link InputStream} to an output using with a specific implemented format.
     *
     * @param unpacker       associated {@link Unpacker} parameter instance
     * @param packagesStream Packages.bin input stream
     * @throws IOException if an underlying IO exception is thrown
     */
    void write(@NonNull Unpacker unpacker, @NonNull InputStream packagesStream) throws IOException;
}
