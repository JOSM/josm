// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.FileNameUtils;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.cli.CLIModule;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.data.preferences.JosmUrls;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.CustomConfigurator;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.gui.progress.CLIProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.GeoJSONMapRouletteWriter;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmChangeReader;
import org.openstreetmap.josm.spi.lifecycle.Lifecycle;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.IPreferences;
import org.openstreetmap.josm.spi.preferences.MemoryPreferences;
import org.openstreetmap.josm.tools.Http1Client;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OptionParser;
import org.openstreetmap.josm.tools.Stopwatch;
import org.openstreetmap.josm.tools.Territories;
import org.openstreetmap.josm.tools.Utils;

/**
 * Add a validate command to the JOSM command line interface.
 * @author Taylor Smock
 * @since 18365
 */
public class ValidatorCLI implements CLIModule {
    /**
     * The unique instance.
     */
    public static final ValidatorCLI INSTANCE = new ValidatorCLI();

    /** The input file(s) */
    private final List<String> input = new ArrayList<>();
    /** The change files. input file -> list of change files */
    private final Map<String, List<String>> changeFiles = new HashMap<>();
    /** The output file(s). If {@code null}, use input filename as base (replace extension with geojson). input -> output */
    private final Map<String, String> output = new HashMap<>();

    private static final Supplier<ProgressMonitor> progressMonitorFactory = CLIProgressMonitor::new;

    /** The log level */
    private Level logLevel;

    private enum Option {
        /** --help                                    Show the help for validate */
        HELP(false, 'h'),
        /** --input=&lt;input-file&gt;                Set the current input file */
        INPUT(true, 'i', OptionParser.OptionCount.MULTIPLE),
        /** --output=&lt;output-file&gt;              Set the output file for the current input file */
        OUTPUT(true, 'o', OptionParser.OptionCount.MULTIPLE),
        /** --change-file=&lt;change-file&gt;         Add a change file */
        CHANGE_FILE(true, 'c', OptionParser.OptionCount.MULTIPLE),
        /** --debug                                   Set logging level to debug */
        DEBUG(false, '*'),
        /** --trace                                   Set logging level to trace */
        TRACE(false, '*'),
        /** --language=&lt;language&gt;                Set the language */
        LANGUAGE(true, 'l'),
        /** --load-preferences=&lt;url-to-xml&gt;      Changes preferences according to the XML file */
        LOAD_PREFERENCES(true, 'p'),
        /** --set=&lt;key&gt;=&lt;value&gt;            Set preference key to value */
        SET(true, 's');

        private final String name;
        private final boolean requiresArgument;
        private final char shortOption;
        private final OptionParser.OptionCount optionCount;
        Option(final boolean requiresArgument, final char shortOption) {
            this(requiresArgument, shortOption, OptionParser.OptionCount.OPTIONAL);
        }

        Option(final boolean requiresArgument, final char shortOption, final OptionParser.OptionCount optionCount) {
            this.name = name().toLowerCase(Locale.ROOT).replace('_', '-');
            this.requiresArgument = requiresArgument;
            this.shortOption = shortOption;
            this.optionCount = optionCount;
        }

        /**
         * Replies the option name
         * @return The option name, in lowercase
         */
        public String getName() {
            return this.name;
        }

        /**
         * Get the number of times this option should be seen
         * @return The option count
         */
        public OptionParser.OptionCount getOptionCount() {
            return this.optionCount;
        }

        /**
         * Replies the short option (single letter) associated with this option.
         * @return the short option or '*' if there is no short option
         */
        public char getShortOption() {
            return this.shortOption;
        }

        /**
         * Determines if this option requires an argument.
         * @return {@code true} if this option requires an argument, {@code false} otherwise
         */
        public boolean requiresArgument() {
            return this.requiresArgument;
        }

    }

    @Override
    public String getActionKeyword() {
        return "validate";
    }

