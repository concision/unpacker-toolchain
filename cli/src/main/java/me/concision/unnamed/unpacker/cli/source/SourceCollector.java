package me.concision.unnamed.unpacker.cli.source;

import lombok.NonNull;
import me.concision.unnamed.decacher.api.TocStreamReader.CacheEntry;
import me.concision.unnamed.unpacker.cli.Unpacker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A standard interface for collectors providing the Packages.bin data stream for collecting packages.
 *
 * @author Concision
 */
public interface SourceCollector {
    /**
     * Relevant .toc file containing Packages.bin {@link CacheEntry}
     */
    String TOC_NAME = new String(Base64.getDecoder().decode("SC5NaXNjLnRvYw=="), StandardCharsets.ISO_8859_1);
    /**
     * Relevant .cache file containing Packages.bin content
     */
    String CACHE_NAME = new String(Base64.getDecoder().decode("SC5NaXNjLmNhY2hl"), StandardCharsets.ISO_8859_1);

    /**
     * Acquires a Packages.bin {@link InputStream}.
     *
     * @param unpacker {@link Unpacker} instance
     * @return a Packages.bin {@link InputStream}
     */
    InputStream acquire(@NonNull Unpacker unpacker) throws IOException;
}
