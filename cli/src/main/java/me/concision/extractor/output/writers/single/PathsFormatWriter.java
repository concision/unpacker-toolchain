package me.concision.extractor.output.writers.single;

import me.concision.extractor.Extractor;
import me.concision.extractor.api.PackageParser;
import me.concision.extractor.output.FormatType;

/**
 * See {@link FormatType#PATHS}
 *
 * @author Concision
 */
public class PathsFormatWriter extends SingleRecordFormatWriter {
    @Override
    public void publish(Extractor extractor, PackageParser.PackageRecord record) {
        outputStream.println(record.fullPath());
    }
}