package me.concision.unnamed.packages.cli.output;

import lombok.NonNull;
import me.concision.unnamed.packages.cli.Extractor;
import me.concision.unnamed.packages.ioapi.PackageParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Queue;

/**
 * Parses Packages.bin input stream into package records and publishes
 *
 * @author Concision
 */
public interface RecordFormatWriter extends OutputFormatWriter {
    Logger log = LogManager.getLogger(RecordFormatWriter.class);

    @Override
    default void format(@NonNull Extractor extractor, @NonNull InputStream packagesStream) {
        // parse packages into record
        Queue<PackageParser.PackageRecord> records;
        try {
            records = PackageParser.parsePackages(packagesStream);
        } catch (Throwable throwable) {
            throw new RuntimeException("failed to parse Packages.bin data stream", throwable);
        }

        // publish records
        while (!records.isEmpty()) {
            // read package record
            PackageParser.PackageRecord record = records.poll();

            // check if matches any patterns
            if (extractor.args().packages.stream().anyMatch(matcher -> matcher.matches(Paths.get(record.fullPath())))) {
                // process record
                log.info("Publishing package: {}", record.fullPath());

                // attempt publish
                try {
                    this.publish(extractor, record);
                } catch (Throwable throwable) {
                    log.error("Failed to publish record: " + record.fullPath(), throwable);
                }
            }
        }
    }

    /**
     * Publishes a package record to the format writer
     *
     * @param extractor associated {@link Extractor} instance
     * @param record    {@link PackageParser.PackageRecord} instance
     */
    void publish(@NonNull Extractor extractor, @NonNull PackageParser.PackageRecord record) throws IOException;
}