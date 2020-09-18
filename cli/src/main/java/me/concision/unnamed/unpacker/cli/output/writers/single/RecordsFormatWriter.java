package me.concision.unnamed.unpacker.cli.output.writers.single;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import me.concision.unnamed.unpacker.api.Lua2JsonConverter;
import me.concision.unnamed.unpacker.api.PackageParser.PackageEntry;
import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.output.OutputType;

import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * See {@link OutputType#RECORDS}.
 *
 * @author Concision
 */
public class RecordsFormatWriter extends SingleRecordFormatWriter {
    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(Unpacker unpacker, PackageEntry record) throws IOException {
        JsonObject map = new JsonObject();

        map.addProperty("path", record.absolutePath());
        if (unpacker.args().skipJsonification) {
            map.addProperty("package", record.contents());
        } else {
            map.add("package", Lua2JsonConverter.parse(record.contents(), unpacker.args().convertStringLiterals));
        }

        JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(outputStream));
        new Gson().toJson(map, jsonWriter);
        jsonWriter.flush();

        outputStream.println();
    }
}
