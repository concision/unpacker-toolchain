package me.concision.unnamed.unpacker.cli.output.writers.single;

import lombok.RequiredArgsConstructor;
import me.concision.unnamed.unpacker.api.Lua2JsonConverter;
import me.concision.unnamed.unpacker.api.PackageParser.PackageEntry;
import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.output.OutputType;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;

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
    private final Document document = new Document();

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(Unpacker unpacker, PackageEntry record) {
        if (unpacker.args().skipJsonification) {
            document.put(record.absolutePath(), record.contents());
        } else {
            document.put(
                    record.absolutePath(),
                    Lua2JsonConverter.parse(record.contents(), unpacker.args().convertStringLiterals)
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (unpacker.args().prettifyJson) {
            outputStream.print(document.toJson(JsonWriterSettings.builder().indent(true).build()));
        } else {
            outputStream.print(document.toJson());
        }
        super.close();
    }
}
