package me.concision.unnamed.unpacker.cli.source;

import lombok.NonNull;
import me.concision.unnamed.unpacker.cli.CommandArguments;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * An interface for collectors providing the Packages.bin data stream for collecting packages.
 * Streams may be less performant if they are lazily generated.
 *
 * @author Concision
 */
public interface SourceCollector {
    /**
     * Relevant .toc file containing Packages.bin entry
     */
    String TOC_NAME = new String(Base64.getDecoder().decode("SC5NaXNjLnRvYw=="), StandardCharsets.ISO_8859_1);
    /**
     * Relevant .cache file containing Packages.bin
     */
    String CACHE_NAME = new String(Base64.getDecoder().decode("SC5NaXNjLmNhY2hl"), StandardCharsets.ISO_8859_1);

    /**
     * Generates a Packages.bin data stream
     *
     * @param args {@link CommandArguments} execution parameters
     * @return a Packages.bin {@link InputStream}
     */
    InputStream generate(@NonNull CommandArguments args) throws IOException;
}
