package me.concision.warframe.decacher.source.collectors;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import lombok.NonNull;
import me.concision.warframe.decacher.CommandArguments;
import me.concision.warframe.decacher.api.PackageDecompressionInputStream;
import me.concision.warframe.decacher.api.TocStreamReader;
import me.concision.warframe.decacher.api.TocStreamReader.PackageEntry;
import me.concision.warframe.decacher.source.SourceCollector;
import me.concision.warframe.decacher.source.SourceType;
import org.apache.commons.io.IOUtils;

/**
 * See {@link SourceType#FOLDER}
 *
 * @author Concision
 * @date 10/21/2019
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
     * @throws IOException
     */
    InputStream generate(@NonNull InputStream tocStream, @NonNull InputStream cacheStream) throws IOException {
        // read Packages.bin entry
        Optional<PackageEntry> entry = new TocStreamReader(tocStream).findEntry("/Packages.bin");
        // verify discovered
        if (!entry.isPresent()) {
            throw new RuntimeException("Packages.bin entry not found in H.Misc.toc");
        }
        // read entry
        PackageEntry packageEntry = entry.get();

        // skip offset in cache stream
        IOUtils.skip(cacheStream, packageEntry.offset());

        return new PackageDecompressionInputStream(cacheStream);
    }
}