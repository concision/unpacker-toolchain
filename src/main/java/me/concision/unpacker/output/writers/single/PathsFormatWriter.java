package me.concision.unpacker.output.writers.single;

import me.concision.unpacker.Unpacker;
import me.concision.unpacker.api.PackageParser;
import me.concision.unpacker.output.FormatType;

/**
 * See {@link FormatType#PATHS}
 *
 * @author Concision
 * @date 10/23/2019
 */
public class PathsFormatWriter extends SingleRecordFormatWriter {
    @Override
    public void publish(Unpacker unpacker, PackageParser.PackageRecord record) {
        outputStream.println(record.fullPath());
    }
}