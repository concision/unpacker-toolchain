package me.concision.unnamed.unpacker.cli;

import me.concision.unnamed.unpacker.cli.output.FormatType;
import me.concision.unnamed.unpacker.cli.output.FormatType.OutputMode;
import me.concision.unnamed.unpacker.cli.source.SourceType;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.impl.type.FileArgumentType;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
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
 * Unpacker CLI entrypoint - processes and validates passed command-line arguments
 *
 * @author Concision
 */
public class UnpackerCmd {
    /**
     * Checks if OS is Windows
     */
    public static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");


    // miscellaneous flags
    public static final String FLAG_VERBOSE_LOGGING = "--verbose";
    public static final String DEST_VERBOSE_LOGGING = "verbose_logging";

    public static final String FLAG_WINE_CMD = "--wine-cmd";
    public static final String DEST_WINE_CMD = "wine_cmd";

    // source flags
    public static final String DEST_SOURCE_TYPE = "source_type";
    public static final String FLAG_SOURCE_TYPE = "--source-type";

    public static final String FLAG_SOURCE_LOCATION = "--source-location";
    public static final String DEST_SOURCE_LOCATION = "source_location";

    // output flags
    public static final String FLAG_OUTPUT_LOCATION = "--output-location";
    public static final String DEST_OUTPUT_LOCATION = "output_location";

    public static final String FLAG_OUTPUT_FORMAT = "--output-format";
    public static final String DEST_OUTPUT_FORMAT = "output_format";

    public static final String FLAG_OUTPUT_SKIP_JSON = "--output-skip-json";
    public static final String DEST_OUTPUT_SKIP_JSON = "output_skip_json";

    public static final String FLAG_OUTPUT_CONVERT_STRING_LITERALS = "--output-convert-string-literals";
    public static final String DEST_OUTPUT_CONVERT_STRING_LITERALS = "output_convert_string_literals";

    public static final String FLAG_OUTPUT_PRETTIFY_JSON = "--output-prettify-json";
    public static final String DEST_OUTPUT_PRETTIFY_JSON = "output_prettify_json";

    // positional arguments
    public static final String ARGUMENT_PACKAGES = "packages";

