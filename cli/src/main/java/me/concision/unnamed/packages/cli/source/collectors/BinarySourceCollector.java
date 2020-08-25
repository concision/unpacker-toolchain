package me.concision.unnamed.packages.cli.source.collectors;

import me.concision.unnamed.packages.cli.CommandArguments;
import me.concision.unnamed.packages.cli.source.SourceCollector;
import me.concision.unnamed.packages.cli.source.SourceType;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * See {@link SourceType#BINARY}
 *
 * @author Concision
 */
public class BinarySourceCollector implements SourceCollector {
    @Override
    public InputStream generate(CommandArguments args) throws IOException {
        return new BufferedInputStream(new FileInputStream(args.sourcePath.getAbsoluteFile()));
    }
}