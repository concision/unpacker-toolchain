package me.concision.warframe.decacher.destination;

/**
 * Specifies how output is directed to destinations during the decaching process
 *
 * @author Concision
 * @date 10/14/2019
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