// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.openstreetmap.josm.CLIModule;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.LatLonParser;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Utils;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

/**
 * Command line interface for projecting coordinates.
 * @since 12792
 */
public class ProjectionCLI implements CLIModule {

    public static final ProjectionCLI INSTANCE = new ProjectionCLI();

    private boolean argInverse = false;         // NOPMD
    private boolean argSwitchInput = false;     // NOPMD
    private boolean argSwitchOutput = false;    // NOPMD

    @Override
    public String getActionKeyword() {
        return "project";
    }

    @Override
    public void processArguments(String[] argArray) {
        Getopt.setI18nHandler(I18n::tr);
        Getopt getopt = new Getopt("JOSM projection", argArray, "Irh", new LongOpt[] {
                new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h')});

        int c;
        while ((c = getopt.getopt()) != -1) {
            switch (c) {
            case 'h':
                showHelp();
                System.exit(0);
            case 'I':
                argInverse = true;
                break;
            case 'r':
                argSwitchInput = true;
                break;
            case 's':
                argSwitchOutput = true;
                break;
            default:
                // ignore
            }
        }

        List<String> projParamFrom = new ArrayList<>();
        List<String> projParamTo = new ArrayList<>();
        List<String> otherPositional = new ArrayList<>();
        boolean toTokenSeen = false;
        // positional arguments:
        for (int i = getopt.getOptind(); i < argArray.length; ++i) {
            String arg = argArray[i];
            if (arg.isEmpty()) throw new IllegalArgumentException("non-empty argument expected");
            if (arg.startsWith("+")) {
                if (arg.equals("+to")) {
                    toTokenSeen = true;
                } else {
                    (toTokenSeen ? projParamTo : projParamFrom).add(arg);
                }
            } else {
                otherPositional.add(arg);
            }
        }
        String fromStr = Utils.join(" ", projParamFrom);
        String toStr = Utils.join(" ", projParamTo);
        try {
            run(fromStr, toStr, otherPositional);
        } catch (ProjectionConfigurationException | IllegalArgumentException | IOException ex) {
            System.err.println(tr("Error: {0}", ex.getMessage()));
            System.exit(1);
        }
        System.exit(0);
    }

    /**
     * Displays help on the console
     */
    public static void showHelp() {
        System.out.println(getHelp());
    }

    private static String getHelp() {
        return tr("JOSM projection command line interface")+"\n\n"+
                tr("Usage")+":\n"+
                "\tjava -jar josm.jar project <options> <crs> +to <crs> [file]\n\n"+
                tr("Description")+":\n"+
                tr("Converts coordinates from one coordinate reference system to another.")+"\n\n"+
                tr("Options")+":\n"+
                "\t--help|-h         "+tr("Show this help")+"\n"+
                "\t-I                "+tr("Switch input and output crs")+"\n"+
                "\t-r                "+tr("Switch order of input coordinates (east/north, lon/lat)")+"\n"+
                "\t-s                "+tr("Switch order of output coordinates (east/north, lon/lat)")+"\n\n"+
                tr("<crs>")+":\n"+
                tr("The format for input and output coordinate reference system"
                        + " is similar to that of the PROJ.4 software.")+"\n\n"+
                tr("[file]")+":\n"+
                tr("Reads input data from one or more files listed as positional arguments. "
                + "When no files are given, or the filename is \"-\", data is read from "
                + "standard input.")+"\n\n"+
                tr("Examples")+":\n"+
                "    java -jar josm.jar project +init=epsg:4326 +to +init=epsg:3857 <<<\"11.232274 50.5685716\"\n"+
                "       => 1250371.1334500168 6545331.055189664\n\n"+
                "    java -jar josm.jar project +proj=lonlat +datum=WGS84 +to +proj=merc +a=6378137 +b=6378137 +nadgrids=@null <<EOF\n" +
                "    11d13'56.19\"E 50d34'6.86\"N\n" +
                "    118d39'30.42\"W 37d20'18.76\"N\n"+
                "    EOF\n"+
                "       => 1250371.1334500168 6545331.055189664\n" +
                "          -1.3208998232319113E7 4486401.160664663\n";
    }

    private void run(String fromStr, String toStr, List<String> files) throws ProjectionConfigurationException, IOException {
        CustomProjection fromProj = createProjection(fromStr);
        CustomProjection toProj = createProjection(toStr);
        if (this.argInverse) {
            CustomProjection tmp = fromProj;
            fromProj = toProj;
            toProj = tmp;
        }

        if (files.isEmpty() || files.get(0).equals("-")) {
            processInput(fromProj, toProj, new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset())));
        } else {
            for (String file : files) {
                try (BufferedReader br = Files.newBufferedReader(Paths.get(file), StandardCharsets.UTF_8)) {
                    processInput(fromProj, toProj, br);
                }
            }
        }
    }

    private void processInput(CustomProjection fromProj, CustomProjection toProj, BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#"))
                continue;
            EastNorth enIn;
            if (fromProj.isGeographic()) {
                enIn = parseEastNorth(line, LatLonParser::parseCoordinate);
            } else {
                enIn = parseEastNorth(line, ProjectionCLI::parseDouble);
            }
            LatLon ll = fromProj.eastNorth2latlon(enIn);
            EastNorth enOut = toProj.latlon2eastNorth(ll);
            double cOut1 = argSwitchOutput ? enOut.north() : enOut.east();
            double cOut2 = argSwitchOutput ? enOut.east() : enOut.north();
            System.out.println(Double.toString(cOut1) + " " + Double.toString(cOut2));
            System.out.flush();
        }
    }

    private CustomProjection createProjection(String params) throws ProjectionConfigurationException {
        CustomProjection proj = new CustomProjection();
        proj.update(params);
        return proj;
    }

    private EastNorth parseEastNorth(String s, Function<String, Double> parser) {
        String[] en = s.split("[;, ]+");
        if (en.length != 2)
            throw new IllegalArgumentException(tr("Expected two coordinates, separated by white space, found {0} in ''{1}''", en.length, s));
        double east = parser.apply(en[0]);
        double north = parser.apply(en[1]);
        if (this.argSwitchInput)
            return new EastNorth(north, east);
        else
            return new EastNorth(east, north);
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(tr("Unable to parse number ''{0}''", s), nfe);
        }
    }

    /**
     * Main class to run just the projection CLI.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        ProjectionCLI.INSTANCE.processArguments(args);
    }
}
