package me.concision.extractor;

import java.io.InputStream;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import me.concision.extractor.output.OutputFormatWriter;

/**
 * Control flow for extraction process
 *
 * @author Concision
*/
@Log4j2
@RequiredArgsConstructor
public class Extractor {
    /**
     * Processed and validated command-line arguments
     */
    @NonNull
    @Getter
    private final CommandArguments args;

    /**
     * Execute decaching with namespaced parameters
     */
    @SneakyThrows
    public void execute() {
        // prepare environment
        this.prepare();
        // decache with environment
        this.decache();
    }

    /**
     * Prepares decaching environment
     */
    private void prepare() {
        System.setProperty("line.separator", "\r\n");
    }

    /**
     * Obtains Packages.bin input stream from source and writes it to destination
     */
    private void decache() {
        log.info("Generating Packages.bin stream");
        // generate packages.bin input stream; thrown away after block has finished execution
        InputStream packagesStream;
        try {
            packagesStream = args.source.generate(args);

            if (packagesStream == null) {
                throw new NullPointerException("source type " + args.source + " generated a null stream");
            }
        } catch (Throwable throwable) {
            throw new RuntimeException("failed to generate Packages.bin input stream", throwable);
        }

        // generate new writer
        log.info("Constructing format writer");
        OutputFormatWriter formatWriter = args.outputFormat.newWriter();

        log.info("Executing format writer");
        try (InputStream __ = packagesStream) {
            formatWriter.format(this, packagesStream);
        } catch (Throwable throwable) {
            throw new RuntimeException("formatter " + args.outputFormat + " failed to write", throwable);
        }
        log.info("Completed");
    }
}