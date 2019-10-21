package me.concision.warframe.decacher.format;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.concision.warframe.decacher.destination.OutputMode;

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
    PATHS(OutputMode.SINGLE),
    /**
     * Outputs a matching JSON record on each line
     * (e.g. {"path": "/Lotus/Path/...", "package": ...}\r\n)
     */
    RECORDS(OutputMode.SINGLE),
    /**
     * Outputs all matching packages into a JSON map
     * (e.g. {"/Lotus/Path/...": ..., ...})
     */
    MAP(OutputMode.SINGLE),
    /**
     * Outputs all matching packages into a JSON array
     * (e.g. [{"path": "/Lotus/Path/...", "package": ...}, ...])
     */
    LIST(OutputMode.SINGLE),

    // multiple
    /**
     * Outputs each matching package as a file with replicated directory structure
     * (e.g. ${--output-location}/Lotus/Path/.../PackageName)
     */
    RECURSIVE(OutputMode.MULTIPLE),
    /**
     * Outputs each matching package as a file without replicating directory structure
     * (e.g. ${--output-location}/PackageName)
     */
    FLATTENED(OutputMode.MULTIPLE);

    /**
     * Associated output mode
     */
    @Getter
    private final OutputMode mode;
}