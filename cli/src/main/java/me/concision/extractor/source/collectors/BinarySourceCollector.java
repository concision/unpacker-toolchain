package me.concision.extractor.source.collectors;

import me.concision.extractor.CommandArguments;
import me.concision.extractor.source.SourceCollector;
import me.concision.extractor.source.SourceType;

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