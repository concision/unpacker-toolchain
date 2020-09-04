package me.concision.unnamed.unpacker.cli.source.collectors;

import lombok.NonNull;
import lombok.extern.java.Log;
import me.concision.unnamed.decacher.api.CacheDecompressionInputStream;
import me.concision.unnamed.decacher.api.TocStreamReader;
import me.concision.unnamed.decacher.api.TocStreamReader.CacheEntry;
import me.concision.unnamed.unpacker.cli.CommandArguments;
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
 * See {@link SourceType#FOLDER}
 *
 * @author Concision
 */
@Log
public class FolderSourceCollector implements SourceCollector {
    @Override
    public InputStream generate(CommandArguments args) throws IOException {
        return generate(args.sourcePath);
    }

    InputStream generate(@NonNull File folder) throws IOException {
        return generate(
                new BufferedInputStream(new FileInputStream(new File(folder, TOC_NAME).getAbsoluteFile())),
                new BufferedInputStream(new FileInputStream(new File(folder, CACHE_NAME).getAbsoluteFile()))
        );
    }

    /**
     * Generates Packages.bin stream from cache files
     *
     * @param tocStream   {@link #TOC_NAME} stream
     * @param cacheStream {@link #CACHE_NAME} stream
     * @return Packages.bin stream
     * @throws IOException if an exception occurs while reading from disk
     */
    InputStream generate(@NonNull InputStream tocStream, @NonNull InputStream cacheStream) throws IOException {
        // read Packages.bin entry
        Optional<CacheEntry> entry = new TocStreamReader(tocStream).findEntry("/Packages.bin");
        // verify discovered
        if (!entry.isPresent()) {
            throw new RuntimeException("Packages.bin entry not found in " + TOC_NAME);
        }
        // read entry
        CacheEntry cacheEntry = entry.get();

        // skip offset in cache stream
        IOUtils.skip(cacheStream, cacheEntry.offset());

        // limit the input stream
        return new CacheDecompressionInputStream(new BoundedInputStream(cacheStream, cacheEntry.compressedSize()));
    }
}
