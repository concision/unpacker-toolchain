package me.concision.unnamed.unpacker.cli.source;

import lombok.NonNull;
import me.concision.unnamed.unpacker.cli.CommandArguments;

import java.io.IOException;
import java.io.InputStream;

/**
 * An interface for collectors providing the Packages.bin data stream for collecting packages.
 * Streams may be less performant if they are lazily generated.
 *
 * @author Concision
 */
public interface SourceCollector {
    /**
     * Generates a Packages.bin data stream
     *
     * @param args {@link CommandArguments} execution parameters
     * @return a Packages.bin {@link InputStream}
     */
    InputStream generate(@NonNull CommandArguments args) throws IOException;
}