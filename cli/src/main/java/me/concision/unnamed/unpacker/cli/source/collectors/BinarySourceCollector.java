package me.concision.unnamed.unpacker.cli.source.collectors;

import lombok.extern.java.Log;
import me.concision.unnamed.unpacker.cli.CommandArguments;
import me.concision.unnamed.unpacker.cli.source.SourceCollector;
import me.concision.unnamed.unpacker.cli.source.SourceType;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * See {@link SourceType#BINARY}
 *
 * @author Concision
 */
@Log
public class BinarySourceCollector implements SourceCollector {
    @Override
    public InputStream generate(CommandArguments args) throws IOException {
        if (args.sourcePath == null) {
            log.info("Using standard input for Packages.bin data source");
            // use standard input
            return new BufferedInputStream(System.in);
        } else {
            // use source path
            log.info("Using source path for Packages.bin data source: " + args.sourcePath.getAbsolutePath());
            return new BufferedInputStream(new FileInputStream(args.sourcePath.getAbsoluteFile()));
        }
    }
}
