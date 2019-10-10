package me.concision.warframe.decacher;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Control flow for extraction process
 *
 * @author Concision
 * @date 10/7/2019
 */
@RequiredArgsConstructor
public class Decacher {
    /**
     * Processed and validated command-line arguments
     */
    @NonNull
    @Getter
    private final CommandArguments args;

    /**
     * Execute decaching with namespaced parameters
     */
    public void execute() {}
}