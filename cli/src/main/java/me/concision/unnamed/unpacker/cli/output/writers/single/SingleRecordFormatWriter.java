package me.concision.unnamed.unpacker.cli.output.writers.single;

import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.output.RecordFormatWriter;

import java.io.InputStream;

/**
 * Supports single destination output writing from {@link SingleFormatWriter} and record publishing from {@link RecordFormatWriter}
 *
 * @author Concision
 */
public abstract class SingleRecordFormatWriter extends SingleFormatWriter implements RecordFormatWriter {
    @Override
    public void format(Unpacker unpacker, InputStream packagesStream) {
        this.open(unpacker);

        try {
            RecordFormatWriter.super.format(unpacker, packagesStream);
        } finally {
            this.close();
        }
    }
}
