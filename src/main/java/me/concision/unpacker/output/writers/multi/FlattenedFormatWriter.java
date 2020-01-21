package me.concision.unpacker.output.writers.multi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import me.concision.unpacker.Unpacker;
import me.concision.unpacker.api.PackageJsonifier;
import me.concision.unpacker.api.PackageParser;
import me.concision.unpacker.output.FormatType;
import me.concision.unpacker.output.RecordFormatWriter;
import org.bson.json.JsonWriterSettings;

/**
 * See {@link FormatType#FLATTENED}
 *
 * @author Concision
*/
public class FlattenedFormatWriter implements RecordFormatWriter {
    @Override
    public void publish(Unpacker unpacker, PackageParser.PackageRecord record) throws IOException {
        File outputPath = unpacker.args().outputPath;
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
                new File(unpacker.args().outputPath, path + ".json").getAbsoluteFile()
        ))) {
            if (unpacker.args().rawMode) {
                output.print(record.contents());
            } else {
                output.print(PackageJsonifier.parse(record.contents())
                        .toJson(JsonWriterSettings.builder().indent(true).build()));
            }
            output.flush();
        }
    }
}