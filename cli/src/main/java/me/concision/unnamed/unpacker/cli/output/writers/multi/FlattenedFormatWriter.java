package me.concision.unnamed.unpacker.cli.output.writers.multi;

import com.google.gson.stream.JsonWriter;
import me.concision.unnamed.unpacker.api.Lua2JsonConverter;
import me.concision.unnamed.unpacker.api.PackageParser.PackageEntry;
import me.concision.unnamed.unpacker.cli.CommandArguments;
import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.output.OutputType;
import me.concision.unnamed.unpacker.cli.output.RecordFormatWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

/**
 * See {@link OutputType#FLATTENED}.
 *
 * @author Concision
 */
public class FlattenedFormatWriter implements RecordFormatWriter {
    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("DuplicatedCode")
    public void publish(Unpacker unpacker, PackageEntry record) throws IOException {
        CommandArguments args = unpacker.args();

        // create parent directory
        File outputPath = unpacker.args().outputPath;
        if (!outputPath.mkdirs()) {
            throw new FileNotFoundException("failed to create directory: " + outputPath.getAbsolutePath());
        }

        // sanitize reserved relative filename specifies
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

        // output file
        File absoluteFile = new File(unpacker.args().outputPath, path + ".json").getAbsoluteFile();
        // write file
        try (PrintStream output = new PrintStream(new FileOutputStream(absoluteFile))) {
            if (args.skipJsonification) {
                output.print(record.contents());
            } else {
                JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(output));
                if (args.prettifyJson) {
                    jsonWriter.setIndent(args.indentationString);
                }
                args.gson.toJson(Lua2JsonConverter.parse(record.contents(), args.convertStringLiterals), jsonWriter);
                jsonWriter.flush();
            }
            output.flush();
        }
    }
}
