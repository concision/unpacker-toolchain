package me.concision.unnamed.unpacker.cli.output.writers.multi;

import me.concision.unnamed.unpacker.api.Lua2JsonConverter;
import me.concision.unnamed.unpacker.api.PackageParser.PackageEntry;
import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.output.OutputType;
import me.concision.unnamed.unpacker.cli.output.RecordFormatWriter;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * See {@link OutputType#RECURSIVE}.
 *
 * @author Concision
 */
public class RecursiveFormatWriter implements RecordFormatWriter {
    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("DuplicatedCode")
    public void publish(Unpacker unpacker, PackageEntry record) throws IOException {
        // output file
        File file = new File(unpacker.args().outputPath, record.absolutePath() + ".json").getAbsoluteFile();
        // create parent directory
        if (!file.getParentFile().mkdirs()) {
            throw new FileNotFoundException("failed to create directory: " + file.getParentFile().getAbsolutePath());
        }

        // sanitize reserved relative filename specifies
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

        // write file
        try (PrintStream output = new PrintStream(new FileOutputStream(filePath))) {
            if (unpacker.args().skipJsonification) {
                output.print(record.contents());
            } else {
                Document json = Lua2JsonConverter.parse(record.contents(), unpacker.args().convertStringLiterals);
                if (unpacker.args().prettifyJson) {
                    output.print(json.toJson(JsonWriterSettings.builder().indent(true).build()));
                } else {
                    output.print(json.toJson());
                }
            }
            output.flush();
        }
    }
}
