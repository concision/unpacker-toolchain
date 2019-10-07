package me.concision.warframe.decacher;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.sourceforge.argparse4j.inf.Namespace;

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
    private final Namespace namespace;

    /**
     * Execute decaching with namespaced parameters
     */
    public void execute() {}
}