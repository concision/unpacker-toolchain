package me.concision.warframe.decacher.output.writers.single;

import java.io.IOException;
import java.io.InputStream;
import lombok.extern.log4j.Log4j2;
import me.concision.warframe.decacher.Decacher;
import me.concision.warframe.decacher.output.FormatType;
import org.apache.commons.compress.utils.IOUtils;

/**
 * See {@link FormatType#BINARY}
 *
 * @author Concision
 * @date 10/23/2019
 */
@Log4j2
public class BinaryFormatWriter extends SingleFormatWriter {
    @Override
    public void format(Decacher decacher, InputStream packagesStream) throws IOException {
        log.info("Saving extracted Packages.bin to file");
        this.open(decacher);

        try {
            IOUtils.copy(packagesStream, outputStream);
            outputStream.flush();
        } finally {
            this.close();
        }
    }
}