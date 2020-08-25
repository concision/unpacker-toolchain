package me.concision.extractor.output.writers.single;

import lombok.extern.log4j.Log4j2;
import me.concision.extractor.Extractor;
import me.concision.extractor.output.FormatType;
import org.apache.commons.compress.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * See {@link FormatType#BINARY}
 *
 * @author Concision
 */
@Log4j2
public class BinaryFormatWriter extends SingleFormatWriter {
    @Override
    public void format(Extractor extractor, InputStream packagesStream) throws IOException {
        log.info("Saving extracted Packages.bin to file");
        this.open(extractor);

        try {
            IOUtils.copy(packagesStream, outputStream);
            outputStream.flush();
        } finally {
            this.close();
        }
    }
}