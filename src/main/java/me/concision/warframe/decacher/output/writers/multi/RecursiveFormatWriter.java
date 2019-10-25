package me.concision.warframe.decacher.output.writers.multi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.stream.Collectors;
import me.concision.warframe.decacher.Decacher;
import me.concision.warframe.decacher.api.PackageJsonifier;
import me.concision.warframe.decacher.api.PackageParser.PackageRecord;
import me.concision.warframe.decacher.output.FormatType;
import me.concision.warframe.decacher.output.RecordFormatWriter;
import org.bson.json.JsonWriterSettings;

/**
 * See {@link FormatType#RECURSIVE}
 *
 * @author Concision
 * @date 10/23/2019
 */
public class RecursiveFormatWriter implements RecordFormatWriter {
    @Override
    public void publish(Decacher decacher, PackageRecord record) throws IOException {
        File outputPath = decacher.args().outputPath;
        if (!outputPath.mkdirs()) {
            throw new RuntimeException("could not create directory: " + outputPath.getAbsolutePath());
        }

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

        try (PrintStream output = new PrintStream(new FileOutputStream(
                new File(outputPath, filePath + ".json")
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