package me.concision.unnamed.unpacker.cli.output.writers.single;

import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.output.FormatType;
import me.concision.unnamed.unpacker.api.Lua2JsonConverter;
import me.concision.unnamed.unpacker.api.PackageParser.PackageEntry;
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
    public void publish(Unpacker unpacker, PackageEntry record) {
        if (unpacker.args().rawMode) {
            document.put(record.absolutePath(), record.contents());
        } else {
            document.put(
                    record.absolutePath(),
                    Lua2JsonConverter.parse(record.contents(), unpacker.args().convertStringLiterals)
            );
        }
    }

    @Override
    public void close() {
        outputStream.print(document.toJson(JsonWriterSettings.builder().indent(true).build()));
        super.close();
    }
}
