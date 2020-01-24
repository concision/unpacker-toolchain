package me.concision.extractor.output.writers.single;

import java.io.InputStream;

import me.concision.extractor.Extractor;
import me.concision.extractor.output.RecordFormatWriter;

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