    @Override
    public void processArguments(final String[] argArray) {
        try {
            // Ensure that preferences are only in memory
            Config.setPreferencesInstance(new MemoryPreferences());
            Logging.setLogLevel(Level.INFO);
            this.parseArguments(argArray);
            if (this.input.isEmpty()) {
                throw new IllegalArgumentException(tr("Missing argument - input data file ({0})", "--input|-i"));
            }
            this.initialize();
            final ProgressMonitor fileMonitor = progressMonitorFactory.get();
            fileMonitor.beginTask(tr("Processing files..."), this.input.size());
            for (String inputFile : this.input) {
                if (inputFile.endsWith(".validator.mapcss")) {
                    this.processValidatorFile(inputFile);
                } else if (inputFile.endsWith(".mapcss")) {
                    this.processMapcssFile(inputFile);
                } else {
                    this.processFile(inputFile);
                }
                fileMonitor.worked(1);
            }
            fileMonitor.finishTask();
        } catch (Exception e) {
            Logging.info(e);
            Lifecycle.exitJosm(true, 1);
        }
        Lifecycle.exitJosm(true, 0);
    }

    /**
     * Process a standard mapcss file
     * @param inputFile The mapcss file to validate
     * @throws ParseException if the file does not match the mapcss syntax
     */
    private void processMapcssFile(final String inputFile) throws ParseException {
        final MapCSSStyleSource styleSource = new MapCSSStyleSource(new File(inputFile).toURI().getPath(), inputFile, inputFile);
        styleSource.loadStyleSource();
        if (!styleSource.getErrors().isEmpty()) {
            throw new ParseException(trn("{0} had {1} error", "{0} had {1} errors", styleSource.getErrors().size(),
                    inputFile, styleSource.getErrors().size()));
        } else {
            Logging.info(tr("{0} had no errors", inputFile));
        }
    }

    /**
     * Process a validator file
     * @param inputFile The file to check
     * @throws IOException if there is a problem reading the file
     * @throws ParseException if the file does not match the validator mapcss syntax
     */
    private void processValidatorFile(final String inputFile) throws ParseException, IOException {
        // Check asserts
        Config.getPref().putBoolean("validator.check_assert_local_rules", true);
        final MapCSSTagChecker mapCSSTagChecker = new MapCSSTagChecker();
        final Collection<String> assertionErrors = new ArrayList<>();
        final MapCSSTagChecker.ParseResult result = mapCSSTagChecker.addMapCSS(new File(inputFile).toURI().getPath(),
                assertionErrors::add);
        if (!result.parseErrors.isEmpty() || !assertionErrors.isEmpty()) {
            for (Throwable throwable : result.parseErrors) {
                Logging.error(throwable);
            }
            for (String error : assertionErrors) {
                Logging.error(error);
            }
            throw new ParseException(trn("{0} had {1} error", "{0} had {1} errors", result.parseErrors.size() + assertionErrors.size(),
                    inputFile, result.parseErrors.size() + assertionErrors.size()));
        } else {
            Logging.info(tr("{0} had no errors"), inputFile);
        }
    }

