package me.concision.warframe.decacher;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Queue;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import me.concision.warframe.decacher.api.PackageParser;
import me.concision.warframe.decacher.api.PackageParser.PackageRecord;
import me.concision.warframe.decacher.output.OutputFormatWriter;
import me.concision.warframe.decacher.output.OutputMode;
import me.concision.warframe.decacher.source.SourceType;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Control flow for extraction process
 *
 * @author Concision
 * @date 10/7/2019
 */
@Log4j2
@RequiredArgsConstructor
public class Decacher {
    /**
     * Processed and validated command-line arguments
     */
    @NonNull
    @Getter
    private final CommandArguments args;

    private OutputStream singleStream;

    /**
     * Execute decaching with namespaced parameters
     */
    @SneakyThrows
    public void execute() {
        // prepare environment
        this.prepare();
        // decache with environment
        this.decache();
    }

    /**
     * Prepares environment
     */
    private void prepare() {
        if (args.outputMode == OutputMode.SINGLE) {
            if (args.sourcePath != null) {
                try {
                    singleStream = new BufferedOutputStream(new FileOutputStream(args.outputPath));
                } catch (FileNotFoundException exception) {
                    throw new RuntimeException("failed to create single output: " + args.outputPath, exception);
                }
            } else {
                singleStream = System.out;
            }
        }
    }

    private void decache() {
        // obtain a list of package records
        Queue<PackageRecord> packageRecords;
        // brackets necessary for garbage collection
        {
            log.info("Generate Packages.bin stream");
            // generate packages.bin input stream; thrown away after block has finished execution
            InputStream packagesStream;
            try {
                packagesStream = args.source.generate(args);
            } catch (Throwable throwable) {
                throw new RuntimeException("failed to generate Packages.bin input stream", throwable);
            }

            // special case for BINARY source output
            if (args.source == SourceType.BINARY) {
                try (OutputStream outputStream = singleStream) {
                    log.info("Saving extracted Packages.bin to file");
                    IOUtils.copy(packagesStream, outputStream);
                    outputStream.flush();
                    return;
                } catch (Throwable throwable) {
                    throw new RuntimeException("failed to write binary output file", throwable);
                }
            }

            // parse packages
            log.info("Parsing packages");
            try (InputStream ignored = packagesStream) {
                packageRecords = PackageParser.parsePackages(packagesStream);
            } catch (Throwable throwable) {
                throw new RuntimeException("failed to parse Packages.bin packages");
            }
        }

        // suggest cleanup package collection memory
        System.gc();

        // generate new writer
        log.info("Generating format writer");
        OutputFormatWriter formatWriter = args.outputFormat.newWriter(args);

        log.info("Formatting packages");
        // iterate over packages
        while (!packageRecords.isEmpty()) {
            // read package record
            PackageRecord record = packageRecords.poll();
            // check if matches any patterns
            if (args.packages.stream().anyMatch(matcher -> matcher.matches(Paths.get(record.fullPath())))) {
                // process record
                log.info("Publishing package: {}", record.fullPath());
                try {
                    formatWriter.publish(record);
                } catch (Throwable throwable) {
                    log.warn("Failed to parse record " + record.fullPath(), throwable);
                }
            }
        }

        // publish resulting
        log.info("Closing format writer");
        try {
            formatWriter.complete();
        } catch (Throwable throwable) {
            throw new RuntimeException("failed to publish output", throwable);
        }
        log.info("Extraction complete");
    }


    // getters

    public OutputStream singleStream() {
        if (singleStream == null) {
            throw new IllegalStateException("Decacher not set into --output-mode " + OutputMode.SINGLE);
        }
        return singleStream;
    }
}