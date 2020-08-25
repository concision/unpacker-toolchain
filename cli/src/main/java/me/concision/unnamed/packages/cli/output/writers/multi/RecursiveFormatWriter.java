package me.concision.unnamed.packages.cli.output.writers.multi;

import me.concision.unnamed.packages.cli.Extractor;
import me.concision.unnamed.packages.cli.output.FormatType;
import me.concision.unnamed.packages.cli.output.RecordFormatWriter;
import me.concision.unnamed.packages.ioapi.PackageJsonifier;
import me.concision.unnamed.packages.ioapi.PackageParser.PackageRecord;
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
    public void publish(Extractor extractor, PackageRecord record) throws IOException {
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