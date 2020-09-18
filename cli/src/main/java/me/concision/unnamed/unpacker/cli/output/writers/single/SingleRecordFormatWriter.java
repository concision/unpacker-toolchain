package me.concision.unnamed.unpacker.cli.output.writers.single;

import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.output.RecordFormatWriter;
import org.apache.commons.compress.utils.IOUtils;

import java.io.InputStream;

/**
 * Supports single destination output writing from {@link SingleFormatWriter} and record publishing from
 * {@link RecordFormatWriter}.
 *
 * @author Concision
 */
public abstract class SingleRecordFormatWriter extends SingleFormatWriter implements RecordFormatWriter {
    /**
     * {@inheritDoc}
     */
    @Override
    public void write(Unpacker unpacker, InputStream packagesStream) {
        // open output destination
        this.open(unpacker);

        // write packages
        try {
            RecordFormatWriter.super.write(unpacker, packagesStream);
        } finally {
            IOUtils.closeQuietly(this);
        }
    }
}
