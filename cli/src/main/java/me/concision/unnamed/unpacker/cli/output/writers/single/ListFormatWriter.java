package me.concision.unnamed.unpacker.cli.output.writers.single;

import com.google.gson.JsonArray;
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
 * See {@link OutputType#LIST}.
 *
 * @author Concision
 */
@RequiredArgsConstructor
public class ListFormatWriter extends SingleRecordFormatWriter {
    /**
     * {@link Unpacker} instance
     */
    private final Unpacker unpacker;

    /**
     * Stores an array of all JSONified {@link PackageEntry} records to later serialized in {@link #close()}.
     */
    private final JsonArray records = new JsonArray();

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(Unpacker unpacker, PackageEntry record) {
        JsonObject object = new JsonObject();

        object.addProperty("path", record.absolutePath());
        if (unpacker.args().skipJsonification) {
            object.addProperty("package", record.contents());
        } else {
            object.add("package", Lua2JsonConverter.parse(record.contents(), unpacker.args().convertStringLiterals));
        }

        records.add(object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(outputStream));
        jsonWriter.setSerializeNulls(true);
        if (unpacker.args().prettifyJson) {
            jsonWriter.setIndent("  ");
        }
        unpacker.args().gson.toJson(records, jsonWriter);
        jsonWriter.flush();

        super.close();
    }
}
