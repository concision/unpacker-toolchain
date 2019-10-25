package me.concision.warframe.decacher.output.writers.single;

import java.io.InputStream;
import me.concision.warframe.decacher.Decacher;
import me.concision.warframe.decacher.output.RecordFormatWriter;

/**
 * Supports single destination output writing from {@link SingleFormatWriter} and record publishing from {@link RecordFormatWriter}
 *
 * @author Concision
 * @date 10/24/2019
 */
public abstract class SingleRecordFormatWriter extends SingleFormatWriter implements RecordFormatWriter {
    @Override
    public void format(Decacher decacher, InputStream packagesStream) {
        this.open(decacher);

        try {
            RecordFormatWriter.super.format(decacher, packagesStream);
        } finally {
            this.close();
        }
    }
}