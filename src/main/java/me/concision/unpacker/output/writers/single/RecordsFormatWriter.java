package me.concision.unpacker.output.writers.single;

import me.concision.unpacker.Unpacker;
import me.concision.unpacker.api.PackageJsonifier;
import me.concision.unpacker.api.PackageParser;
import me.concision.unpacker.output.FormatType;
import org.bson.Document;

/**
 * See {@link FormatType#RECORDS}
 *
 * @author Concision
*/
public class RecordsFormatWriter extends SingleRecordFormatWriter {
    @Override
    public void publish(Unpacker unpacker, PackageParser.PackageRecord record) {
        Document document = new Document();

        document.put("path", record.fullPath());
        if (unpacker.args().rawMode) {
            document.put("package", record.contents());
        } else {
            document.put("package", PackageJsonifier.parse(record.contents()));
        }

        outputStream.println(document.toJson());
    }
}