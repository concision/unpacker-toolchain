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
    public final SourceType source;
    public final File sourcePath;

    // output
    public final File outputPath;
    @NonNull
    public final FormatType outputFormat;

    // flags
    public final boolean rawMode;
    public final boolean convertStringLiterals;

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
                namespace.getString("wine_cmd"),
                // source
                namespace.get("source_type"),
                namespace.get("source_location"),
                // output
                namespace.get("output_location"),
                namespace.get("output_format"),
                // output flags
                namespace.getBoolean("output_format_raw"),
                namespace.getBoolean("output_convert_string_literals"),
                // namespace.getBoolean("cache"),
                // package glob patterns
                Collections.unmodifiableList(namespace.getList("packages"))
        );
    }
}