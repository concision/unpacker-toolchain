package me.concision.unnamed.unpacker.cli.output.writers.multi;

import com.google.gson.Gson;
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
        CommandArguments args = unpacker.args();

        // output file
        File file = new File(args.outputPath, record.absolutePath() + ".json").getAbsoluteFile();
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
            if (unpacker.buildVersion() != null) {
                output.println(unpacker.buildVersion());
            }
            if (args.skipJsonification) {
                output.print(record.contents());
            } else {
                JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(output));
                if (args.prettifyJson) {
                    jsonWriter.setIndent(args.indentationString);
                }
                new Gson().toJson(Lua2JsonConverter.parse(record.contents(), args.convertStringLiterals), jsonWriter);
                jsonWriter.flush();
            }
            output.flush();
        }
    }
}
