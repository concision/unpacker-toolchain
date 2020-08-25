package me.concision.unnamed.packages.cli.output.writers.single;

import me.concision.unnamed.packages.cli.Extractor;
import me.concision.unnamed.packages.cli.output.FormatType;
import me.concision.unnamed.packages.ioapi.PackageJsonifier;
import me.concision.unnamed.packages.ioapi.PackageParser.PackageRecord;
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
    public void publish(Extractor extractor, PackageRecord record) {
        Document document = new Document();

        document.put("path", record.fullPath());
        if (extractor.args().rawMode) {
            document.put("package", record.contents());
        } else {
            document.put("package", PackageJsonifier.parse(record.contents()));
        }

        list.put(document);
    }
}