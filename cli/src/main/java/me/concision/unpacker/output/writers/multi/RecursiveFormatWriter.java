package me.concision.unpacker.output.writers.multi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.stream.Collectors;

import me.concision.unpacker.Unpacker;
import me.concision.unpacker.api.PackageJsonifier;
import me.concision.unpacker.api.PackageParser;
import me.concision.unpacker.output.FormatType;
import me.concision.unpacker.output.RecordFormatWriter;
import org.bson.json.JsonWriterSettings;

/**
 * See {@link FormatType#RECURSIVE}
 *
 * @author Concision
*/
public class RecursiveFormatWriter implements RecordFormatWriter {
    @Override
    public void publish(Unpacker unpacker, PackageParser.PackageRecord record) throws IOException {
        File file = new File(unpacker.args().outputPath, record.fullPath() + ".json").getAbsoluteFile();
        file.getParentFile().mkdirs();

        String filePath = Arrays.stream(record.fullPath().split("/"))
                .map(path -> {
                    switch (path) {
                        case ".":
                            return "_";
                        case "..":
                            return "__";
                        default:
                            return path.replaceAll("[^a-zA-Z0-9.-]", "_");
                    }
                })
                .filter(path -> !path.isEmpty())
                .collect(Collectors.joining("/"));

        try (PrintStream output = new PrintStream(new FileOutputStream(file))) {
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