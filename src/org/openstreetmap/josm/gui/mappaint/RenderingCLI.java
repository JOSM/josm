// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.MemoryPreferences;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.JosmDecimalFormatSymbolsProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.RightAndLefthandTraffic;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

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

        LongOpt toLongOpt() {
            return new LongOpt(getName(), requiresArgument() ? LongOpt.REQUIRED_ARGUMENT : LongOpt.NO_ARGUMENT, null, getShortOption());
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
            DataSet ds = loadDataset();
            RenderingArea area = determineRenderingArea(ds);
            RenderingHelper rh = new RenderingHelper(ds, area.bounds, area.scale, argStyles);
            checkPreconditions(rh);
            BufferedImage image = rh.render();
            writeImageToFile(image);
        } catch (FileNotFoundException e) {
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
        Getopt.setI18nHandler(I18n::tr);
        Logging.setLogLevel(Level.INFO);

        LongOpt[] opts = new LongOpt[Option.values().length];
        StringBuilder optString = new StringBuilder();
        for (Option o : Option.values()) {
            opts[o.ordinal()] = o.toLongOpt();
            if (o.getShortOption() != '*') {
                optString.append(o.getShortOption());
                if (o.requiresArgument()) {
                    optString.append(':');
                }
            }
        }

        Getopt getopt = new Getopt("JOSM rendering", argArray, optString.toString(), opts);

        StyleData currentStyle = new StyleData();
        argStyles = new ArrayList<>();

        int c;
        while ((c = getopt.getopt()) != -1) {
            switch (c) {
            case 'h':
                showHelp();
                System.exit(0);
            case 'i':
                argInput = getopt.getOptarg();
                break;
            case 's':
                if (currentStyle.styleUrl != null) {
                    argStyles.add(currentStyle);
                    currentStyle = new StyleData();
                }
                currentStyle.styleUrl = getopt.getOptarg();
                break;
            case 'o':
                argOutput = getopt.getOptarg();
                break;
            case 'z':
                try {
                    argZoom = Integer.valueOf(getopt.getOptarg());
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException(
                            tr("Expected integer number for option {0}, but got ''{1}''", "--zoom", getopt.getOptarg()), nfe);
                }
                if (argZoom < 0)
                    throw new IllegalArgumentException(
                            tr("Expected integer number >= 0 for option {0}, but got ''{1}''", "--zoom", getopt.getOptarg()));
                break;
            case 'b':
                if (!"auto".equals(getopt.getOptarg())) {
                    try {
                        argBounds = new Bounds(getopt.getOptarg(), ",", Bounds.ParseMethod.LEFT_BOTTOM_RIGHT_TOP, false);
                    } catch (IllegalArgumentException iae) { // NOPMD
                        throw new IllegalArgumentException(tr("Unable to parse {0} parameter: {1}", "--bounds", iae.getMessage()), iae);
                    }
                }
                break;
            case '*':
                switch (Option.values()[getopt.getLongind()]) {
                case DEBUG:
                    argDebug = true;
                    break;
                case TRACE:
                    argTrace = true;
                    break;
                case SETTING:
                    String keyval = getopt.getOptarg();
                    String[] comp = keyval.split(":");
                    if (comp.length != 2)
                        throw new IllegalArgumentException(
                                tr("Expected key and value, separated by '':'' character for option {0}, but got ''{1}''",
                                        "--setting", getopt.getOptarg()));
                    currentStyle.settings.put(comp[0].trim(), comp[1].trim());
                    break;
                case SCALE:
                    try {
                        argScale = JosmDecimalFormatSymbolsProvider.parseDouble(getopt.getOptarg());
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException(
                                tr("Expected floating point number for option {0}, but got ''{1}''", "--scale", getopt.getOptarg()), nfe);
                    }
                    break;
                case ANCHOR:
                    String[] parts = getopt.getOptarg().split(",");
                    if (parts.length != 2)
                        throw new IllegalArgumentException(
                                tr("Expected two coordinates, separated by comma, for option {0}, but got ''{1}''",
                                "--anchor", getopt.getOptarg()));
                    try {
                        double lon = LatLonParser.parseCoordinate(parts[0]);
                        double lat = LatLonParser.parseCoordinate(parts[1]);
                        argAnchor = new LatLon(lat, lon);
                    } catch (IllegalArgumentException iae) { // NOPMD
                        throw new IllegalArgumentException(tr("In option {0}: {1}", "--anchor", iae.getMessage()), iae);
                    }
                    break;
                case WIDTH_M:
                    try {
                        argWidthM = JosmDecimalFormatSymbolsProvider.parseDouble(getopt.getOptarg());
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException(
                                tr("Expected floating point number for option {0}, but got ''{1}''", "--width-m", getopt.getOptarg()), nfe);
                    }
                    if (argWidthM <= 0) throw new IllegalArgumentException(
                            tr("Expected floating point number > 0 for option {0}, but got ''{1}''", "--width-m", getopt.getOptarg()));
                    break;
                case HEIGHT_M:
                    try {
                        argHeightM = JosmDecimalFormatSymbolsProvider.parseDouble(getopt.getOptarg());
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException(
                                tr("Expected floating point number for option {0}, but got ''{1}''", "--height-m", getopt.getOptarg()), nfe);
                    }
                    if (argHeightM <= 0) throw new IllegalArgumentException(
                            tr("Expected floating point number > 0 for option {0}, but got ''{1}''", "--width-m", getopt.getOptarg()));
                    break;
                case WIDTH_PX:
                    try {
                        argWidthPx = Integer.valueOf(getopt.getOptarg());
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException(
                                tr("Expected integer number for option {0}, but got ''{1}''", "--width-px", getopt.getOptarg()), nfe);
                    }
                    if (argWidthPx <= 0) throw new IllegalArgumentException(
                            tr("Expected integer number > 0 for option {0}, but got ''{1}''", "--width-px", getopt.getOptarg()));
                    break;
                case HEIGHT_PX:
                    try {
                        argHeightPx = Integer.valueOf(getopt.getOptarg());
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException(
                                tr("Expected integer number for option {0}, but got ''{1}''", "--height-px", getopt.getOptarg()), nfe);
                    }
                    if (argHeightPx <= 0) throw new IllegalArgumentException(
                            tr("Expected integer number > 0 for option {0}, but got ''{1}''", "--height-px", getopt.getOptarg()));
                    break;
                case PROJECTION:
                    argProjection = getopt.getOptarg();
                    break;
                case MAX_IMAGE_SIZE:
                    try {
                        argMaxImageSize = Integer.valueOf(getopt.getOptarg());
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException(
                                tr("Expected integer number for option {0}, but got ''{1}''", "--max-image-size", getopt.getOptarg()), nfe);
                    }
                    if (argMaxImageSize < 0) throw new IllegalArgumentException(
                            tr("Expected integer number >= 0 for option {0}, but got ''{1}''", "--max-image-size", getopt.getOptarg()));
                    break;
                default:
                    throw new AssertionError("Unexpected option index: " + getopt.getLongind());
                }
                break;
            case '?':
                throw new IllegalArgumentException();   // getopt error
            default:
                throw new AssertionError("Unrecognized option: " + c);
            }
        }
        if (currentStyle.styleUrl != null) {
            argStyles.add(currentStyle);
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

        Config.setBaseDirectoriesProvider(JosmBaseDirectories.getInstance()); // for right-left-hand traffic cache file
        Config.setPreferencesInstance(new MemoryPreferences());
        Config.setUrlsProvider(JosmUrls.getInstance());
        Config.getPref().putBoolean("mappaint.auto_reload_local_styles", false); // unnecessary to listen for external changes
        String projCode = Optional.ofNullable(argProjection).orElse("epsg:3857");
        ProjectionRegistry.setProjection(Projections.getProjectionByCode(projCode.toUpperCase(Locale.US)));

        RightAndLefthandTraffic.initialize();
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
            scale = OsmMercator.EARTH_RADIUS * Math.PI * 2 / Math.pow(2, argZoom) / OsmMercator.DEFAUL_TILE_SIZE / proj.getMetersPerUnit();
        }
        Bounds bounds = argBounds;
        ProjectionBounds pb = null;

        if (bounds == null) {
            if (argAnchor != null) {
                EastNorth projAnchor = proj.latlon2eastNorth(argAnchor);

                double enPerMeter = Double.NaN;
                DoubleSupplier getEnPerMeter = () -> {
                    double shiftMeter = 10;
                    EastNorth projAnchorShifted = projAnchor.add(
                            shiftMeter / proj.getMetersPerUnit(), shiftMeter / proj.getMetersPerUnit());
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
                                tr("Argument {0} given, but scale cannot be determined from remaining arguments", "--anchor"));
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
                    throw new IllegalArgumentException(tr("{0} mode, but no bounds found in osm data input file", "--bounds=auto"));
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
                double enPerMeter = pb.getMin().distance(pb.getMax()) / bounds.getMin().greatCircleDistance(bounds.getMax());
                scale = argScale * enPerMeter / PIXEL_PER_METER;
            } else if (argWidthPx != null) {
                scale = (pb.maxEast - pb.minEast) / argWidthPx;
            } else if (argHeightPx != null) {
                scale = (pb.maxNorth - pb.minNorth) / argHeightPx;
            } else {
                throw new IllegalArgumentException(
                        tr("Unable to determine scale, one of the options {0}, {1}, {2} or {3} expected",
                                "--zoom", "--scale", "--width-px", "--height-px"));
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
        try {
            return OsmReader.parseDataSet(Files.newInputStream(Paths.get(argInput)), null);
        } catch (IllegalDataException e) {
            throw new IllegalDataException(tr("In .osm data file ''{0}'' - ", argInput) + e.getMessage(), e);
        }
    }

    private void checkPreconditions(RenderingHelper rh) {
        if (argStyles.isEmpty())
            throw new IllegalArgumentException(tr("Missing argument - at least one style expected ({0})", "--style"));

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
