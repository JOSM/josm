// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.cli;

/**
 * A command line interface module.
 * <p>
 * The user can provide an action keyword as first argument. This will invoke the
 * corresponding {@code CLIModule}, which has its own set of options and will do
 * a specific job.
 * @since 12793
 */
public interface CLIModule {

    /**
     * Get the action keyword that the user needs to provide as first command
     * line argument to invoke this module.
     * @return the action keyword of this module
     */
    String getActionKeyword();

    /**
     * Process the remaining command line arguments and run any of the requested actions.
     * @param argArray command line arguments without the initial action keyword
     */
    void processArguments(String[] argArray);
}
