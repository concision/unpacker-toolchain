package me.concision.extractor.output.writers.single;

import me.concision.extractor.Extractor;
import me.concision.extractor.api.PackageJsonifier;
import me.concision.extractor.api.PackageParser;
import me.concision.extractor.output.FormatType;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;

/**
 * See {@link FormatType#MAP}
 *
 * @author Concision
 */
public class MapFormatWriter extends SingleRecordFormatWriter {
    private final Document document = new Document();

    @Override
    public void publish(Extractor extractor, PackageParser.PackageRecord record) {
        if (extractor.args().rawMode) {
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