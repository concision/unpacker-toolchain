package me.concision.unnamed.unpacker.cli;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import me.concision.unnamed.unpacker.cli.output.OutputFormatWriter;

import java.io.InputStream;

/**
 * High level control flow for executing the unpacking process.
 *
 * @author Concision
 */
@Log
@RequiredArgsConstructor
public class Unpacker {
    /**
     * Processed and validated command-line arguments
     */
    @NonNull
    @Getter
    private final CommandArguments args;

    /**
     * Initiate unpacking process with the specified arguments
     */
    public void execute() {
        // prepare environment
        this.prepare();
        // cache with environment
        this.decache();
    }

    /**
     * Prepares decaching environment
     */
    private void prepare() {
        // sets Windows compatible line endings
        System.setProperty("line.separator", "\r\n");
    }

    /**
     * Obtains Packages.bin input stream from source and writes it to output destination
     */
    private void decache() {
        log.info("Generating Packages.bin source stream");
        // generate packages.bin input stream
        InputStream packagesStream;
        try {
            packagesStream = args.sourceType.generate(args);

            if (packagesStream == null) {
                throw new NullPointerException("source type " + args.sourceType + " generated a null stream");
            }
        } catch (Throwable throwable) {
            throw new RuntimeException("failed to generate Packages.bin input stream", throwable);
        }

        // instantiate output writer
        log.info("Initializing output writer");
        OutputFormatWriter writer = args.outputFormat.newWriter(this);
        // publish stream to output writer
        log.info("Processing output writing");
        try (InputStream __ = packagesStream) {
            writer.write(this, packagesStream);
        } catch (Throwable throwable) {
            throw new RuntimeException("formatter " + args.outputFormat + " failed to write", throwable);
        }
        log.info("Completed unpacking. Goodbye.");
    }
}
