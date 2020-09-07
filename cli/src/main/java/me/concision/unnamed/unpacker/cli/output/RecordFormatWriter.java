package me.concision.unnamed.unpacker.cli.output;

import lombok.NonNull;
import lombok.extern.java.Log;
import me.concision.unnamed.unpacker.api.PackageParser;
import me.concision.unnamed.unpacker.api.PackageParser.PackageEntry;
import me.concision.unnamed.unpacker.cli.Unpacker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses Packages.bin input stream into package records and publishes
 *
 * @author Concision
 */
public interface RecordFormatWriter extends OutputFormatWriter {
    /**
     * {@link Log} is incompatible with interfaces.
     */
    Logger log = Logger.getLogger(RecordFormatWriter.class.getName());

    /**
     * {@inheritDoc}
     */
    @Override
    default void write(@NonNull Unpacker unpacker, @NonNull InputStream packagesStream) {
        // parse packages into record
        Queue<PackageEntry> records;
        try {
            records = PackageParser.parseStream(packagesStream);
        } catch (Throwable throwable) {
            throw new RuntimeException("failed to parse Packages.bin data stream", throwable);
        }

        // publish records
        while (!records.isEmpty()) {
            // read package record
            PackageEntry record = records.poll();

            // check if matches any patterns
            if (unpacker.args().packages.stream().anyMatch(matcher -> matcher.matches(Paths.get(record.absolutePath())))) {
                // process record
                log.info("Publishing package: " + record.absolutePath());

                // attempt publish
                try {
                    this.publish(unpacker, record);
                } catch (Throwable throwable) {
                    log.log(Level.SEVERE, "Failed to publish record: " + record.absolutePath(), throwable);
                }
            }
        }
    }

    /**
     * Publishes a single {@link PackageEntry} to the output writer.
     *
     * @param unpacker associated {@link Unpacker} instance
     * @param record   a {@link PackageEntry}
     */
    void publish(@NonNull Unpacker unpacker, @NonNull PackageEntry record) throws IOException;
}
