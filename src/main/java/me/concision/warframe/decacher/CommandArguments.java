package me.concision.warframe.decacher;

import java.io.File;
import java.nio.file.PathMatcher;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.concision.warframe.decacher.output.OutputDestination;
import me.concision.warframe.decacher.output.OutputFormat;
import me.concision.warframe.decacher.source.PackagesSource;
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
    public final PackagesSource source;
    public final File sourceFile;

    @NonNull
    public final OutputDestination destination;
    public final File destinationFile;

    @NonNull
    public final OutputFormat formatType;

    public final boolean rawMode;
    public final boolean cache;

    @NonNull
    public final List<PathMatcher> packages;

    /**
     * Constructs a new runtime arguments object from an argparse4j namespace
     *
     * @param namespace argparse4j {@link Namespace}
     * @return runtime configuration
     */
    public static CommandArguments from(Namespace namespace) {
        // determine source
        PackagesSource source;
        File sourceFile = null;
        if (namespace.get("source_origin") != null) {
            source = PackagesSource.ORIGIN_SERVER;
        } else if (namespace.get("source_binary") != null) {
            source = PackagesSource.LOCAL_BINARY;
            sourceFile = namespace.get("source_binary");
        } else /* must be warframe directory*/ {
            source = PackagesSource.LOCAL_INSTALLATION;
            // not guaranteed to be non-null
            sourceFile = namespace.get("source_warframe_install");
        }

        // determine destination
        OutputDestination destination;
        File destinationFile = null;
        if (namespace.get("output_directory") != null) {
            destination = OutputDestination.DIRECTORY;
            destinationFile = namespace.get("output_directory");
        } else if (namespace.get("output_file") != null) {
            destination = OutputDestination.FILE;
            destinationFile = namespace.get("output_file");
        } else if (namespace.getBoolean("output_stdout")) {
            destination = OutputDestination.STANDARD_OUT;
        } else {
            throw new IllegalArgumentException("no output destination specified");
        }

        return new CommandArguments(
                namespace,
                // source
                source,
                sourceFile,
                // destination
                destination,
                destinationFile,
                // output
                namespace.get("output_format"),
                namespace.getBoolean("output_format_raw"),
                namespace.getBoolean("cache"),
                // package glob patterns
                Collections.unmodifiableList(namespace.getList("packages"))
        );
    }
}