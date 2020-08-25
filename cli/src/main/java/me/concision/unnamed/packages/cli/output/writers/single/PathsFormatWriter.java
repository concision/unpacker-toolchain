package me.concision.unnamed.packages.cli.output.writers.single;

import me.concision.unnamed.packages.cli.Extractor;
import me.concision.unnamed.packages.cli.output.FormatType;
import me.concision.unnamed.packages.ioapi.PackageParser.PackageRecord;

/**
 * See {@link FormatType#PATHS}
 *
 * @author Concision
 */
public class PathsFormatWriter extends SingleRecordFormatWriter {
    @Override
    public void publish(Extractor extractor, PackageRecord record) {
        outputStream.println(record.fullPath());
    }
}