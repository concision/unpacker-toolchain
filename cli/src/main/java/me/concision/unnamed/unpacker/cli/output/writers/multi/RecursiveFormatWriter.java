package me.concision.unnamed.unpacker.cli.output.writers.multi;

import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.output.FormatType;
import me.concision.unnamed.unpacker.cli.output.RecordFormatWriter;
import me.concision.unnamed.unpacker.api.Lua2JsonConverter;
import me.concision.unnamed.unpacker.api.PackageParser.PackageEntry;
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
    public void publish(Unpacker unpacker, PackageEntry record) throws IOException {
        File file = new File(unpacker.args().outputPath, record.absolutePath() + ".json").getAbsoluteFile();
        if (!file.getParentFile().mkdirs()) {
            throw new FileNotFoundException("failed to create directory: " + file.getParentFile().getAbsolutePath());
        }

        String filePath = Arrays.stream(record.absolutePath().split("/"))
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
            if (unpacker.args().rawMode) {
                output.print(record.contents());
            } else {
                output.print(Lua2JsonConverter.parse(record.contents(), unpacker.args().convertStringLiterals)
                        .toJson(JsonWriterSettings.builder().indent(true).build()));
            }
            output.flush();
        }
    }
}