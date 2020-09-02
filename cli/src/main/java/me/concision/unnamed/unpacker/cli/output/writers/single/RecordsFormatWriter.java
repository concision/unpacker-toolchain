package me.concision.unnamed.unpacker.cli.output.writers.single;

import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.output.FormatType;
import me.concision.unnamed.unpacker.api.Lua2JsonConverter;
import me.concision.unnamed.unpacker.api.PackageParser.PackageEntry;
import org.bson.Document;

/**
 * See {@link FormatType#RECORDS}
 *
 * @author Concision
 */
public class RecordsFormatWriter extends SingleRecordFormatWriter {
    @Override
    public void publish(Unpacker unpacker, PackageEntry record) {
        Document document = new Document();

        document.put("path", record.absolutePath());
        if (unpacker.args().rawMode) {
            document.put("package", record.contents());
        } else {
            document.put("package", Lua2JsonConverter.parse(record.contents(), unpacker.args().convertStringLiterals));
        }

        outputStream.println(document.toJson());
    }
}