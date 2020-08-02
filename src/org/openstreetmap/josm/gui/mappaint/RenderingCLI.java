// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.DoubleSupplier;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.openstreetmap.gui.jmapviewer.OsmMercator;
import org.openstreetmap.josm.cli.CLIModule;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.LatLonParser;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.data.preferences.JosmUrls;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.mappaint.RenderingHelper.StyleData;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.MemoryPreferences;
import org.openstreetmap.josm.tools.Http1Client;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.JosmDecimalFormatSymbolsProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OptionParser;
import org.openstreetmap.josm.tools.OptionParser.OptionCount;
import org.openstreetmap.josm.tools.OptionParser.OptionParseException;
import org.openstreetmap.josm.tools.Stopwatch;
import org.openstreetmap.josm.tools.Territories;

/**
 * Command line interface for rendering osm data to an image file.
 *
 * @since 12906
 */
public class RenderingCLI implements CLIModule {

    /**
     * The singleton instance of this class.
     */
    public static final RenderingCLI INSTANCE = new RenderingCLI();

    private static final double PIXEL_PER_METER = 96 / 2.54 * 100; // standard value of 96 dpi display resolution
    private static final int DEFAULT_MAX_IMAGE_SIZE = 20000;

    private boolean argDebug;
    private boolean argTrace;
    private String argInput;
    private String argOutput;
    private List<StyleData> argStyles;
    private Integer argZoom;
    private Double argScale;
    private Bounds argBounds;
    private LatLon argAnchor;
    private Double argWidthM;
    private Double argHeightM;
    private Integer argWidthPx;
    private Integer argHeightPx;
    private String argProjection;
    private Integer argMaxImageSize;

    private StyleData argCurrentStyle;

    private enum Option {
        HELP(false, 'h'),
        DEBUG(false, '*'),
        TRACE(false, '*'),
        INPUT(true, 'i'),
        STYLE(true, 's'),
        SETTING(true, '*'),
        OUTPUT(true, 'o'),
        ZOOM(true, 'z'),
        SCALE(true, '*'),
        BOUNDS(true, 'b'),
        ANCHOR(true, '*'),
        WIDTH_M(true, '*'),
        HEIGHT_M(true, '*'),
        WIDTH_PX(true, '*'),
        HEIGHT_PX(true, '*'),
        PROJECTION(true, '*'),
        MAX_IMAGE_SIZE(true, '*');

        private final String name;
        private final boolean requiresArg;
        private final char shortOption;

        Option(boolean requiresArgument, char shortOption) {
            this.name = name().toLowerCase(Locale.US).replace('_', '-');
            this.requiresArg = requiresArgument;
            this.shortOption = shortOption;
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

        /**
         * Replies the short option (single letter) associated with this option.
         * @return the short option or '*' if there is no short option
         */
        public char getShortOption() {
            return shortOption;
        }
    }

    /**
     * Data class to hold return values for {@link #determineRenderingArea(DataSet)}.
     *
     * Package private access for unit tests.
     */
    static class RenderingArea {
        public Bounds bounds;
        public double scale; // in east-north units per pixel (unlike the --scale option, which is in meter per meter)
    }

    RenderingCLI() {
        // hide constructor (package private access for unit tests)
    }

    @Override
    public String getActionKeyword() {
        return "render";
    }

