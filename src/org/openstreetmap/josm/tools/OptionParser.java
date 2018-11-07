// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A replacement of getopt.
 * <p>
 * Allows parsing command line options
 *
 * @author Michael Zangl
 * @since 14415
 */
public class OptionParser {

    private HashMap<String, AvailableOption> availableOptions = new HashMap<>();
    private final String program;

    /**
     * Create a new option parser.
     * @param program The program name.
     */
    public OptionParser(String program) {
        Objects.requireNonNull(program, "program name must be provided");
        this.program = program;
    }

    /**
     * Adds an alias for the long option --optionName to the short version -name
     * @param optionName The long option
     * @param shortName The short version
      * @return this {@link OptionParser}
    */
    public OptionParser addShortAlias(String optionName, String shortName) {
        if (!shortName.matches("\\w")) {
            throw new IllegalArgumentException("Short name " + shortName + " must be one character");
        }
        if (availableOptions.containsKey("-" + shortName)) {
            throw new IllegalArgumentException("Short name " + shortName + " is already used");
        }
        AvailableOption longDefinition = availableOptions.get("--" + optionName);
        if (longDefinition == null) {
            throw new IllegalArgumentException("No long definition for " + optionName
                    + " was defined. Define the long definition first before creating " + "a short definition for it.");
        }
        availableOptions.put("-" + shortName, longDefinition);
        return this;
    }

    /**
     * Adds an option that may be used as a flag, e.g. --debug
     * @param optionName The parameter name
     * @param handler The handler that is called when the flag is encountered.
     * @return this {@link OptionParser}
     */
    public OptionParser addFlagParameter(String optionName, Runnable handler) {
        checkOptionName(optionName);
        availableOptions.put("--" + optionName, new AvailableOption() {
            @Override
            public void runFor(String parameter) {
                handler.run();
            }
        });
        return this;
    }

    private void checkOptionName(String optionName) {
        if (!optionName.matches("\\w([\\w-]*\\w)?")) {
            throw new IllegalArgumentException("Illegal option name: " + optionName);
        }
        if (availableOptions.containsKey("--" + optionName)) {
            throw new IllegalArgumentException("The option --" + optionName + " is already registered");
        }
    }

    /**
     * Add a parameter that expects a string attribute. E.g.: --config=/path/to/file
     * @param optionName The name of the parameter.
     * @param count The number of times the parameter may occur.
     * @param handler A function that gets the current object and the parameter.
     *                It should throw an {@link OptionParseException} if the parameter cannot be handled / is invalid.
     * @return this {@link OptionParser}
     */
    public OptionParser addArgumentParameter(String optionName, OptionCount count, Consumer<String> handler) {
        checkOptionName(optionName);
        availableOptions.put("--" + optionName, new AvailableOption() {
            @Override
            public boolean requiresParameter() {
                return true;
            }

            @Override
            public OptionCount getRequiredCount() {
                return count;
            }

            @Override
            public void runFor(String parameter) {
                Objects.requireNonNull(parameter, "parameter");
                handler.accept(parameter);
            }
        });
        return this;
    }

    /**
     * Same as {@link #parseOptions(List)}, but exits if option parsing fails.
     * @param arguments The options
     * @return The remaining program arguments that are no options.
     */
    public List<String> parseOptionsOrExit(List<String> arguments) {
        try {
            return parseOptions(arguments);
        } catch (OptionParseException e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
            // unreachable, but makes compilers happy
            throw e;
        }
    }

