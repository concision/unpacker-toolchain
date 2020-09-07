package me.concision.unnamed.unpacker.cli.output.writers.single;

import me.concision.unnamed.unpacker.api.PackageParser.PackageEntry;
import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.output.OutputType;

/**
 * See {@link OutputType#PATHS}.
 *
 * @author Concision
 */
public class PathsFormatWriter extends SingleRecordFormatWriter {
    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(Unpacker unpacker, PackageEntry record) {
        outputStream.println(record.absolutePath());
    }
}
