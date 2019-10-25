package me.concision.warframe.decacher.output.writers.single;

import me.concision.warframe.decacher.Decacher;
import me.concision.warframe.decacher.api.PackageJsonifier;
import me.concision.warframe.decacher.api.PackageParser.PackageRecord;
import me.concision.warframe.decacher.output.FormatType;
import org.bson.Document;

/**
 * See {@link FormatType#RECORDS}
 *
 * @author Concision
 * @date 10/23/2019
 */
public class RecordsFormatWriter extends SingleRecordFormatWriter {
    @Override
    public void publish(Decacher decacher, PackageRecord record) {
        Document document = new Document();

        document.put("path", record.fullPath());
        if (decacher.args().rawMode) {
            document.put("package", record.contents());
        } else {
            document.put("package", PackageJsonifier.parse(record.contents()));
        }

        outputStream.println(document.toJson());
    }
}