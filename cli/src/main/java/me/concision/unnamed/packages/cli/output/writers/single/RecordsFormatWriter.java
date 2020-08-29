package me.concision.unnamed.packages.cli.output.writers.single;

import me.concision.unnamed.packages.cli.Extractor;
import me.concision.unnamed.packages.cli.output.FormatType;
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
    public void publish(Extractor extractor, PackageEntry record) {
        Document document = new Document();

        document.put("path", record.absolutePath());
        if (extractor.args().rawMode) {
            document.put("package", record.contents());
        } else {
            document.put("package", Lua2JsonConverter.parse(record.contents()));
        }

        outputStream.println(document.toJson());
    }
}