    @Override
    public void processArguments(String[] argArray) {
        try {
            parseArguments(argArray);
            initialize();
            Stopwatch stopwatch = Stopwatch.createStarted();
            String task = tr("Rendering {0} to {1}", argInput, argOutput);
            System.err.println(task);
            DataSet ds = loadDataset();
            RenderingArea area = determineRenderingArea(ds);
            RenderingHelper rh = new RenderingHelper(ds, area.bounds, area.scale, argStyles);
            checkPreconditions(rh);
            BufferedImage image = rh.render();
            writeImageToFile(image);
            System.err.println(stopwatch.toString(task));
        } catch (FileNotFoundException | NoSuchFileException e) {
            if (Logging.isDebugEnabled()) {
                e.printStackTrace();
            }
            System.err.println(tr("Error - file not found: ''{0}''", e.getMessage()));
            System.exit(1);
        } catch (IllegalArgumentException | IllegalDataException | IOException e) {
            if (Logging.isDebugEnabled()) {
                e.printStackTrace();
            }
            if (e.getMessage() != null) {
                System.err.println(tr("Error: {0}", e.getMessage()));
            }
            System.exit(1);
        }
        System.exit(0);
    }

    /**
     * Parse command line arguments and do some low-level error checking.
     * @param argArray the arguments array
     */
    void parseArguments(String[] argArray) {
        Logging.setLogLevel(Level.INFO);

        OptionParser parser = new OptionParser("JOSM rendering");
        for (Option o : Option.values()) {
            if (o.requiresArgument()) {
                parser.addArgumentParameter(o.getName(),
                        o == Option.SETTING ? OptionCount.MULTIPLE : OptionCount.OPTIONAL,
                        arg -> handleOption(o, arg));
            } else {
                parser.addFlagParameter(o.getName(), () -> handleOption(o));
            }
            if (o.getShortOption() != '*') {
                parser.addShortAlias(o.getName(), Character.toString(o.getShortOption()));
            }
        }

        argCurrentStyle = new StyleData();
        argStyles = new ArrayList<>();

        parser.parseOptionsOrExit(Arrays.asList(argArray));

        if (argCurrentStyle.styleUrl != null) {
            argStyles.add(argCurrentStyle);
        } else if (argStyles.isEmpty()) {
            argCurrentStyle.styleUrl = "resource://styles/standard/elemstyles.mapcss";
            argStyles.add(argCurrentStyle);
        }
    }

    private void handleOption(Option o) {
        switch (o) {
        case HELP:
            showHelp();
            System.exit(0);
            break;
        case DEBUG:
            argDebug = true;
            break;
        case TRACE:
            argTrace = true;
            break;
        default:
            throw new AssertionError("Unexpected option index: " + o);
        }
    }

