package me.concision.unnamed.unpacker.cli;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.concision.unnamed.unpacker.cli.output.FormatType;
import me.concision.unnamed.unpacker.cli.source.SourceType;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;
import java.nio.file.PathMatcher;
import java.util.Collections;
import java.util.List;

/**
 * A runtime configuration to allow code to be more concise when referencing arguments
 *
 * @author Concision
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CommandArguments {
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
    public final FormatType outputFormat;

    // flags
    public final boolean skipJsonificiation;
    public final boolean convertStringLiterals;
    public final boolean prettifyJson;

    @NonNull
    public final List<PathMatcher> packages;

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
                namespace.getBoolean(UnpackerCmd.DEST_OUTPUT_SKIP_JSON),
                namespace.getBoolean(UnpackerCmd.DEST_OUTPUT_CONVERT_STRING_LITERALS),
                namespace.getBoolean(UnpackerCmd.DEST_OUTPUT_PRETTIFY_JSON),
                // namespace.getBoolean("cache"),
                // package glob patterns
                Collections.unmodifiableList(namespace.getList(UnpackerCmd.ARGUMENT_PACKAGES))
        );
    }
}