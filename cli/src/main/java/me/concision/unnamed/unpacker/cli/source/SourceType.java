package me.concision.unnamed.unpacker.cli.source;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.concision.unnamed.unpacker.cli.CommandArguments;
import me.concision.unnamed.unpacker.cli.source.collectors.BinarySourceCollector;
import me.concision.unnamed.unpacker.cli.source.collectors.DirectorySourceCollector;
import me.concision.unnamed.unpacker.cli.source.collectors.InstallSourceCollector;
import me.concision.unnamed.unpacker.cli.source.collectors.OriginSourceCollector;
import me.concision.unnamed.unpacker.cli.source.collectors.UpdaterSourceCollector;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * A source type for acquiring a Packages.bin {@link InputStream}.
 *
 * @author Concision
 */
@RequiredArgsConstructor
public enum SourceType {
    /**
     * Launches game client updater to fetch latest files.
     */
    UPDATER(false, UpdaterSourceCollector::new),
    /**
     * Streams cached files directly from cached CDN origin server (may be out of date).
     */
    ORIGIN(false, OriginSourceCollector::new),
    /**
     * Streams files from a local game installation on the host. Only compatible with Windows, local installation is
     * auto-discovered from the Windows Registry.
     */
    INSTALL(false, InstallSourceCollector::new),
    /**
     * Streams files from a cache directory; {@link CommandArguments#sourcePath} indicates directory.
     */
    DIRECTORY(true, DirectorySourceCollector::new),
    /**
     * Streams raw Packages.bin from a file or standard input. {@link CommandArguments#sourcePath} optionally indicates
     * the file (otherwise standard input is used).
     */
    BINARY(false, BinarySourceCollector::new);

    /**
     * Indicates the source type requires an additional {@link CommandArguments#sourcePath}
     */
    @Getter
    private final boolean requiresSource;

    /**
     * Source type collector instance constructor
     */
    @NonNull
    private final Supplier<SourceCollector> collector;

    /**
     * Instantiates a new {@link SourceCollector} and executes {@link SourceCollector#acquire(CommandArguments)}.
     *
     * @param arguments {@link CommandArguments} execution parameters
     * @return acquired Packages.bin {@link InputStream}
     */
    public InputStream generate(@NonNull CommandArguments arguments) throws IOException {
        return collector.get().acquire(arguments);
    }
}
