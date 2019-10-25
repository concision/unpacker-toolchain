package me.concision.warframe.decacher.output.writers.single;

import me.concision.warframe.decacher.Decacher;
import me.concision.warframe.decacher.api.PackageParser.PackageRecord;
import me.concision.warframe.decacher.output.FormatType;

/**
 * See {@link FormatType#PATHS}
 *
 * @author Concision
 * @date 10/23/2019
 */
public class PathsFormatWriter extends SingleRecordFormatWriter {
    @Override
    public void publish(Decacher decacher, PackageRecord record) {
        outputStream.println(record.fullPath());
    }
}