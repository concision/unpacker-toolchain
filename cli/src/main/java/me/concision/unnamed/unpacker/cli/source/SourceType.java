package me.concision.unnamed.unpacker.cli.source;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.concision.unnamed.unpacker.cli.CommandArguments;
import me.concision.unnamed.unpacker.cli.source.collectors.BinarySourceCollector;
import me.concision.unnamed.unpacker.cli.source.collectors.FolderSourceCollector;
import me.concision.unnamed.unpacker.cli.source.collectors.InstallSourceCollector;
import me.concision.unnamed.unpacker.cli.source.collectors.OriginSourceCollector;
import me.concision.unnamed.unpacker.cli.source.collectors.UpdaterSourceCollector;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Specifies the data source for acquiring Packages.bin
 *
 * @author Concision
 */
@RequiredArgsConstructor
public enum SourceType {
    /**
     * Launches game client updater to fetch latest files
     */
    UPDATER(false, UpdaterSourceCollector::new),
    /**
     * Streams cached files directly from origin server
     */
    ORIGIN(false, OriginSourceCollector::new),
    /**
     * Extracts from base/packages directory.
     * If no directory argument ({@link CommandArguments#sourcePath}) is specified, searches for default install location
     */
    INSTALL(false, InstallSourceCollector::new),
    /**
     * Extracts from a packages directory. {@link CommandArguments#sourcePath} indicates directory.
     */
    FOLDER(true, FolderSourceCollector::new),
    /**
     * Extracts from a raw Packages.bin file. {@link CommandArguments#sourcePath} indicates file.
     */
    BINARY(true, BinarySourceCollector::new);

    /**
     * Requires an additional {@link CommandArguments#sourcePath}
     */
    @Getter
    private final boolean requiresSource;

    /**
     * Type-specific collector instance generator
     */
    @NonNull
    private final Supplier<SourceCollector> collector;

    /**
     * See {@link SourceCollector#generate(CommandArguments)}
     *
     * @param arguments {@link CommandArguments} execution parameters
     * @return generated Packages.bin data stream
     */
    public InputStream generate(@NonNull CommandArguments arguments) throws IOException {
        return collector.get().generate(arguments);
    }
}
