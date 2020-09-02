package me.concision.unnamed.unpacker.cli;

import lombok.val;
import me.concision.unnamed.unpacker.cli.output.FormatType;
import me.concision.unnamed.unpacker.cli.output.FormatType.OutputMode;
import me.concision.unnamed.unpacker.cli.source.SourceType;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.impl.type.FileArgumentType;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.nio.file.FileSystems;
import java.util.Collections;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.PatternSyntaxException;

/**
 * Command-line entrypoint; processes and validates command-line arguments
 *
 * @author Concision
 */
public class UnpackerCmd {
    public static void main(String[] args) {
        // construct argument parser
        ArgumentParser parser = ArgumentParsers.newFor("unpacker")
                // flag prefix
                .prefixChars("--")
                // width
                .defaultFormatWidth(128)
                .terminalWidthDetection(true)
                // sources
                .fromFilePrefix("@") // allow specifying paths from a file
                .build()
                .description("Extracts and processes data from Packages.bin");

        // additional flags
        val flagsGroup = parser.addArgumentGroup("flags");
        // verbose logging
        flagsGroup.addArgument("--verbose")
                .help("Verbosely logs information to standard error stream")
                .dest("verbose_logging")
                .action(Arguments.storeTrue());
        // wine command
        val wineCmdArgument = flagsGroup.addArgument("--wine-cmd")
                .help("Specifies Wine command for Linux and macOS. UNPACKER_COMMAND is used as a placeholder\n" +
                        "(e.g. --wine-cmd \"/usr/bin/wine64 UNPACKER_COMMAND\").\n" +
                        "Must be specified when \"--source-type UPDATER\".")
                .dest("wine_cmd")
                .metavar("/usr/bin/wine64")
                .nargs("?");

        // source flags
        // data source
        val sourceGroup = parser.addArgumentGroup("source");
        sourceGroup.addArgument("--source-type")
                .help("Method of obtaining a Packages.bin data source\n" +
                        "UPDATER: Downloads and executes the game updater to fetch latest\n" +
                        "         (note: 64-bit wine is required for Linux and macOS; see \"--wine-cmd\")\n" +
                        "ORIGIN: Streams cached CDN files directly from update servers\n" +
                        "        (note: these files may be out of date, use UPDATER for latest)\n" +
                        "INSTALL: Searches for last used install location\n" +
                        "FOLDER: Specifies H.Misc.cache/H.Misc.toc folder (specify a --source-location DIRECTORY)\n" +
                        "BINARY: Specifies a raw extracted Packages.bin file (specify a --source-location FILE)")
                .dest("source_type")
                .type(Arguments.caseInsensitiveEnumType(SourceType.class))
                .required(true);
        val sourceLocationArgument = sourceGroup.addArgument("--source-location")
                .help("A path to a source location on the filesystem, if required by the specified --source-type")
                .dest("source_location")
                .metavar("PATH")
                .nargs("?")
                .type(new FileArgumentType().verifyExists().verifyCanRead());


        // data output
        val outputGroup = parser.addArgumentGroup("output");
        outputGroup.description("Specifies how output is controlled; there are two types of outputs:\n" +
                "Single: Outputs all relevant package records into single output (e.g. file, stdout)\n" +
                "        if '--output-location FILE' is specified, writes to file, otherwise to stdout\n" +
                "Multiple: Outputs relevant package records into multiple independent files\n" +
                "          '--output-location DIRECTORY' argument must be specified");
        val outputLocationArgument = outputGroup.addArgument("--output-location")
                .help("Output path destination; omitting this flag will print the output to standard output")
                .metavar("PATH")
                .nargs("?")
                .type(new FileArgumentType().verifyCanCreate());
        outputGroup.addArgument("--output-format")
                .help("Specifies the output format\n" +
                        "SINGLE:\n" +
                        "  BINARY: Outputs a raw decompressed Packages.bin\n" +
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
                .help("Skips conversion of LUA Tables to JSON (default: false)\n" +
                        "Mutually exclusive with --output-convert-string-literals")
                .dest("output_format_raw")
                .action(Arguments.storeTrue());
        val outputConvertStringLiteralsArgument = outputGroup.addArgument("--output-convert-string-literals")
                .help("Strips quotes for string literals when converting LUA tables to JSON " +
                        "(e.g. \"\\\"string\\\"\" is converted to \"string\") (default: false).\n" +
                        "Mutually exclusive with \"--output-format BINARY\" and \"--output-raw\"")
                .dest("output_convert_string_literals")
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
                .setDefault(Collections.singletonList(FileSystems.getDefault().getPathMatcher("glob:**/*")));

        parser.epilog("In lieu of a package list, a file containing a list may be specified with \"@file\"");

        // parse namespace, or exit runtime
        Namespace namespace = parser.parseArgsOrFail(args);


        // validate
        // verify source has source location
        try {
            SourceType sourceType = namespace.get("source_type");
            FormatType formatType = namespace.get("output_format");

            if (sourceType.requiresSource() && namespace.get("source_location") == null) {
                throw new ArgumentParserException("source type " + sourceType + " requires a specified --source-location PATH", parser, sourceLocationArgument);
            }

            // verify output destination
            if (formatType.mode() == OutputMode.MULTIPLE && namespace.get("output_location") == null) {
                throw new ArgumentParserException("output mode " + OutputMode.MULTIPLE + " requires a specified --output-location DIRECTORY", parser, outputLocationArgument);
            }

            if (namespace.getBoolean("output_format_raw")) {
                if (namespace.getBoolean("output_convert_string_literals")) {
                    throw new ArgumentParserException("--output-raw is mutually exclusive", parser, outputConvertStringLiteralsArgument);
                }
            }

            if (formatType == FormatType.BINARY) {
                if (namespace.getBoolean("output_convert_string_literals")) {
                    throw new ArgumentParserException("--output-format " + FormatType.BINARY + " is mutually exclusive", parser, outputConvertStringLiteralsArgument);
                }
            }

            boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
            if (!isWindows) {
                if (sourceType == SourceType.UPDATER) {
                    if (namespace.get("wine_cmd") == null) {
                        throw new ArgumentParserException("--source-type " + SourceType.UPDATER + " requires --wine-cmd flag on non-Windows OSes", parser, wineCmdArgument);
                    }
                }
            }
        } catch (ArgumentParserException exception) {
            parser.handleError(exception);
            System.exit(-1);
        }


        // initialize environment
        // initialize logging mechanism
        Logger rootLogger = Logger.getLogger(UnpackerCmd.class.getPackageName());
        rootLogger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            @Override
            public synchronized String format(LogRecord record) {
                return String.format(
                        "[%1$tF %1$tT] %2$-7s %3$s %n",
                        new Date(record.getMillis()),
                        record.getLevel().getLocalizedName(),
                        record.getMessage()
                );
            }
        });
        rootLogger.addHandler(handler);
        // set logging verbosity
        if (!namespace.getBoolean("verbose_logging")) {
            rootLogger.setLevel(Level.OFF);
        }
        // initialize logger for current file
        rootLogger.config("Namespace: " + namespace);

        // start extraction
        try {
            new Unpacker(CommandArguments.from(namespace))
                    .execute();
        } catch (Throwable throwable) {
            rootLogger.log(Level.SEVERE, "An unexpected exception occurred during extraction", throwable);
            System.exit(-1);
        }
    }
}
