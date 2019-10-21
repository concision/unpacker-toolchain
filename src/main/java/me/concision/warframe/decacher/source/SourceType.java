package me.concision.warframe.decacher.source;

import java.io.InputStream;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.concision.warframe.decacher.CommandArguments;

/**
 * Specifies the data source for acquiring Packages.bin
 *
 * @author Concision
 * @date 10/7/2019
 */
@RequiredArgsConstructor
public enum SourceType {
    /**
     * Streams directly from Warframe origin server
     */
    ORIGIN(() -> {throw new UnsupportedOperationException();}),
    /**
     * Extracts from Warframe base/packages directory.
     * If no directory argument ({@link CommandArguments#sourcePath}) is specified, searches for default install location
     */
    INSTALL(() -> {throw new UnsupportedOperationException();}),
    /**
     * Extracts from a packages directory. {@link CommandArguments#sourcePath} indicates directory.
     */
    FOLDER(() -> {throw new UnsupportedOperationException();}),
    /**
     * Extracts from a raw Packages.bin file. {@link CommandArguments#sourcePath} indicates file.
     */
    BINARY(() -> {throw new UnsupportedOperationException();});

    /**
     * Type-specific collector instance generator
     */
    @NonNull
    private final Supplier<SourceCollector> collector;


    /**
     * {@link SourceCollector#generate(CommandArguments)}
     *
     * @param arguments {@link CommandArguments} execution parameters
     * @return generated Packages.bin data stream
     */
    public InputStream generate(@NonNull CommandArguments arguments) {
        return collector.get().generate(arguments);
    }
}