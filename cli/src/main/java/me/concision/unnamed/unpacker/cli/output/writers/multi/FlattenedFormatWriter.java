package me.concision.unnamed.unpacker.cli.output.writers.multi;

import me.concision.unnamed.unpacker.api.Lua2JsonConverter;
import me.concision.unnamed.unpacker.api.PackageParser.PackageEntry;
import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.output.FormatType;
import me.concision.unnamed.unpacker.cli.output.RecordFormatWriter;
import org.bson.Document;
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
    @SuppressWarnings("DuplicatedCode")
    public void publish(Unpacker unpacker, PackageEntry record) throws IOException {
        File outputPath = unpacker.args().outputPath;
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
                new File(unpacker.args().outputPath, path + ".json").getAbsoluteFile()
        ))) {
            if (unpacker.args().skipJsonificiation) {
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
