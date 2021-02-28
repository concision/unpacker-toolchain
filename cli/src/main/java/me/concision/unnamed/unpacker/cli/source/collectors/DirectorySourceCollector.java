package me.concision.unnamed.unpacker.cli.source.collectors;

import lombok.NonNull;
import lombok.extern.java.Log;
import me.concision.unnamed.decacher.api.CacheDecompressionInputStream;
import me.concision.unnamed.decacher.api.TocStreamReader;
import me.concision.unnamed.decacher.api.TocStreamReader.CacheEntry;
import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.source.SourceCollector;
import me.concision.unnamed.unpacker.cli.source.SourceType;
import org.apache.commons.compress.utils.BoundedInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * See {@link SourceType#DIRECTORY}
 *
 * @author Concision
 */
@Log
public class DirectorySourceCollector implements SourceCollector {
    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream acquire(@NonNull Unpacker unpacker) throws IOException {
        return generate(unpacker.args().sourcePath);
    }

    /**
     * Acquires a Packages.bin {@link InputStream} from a cache directory containing a {@link #TOC_NAME} file and
     * {@link #CACHE_NAME} file.
     *
     * @param directory .toc and .cache file directory
     * @return Packages.bin {@link InputStream}
     * @throws IOException if an underlying I/O exception occurs
     */
    public InputStream generate(@NonNull File directory) throws IOException {
        return generate(
                new BufferedInputStream(new FileInputStream(new File(directory, TOC_NAME).getAbsoluteFile())),
                new BufferedInputStream(new FileInputStream(new File(directory, CACHE_NAME).getAbsoluteFile()))
        );
    }

    /**
     * Acquires a Packages.bin {@link InputStream} from a {@link #TOC_NAME} file and {@link #CACHE_NAME} file.
     *
     * @param tocStream   {@link #TOC_NAME} {@link InputStream}
     * @param cacheStream {@link #CACHE_NAME} {@link InputStream}
     * @return Packages.bin {@link InputStream}
     * @throws IOException if an underlying I/O exception occurs
     */
    public InputStream generate(@NonNull InputStream tocStream, @NonNull InputStream cacheStream) throws IOException {
        // read Packages.bin entry
        Optional<CacheEntry> entry = new TocStreamReader(tocStream).findEntry("/Packages.bin");
        // verify an entry is present
        if (!entry.isPresent()) {
            throw new RuntimeException("Packages.bin entry not found in " + TOC_NAME);
        }
        // read entry
        CacheEntry cacheEntry = entry.get();
        log.info("Toc entry: " + cacheEntry);

        // skip offset in cache stream
        IOUtils.skip(cacheStream, cacheEntry.offset());

        // limit the input stream
        return new CacheDecompressionInputStream(new BoundedInputStream(cacheStream, cacheEntry.compressedSize()));
    }
}
