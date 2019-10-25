package me.concision.warframe.decacher.source.collectors;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import me.concision.warframe.decacher.CommandArguments;
import me.concision.warframe.decacher.source.SourceCollector;
import me.concision.warframe.decacher.source.SourceType;

/**
 * See {@link SourceType#BINARY}
 *
 * @author Concision
 * @date 10/21/2019
 */
public class BinarySourceCollector implements SourceCollector {
    @Override
    public InputStream generate(CommandArguments args) throws IOException {
        return new BufferedInputStream(new FileInputStream(args.sourcePath.getAbsoluteFile()));
    }
}