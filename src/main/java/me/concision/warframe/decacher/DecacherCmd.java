package me.concision.warframe.decacher;

import java.nio.file.FileSystems;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.PatternSyntaxException;
import lombok.val;
import me.concision.warframe.decacher.destination.OutputMode;
import me.concision.warframe.decacher.format.FormatType;
import me.concision.warframe.decacher.source.SourceType;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.impl.type.FileArgumentType;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
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
                // flag prefix
                .prefixChars("--")
                // width
                .defaultFormatWidth(128)
                .terminalWidthDetection(true)
                // sources
                .fromFilePrefix("@") // allow specifying paths from a file
                .build()
                .description("Extracts and processes data from Warframe's Packages.bin");

        // source flags
        val flagsGroup = parser.addArgumentGroup("flags");
        flagsGroup.addArgument("--verbose")
                .help("Verbosely logs information to standard error stream")
                .dest("verbose_logging")
                .action(Arguments.storeTrue());

        // data source
        val sourceGroup = parser.addArgumentGroup("source");
        sourceGroup.addArgument("--source-type")
                .help("Method of obtaining a Packages.bin data source\n" +
                        "ORIGIN: Streams directly from Warframe update servers\n" +
                        "INSTALL: Searches for last used Warframe install location\n" +
                        "FOLDER: Specifies H.Misc.cache/H.Misc.toc folder (specify a --source-location DIRECTORY)\n" +
                        "BINARY: Specifies a raw extracted Packages.bin file (specify a --source-location FILE)")
                .dest("source_type")
                .type(Arguments.caseInsensitiveEnumType(SourceType.class))
                .required(true);
        sourceGroup.addArgument("--source-location")
                .help("A path to a source location on the filesystem, if required by the specified --source-type")
                .dest("source_location")
                .metavar("PATH")
                .nargs("?")
                .type(new FileArgumentType().verifyExists().verifyCanRead());
        sourceGroup.addArgument("--cache")
                .help("Caches intermediate results in $TEMP folder to increase performance of multiple executions")
                .dest("cache")
                .action(Arguments.storeTrue());
        sourceGroup.addArgument("--algorithm") // memes
                .help("Package extraction algorithm to apply during the decaching process")
                .dest("algorithm")
                .type(Arguments.caseInsensitiveEnumType(AlgorithmType.class))
                .required(true);

        // data output
        val outputGroup = parser.addArgumentGroup("output");
        outputGroup.addArgument("--output-mode")
                .help("Specifies how output is controlled; options:\n" +
                        "SINGLE: Outputs all relevant package records into single output (e.g. file, stdout)\n" +
                        "        if '--output-location FILE' is specified, writes to file, otherwise to stdout\n" +
                        "MULTIPLE: Outputs relevant package records into multiple independent files\n" +
                        "       '--output-location DIRECTORY' argument must be specified")
                .dest("output_mode")
                .type(Arguments.caseInsensitiveEnumType(OutputMode.class))
                .required(true);
        outputGroup.addArgument("--output-location")
                .help("Output path destination")
                .metavar("PATH")
                .nargs("?")
                .type(new FileArgumentType().verifyCanCreate());
        val outputFormatArgument = outputGroup.addArgument("--output-format")
                .help("Specifies the output format for the given --output-mode\n" +
                        "SINGLE:\n" +
                        "  PATHS: Outputs matching package paths on each line\n" +
                        "         (e.g. /Lotus/Path/.../PackageName\\r\\n)\n" +
                        "  RECORDS: Outputs a matching package JSON record on each line\n" +
                        "           (e.g. {\"path\": \"/Lotus/Path/...\", \"package\": ...}\\r\\n)\n" +
                        "  MAP: Outputs all matching packages into a JSON map\n" +
                        "       (e.g. {\"/Lotus/Path/...\": ..., ...})\n" +
                        "  LIST: Outputs all matching packages into a JSON array\n" +
                        "        (e.g. [{\"path\": \"/Lotus/Path/...\", \"package\": ...}, ...])\n" +
                        "MULTIPLE:\n" +
                        "  RECURSIVE: Outputs each matching package as a file with replicated directory structure\n" +
                        "             (e.g. ${--output-location}/Lotus/Path/.../PackageName)\n" +
                        "  FLATTENED: Outputs each matching package as a file without replicating directory structure\n" +
                        "             (e.g. ${--output-location}/PackageName)")
                .metavar("FORMAT")
                .dest("output_format")
                .type(Arguments.caseInsensitiveEnumType(FormatType.class))
                .required(true);
        outputGroup.addArgument("--output-raw")
                .help("Skips conversion of LUA Tables to JSON (default: false)")
                .dest("output_format_raw")
                .action(Arguments.storeTrue());

        // specify positional glob
        parser.addArgument("packages")
                .help("List of packages to extract using glob patterns (default: \"**/*\")")
                .dest("packages")
                .nargs("*")
                // parse to globs
                .type((argumentParser, arg, value) -> {
                    try {
                        return FileSystems.getDefault().getPathMatcher("glob:" + value);
                    } catch (PatternSyntaxException exception) {
                        throw new ArgumentParserException("invalid glob syntax" + (exception.getMessage() != null ? ": " + exception.getMessage() : ""), argumentParser, arg);
                    }
                })
                .metavar("/glob/**/pattern/*file*")
                .setDefault(FileSystems.getDefault().getPathMatcher("glob:**/*"));

        parser.epilog("In lieu of a package list, a file containing a list may be specified with \"@file\"");

        // parse namespace, or exit runtime
        Namespace namespace = parser.parseArgsOrFail(args);


        // validate
        // memes
        AlgorithmType algorithm = namespace.get("algorithm");
        if (algorithm == AlgorithmType.MECHAGENT
                || algorithm == AlgorithmType.RANDOM && ThreadLocalRandom.current().nextInt(2) == 0
        ) {
            throw new OutOfMemoryError("please reference https://downloadmorewam.com for detailed fix");
        }
        // verify output destination
        OutputMode outputMode = namespace.get("output_mode");
        if (outputMode == OutputMode.MULTIPLE && namespace.get("output_location") == null) {
            parser.handleError(new ArgumentParserException("output mode " + OutputMode.MULTIPLE + " requires a specified --output-location DIRECTORY", parser, outputFormatArgument));
            System.exit(-1);
        }
        // check output mode with format type
        FormatType formatType = namespace.get("output_format");
        if (formatType.mode() != outputMode) {
            parser.handleError(new ArgumentParserException("output mode " + outputMode + " incompatible with format type " + formatType, parser, outputFormatArgument));
            System.exit(-1);
        }


        // initialize environment
        // set logging verbosity
        if (namespace.getBoolean("verbose_logging")) {
            System.setProperty("decacher.verbose", "ALL");
        }
        // initialize logging mechanism
        Logger log = LogManager.getLogger(DecacherCmd.class);
        log.debug("Namespace: {}", namespace);
        // stdout redirection warning
        if (outputMode == OutputMode.SINGLE && namespace.get("output_location") == null) {
            log.warn("No --output-location specified for --outout-mode SINGLE, defaulting to STDOUT");
        }


        // start extraction
        try {
            new Decacher(CommandArguments.from(namespace))
                    .execute();
        } catch (Throwable throwable) {
            // TODO: submit to Sentry
            throw new Error("an unexpected exception occurred during extraction", throwable);
        }
    }


    /**
     * Meme algorithm type, only {@link #NOT_MECHAGENT} provides deterministic decaching behaviour
     */
    private enum AlgorithmType {
        /**
         * Executes the actual decaching process
         */
        MECHAGENT,
        /**
         * Throws an {@link OutOfMemoryError} immediately
         */
        NOT_MECHAGENT,
        /**
         * Arbitrarily selects a {@link AlgorithmType}
         */
        RANDOM
    }
}