    private void handleOption(Option o, String arg) {
        switch (o) {
        case INPUT:
            argInput = arg;
            break;
        case STYLE:
            if (argCurrentStyle.styleUrl != null) {
                argStyles.add(argCurrentStyle);
                argCurrentStyle = new StyleData();
            }
            argCurrentStyle.styleUrl = arg;
            break;
        case OUTPUT:
            argOutput = arg;
            break;
        case ZOOM:
            try {
                argZoom = Integer.valueOf(arg);
            } catch (NumberFormatException nfe) {
                throw new OptionParseException(
                        tr("Expected integer number for option {0}, but got ''{1}''", "--zoom", arg), nfe);
            }
            if (argZoom < 0) {
                throw new OptionParseException(
                        tr("Expected integer number >= 0 for option {0}, but got ''{1}''", "--zoom", arg));
            }
            break;
        case BOUNDS:
            if (!"auto".equals(arg)) {
                try {
                    argBounds = new Bounds(arg, ",", Bounds.ParseMethod.LEFT_BOTTOM_RIGHT_TOP, false);
                } catch (IllegalArgumentException iae) { // NOPMD
                    throw new OptionParseException(
                            tr("Unable to parse {0} parameter: {1}", "--bounds", iae.getMessage()), iae);
                }
            }
            break;

        case SETTING:
            String keyval = arg;
            String[] comp = keyval.split(":", 2);
            if (comp.length != 2) {
                throw new OptionParseException(
                        tr("Expected key and value, separated by '':'' character for option {0}, but got ''{1}''",
                                "--setting", arg));
            }
            argCurrentStyle.settings.put(comp[0].trim(), comp[1].trim());
            break;
        case SCALE:
            try {
                argScale = JosmDecimalFormatSymbolsProvider.parseDouble(arg);
            } catch (NumberFormatException nfe) {
                throw new OptionParseException(
                        tr("Expected floating point number for option {0}, but got ''{1}''", "--scale", arg), nfe);
            }
            break;
        case ANCHOR:
            String[] parts = arg.split(",", -1);
            if (parts.length != 2)
                throw new OptionParseException(
                        tr("Expected two coordinates, separated by comma, for option {0}, but got ''{1}''", "--anchor",
                                arg));
            try {
                double lon = LatLonParser.parseCoordinate(parts[0]);
                double lat = LatLonParser.parseCoordinate(parts[1]);
                argAnchor = new LatLon(lat, lon);
            } catch (IllegalArgumentException iae) { // NOPMD
                throw new OptionParseException(tr("In option {0}: {1}", "--anchor", iae.getMessage()), iae);
            }
            break;
        case WIDTH_M:
            try {
                argWidthM = JosmDecimalFormatSymbolsProvider.parseDouble(arg);
            } catch (NumberFormatException nfe) {
                throw new OptionParseException(
                        tr("Expected floating point number for option {0}, but got ''{1}''", "--width-m", arg), nfe);
            }
            if (argWidthM <= 0)
                throw new OptionParseException(
                        tr("Expected floating point number > 0 for option {0}, but got ''{1}''", "--width-m", arg));
            break;
        case HEIGHT_M:
            try {
                argHeightM = JosmDecimalFormatSymbolsProvider.parseDouble(arg);
            } catch (NumberFormatException nfe) {
                throw new OptionParseException(
                        tr("Expected floating point number for option {0}, but got ''{1}''", "--height-m", arg), nfe);
            }
            if (argHeightM <= 0)
                throw new OptionParseException(
                        tr("Expected floating point number > 0 for option {0}, but got ''{1}''", "--width-m", arg));
            break;
        case WIDTH_PX:
            try {
                argWidthPx = Integer.valueOf(arg);
            } catch (NumberFormatException nfe) {
                throw new OptionParseException(
                        tr("Expected integer number for option {0}, but got ''{1}''", "--width-px", arg), nfe);
            }
            if (argWidthPx <= 0)
                throw new OptionParseException(
                        tr("Expected integer number > 0 for option {0}, but got ''{1}''", "--width-px", arg));
            break;
        case HEIGHT_PX:
            try {
                argHeightPx = Integer.valueOf(arg);
            } catch (NumberFormatException nfe) {
                throw new OptionParseException(
                        tr("Expected integer number for option {0}, but got ''{1}''", "--height-px", arg), nfe);
            }
            if (argHeightPx <= 0) {
                throw new OptionParseException(
                        tr("Expected integer number > 0 for option {0}, but got ''{1}''", "--height-px", arg));
            }
            break;
        case PROJECTION:
            argProjection = arg;
            break;
        case MAX_IMAGE_SIZE:
            try {
                argMaxImageSize = Integer.valueOf(arg);
            } catch (NumberFormatException nfe) {
                throw new OptionParseException(
                        tr("Expected integer number for option {0}, but got ''{1}''", "--max-image-size", arg), nfe);
            }
            if (argMaxImageSize < 0) {
                throw new OptionParseException(
                        tr("Expected integer number >= 0 for option {0}, but got ''{1}''", "--max-image-size", arg));
            }
            break;
        default:
            throw new AssertionError("Unexpected option index: " + o);
        }
    }

    /**
     * Displays help on the console
     */
    public static void showHelp() {
        System.out.println(getHelp());
    }

