package me.concision.unnamed.unpacker.cli.output.writers.single;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import lombok.RequiredArgsConstructor;
import me.concision.unnamed.unpacker.api.Lua2JsonConverter;
import me.concision.unnamed.unpacker.api.PackageParser.PackageEntry;
import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.output.OutputType;

import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * See {@link OutputType#MAP}.
 *
 * @author Concision
 */
@RequiredArgsConstructor
public class MapFormatWriter extends SingleRecordFormatWriter {
    /**
     * {@link Unpacker} instance
     */
    private final Unpacker unpacker;

    /**
     * Stores a map of all JSONified {@link PackageEntry} key-value records to later serialized in {@link #close()}.
     */
    private final JsonObject map = new JsonObject();

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(Unpacker unpacker, PackageEntry record) {
        if (unpacker.args().skipJsonification) {
            map.addProperty(record.absolutePath(), record.contents());
        } else {
            map.add(
                    record.absolutePath(),
                    Lua2JsonConverter.parse(record.contents(), unpacker.args().convertStringLiterals)
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(outputStream));
        if (unpacker.args().prettifyJson) {
            jsonWriter.setIndent(unpacker.args().indentationString);
        }
        new Gson().toJson(map, jsonWriter);
        jsonWriter.flush();

        super.close();
    }
}
