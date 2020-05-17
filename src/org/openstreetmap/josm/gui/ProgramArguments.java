// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OptionParser;
import org.openstreetmap.josm.tools.OptionParser.OptionCount;

/**
 * This class holds the arguments passed on to {@link MainApplication#main}.
 * @author Michael Zangl
 * @since 10899
 */
public class ProgramArguments {

    /**
     * JOSM command line options.
     * @see <a href="https://josm.openstreetmap.de/wiki/Help/CommandLineOptions">Help/CommandLineOptions</a>
     */
    public enum Option {
        /** --help|-h                                  Show this help */
        HELP(false),
        /** --version                                  Displays the JOSM version and exits */
        VERSION(false),
        /** --debug                                    Print debugging messages to console */
        DEBUG(false),
        /** --trace                                    Print detailed debugging messages to console */
        TRACE(false),
        /** --language=&lt;language&gt;                Set the language */
        LANGUAGE(true),
        /** --reset-preferences                        Reset the preferences to default */
        RESET_PREFERENCES(false),
        /** --load-preferences=&lt;url-to-xml&gt;      Changes preferences according to the XML file */
        LOAD_PREFERENCES(true),
        /** --set=&lt;key&gt;=&lt;value&gt;            Set preference key to value */
        SET(true),
        /** --geometry=widthxheight(+|-)x(+|-)y        Standard unix geometry argument */
        GEOMETRY(true),
        /** --no-maximize                              Do not launch in maximized mode */
        NO_MAXIMIZE(false),
        /** --maximize                                 Launch in maximized mode */
        MAXIMIZE(false),
        /** --download=minlat,minlon,maxlat,maxlon     Download the bounding box <br>
         *  --download=&lt;URL&gt;                     Download the location at the URL (with lat=x&amp;lon=y&amp;zoom=z) <br>
         *  --download=&lt;filename&gt;                Open a file (any file type that can be opened with File/Open) */
        DOWNLOAD(true),
        /** --downloadgps=minlat,minlon,maxlat,maxlon  Download the bounding box as raw GPS <br>
         *  --downloadgps=&lt;URL&gt;                  Download the location at the URL (with lat=x&amp;lon=y&amp;zoom=z) as raw GPS */
        DOWNLOADGPS(true),
        /** --selection=&lt;searchstring&gt;           Select with the given search */
        SELECTION(true),
        /** --offline=&lt;OSM_API|JOSM_WEBSITE|CACHE_UPDATES|CERTIFICATES|ALL&gt; Disable access to the given resource(s), delimited by comma */
        OFFLINE(true),
        /** --skip-plugins */
        SKIP_PLUGINS(false);

        private final String name;
        private final boolean requiresArg;

        Option(boolean requiresArgument) {
            this.name = name().toLowerCase(Locale.ENGLISH).replace('_', '-');
            this.requiresArg = requiresArgument;
        }

        /**
         * Replies the option name
         * @return The option name, in lowercase
         */
        public String getName() {
            return name;
        }

        /**
         * Determines if this option requires an argument.
         * @return {@code true} if this option requires an argument, {@code false} otherwise
         */
        public boolean requiresArgument() {
            return requiresArg;
        }
    }

    private final Map<Option, List<String>> argMap = new EnumMap<>(Option.class);

    /**
     * Construct the program arguments object
     * @param args The args passed to main.
     * @since 10936
     */
    public ProgramArguments(String... args) {
        Stream.of(Option.values()).forEach(o -> argMap.put(o, new ArrayList<>()));
        buildCommandLineArgumentMap(args);
    }

    /**
     * Builds the command-line argument map.
     * @param args command-line arguments array
     */
    private void buildCommandLineArgumentMap(String... args) {
        OptionParser parser = new OptionParser("JOSM");
        for (Option o : Option.values()) {
            if (o.requiresArgument()) {
                parser.addArgumentParameter(o.getName(), OptionCount.MULTIPLE, p -> addOption(o, p));
            } else {
                parser.addFlagParameter(o.getName(), () -> addOption(o, ""));
            }
        }

        parser.addShortAlias(Option.HELP.getName(), "h");
        parser.addShortAlias(Option.VERSION.getName(), "v");

        List<String> remaining = parser.parseOptionsOrExit(Arrays.asList(args));

        // positional arguments are a shortcut for the --download ... option
        for (String arg : remaining) {
            addOption(Option.DOWNLOAD, arg);
        }
    }

    private void addOption(Option opt, String optarg) {
        argMap.get(opt).add(optarg);
    }

    /**
     * Gets a single argument (the first) that was given for the given option.
     * @param option The option to search
     * @return The argument as optional value.
     */
    public Optional<String> getSingle(Option option) {
        return get(option).stream().findFirst();
    }

    /**
     * Gets all values that are given for a given option
     * @param option The option
     * @return The values that were given. May be empty.
     */
    public Collection<String> get(Option option) {
        return Collections.unmodifiableList(argMap.get(option));
    }

    /**
     * Test if a given option was used by the user.
     * @param option The option to test for
     * @return <code>true</code> if the user used it.
     */
    public boolean hasOption(Option option) {
        return !get(option).isEmpty();
    }

    /**
     * Helper method to indicate if version should be displayed.
     * @return <code>true</code> to display version
     */
    public boolean showVersion() {
        return hasOption(Option.VERSION);
    }

    /**
     * Helper method to indicate if help should be displayed.
     * @return <code>true</code> to display version
     */
    public boolean showHelp() {
        return !get(Option.HELP).isEmpty();
    }

    /**
     * Get the log level the user wants us to use.
     * @return The log level.
     */
    public Level getLogLevel() {
        if (hasOption(Option.TRACE)) {
            return Logging.LEVEL_TRACE;
        } else if (hasOption(Option.DEBUG)) {
            return Logging.LEVEL_DEBUG;
        } else {
            return Logging.LEVEL_INFO;
        }
    }

    /**
     * Gets a map of all preferences the user wants to set.
     * @return The preferences to set. It contains null values for preferences to unset
     */
    public Map<String, String> getPreferencesToSet() {
        HashMap<String, String> map = new HashMap<>();
        get(Option.SET).stream().map(i -> i.split("=", 2)).forEach(kv -> map.put(kv[0], getValue(kv)));
        return map;
    }

    private static String getValue(String... kv) {
        if (kv.length < 2) {
            return "";
        } else if ("null".equals(kv[1])) {
            return null;
        } else {
            return kv[1];
        }
    }
}