    private static String getHelp() {
        return tr("JOSM rendering command line interface")+"\n\n"+
                tr("Usage")+":\n"+
                "\tjava -jar josm.jar render <options>\n\n"+
                tr("Description")+":\n"+
                tr("Renders data and saves the result to an image file.")+"\n\n"+
                tr("Options")+":\n"+
                "\t--help|-h                 "+tr("Show this help")+"\n"+
                "\t--input|-i <file>         "+tr("Input data file name (.osm)")+"\n"+
                "\t--output|-o <file>        "+tr("Output image file name (.png); defaults to ''{0}''", "out.png")+"\n"+
                "\t--style|-s <file>         "+tr("Style file to use for rendering (.mapcss or .zip)")+"\n"+
                "\t                          "+tr("This option can be repeated to load multiple styles.")+"\n"+
                "\t--setting <key>:<value>   "+tr("Style setting (in JOSM accessible in the style list dialog right click menu)")+"\n"+
                "\t                          "+tr("Applies to the last style loaded with the {0} option.", "--style")+"\n"+
                "\t--zoom|-z <lvl>           "+tr("Select zoom level to render. (integer value, 0=entire earth, 18=street level)")+"\n"+
                "\t--scale <scale>           "+tr("Select the map scale")+"\n"+
                "\t                          "+tr("A value of 10000 denotes a scale of 1:10000 (1 cm on the map equals 100 m on the ground; "
                                                + "display resolution: 96 dpi)")+"\n"+
                "\t                          "+tr("Options {0} and {1} are mutually exclusive.", "--zoom", "--scale")+"\n"+
                "\t--bounds|-b auto|<min_lon>,<min_lat>,<max_lon>,<max_lat>\n"+
                "\t                          "+tr("Area to render, default value is ''{0}''", "auto")+"\n"+
                "\t                          "+tr("With keyword ''{0}'', the downloaded area in the .osm input file will be used (if recorded).",
                                                  "auto")+"\n"+
                "\t--anchor <lon>,<lat>      "+tr("Specify bottom left corner of the rendering area")+"\n"+
                "\t                          "+tr("Used in combination with width and height options to determine the area to render.")+"\n"+
                "\t--width-m <number>        "+tr("Width of the rendered area, in meter")+"\n"+
                "\t--height-m <number>       "+tr("Height of the rendered area, in meter")+"\n"+
                "\t--width-px <number>       "+tr("Width of the target image, in pixel")+"\n"+
                "\t--height-px <number>      "+tr("Height of the target image, in pixel")+"\n"+
                "\t--projection <code>       "+tr("Projection to use, default value ''{0}'' (web-Mercator)", "epsg:3857")+"\n"+
                "\t--max-image-size <number> "+tr("Maximum image width/height in pixel (''{0}'' means no limit), default value: {1}",
                                                   0, Integer.toString(DEFAULT_MAX_IMAGE_SIZE))+"\n"+
                "\n"+
                tr("To specify the rendered area and scale, the options can be combined in various ways")+":\n"+
                "  * --bounds (--zoom|--scale|--width-px|--height-px)\n"+
                "  * --anchor (--width-m|--width-px) (--height-m|--height-px) (--zoom|--scale)\n"+
                "  * --anchor --width-m --height-m (--width-px|--height-px)\n"+
                "  * --anchor --width-px --height-px (--width-m|--height-m)\n"+
                tr("If neither ''{0}'' nor ''{1}'' is given, the default value {2} takes effect "
                        + "and the bounds of the download area in the .osm input file are used.",
                        "bounds", "anchor", "--bounds=auto")+"\n\n"+
                tr("Examples")+":\n"+
                "  java -jar josm.jar render -i data.osm -s style.mapcss -z 16\n"+
                "  josm render -i data.osm -s style.mapcss --scale 5000\n"+
                "  josm render -i data.osm -s style.mapcss -z 16 -o image.png\n"+
                "  josm render -i data.osm -s elemstyles.mapcss --setting hide_icons:false -z 16\n"+
                "  josm render -i data.osm -s style.mapcss -s another_style.mapcss -z 16 -o image.png\n"+
                "  josm render -i data.osm -s style.mapcss --bounds 21.151,51.401,21.152,51.402 -z 16\n"+
                "  josm render -i data.osm -s style.mapcss --anchor 21.151,51.401 --width-m 500 --height-m 300 -z 16\n"+
                "  josm render -i data.osm -s style.mapcss --anchor 21.151,51.401 --width-m 500 --height-m 300 --width-px 1800\n"+
                "  josm render -i data.osm -s style.mapcss --scale 5000 --projection epsg:4326\n";
    }

