package me.concision.extractor.output.writers.multi;

import me.concision.extractor.Extractor;
import me.concision.extractor.api.PackageJsonifier;
import me.concision.extractor.api.PackageParser;
import me.concision.extractor.output.FormatType;
import me.concision.extractor.output.RecordFormatWriter;
import org.bson.json.JsonWriterSettings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * See {@link FormatType#FLATTENED}
 *
 * @author Concision
 */
public class FlattenedFormatWriter implements RecordFormatWriter {
    @Override
    public void publish(Extractor extractor, PackageParser.PackageRecord record) throws IOException {
        File outputPath = extractor.args().outputPath;
        if (!outputPath.mkdirs()) {
            throw new FileNotFoundException("failed to create directory: " + outputPath.getAbsolutePath());
        }

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
                new File(extractor.args().outputPath, path + ".json").getAbsoluteFile()
        ))) {
            if (extractor.args().rawMode) {
                output.print(record.contents());
            } else {
                output.print(PackageJsonifier.parse(record.contents())
                        .toJson(JsonWriterSettings.builder().indent(true).build()));
            }
            output.flush();
        }
    }
}