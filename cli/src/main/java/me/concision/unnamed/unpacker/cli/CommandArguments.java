package me.concision.unnamed.unpacker.cli;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.concision.unnamed.unpacker.cli.output.OutputType;
import me.concision.unnamed.unpacker.cli.source.SourceType;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * A runtime configuration enabling more concise code when referencing arguments.
 *
 * @author Concision
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CommandArguments {
    /**
     * Original parsed namespace from CLI arguments
     */
    @NonNull
    public final Namespace namespace;

    // miscellaneous
    public final String wineCmd;

    // source
    @NonNull
    public final SourceType sourceType;
    public final File sourcePath;

    // output
    public final File outputPath;
    @NonNull
    public final OutputType outputFormat;

    // flags
    public final boolean printBuildVersion;
    public final boolean skipJsonification;
    public final boolean convertStringLiterals;
    public final boolean prettifyJson;
    public final String indentationString;

    @NonNull
    public final List<Predicate<String>> packages;

    /**
     * Constructs a new runtime arguments object from an argparse4j namespace
     *
     * @param namespace argparse4j {@link Namespace}
     * @return runtime configuration
     */
    public static CommandArguments from(Namespace namespace) {
        return new CommandArguments(
                namespace,
                // miscellaneous
                namespace.getString(UnpackerCmd.DEST_WINE_CMD),
                // source
                namespace.get(UnpackerCmd.DEST_SOURCE_TYPE),
                namespace.get(UnpackerCmd.DEST_SOURCE_PATH),
                // output
                namespace.get(UnpackerCmd.DEST_OUTPUT_PATH),
                namespace.get(UnpackerCmd.DEST_OUTPUT_FORMAT),
                // output flags
                namespace.getBoolean(UnpackerCmd.DEST_PRINT_BUILD_VERSION),
                namespace.getBoolean(UnpackerCmd.DEST_OUTPUT_SKIP_JSON),
                namespace.getBoolean(UnpackerCmd.DEST_OUTPUT_CONVERT_STRING_LITERALS),
                namespace.getBoolean(UnpackerCmd.DEST_OUTPUT_PRETTIFY_JSON),
                namespace.getString(UnpackerCmd.DEST_OUTPUT_JSON_INDENT),
                // package glob patterns
                Collections.unmodifiableList(namespace.getList(UnpackerCmd.ARGUMENT_PACKAGES))
        );
    }
}