    /**
     * Process an OSM file
     * @param inputFile The input filename
     * @throws IllegalArgumentException If an argument is not valid
     * @throws IllegalDataException If there is bad data
     * @throws IOException If a file could not be read or written
     */
    private void processFile(final String inputFile) throws IllegalDataException, IOException {
        final File inputFileFile = new File(inputFile);
        final List<FileImporter> inputFileImporters = ExtensionFileFilter.getImporters().stream()
                .filter(importer -> importer.acceptFile(inputFileFile)).collect(Collectors.toList());
        final Stopwatch stopwatch = Stopwatch.createStarted();
        if (inputFileImporters.stream().noneMatch(fileImporter ->
                fileImporter.importDataHandleExceptions(inputFileFile, progressMonitorFactory.get()))) {
            throw new IOException(tr("Could not load input file: {0}", inputFile));
        }
        final String outputFile = Optional.ofNullable(this.output.get(inputFile)).orElseGet(() -> getDefaultOutputName(inputFile));
        final String task = tr("Validating {0}, saving output to {1}", inputFile, outputFile);
        OsmDataLayer dataLayer = null;
        try {
            Logging.info(task);
            OsmValidator.initializeTests();
            dataLayer = MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class)
                    .stream().filter(layer -> inputFileFile.equals(layer.getAssociatedFile()))
                    .findFirst().orElseThrow(() -> new JosmRuntimeException(tr("Could not find a layer for {0}", inputFile)));
            final DataSet dataSet = dataLayer.getDataSet();
            if (this.changeFiles.containsKey(inputFile)) {
                ProgressMonitor changeFilesMonitor = progressMonitorFactory.get();
                for (String changeFile : this.changeFiles.getOrDefault(inputFile, Collections.emptyList())) {
                    try (InputStream changeStream = Compression.getUncompressedFileInputStream(Paths.get(changeFile))) {
                        dataSet.mergeFrom(OsmChangeReader.parseDataSet(changeStream, changeFilesMonitor));
                    }
                }
            }
            Collection<Test> tests = OsmValidator.getEnabledTests(false);
            if (Files.isRegularFile(Paths.get(outputFile)) && !Files.deleteIfExists(Paths.get(outputFile))) {
                Logging.error("Could not delete {0}, attempting to append", outputFile);
            }
            GeoJSONMapRouletteWriter geoJSONMapRouletteWriter = new GeoJSONMapRouletteWriter(dataSet);
            try (OutputStream fileOutputStream = Files.newOutputStream(Paths.get(outputFile))) {
                tests.parallelStream().forEach(test -> runTest(test, geoJSONMapRouletteWriter, fileOutputStream, dataSet));
            }
        } finally {
            if (dataLayer != null) {
                MainApplication.getLayerManager().removeLayer(dataLayer);
            }
            Logging.info(stopwatch.toString(task));
        }
    }

    /**
     * Get the default output name
     * @param inputString The input file
     * @return The default output name for the input file (extension stripped, ".geojson" added)
     */
    private static String getDefaultOutputName(final String inputString) {
        final String extension = FileNameUtils.getExtension(inputString);
        if (!Arrays.asList("zip", "bz", "xz", "geojson").contains(extension)) {
            return FileNameUtils.getBaseName(inputString) + ".geojson";
        } else if ("geojson".equals(extension)) {
            // Account for geojson input files
            return FileNameUtils.getBaseName(inputString) + ".validated.geojson";
        }
        return FileNameUtils.getBaseName(FileNameUtils.getBaseName(inputString)) + ".geojson";
    }

    /**
     * Run a test
     * @param test The test to run
     * @param geoJSONMapRouletteWriter The object to use to create challenges
     * @param fileOutputStream The location to write data to
     * @param dataSet The dataset to check
     */
    private void runTest(final Test test, final GeoJSONMapRouletteWriter geoJSONMapRouletteWriter,
            final OutputStream fileOutputStream, DataSet dataSet) {
        test.startTest(progressMonitorFactory.get());
        test.visit(dataSet.allPrimitives());
        test.endTest();
        test.getErrors().stream().map(geoJSONMapRouletteWriter::write)
                .filter(Optional::isPresent).map(Optional::get)
                .map(jsonObject -> jsonObject.toString().getBytes(StandardCharsets.UTF_8)).forEach(bytes -> {
                    try {
                        writeToFile(fileOutputStream, bytes);
                    } catch (IOException e) {
                        throw new JosmRuntimeException(e);
                    }
                });
        test.clear();
    }

    /**
     * Write to a file. Synchronized to avoid writing to the same file in different threads.
     *
     * @param fileOutputStream The file output stream to read
     * @param bytes The bytes to write (surrounded by RS and LF)
     * @throws IOException If we couldn't write to file
     */
    private synchronized void writeToFile(final OutputStream fileOutputStream, final byte[] bytes)
            throws IOException {
        // Write the ASCII Record Separator character
        fileOutputStream.write(0x1e);
        fileOutputStream.write(bytes);
        // Write the ASCII Line Feed character
        fileOutputStream.write(0x0a);
    }

    /**
     * Initialize everything that might be needed
     *
     * Arguments may need to be parsed first.
     */
    void initialize() {
        Logging.setLogLevel(this.logLevel);
        HttpClient.setFactory(Http1Client::new);
        Config.setBaseDirectoriesProvider(JosmBaseDirectories.getInstance()); // for right-left-hand traffic cache file
        Config.setUrlsProvider(JosmUrls.getInstance());
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("epsg:3857".toUpperCase(Locale.ROOT)));

        Territories.initializeInternalData();
        OsmValidator.initialize();
        MapPaintStyles.readFromPreferences();
    }

    /**
     * Parse command line arguments and do some low-level error checking.
     * @param argArray the arguments array
     */
    void parseArguments(String[] argArray) {
        Logging.setLogLevel(Level.INFO);

        OptionParser parser = new OptionParser("JOSM validate");
        final AtomicReference<String> currentInput = new AtomicReference<>(null);
        for (Option o : Option.values()) {
            if (o.requiresArgument()) {
                parser.addArgumentParameter(o.getName(),
                        o.getOptionCount(),
                        arg -> handleOption(currentInput.get(), o, arg).ifPresent(currentInput::set));
            } else {
                parser.addFlagParameter(o.getName(), () -> handleOption(o));
            }
            if (o.getShortOption() != '*') {
                parser.addShortAlias(o.getName(), Character.toString(o.getShortOption()));
            }
        }
        parser.parseOptionsOrExit(Arrays.asList(argArray));
    }

    private void handleOption(final Option option) {
        switch (option) {
        case HELP:
            showHelp();
            Lifecycle.exitJosm(true, 0);
            break;
        case DEBUG:
            this.logLevel = Logging.LEVEL_DEBUG;
            break;
        case TRACE:
            this.logLevel = Logging.LEVEL_TRACE;
            break;
        default:
            throw new AssertionError("Unexpected option: " + option);
        }
    }

    /**
     * Handle an option
     * @param currentInput The current input file, if any. May be {@code null}.
     * @param option The option to parse
     * @param argument The argument for the option
     * @return The new input file, if any.
     */
    private Optional<String> handleOption(final String currentInput, final Option option, final String argument) {
        switch (option) {
        case INPUT:
            this.input.add(argument);
            return Optional.of(argument);
        case OUTPUT:
            this.output.put(currentInput, argument);
            break;
        case CHANGE_FILE:
            this.changeFiles.computeIfAbsent(currentInput, key -> new ArrayList<>()).add(argument);
            break;
        case LANGUAGE:
            I18n.set(argument);
            break;
        case LOAD_PREFERENCES:
            final Preferences tempPreferences = new Preferences();
            tempPreferences.enableSaveOnPut(false);
            CustomConfigurator.XMLCommandProcessor config = new CustomConfigurator.XMLCommandProcessor(tempPreferences);
            try (InputStream is = Utils.openStream(new File(argument).toURI().toURL())) {
                config.openAndReadXML(is);
            } catch (IOException e) {
                throw new JosmRuntimeException(e);
            }
            final IPreferences pref = Config.getPref();
            if (pref instanceof MemoryPreferences) {
                final MemoryPreferences memoryPreferences = (MemoryPreferences) pref;
                tempPreferences.getAllSettings().entrySet().stream().filter(entry -> entry.getValue().isNew())
                        .forEach(entry -> memoryPreferences.putSetting(entry.getKey(), entry.getValue()));
            } else {
                throw new JosmRuntimeException(tr("Preferences are not the expected type"));
            }
            break;
        case SET:

        default:
            throw new AssertionError("Unexpected option: " + option);
        }
        return Optional.empty();
    }

    private static void showHelp() {
        System.out.println(getHelp());
    }

    private static String getHelp() {
        final String helpPadding = "\t                          ";
        // CHECKSTYLE.OFF: SingleSpaceSeparator
        return tr("JOSM Validation command line interface") + "\n\n" +
                tr("Usage") + ":\n" +
                "\tjava -jar josm.jar validate <options>\n\n" +
                tr("Description") + ":\n" +
                tr("Validates data and saves the result to a file.") + "\n\n"+
                tr("Options") + ":\n" +
                "\t--help|-h                 " + tr("Show this help") + "\n" +
                "\t--input|-i <file>         " + tr("Input data file name (.osm, .validator.mapcss, .mapcss).") + '\n' +
                helpPadding                    + tr("OSM files can be specified multiple times. Required.") + '\n' +
                helpPadding                    + tr(".validator.mapcss and .mapcss files will stop processing on first error.") + '\n' +
                helpPadding                    + tr("Non-osm files do not use --output or --change-file") + '\n' +
                "\t--output|-o <file>        " + tr("Output data file name (.geojson, line-by-line delimited for MapRoulette). Optional.")
                                               + '\n' +
                "\t--change-file|-c <file>   " + tr("Change file name (.osc). Can be specified multiple times per input.") + '\n' +
                helpPadding                    + tr("Changes will be applied in the specified order. Optional.");
        // CHECKSTYLE.ON: SingleSpaceSeparator
    }
}
