package me.concision.unnamed.packages.cli.output.writers.single;

import me.concision.unnamed.packages.cli.Extractor;
import me.concision.unnamed.packages.cli.output.RecordFormatWriter;

import java.io.InputStream;

/**
 * Supports single destination output writing from {@link SingleFormatWriter} and record publishing from {@link RecordFormatWriter}
 *
 * @author Concision
 */
public abstract class SingleRecordFormatWriter extends SingleFormatWriter implements RecordFormatWriter {
    @Override
    public void format(Extractor extractor, InputStream packagesStream) {
        this.open(extractor);

        try {
            RecordFormatWriter.super.format(extractor, packagesStream);
        } finally {
            this.close();
        }
    }
}