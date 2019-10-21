package me.concision.warframe.decacher.output;

import lombok.NonNull;
import me.concision.warframe.decacher.CommandArguments;
import me.concision.warframe.decacher.api.PackageParser.PackageRecord;

/**
 * Formats package records and writes to output destination.
 *
 * @author Concision
 * @date 10/21/2019
 */
public interface OutputFormatWriter {
    /**
     * Sets format writer context, {@link #complete()} should be executed to conclude writing.
     *
     * @param arguments {@link CommandArguments} parameters
     */
    void setContext(CommandArguments arguments);

    /**
     * Publishes a package record to the format writer
     *
     * @param record {@link PackageRecord} instance
     */
    void publish(@NonNull PackageRecord record);

    /**
     * Concludes output writing; releases resources
     */
    void complete();
}