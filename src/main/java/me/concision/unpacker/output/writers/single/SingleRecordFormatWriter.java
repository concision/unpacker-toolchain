package me.concision.unpacker.output.writers.single;

import java.io.InputStream;

import me.concision.unpacker.Unpacker;
import me.concision.unpacker.output.RecordFormatWriter;

/**
 * Supports single destination output writing from {@link SingleFormatWriter} and record publishing from {@link RecordFormatWriter}
 *
 * @author Concision
 * @date 10/24/2019
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