    /**
     * Parses the options.
     * <p>
     * It first checks if all required options are present, if all options are known and validates the option count.
     * <p>
     * Then, all option handlers are called in the order in which the options are encountered.
     * @param arguments Program arguments
     * @return The remaining program arguments that are no options.
     * @throws OptionParseException The error to display if option parsing failed.
     */
    public List<String> parseOptions(List<String> arguments) {
        LinkedList<String> toHandle = new LinkedList<>(arguments);
        List<String> remainingArguments = new LinkedList<>();
        boolean argumentOnlyMode = false;
        List<FoundOption> options = new LinkedList<>();

        while (!toHandle.isEmpty()) {
            String next = toHandle.removeFirst();
            if (argumentOnlyMode || !next.matches("-.+")) {
                // argument found, add it to arguments list
                remainingArguments.add(next);
            } else if ("--".equals(next)) {
                // we are done, the remaining should be used as arguments.
                argumentOnlyMode = true;
            } else {
                if (next.matches("-\\w\\w+")) {
                    // special case: concatenated short options like -hv
                    // We handle them as if the user wrote -h -v by just scheduling the remainder for the next loop.
                    toHandle.addFirst("-" + next.substring(2));
                    next = next.substring(0, 2);
                }

                String[] split = next.split("=", 2);
                String optionName = split[0];
                AvailableOption definition = findParameter(optionName);
                String parameter = null;
                if (definition.requiresParameter()) {
                    if (split.length > 1) {
                        parameter = split[1];
                    } else {
                        if (toHandle.isEmpty() || toHandle.getFirst().equals("--")) {
                            throw new OptionParseException(tr("{0}: option ''{1}'' requires an argument", program));
                        }
                        parameter = toHandle.removeFirst();
                    }
                } else if (split.length > 1) {
                    throw new OptionParseException(
                            tr("{0}: option ''{1}'' does not allow an argument", program, optionName));
                }
                options.add(new FoundOption(optionName, definition, parameter));
            }
        }

        // Count how often they are used
        availableOptions.values().stream().distinct().forEach(def -> {
            long count = options.stream().filter(p -> def.equals(p.option)).count();
            if (count < def.getRequiredCount().min) {
                // min may be 0 or 1 at the moment
                throw new OptionParseException(tr("{0}: option ''{1}'' is required"));
            } else if (count > def.getRequiredCount().max) {
                // max may be 1 or MAX_INT at the moment
                throw new OptionParseException(tr("{0}: option ''{1}'' may not appear multiple times"));
            }
        });

        // Actually apply the parameters.
        for (FoundOption option : options) {
            try {
                option.option.runFor(option.parameter);
            } catch (OptionParseException e) {
                String message;
                // Just add a nicer error message
                if (option.parameter == null) {
                    message = tr("{0}: Error while handling option ''{1}''", program, option.optionName);
                } else {
                    message = tr("{0}: Invalid value {2} for option ''{1}''", program, option.optionName,
                            option.parameter);
                }
                if (!e.getLocalizedMessage().isEmpty()) {
                    message += ": " + e.getLocalizedMessage().isEmpty();
                }
                throw new OptionParseException(message);
            }
        }
        return remainingArguments;
    }

    private AvailableOption findParameter(String optionName) {
        AvailableOption exactMatch = availableOptions.get(optionName);
        if (exactMatch != null) {
            return exactMatch;
        } else if (optionName.startsWith("--")) {
            List<AvailableOption> alternatives = availableOptions.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(optionName)).map(Entry::getValue).distinct()
                    .collect(Collectors.toList());

            if (alternatives.size() == 1) {
                return alternatives.get(0);
            } else if (alternatives.size() > 1) {
                throw new OptionParseException(tr("{0}: option ''{1}'' is ambiguous", program));
            }
        }
        throw new OptionParseException(tr("{0}: unrecognized option ''{1}''", program, optionName));
    }

    /**
     * How often an option may / must be specified on the command line.
     * @author Michael Zangl
     */
    public enum OptionCount {
        /**
         * The option may be specified once
         */
        OPTIONAL(0, 1),
        /**
         * The option is required exactly once
         */
        REQUIRED(1, 1),
        /**
         * The option may be specified multiple times
         */
        MULTIPLE(0, Integer.MAX_VALUE);

        private int min;
        private int max;

        OptionCount(int min, int max) {
            this.min = min;
            this.max = max;

        }
    }

    protected abstract static class AvailableOption {

        public boolean requiresParameter() {
            return false;
        }

        public OptionCount getRequiredCount() {
            return OptionCount.OPTIONAL;
        }

        /**
         * Called once if the parameter is encountered, afer basic validation.
         * @param parameter The parameter if {@link #requiresParameter()} is true, <code>null</code> otherwise.
         */
        public abstract void runFor(String parameter);

    }

    private static class FoundOption {
        private final String optionName;
        private final AvailableOption option;
        private final String parameter;

        FoundOption(String optionName, AvailableOption option, String parameter) {
            this.optionName = optionName;
            this.option = option;
            this.parameter = parameter;
        }
    }

    /**
     * @author Michael Zangl
     */
    public static class OptionParseException extends RuntimeException {
        // Don't rely on JAVA handling this correctly.
        private final String localizedMessage;

        /**
         * Create an empty error with no description
         */
        public OptionParseException() {
            super();
            localizedMessage = "";
        }

        /**
         * @param localizedMessage The message to display to the user.
         */
        public OptionParseException(String localizedMessage) {
            super(localizedMessage);
            this.localizedMessage = localizedMessage;
        }

        /**
         * @param localizedMessage The message to display to the user.
         * @param t The error that caused this message to be displayed.
         */
        public OptionParseException(String localizedMessage, Throwable t) {
            super(localizedMessage, t);
            this.localizedMessage = localizedMessage;
        }

        @Override
        public String getLocalizedMessage() {
            return localizedMessage;
        }
    }
}
