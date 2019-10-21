package me.concision.warframe.decacher.source;

import me.concision.warframe.decacher.CommandArguments;

/**
 * Indicates a data source for acquiring Packages.bin
 *
 * @author Concision
 * @date 10/7/2019
 */
public enum SourceType {
    /**
     * Streams directly from Warframe origin server
     */
    ORIGIN,
    /**
     * Extracts from Warframe base/packages directory.
     * If no directory argument ({@link CommandArguments#sourcePath}) is specified, searches for default install location
     */
    INSTALL,
    /**
     * Extracts from a packages directory. {@link CommandArguments#sourcePath} indicates directory.
     */
    FOLDER,
    /**
     * Extracts from a raw Packages.bin file. {@link CommandArguments#sourcePath} indicates file.
     */
    BINARY;
}