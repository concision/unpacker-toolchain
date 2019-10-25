package me.concision.warframe.decacher.output;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Queue;
import lombok.NonNull;
import me.concision.warframe.decacher.Decacher;
import me.concision.warframe.decacher.api.PackageParser;
import me.concision.warframe.decacher.api.PackageParser.PackageRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Parses Packages.bin input stream into package records and publishes
 *
 * @author Concision
 * @date 10/23/2019
 */
public interface RecordFormatWriter extends OutputFormatWriter {
    Logger log = LogManager.getLogger(RecordFormatWriter.class);

    @Override
    default void format(@NonNull Decacher decacher, @NonNull InputStream packagesStream) {
        // parse packages into record
        Queue<PackageRecord> records;
        try {
            records = PackageParser.parsePackages(packagesStream);
        } catch (Throwable throwable) {
            throw new RuntimeException("failed to parse Packages.bin data stream", throwable);
        }

        // publish records
        while (!records.isEmpty()) {
            // read package record
            PackageRecord record = records.poll();

            // check if matches any patterns
            if (decacher.args().packages.stream().anyMatch(matcher -> matcher.matches(Paths.get(record.fullPath())))) {
                // process record
                log.info("Publishing package: {}", record.fullPath());

                // attempt publish
                try {
                    this.publish(decacher, record);
                } catch (Throwable throwable) {
                    log.error("Failed to publish record: " + record.fullPath(), throwable);
                }
            }
        }
    }

    /**
     * Publishes a package record to the format writer
     *
     * @param decacher associated {@link Decacher} instance
     * @param record   {@link PackageRecord} instance
     */
    void publish(@NonNull Decacher decacher, @NonNull PackageRecord record) throws IOException;
}