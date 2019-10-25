package me.concision.warframe.decacher.output.writers.single;

import me.concision.warframe.decacher.Decacher;
import me.concision.warframe.decacher.api.PackageJsonifier;
import me.concision.warframe.decacher.api.PackageParser.PackageRecord;
import me.concision.warframe.decacher.output.FormatType;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;

/**
 * See {@link FormatType#MAP}
 *
 * @author Concision
 * @date 10/23/2019
 */
public class MapFormatWriter extends SingleRecordFormatWriter {
    private final Document document = new Document();

    @Override
    public void publish(Decacher decacher, PackageRecord record) {
        if (decacher.args().rawMode) {
            document.put(record.fullPath(), record.contents());
        } else {
            document.put(record.fullPath(), PackageJsonifier.parse(record.contents()));
        }
    }

    @Override
    public void close() {
        outputStream.print(document.toJson(JsonWriterSettings.builder().indent(true).build()));
        super.close();
    }
}