    /**
     * Initialization.
     *
     * Requires arguments to be parsed already ({@link #parseArguments(java.lang.String[])}).
     */
    void initialize() {
        Logging.setLogLevel(getLogLevel());
        HttpClient.setFactory(Http1Client::new);

        Config.setBaseDirectoriesProvider(JosmBaseDirectories.getInstance()); // for right-left-hand traffic cache file
        Config.setPreferencesInstance(new MemoryPreferences());
        Config.setUrlsProvider(JosmUrls.getInstance());
        Config.getPref().putBoolean("mappaint.auto_reload_local_styles", false); // unnecessary to listen for external changes
        String projCode = Optional.ofNullable(argProjection).orElse("epsg:3857");
        ProjectionRegistry.setProjection(Projections.getProjectionByCode(projCode.toUpperCase(Locale.US)));

        Territories.initializeInternalData();
    }

    private Level getLogLevel() {
        if (argTrace) {
            return Logging.LEVEL_TRACE;
        } else if (argDebug) {
            return Logging.LEVEL_DEBUG;
        } else {
            return Logging.LEVEL_INFO;
        }
    }

    /**
     * Find the area to render and the scale, given certain command line options and the dataset.
     * @param ds the dataset
     * @return area to render and the scale
     */
    RenderingArea determineRenderingArea(DataSet ds) {

        Projection proj = ProjectionRegistry.getProjection();
        Double scale = null; // scale in east-north units per pixel
        if (argZoom != null) {
            scale = OsmMercator.EARTH_RADIUS * Math.PI * 2 / Math.pow(2, argZoom) / OsmMercator.DEFAUL_TILE_SIZE
                    / proj.getMetersPerUnit();
        }
        Bounds bounds = argBounds;
        ProjectionBounds pb = null;

        if (bounds == null) {
            if (argAnchor != null) {
                EastNorth projAnchor = proj.latlon2eastNorth(argAnchor);

                double enPerMeter = Double.NaN;
                DoubleSupplier getEnPerMeter = () -> {
                    double shiftMeter = 10;
                    EastNorth projAnchorShifted = projAnchor.add(shiftMeter / proj.getMetersPerUnit(),
                            shiftMeter / proj.getMetersPerUnit());
                    LatLon anchorShifted = proj.eastNorth2latlon(projAnchorShifted);
                    return projAnchor.distance(projAnchorShifted) / argAnchor.greatCircleDistance(anchorShifted);
                };

                if (scale == null) {
                    if (argScale != null) {
                        enPerMeter = getEnPerMeter.getAsDouble();
                        scale = argScale * enPerMeter / PIXEL_PER_METER;
                    } else if (argWidthM != null && argWidthPx != null) {
                        enPerMeter = getEnPerMeter.getAsDouble();
                        scale = argWidthM / argWidthPx * enPerMeter;
                    } else if (argHeightM != null && argHeightPx != null) {
                        enPerMeter = getEnPerMeter.getAsDouble();
                        scale = argHeightM / argHeightPx * enPerMeter;
                    } else {
                        throw new IllegalArgumentException(
                                tr("Argument {0} given, but scale cannot be determined from remaining arguments",
                                        "--anchor"));
                    }
                }

                double widthEn;
                if (argWidthM != null) {
                    if (Double.isNaN(enPerMeter)) {
                        enPerMeter = getEnPerMeter.getAsDouble();
                    }
                    widthEn = argWidthM * enPerMeter;
                } else if (argWidthPx != null) {
                    widthEn = argWidthPx * scale;
                } else {
                    throw new IllegalArgumentException(
                            tr("Argument {0} given, expected {1} or {2}", "--anchor", "--width-m", "--width-px"));
                }

                double heightEn;
                if (argHeightM != null) {
                    if (Double.isNaN(enPerMeter)) {
                        enPerMeter = getEnPerMeter.getAsDouble();
                    }
                    heightEn = argHeightM * enPerMeter;
                } else if (argHeightPx != null) {
                    heightEn = argHeightPx * scale;
                } else {
                    throw new IllegalArgumentException(
                            tr("Argument {0} given, expected {1} or {2}", "--anchor", "--height-m", "--height-px"));
                }
                pb = new ProjectionBounds(projAnchor);
                pb.extend(new EastNorth(projAnchor.east() + widthEn, projAnchor.north() + heightEn));
                bounds = new Bounds(proj.eastNorth2latlon(pb.getMin()), false);
                bounds.extend(proj.eastNorth2latlon(pb.getMax()));
            } else {
                if (ds.getDataSourceBounds().isEmpty()) {
                    throw new IllegalArgumentException(
                            tr("{0} mode, but no bounds found in osm data input file", "--bounds=auto"));
                }
                bounds = ds.getDataSourceBounds().get(0);
            }
        }

        if (pb == null) {
            pb = new ProjectionBounds();
            pb.extend(proj.latlon2eastNorth(bounds.getMin()));
            pb.extend(proj.latlon2eastNorth(bounds.getMax()));
        }

        if (scale == null) {
            if (argScale != null) {
                double enPerMeter = pb.getMin().distance(pb.getMax())
                        / bounds.getMin().greatCircleDistance(bounds.getMax());
                scale = argScale * enPerMeter / PIXEL_PER_METER;
            } else if (argWidthPx != null) {
                scale = (pb.maxEast - pb.minEast) / argWidthPx;
            } else if (argHeightPx != null) {
                scale = (pb.maxNorth - pb.minNorth) / argHeightPx;
            } else {
                throw new IllegalArgumentException(
                        tr("Unable to determine scale, one of the options {0}, {1}, {2} or {3} expected", "--zoom",
                                "--scale", "--width-px", "--height-px"));
            }
        }

        RenderingArea ra = new RenderingArea();
        ra.bounds = bounds;
        ra.scale = scale;
        return ra;
    }

