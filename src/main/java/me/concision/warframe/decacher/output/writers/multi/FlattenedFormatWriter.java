package me.concision.warframe.decacher.output.writers.multi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import me.concision.warframe.decacher.Decacher;
import me.concision.warframe.decacher.api.PackageJsonifier;
import me.concision.warframe.decacher.api.PackageParser.PackageRecord;
import me.concision.warframe.decacher.output.FormatType;
import me.concision.warframe.decacher.output.RecordFormatWriter;
import org.bson.json.JsonWriterSettings;

/**
 * See {@link FormatType#FLATTENED}
 *
 * @author Concision
 * @date 10/23/2019
 */
public class FlattenedFormatWriter implements RecordFormatWriter {
    @Override
    public void publish(Decacher decacher, PackageRecord record) throws IOException {
        File outputPath = decacher.args().outputPath;
        outputPath.mkdirs();

        String path;
        switch (record.name()) {
            case ".":
                path = "_";
                break;
            case "..":
                path = "__";
                break;
            default:
                path = record.name().replaceAll("[^a-zA-Z0-9.-]", "_");
        }

        try (PrintStream output = new PrintStream(new FileOutputStream(
                new File(decacher.args().outputPath, path + ".json").getAbsoluteFile()
        ))) {
            if (decacher.args().rawMode) {
                output.print(record.contents());
            } else {
                output.print(PackageJsonifier.parse(record.contents())
                        .toJson(JsonWriterSettings.builder().indent(true).build()));
            }
            output.flush();
        }
    }
}