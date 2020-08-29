package me.concision.unnamed.packages.cli.source.collectors;

import lombok.NonNull;
import me.concision.unnamed.packages.cli.CommandArguments;
import me.concision.unnamed.packages.cli.source.SourceCollector;
import me.concision.unnamed.packages.cli.source.SourceType;
import me.concision.unnamed.decacher.api.CacheDecompressionInputStream;
import me.concision.unnamed.decacher.api.TocStreamReader;
import me.concision.unnamed.decacher.api.TocStreamReader.CacheEntry;
import org.apache.commons.io.IOUtils;

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
public class FolderSourceCollector implements SourceCollector {
    @Override
    public InputStream generate(CommandArguments args) throws IOException {
        return generate(args.sourcePath);
    }

    InputStream generate(@NonNull File folder) throws IOException {
        return generate(
                new BufferedInputStream(new FileInputStream(new File(folder, "H.Misc.toc").getAbsoluteFile())),
                new BufferedInputStream(new FileInputStream(new File(folder, "H.Misc.cache").getAbsoluteFile()))
        );
    }

    /**
     * Generates Packages.bin stream from cache files
     *
     * @param tocStream   H.Misc.toc stream
     * @param cacheStream H.Misc.cache stream
     * @return Packages.bin stream
     * @throws IOException if an exception occurs while reading from disk
     */
    InputStream generate(@NonNull InputStream tocStream, @NonNull InputStream cacheStream) throws IOException {
        // read Packages.bin entry
        Optional<CacheEntry> entry = new TocStreamReader(tocStream).findEntry("/Packages.bin");
        // verify discovered
        if (!entry.isPresent()) {
            throw new RuntimeException("Packages.bin entry not found in H.Misc.toc");
        }
        // read entry
        CacheEntry cacheEntry = entry.get();

        // skip offset in cache stream
        IOUtils.skip(cacheStream, cacheEntry.offset());

        return new CacheDecompressionInputStream(cacheStream);
    }
}