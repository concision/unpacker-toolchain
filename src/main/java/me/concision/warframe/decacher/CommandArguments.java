package me.concision.warframe.decacher;

import java.io.File;
import java.nio.file.PathMatcher;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.concision.warframe.decacher.output.OutputMode;
import me.concision.warframe.decacher.output.FormatType;
import me.concision.warframe.decacher.source.SourceType;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * A runtime configuration to allow code to be more concise when referencing arguments
 *
 * @author Concision
 * @date 10/7/2019
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CommandArguments {
    @NonNull
    public final Namespace namespace;

    @NonNull
    public final SourceType source;
    public final File sourcePath;

    @NonNull
    public final OutputMode outputMode;
    public final File outputPath;
    @NonNull

    public final FormatType outputFormat;

    public final boolean rawMode;
    // public final boolean cache;

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
                // source
                namespace.get("source_type"),
                namespace.get("source_location"),
                // output
                namespace.get("output_mode"),
                namespace.get("output_location"),
                // format
                namespace.get("output_format"),
                // output flags
                namespace.getBoolean("output_format_raw"),
                // namespace.getBoolean("cache"),
                // package glob patterns
                Collections.unmodifiableList(namespace.getList("packages"))
        );
    }
}