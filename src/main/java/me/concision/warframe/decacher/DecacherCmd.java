package me.concision.warframe.decacher;

import java.io.PrintWriter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.impl.type.FileArgumentType;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Command-line entrypoint; processes and validates command-line arguments
 *
 * @author Concision
 * @date 10/5/2019
 */
public class DecacherCmd {
    public static void main(String[] args) {
        // construct argument parser
        ArgumentParser parser = ArgumentParsers.newFor("decacher")
                .prefixChars("--")
                // width
                .defaultFormatWidth(128)
                .cjkWidthHack(true)
                .terminalWidthDetection(true)
                // sources
                .fromFilePrefix("@")
                .build();
        parser.description("Extracts and processes data from Warframe's Packages.bin");
        parser.epilog("In lieu of a package list, a file containing the list may be specified with \"@file\"");

        // source flags
        ArgumentGroup flagsGroup = parser.addArgumentGroup("flags");
        flagsGroup.addArgument("--algorithm") // memes
                .help("Package extraction algorithm (required)")
                .dest("algorithm")
                .required(true)
                .choices("MECHAGENT", "NOT-MECHAGENT", "RANDOM");
        flagsGroup.addArgument("--cache")
                .help("Caches intermediate results in $TEMP folder to increase performance of multiple executions")
                .dest("cache")
                .action(Arguments.storeTrue());
        flagsGroup.addArgument("--verbose")
                .dest("verbose")
                .help("Verbosely outputs progress to standard error stream")
                .action(Arguments.storeTrue());

        // data sources
        MutuallyExclusiveGroup sourceGroup = parser.addMutuallyExclusiveGroup("source")
                .description("Packages.bin data source")
                .required(true);
        sourceGroup.addArgument("--download-origin")
                .help("Streams directly from Warframe origin server")
                .dest("source_origin")
                .action(Arguments.storeTrue());
        sourceGroup.addArgument("--warframe-dir")
                .help("Extracts from Warframe base/packages directory (default: search for install location)")
                .dest("source_warframe_install")
                .metavar("DIR")
                .nargs("?")
                .type(new FileArgumentType().verifyCanRead().verifyIsDirectory().verifyExists());
        sourceGroup.addArgument("--bin-file")
                .help("Extracts from a raw Packages.bin file")
                .dest("source_binary")
                .metavar("FILE")
                .type(new FileArgumentType().verifyCanRead().verifyIsFile().verifyExists());

        // output files
        MutuallyExclusiveGroup outputGroup = parser.addMutuallyExclusiveGroup("output destination")
                .description("Extracted package data destination")
                .required(true);
        outputGroup.addArgument("--output-dir")
                .help("Outputs each package record into a file (incompatible with --list)")
                .dest("output_directory")
                .metavar("DIR")
                .type(new FileArgumentType().verifyCanWrite().verifyIsDirectory().verifyExists());
        outputGroup.addArgument("--output-file")
                .help("Outputs results into an output file")
                .dest("output_file")
                .metavar("FILE")
                .type(new FileArgumentType().verifyCanWrite().verifyNotExists());
        outputGroup.addArgument("--output-stdout")
                .help("Outputs results directly into standard output stream")
                .dest("output_stdout")
                .action(Arguments.storeTrue());

        // output format
        MutuallyExclusiveGroup outputTypeGroup = parser.addMutuallyExclusiveGroup("output format")
                .description("Format of extracted data to write to destination")
                .required(true);
        outputTypeGroup.addArgument("--paths")
                .help("Lists all matching package paths (incompatible with --output-dir)")
                .dest("format_paths")
                .action(Arguments.storeTrue());
        outputTypeGroup.addArgument("--records")
                .help("Each line is a package data record (e.g. {\"path\": \"/Lotus/Path/...\", \"data\": ...})  (incompatible with --output-dir)")
                .dest("format_records")
                .action(Arguments.storeTrue());
        outputTypeGroup.addArgument("--recursive")
                .help("Nests package data into a recursive structure (e.g. {\"Lotus\": {\"Path\": ...}})")
                .dest("format_recursive")
                .action(Arguments.storeTrue());
        outputTypeGroup.addArgument("--flatten")
                .help("Flattens package data into absolute paths (e.g. {\"/Lotus/Path/...\": ..., ...})")
                .dest("format_flattened")
                .action(Arguments.storeTrue());

        // output format flags
        ArgumentGroup outputFlagsGroup = parser.addArgumentGroup("output flags");
        outputFlagsGroup.addArgument("--raw")
                .help("Skips conversion of LUA Tables to JSON (default: false)")
                .dest("format_raw_mode")
                .action(Arguments.storeTrue());

        // specify positional glob
        parser.addArgument("packages")
                .help("List of packages to extract using glob patterns (default: /**/*)")
                .dest("packages")
                .nargs("*")
                .metavar("/glob/**/pattern/*file*")
                .setDefault(new String[]{"/**/*"})
                .type(String.class);


        // parse namespace, or exit runtime
        Namespace namespace = parser.parseArgsOrFail(args);


        // validate a few parameters
        if (namespace.get("output_directory") != null) { // if --output-dir is set
            if (Stream.of("format_paths", "format_records").anyMatch(namespace::getBoolean)) { // if incompatible flags for --output-dir are used
                parser.printUsage(new PrintWriter(System.err, true));
                System.err.println("decacher: error: --output-dir is incompatible with --paths and --records");
                System.exit(-1);
                return;
            }
        }

        // memes
        String algorithm = namespace.getString("algorithm");
        if ("MECHAGENT".equalsIgnoreCase(algorithm)
                || "RANDOM".equalsIgnoreCase(algorithm) && ThreadLocalRandom.current().nextInt(2) == 0
        ) {
            throw new OutOfMemoryError("download more dedicated wam");
        }


        // initialize environment
        // set logging verbosity
        if (namespace.getBoolean("verbose")) {
            System.setProperty("decacher.verbose", "ALL");
        }
        // initialize logging mechanism
        Logger log = LogManager.getLogger(DecacherCmd.class);
        log.debug("Namespace: {}", namespace);


        // start extraction
        try {
            new Decacher(namespace).execute();
        } catch (Throwable throwable) {
            // TODO: submit to Sentry
            throw new Error("an unexpected exception occurred during extraction", throwable);
        }
    }
}