    private DataSet loadDataset() throws IOException, IllegalDataException {
        if (argInput == null) {
            throw new IllegalArgumentException(tr("Missing argument - input data file ({0})", "--input|-i"));
        }
        try (InputStream inputStream = Compression.getUncompressedFileInputStream(Paths.get(argInput))) {
            return OsmReader.parseDataSet(inputStream, null);
        } catch (IllegalDataException e) {
            throw new IllegalDataException(tr("In .osm data file ''{0}'' - ", argInput) + e.getMessage(), e);
        }
    }

    private void checkPreconditions(RenderingHelper rh) {
        Dimension imgSize = rh.getImageSize();
        Logging.debug("image size (px): {0}x{1}", imgSize.width, imgSize.height);
        int maxSize = Optional.ofNullable(argMaxImageSize).orElse(DEFAULT_MAX_IMAGE_SIZE);
        if (maxSize != 0 && (imgSize.width > maxSize || imgSize.height > maxSize)) {
            throw new IllegalArgumentException(
                    tr("Image dimensions ({0}x{1}) exceeds maximum image size {2} (use option {3} to change limit)",
                            imgSize.width, imgSize.height, maxSize, "--max-image-size"));
        }
    }

    private void writeImageToFile(BufferedImage image) throws IOException {
        String output = Optional.ofNullable(argOutput).orElse("out.png");
        ImageIO.write(image, "png", new File(output));
    }
}
