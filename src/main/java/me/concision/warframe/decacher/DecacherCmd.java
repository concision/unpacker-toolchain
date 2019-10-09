package me.concision.warframe.decacher;

import java.io.PrintWriter;
import java.util.concurrent.ThreadLocalRandom;
import lombok.val;
import me.concision.warframe.decacher.CommandArguments.OutputFormat;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.impl.type.FileArgumentType;
import net.sourceforge.argparse4j.inf.ArgumentParser;
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
                .terminalWidthDetection(true)
                // sources
                .fromFilePrefix("@")
                .build();
        parser.description("Extracts and processes data from Warframe's Packages.bin");
        parser.epilog("In lieu of a package list, a file containing the list may be specified with \"@file\"");

        // source flags
        val flagsGroup = parser.addArgumentGroup("flags");
        flagsGroup.addArgument("--algorithm") // memes
                .help("Package extraction algorithm (required)")
                .dest("algorithm")
                .required(true)
                .metavar("TYPE")
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
        val sourceGroup = parser.addMutuallyExclusiveGroup("source")
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
                .type(new FileArgumentType().verifyExists().verifyCanRead().verifyIsDirectory());
        sourceGroup.addArgument("--bin-file")
                .help("Extracts from a raw Packages.bin file")
                .dest("source_binary")
                .metavar("FILE")
                .type(new FileArgumentType().verifyExists().verifyCanRead().verifyIsFile());

        // output destinations
        val outputGroup = parser.addMutuallyExclusiveGroup("output destination")
                .description("Extracted package data destination")
                .required(true);
        outputGroup.addArgument("--output-dir")
                .help("Outputs each package record into a file (incompatible with --list)")
                .dest("output_directory")
                .metavar("DIR")
                .type(new FileArgumentType().verifyExists().verifyCanWrite().verifyIsDirectory());
        outputGroup.addArgument("--output-file")
                .help("Outputs results into an output file")
                .dest("output_file")
                .metavar("FILE")
                .type(new FileArgumentType().verifyNotExists().verifyCanWrite());
        outputGroup.addArgument("--output-stdout")
                .help("Outputs results directly into standard output stream")
                .dest("output_stdout")
                .action(Arguments.storeTrue());

        // output format
        val outputTypeGroup = parser.addArgumentGroup("output format")
                .description("Format of extracted data to write to destination");
        outputTypeGroup.addArgument("--format")
                .help("PATHS: Lists all matching package paths (incompatible with --output-dir)\n" +
                        "RECORDS: Each line is a package data record (e.g. {\"path\": \"/Lotus/Path/...\", \"package\": ...})  (incompatible with --output-dir)\n" +
                        "RECURSIVE: Nests package data into a recursive structure (e.g. {\"Lotus\": {\"Path\": ...}})\n" +
                        "FLATTENED: Flattens package data into absolute paths (e.g. {\"/Lotus/Path/...\": ..., ...})"
                )
                .dest("output_format")
                .required(true)
                .metavar("TYPE")
                .type(Arguments.caseInsensitiveEnumType(OutputFormat.class));
        // output format flags
        outputTypeGroup.addArgument("--raw")
                .help("Skips conversion of LUA Tables to JSON (default: false)")
                .dest("output_format_raw")
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
            OutputFormat outputFormat = namespace.get("output_format");
            if (outputFormat == OutputFormat.PATHS || outputFormat == OutputFormat.RECORDS) { // if incompatible flags for --output-dir are used
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