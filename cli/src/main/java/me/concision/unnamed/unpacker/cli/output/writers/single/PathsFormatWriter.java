package me.concision.unnamed.unpacker.cli.output.writers.single;

import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.output.FormatType;
import me.concision.unnamed.unpacker.api.PackageParser.PackageEntry;

/**
 * See {@link FormatType#PATHS}
 *
 * @author Concision
 */
public class PathsFormatWriter extends SingleRecordFormatWriter {
    @Override
    public void publish(Unpacker unpacker, PackageEntry record) {
        outputStream.println(record.absolutePath());
    }
}