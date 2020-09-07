package me.concision.unnamed.unpacker.cli.output;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.concision.unnamed.unpacker.cli.Unpacker;
import me.concision.unnamed.unpacker.cli.output.writers.multi.FlattenedFormatWriter;
import me.concision.unnamed.unpacker.cli.output.writers.multi.RecursiveFormatWriter;
import me.concision.unnamed.unpacker.cli.output.writers.single.BinaryFormatWriter;
import me.concision.unnamed.unpacker.cli.output.writers.single.ListFormatWriter;
import me.concision.unnamed.unpacker.cli.output.writers.single.MapFormatWriter;
import me.concision.unnamed.unpacker.cli.output.writers.single.PathsFormatWriter;
import me.concision.unnamed.unpacker.cli.output.writers.single.RecordsFormatWriter;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Specifies the output format for writing Packages.bin.
 *
 * @author Concision
 */
@RequiredArgsConstructor
public enum OutputType {
    // single
    /**
     * Outputs Package.bin raw decompressed file
     */
    BINARY(OutputMode.SINGLE, BinaryFormatWriter::new),
    /**
     * Outputs matching package paths on each line
     * (e.g. /Lotus/Path/.../PackageName\r\n)
     */
    PATHS(OutputMode.SINGLE, PathsFormatWriter::new),
    /**
     * Outputs a matching JSON record on each line
     * (e.g. {"path": "/Lotus/Path/...", "package": ...}\r\n)
     */
    RECORDS(OutputMode.SINGLE, RecordsFormatWriter::new),
    /**
     * Outputs all matching packages into a JSON map
     * (e.g. {"/Lotus/Path/...": ..., ...})
     */
    MAP(OutputMode.SINGLE, MapFormatWriter::new),
    /**
     * Outputs all matching packages into a JSON array
     * (e.g. [{"path": "/Lotus/Path/...", "package": ...}, ...])
     */
    LIST(OutputMode.SINGLE, ListFormatWriter::new),

    // multiple
    /**
     * Outputs each matching package as a file with replicated directory structure
     * (e.g. ${--output-location}/Lotus/Path/.../PackageName)
     */
    RECURSIVE(OutputMode.MULTIPLE, RecursiveFormatWriter::new),
    /**
     * Outputs each matching package as a file without replicating directory structure
     * (e.g. ${--output-location}/PackageName)
     */
    FLATTENED(OutputMode.MULTIPLE, FlattenedFormatWriter::new);

    /**
     * {@link OutputMode} type
     */
    @Getter
    @NonNull
    private final OutputMode mode;

    /**
     * Output type instance constructor
     */
    @NonNull
    private final Function<Unpacker, OutputFormatWriter> writerGenerator;

    /**
     * Cross-compatibility with {@link #OutputType(OutputMode, Function)}.
     */
    OutputType(OutputMode mode, Supplier<OutputFormatWriter> supplier) {
        this(mode, (Unpacker unpacker) -> supplier.get());
    }

    /**
     * Instantiates a new {@link OutputFormatWriter} with a {@link Unpacker} instance.
     *
     * @return a new {@link OutputFormatWriter} instance
     */
    public OutputFormatWriter newWriter(@NonNull Unpacker unpacker) {
        return writerGenerator.apply(unpacker);
    }


    /**
     * Specifies how output is directed to destinations during the unpacking process
     */
    public enum OutputMode {
        /**
         * Outputs to a single destination (file/STDOUT)
         */
        SINGLE,
        /**
         * Outputs to a single destination (directory)
         */
        MULTIPLE
    }
}
