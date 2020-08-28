package me.concision.extractor.output.writers.single;

import me.concision.extractor.Extractor;
import me.concision.extractor.api.PackageJsonifier;
import me.concision.extractor.api.PackageParser;
import me.concision.extractor.output.FormatType;
import org.bson.Document;

/**
 * See {@link FormatType#RECORDS}
 *
 * @author Concision
*/
public class RecordsFormatWriter extends SingleRecordFormatWriter {
    @Override
    public void publish(Extractor extractor, PackageParser.PackageRecord record) {
        Document document = new Document();

        document.put("path", record.fullPath());
        if (extractor.args().rawMode) {
            document.put("package", record.contents());
        } else {
            document.put("package", PackageJsonifier.parse(record.contents(), extractor.args().convertStringLiterals));
        }

        outputStream.println(document.toJson());
    }
}