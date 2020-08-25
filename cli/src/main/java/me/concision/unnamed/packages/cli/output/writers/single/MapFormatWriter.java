package me.concision.unnamed.packages.cli.output.writers.single;

import me.concision.unnamed.packages.cli.Extractor;
import me.concision.unnamed.packages.cli.output.FormatType;
import me.concision.unnamed.packages.ioapi.PackageJsonifier;
import me.concision.unnamed.packages.ioapi.PackageParser.PackageRecord;
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
    public void publish(Extractor extractor, PackageRecord record) {
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