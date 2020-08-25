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
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * See {@link FormatType#RECURSIVE}
 *
 * @author Concision
 */
public class RecursiveFormatWriter implements RecordFormatWriter {
    @Override
    public void publish(Extractor extractor, PackageParser.PackageRecord record) throws IOException {
        File file = new File(extractor.args().outputPath, record.fullPath() + ".json").getAbsoluteFile();
        if (!file.getParentFile().mkdirs()) {
            throw new FileNotFoundException("failed to create directory: " + file.getParentFile().getAbsolutePath());
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

        try (PrintStream output = new PrintStream(new FileOutputStream(filePath))) {
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