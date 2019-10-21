package me.concision.warframe.decacher.output;

import java.util.function.Supplier;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.concision.warframe.decacher.CommandArguments;

/**
 * Specifies the output format type for a specified {@link OutputMode}
 *
 * @author Concision
 * @date 10/9/2019
 */
@RequiredArgsConstructor
public enum FormatType {
    // single
    /**
     * Outputs matching package paths on each line
     * (e.g. /Lotus/Path/.../PackageName\r\n)
     */
    PATHS(OutputMode.SINGLE, () -> {throw new UnsupportedOperationException();}),
    /**
     * Outputs a matching JSON record on each line
     * (e.g. {"path": "/Lotus/Path/...", "package": ...}\r\n)
     */
    RECORDS(OutputMode.SINGLE, () -> {throw new UnsupportedOperationException();}),
    /**
     * Outputs all matching packages into a JSON map
     * (e.g. {"/Lotus/Path/...": ..., ...})
     */
    MAP(OutputMode.SINGLE, () -> {throw new UnsupportedOperationException();}),
    /**
     * Outputs all matching packages into a JSON array
     * (e.g. [{"path": "/Lotus/Path/...", "package": ...}, ...])
     */
    LIST(OutputMode.SINGLE, () -> {throw new UnsupportedOperationException();}),

    // multiple
    /**
     * Outputs each matching package as a file with replicated directory structure
     * (e.g. ${--output-location}/Lotus/Path/.../PackageName)
     */
    RECURSIVE(OutputMode.MULTIPLE, () -> {throw new UnsupportedOperationException();}),
    /**
     * Outputs each matching package as a file without replicating directory structure
     * (e.g. ${--output-location}/PackageName)
     */
    FLATTENED(OutputMode.MULTIPLE, () -> {throw new UnsupportedOperationException();});

    /**
     * Associated output mode
     */
    @Getter
    @NonNull
    private final OutputMode mode;

    /**
     * Type-specific format writer instance generator
     */
    @NonNull
    private final Supplier<OutputFormatWriter> writerSupplier;

    /**
     * Constructs a {@link OutputFormatWriter} with the passed parameters
     *
     * @param arguments {@link CommandArguments}
     * @return parameterized {@link OutputFormatWriter} instance
     */
    public OutputFormatWriter newWriter(@NonNull CommandArguments arguments) {
        OutputFormatWriter writer = writerSupplier.get();
        writer.setContext(arguments);
        return writer;
    }
}