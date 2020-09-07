package me.concision.unnamed.unpacker.cli.output.writers.single;

import lombok.RequiredArgsConstructor;
import me.concision.unnamed.unpacker.api.Lua2JsonConverter;
import me.concision.unnamed.unpacker.api.PackageParser.PackageEntry;
import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.output.OutputType;
import org.bson.Document;
import org.json.JSONArray;

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
    private final JSONArray records = new JSONArray();

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(Unpacker unpacker, PackageEntry record) {
        Document document = new Document();

        document.put("path", record.absolutePath());
        if (unpacker.args().skipJsonification) {
            document.put("package", record.contents());
        } else {
            document.put("package", Lua2JsonConverter.parse(record.contents(), unpacker.args().convertStringLiterals));
        }

        records.put(document);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (unpacker.args().prettifyJson) {
            outputStream.print(records.toString(2));
        } else {
            outputStream.print(records.toString());
        }
        super.close();
    }
}
