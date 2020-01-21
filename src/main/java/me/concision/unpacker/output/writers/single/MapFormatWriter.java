package me.concision.unpacker.output.writers.single;

import me.concision.unpacker.Unpacker;
import me.concision.unpacker.api.PackageJsonifier;
import me.concision.unpacker.api.PackageParser;
import me.concision.unpacker.output.FormatType;
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
    public void publish(Unpacker unpacker, PackageParser.PackageRecord record) {
        if (unpacker.args().rawMode) {
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