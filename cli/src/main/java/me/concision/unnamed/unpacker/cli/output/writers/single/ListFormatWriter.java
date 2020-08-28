package me.concision.unnamed.unpacker.cli.output.writers.single;

import me.concision.unnamed.unpacker.api.Lua2JsonConverter;
import me.concision.unnamed.unpacker.api.PackageParser.PackageEntry;
import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.output.FormatType;
import org.bson.Document;
import org.json.JSONArray;

/**
 * See {@link FormatType#LIST}
 *
 * @author Concision
 */
public class ListFormatWriter extends SingleRecordFormatWriter {
    private final JSONArray list = new JSONArray();

    @Override
    public void close() {
        outputStream.print(list.toString(2));
        super.close();
    }

    @Override
    public void publish(Unpacker unpacker, PackageEntry record) {
        Document document = new Document();

        document.put("path", record.absolutePath());
        if (unpacker.args().rawMode) {
            document.put("package", record.contents());
        } else {
            document.put("package", Lua2JsonConverter.parse(record.contents(), unpacker.args().convertStringLiterals));
        }

        list.put(document);
    }
}