package me.concision.unpacker.output.writers.single;

import me.concision.unpacker.Unpacker;
import me.concision.unpacker.api.PackageJsonifier;
import me.concision.unpacker.api.PackageParser;
import me.concision.unpacker.output.FormatType;
import org.bson.Document;
import org.json.JSONArray;

/**
 * See {@link FormatType#LIST}
 *
 * @author Concision
 * @date 10/23/2019
 */
public class ListFormatWriter extends SingleRecordFormatWriter {
    private final JSONArray list = new JSONArray();

    @Override
    public void close() {
        outputStream.print(list.toString(2));
        super.close();
    }

    @Override
    public void publish(Unpacker unpacker, PackageParser.PackageRecord record) {
        Document document = new Document();

        document.put("path", record.fullPath());
        if (unpacker.args().rawMode) {
            document.put("package", record.contents());
        } else {
            document.put("package", PackageJsonifier.parse(record.contents()));
        }

        list.put(document);
    }
}