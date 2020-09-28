package me.concision.unnamed.unpacker.cli.source.collectors;

import lombok.NonNull;
import lombok.extern.java.Log;
import me.concision.unnamed.unpacker.cli.CommandArguments;
import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.source.SourceCollector;
import me.concision.unnamed.unpacker.cli.source.SourceType;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * See {@link SourceType#BINARY}.
 *
 * @author Concision
 */
@Log
public class BinarySourceCollector implements SourceCollector {
    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream acquire(@NonNull Unpacker unpacker) throws IOException {
        CommandArguments args = unpacker.args();

        if (args.sourcePath == null) {
            // use standard input
            log.info("Using standard input for Packages.bin data source");
            return new BufferedInputStream(System.in);
        } else {
            // use source path
            log.info("Using source path for Packages.bin data source: " + args.sourcePath.getAbsolutePath());
            return new BufferedInputStream(new FileInputStream(args.sourcePath.getAbsoluteFile()));
        }
    }
}