    public static void main(String... cliArgs) {
        // ensure arguments are specified if erroneously executed by
        if (cliArgs == null) {
            cliArgs = new String[]{};
        }


        // construct argument parser
        ArgumentParser parser = ArgumentParsers.newFor("unpacker")
                // flag prefix
                .prefixChars("--")
                // width
                .defaultFormatWidth(128)
                .terminalWidthDetection(true)
                // allow specifying paths from a file
                .fromFilePrefix("@")
                // build parser
                .build()
                .description("Unpacks package entries from Packages.bin");

        // miscellaneous flags
        ArgumentGroup flagsGroup = parser.addArgumentGroup("flags");
        // verbose logging
        flagsGroup.addArgument(FLAG_VERBOSE_LOGGING)
                .help("Enables verbose logging to standard error (default: false)")
                .dest(DEST_VERBOSE_LOGGING)
                .action(Arguments.storeTrue());
        // wine command
        Argument wineCmdArgument = flagsGroup.addArgument(FLAG_WINE_CMD)
                .help("Specifies Wine command for non-Windows OSes with %UNPACKER_COMMAND% as a placeholder\n" +
                        "(e.g. '" + FLAG_WINE_CMD + " \"/usr/bin/wine64 %UNPACKER_COMMAND%\"').\n" +
                        "Required if '" + FLAG_SOURCE_TYPE + " UPDATER'.")
                .dest(DEST_WINE_CMD)
                .required(false)
                .metavar("/usr/bin/wine64 %UNPACKER_COMMAND%")
                .nargs("?");

        // source flags
        ArgumentGroup sourceGroup = parser.addArgumentGroup("source");
        sourceGroup.description("Specifies how the unpacker should locate a Packages.bin");
        // source type
        sourceGroup.addArgument(FLAG_SOURCE_TYPE)
                .help("Method for obtaining a Packages.bin source\n" +
                        "UPDATER: Downloads and executes the game client updater to fetch latest\n" +
                        "         (note: 64-bit Wine is required for Linux and macOS; see '" + FLAG_WINE_CMD + "')\n" +
                        "ORIGIN: Streams cached CDN files directly from origin servers\n" +
                        "        (note: these files may slightly be out of date, use UPDATER for latest)\n" +
                        "INSTALL: Searches for install location in Windows registry (note: Windows only)\n" +
                        "FOLDER: Specifies Cache.Windows folder (requires '" + FLAG_SOURCE_LOCATION + " DIRECTORY')\n" +
                        "BINARY: Specifies a raw extracted Packages.bin file (requires '" + FLAG_SOURCE_LOCATION + " FILE')")
                .dest(DEST_SOURCE_TYPE)
                .type(Arguments.caseInsensitiveEnumType(SourceType.class))
                .required(true);
        // source location
        Argument sourceLocationArgument = sourceGroup.addArgument(FLAG_SOURCE_LOCATION)
                .help("Filesystem path to source location, if required by the specified '" + FLAG_SOURCE_TYPE + "'")
                .dest(DEST_SOURCE_LOCATION)
                .metavar("PATH")
                .required(false)
                .nargs("?")
                .type(new FileArgumentType().verifyExists().verifyCanRead());

        // output flags
        ArgumentGroup outputGroup = parser.addArgumentGroup("output");
        outputGroup.description("Processed Packages.bin output format; there are two types of outputs:\n" +
                "Single: Outputs matching package records into one output (e.g. file, stdout)\n" +
                "        if '" + FLAG_OUTPUT_LOCATION + " FILE' is unspecified, output is written to standard out\n" +
                "Multiple: Outputs matching package records into multiple independent files\n" +
                "          '" + FLAG_OUTPUT_LOCATION + " DIRECTORY' argument must be specified");
        // output format
        outputGroup.addArgument(FLAG_OUTPUT_FORMAT)
                .help("Specifies the output format\n" +
                        "Single target:\n" +
                        "  BINARY: Outputs a raw cache-decompressed Packages.bin\n" +
                        "  PATHS: Outputs matching package absolute paths on each line\n" +
                        "         (e.g. /Path/Directory/.../PackageName\\r\\n)\n" +
                        "  RECORDS: Outputs a matching package JSON record on each line\n" +
                        "           (e.g. {\"path\": \"/Path/Directory/.../PackageName\", \"package\": ...}\\r\\n)\n" +
                        "  MAP: Outputs all matching packages into a JSON map\n" +
                        "       (e.g. {\"/Path/Directory/...PackageName\": ..., ...})\n" +
                        "  LIST: Outputs all matching packages into a JSON array\n" +
                        "        (e.g. [{\"path\": \"/Path/Directory/.../PackageName\", \"package\": ...}, ...])\n" +
                        "Multiple targets ('" + FLAG_SOURCE_LOCATION + " DIRECTORY' is required):\n" +
                        "  RECURSIVE: Outputs each matching package as a file with replicated directory structure\n" +
                        "             (e.g. ${" + FLAG_OUTPUT_LOCATION + "}/Path/Directory/.../PackageName)\n" +
                        "  FLATTENED: Outputs each matching package as a file without replicating directory structure\n" +
                        "             (e.g. ${" + FLAG_OUTPUT_LOCATION + "}/PackageName)")
                .metavar("FORMAT")
                .dest(DEST_OUTPUT_FORMAT)
                .type(Arguments.caseInsensitiveEnumType(FormatType.class))
                .required(true)
                .nargs("?");
        // output location
        Argument outputLocationArgument = outputGroup.addArgument(FLAG_OUTPUT_LOCATION)
                .help("Output path destination; omitting this flag will print the output to standard output if '" + FLAG_OUTPUT_FORMAT + "' is a single target")
                .metavar("PATH")
                .dest(DEST_OUTPUT_LOCATION)
                .nargs("?")
                .type(new FileArgumentType().verifyCanCreate());
        // skip jsonification
        outputGroup.addArgument(FLAG_OUTPUT_SKIP_JSON)
                .help("Skips conversion of LUA Tables to JSON (default: false)\n" +
                        "Mutually exclusive with '" + FLAG_OUTPUT_CONVERT_STRING_LITERALS + "'")
                .dest(DEST_OUTPUT_SKIP_JSON)
                .action(Arguments.storeTrue());
        // convert string literals
        Argument outputConvertStringLiteralsArgument = outputGroup.addArgument(FLAG_OUTPUT_CONVERT_STRING_LITERALS)
                .help("Strips quotes for string literals when converting LUA tables to JSON " +
                        "(e.g. \"\\\"string\\\"\" is converted to \"string\") (default: false).\n" +
                        "Mutually exclusive with either '" + FLAG_OUTPUT_FORMAT + " BINARY' or '" + FLAG_OUTPUT_SKIP_JSON + "'")
                .dest(DEST_OUTPUT_CONVERT_STRING_LITERALS)
                .action(Arguments.storeTrue());
        // prettify json
        Argument outputPrettifyJsonArgument = outputGroup.addArgument(FLAG_OUTPUT_PRETTIFY_JSON)
                .help("Prettifies outputted JSON with proper indentation (default: false).\n" +
                        "Mutually exclusive with either '" + FLAG_OUTPUT_FORMAT + " BINARY', '" + FLAG_OUTPUT_FORMAT + " RECORDS', '"
                        + FLAG_OUTPUT_FORMAT + " PATHS', or '" + FLAG_OUTPUT_SKIP_JSON + "'")
                .dest(DEST_OUTPUT_PRETTIFY_JSON)
                .action(Arguments.storeTrue());

        // positional glob patterns
        parser.addArgument(ARGUMENT_PACKAGES)
                .help("List of packages to extract using glob patterns (default: \"**/*\")")
                .dest(ARGUMENT_PACKAGES)
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

        // trailing help
        parser.epilog("In lieu of a package list, a file containing a list may be specified with \"@file\"");


        // parse namespace, or exit runtime
        Namespace namespace = parser.parseArgsOrFail(cliArgs);
        CommandArguments arguments = CommandArguments.from(namespace);

        // validate additional constraints
        try {
            // validate a source location is specified
            if (arguments.sourceType.requiresSource() && arguments.sourcePath == null) {
                throw new ArgumentParserException("'" + FLAG_SOURCE_TYPE + " " + arguments.sourceType + "' requires a specified '" + FLAG_SOURCE_LOCATION + " PATH'", parser, sourceLocationArgument);
            }

            // validate a output destination is specified
            if (arguments.outputFormat.mode() == OutputMode.MULTIPLE && arguments.outputPath == null) {
                throw new ArgumentParserException("'" + FLAG_OUTPUT_FORMAT + " " + arguments.outputFormat + "' requires a specified '" + FLAG_OUTPUT_LOCATION + " DIRECTORY'", parser, outputLocationArgument);
            }

            // validate mutually exclusive arguments
            if (arguments.convertStringLiterals) {
                if (arguments.sourceType == SourceType.BINARY) {
                    throw new ArgumentParserException("'" + FLAG_SOURCE_LOCATION + " BINARY' is mutually exclusive", parser, outputConvertStringLiteralsArgument);
                }

                if (arguments.skipJsonificiation) {
                    throw new ArgumentParserException("'" + FLAG_OUTPUT_SKIP_JSON + "' is mutually exclusive", parser, outputConvertStringLiteralsArgument);
                }
            }

            // validate mutually exclusive arguments
            if (arguments.prettifyJson) {
                if (arguments.outputFormat == FormatType.BINARY || arguments.outputFormat == FormatType.RECORDS) {
                    throw new ArgumentParserException("'" + FLAG_OUTPUT_FORMAT + " " + arguments.outputFormat + "' is mutually exclusive", parser, outputPrettifyJsonArgument);
                }

                if (arguments.skipJsonificiation) {
                    throw new ArgumentParserException("'" + FLAG_OUTPUT_SKIP_JSON + "' is mutually exclusive", parser, outputPrettifyJsonArgument);
                }
            }

            // validate non-Windows compatibility
            if (!IS_WINDOWS) {
                if (arguments.sourceType == SourceType.INSTALL) {
                    throw new ArgumentParserException("'" + FLAG_SOURCE_TYPE + " " + SourceType.INSTALL + "' is only available on Windows", parser, wineCmdArgument);
                }
                if (arguments.sourceType == SourceType.UPDATER) {
                    if (arguments.wineCmd == null) {
                        throw new ArgumentParserException("'" + FLAG_SOURCE_TYPE + " " + SourceType.UPDATER + "' requires " + FLAG_WINE_CMD + " flag on non-Windows OSes", parser, wineCmdArgument);
                    }
                }
            }
        } catch (ArgumentParserException exception) {
            parser.handleError(exception);
            System.exit(-1);
        }


        // initialize logging mechanism
        Logger log = Logger.getLogger(UnpackerCmd.class.getPackageName());
        log.setUseParentHandlers(false);
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
        log.addHandler(handler);
        // set logging verbosity
        if (!namespace.getBoolean(DEST_VERBOSE_LOGGING)) {
            log.setLevel(Level.OFF);
        }

        // start extraction
        log.config("Argument namespace: " + namespace);
        try {
            new Unpacker(arguments).execute();
        } catch (Throwable throwable) {
            log.log(Level.SEVERE, "An unexpected exception occurred during unpacking", throwable);
            System.exit(-1);
        }
    }
}
