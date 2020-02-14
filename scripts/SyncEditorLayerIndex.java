// License: GPL. For details, see LICENSE file.
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryBounds;
import org.openstreetmap.josm.data.imagery.Shape;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.data.preferences.JosmUrls;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.data.validation.routines.DomainValidator;
import org.openstreetmap.josm.io.imagery.ImageryReader;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OptionParser;
import org.openstreetmap.josm.tools.OptionParser.OptionCount;
import org.openstreetmap.josm.tools.ReflectionUtils;
import org.xml.sax.SAXException;

/**
 * Compare and analyse the differences of the editor layer index and the JOSM imagery list.
 * The goal is to keep both lists in sync.
 *
 * The editor layer index project (https://github.com/osmlab/editor-layer-index)
 * provides also a version in the JOSM format, but the GEOJSON is the original source
 * format, so we read that.
 *
 * How to run:
 * -----------
 *
 * Main JOSM binary needs to be in classpath, e.g.
 *
 * $ java -cp ../dist/josm-custom.jar SyncEditorLayerIndex
 *
 * Add option "-h" to show the available command line flags.
 */
@SuppressWarnings("unchecked")
public class SyncEditorLayerIndex {

    private static final int MAXLEN = 140;

    private List<ImageryInfo> josmEntries;
    private JsonArray eliEntries;

    private final Map<String, JsonObject> eliUrls = new HashMap<>();
    private final Map<String, ImageryInfo> josmUrls = new HashMap<>();
    private final Map<String, ImageryInfo> josmMirrors = new HashMap<>();
    private static final Map<String, String> oldproj = new HashMap<>();
    private static final List<String> ignoreproj = new LinkedList<>();

    private static String eliInputFile = "imagery_eli.geojson";
    private static String josmInputFile = "imagery_josm.imagery.xml";
    private static String ignoreInputFile = "imagery_josm.ignores.txt";
    private static OutputStream outputFile;
    private static OutputStreamWriter outputStream;
    private static String optionOutput;
    private static boolean optionShorten;
    private static boolean optionNoSkip;
    private static boolean optionXhtmlBody;
    private static boolean optionXhtml;
    private static String optionEliXml;
    private static String optionJosmXml;
    private static String optionEncoding;
    private static boolean optionNoEli;
    private Map<String, String> skip = new HashMap<>();
    private Map<String, String> skipStart = new HashMap<>();

    /**
     * Main method.
     * @param args program arguments
     * @throws IOException if any I/O error occurs
     * @throws ReflectiveOperationException if any reflective operation error occurs
     * @throws SAXException if any SAX error occurs
     */
    public static void main(String[] args) throws IOException, SAXException, ReflectiveOperationException {
        Locale.setDefault(Locale.ROOT);
        parseCommandLineArguments(args);
        Config.setUrlsProvider(JosmUrls.getInstance());
        Preferences pref = new Preferences(JosmBaseDirectories.getInstance());
        Config.setPreferencesInstance(pref);
        pref.init(false);
        SyncEditorLayerIndex script = new SyncEditorLayerIndex();
        script.setupProj();
        script.loadSkip();
        script.start();
        script.loadJosmEntries();
        if (optionJosmXml != null) {
            try (OutputStreamWriter stream = new OutputStreamWriter(Files.newOutputStream(Paths.get(optionJosmXml)), UTF_8)) {
                script.printentries(script.josmEntries, stream);
            }
        }
        script.loadELIEntries();
        if (optionEliXml != null) {
            try (OutputStreamWriter stream = new OutputStreamWriter(Files.newOutputStream(Paths.get(optionEliXml)), UTF_8)) {
                script.printentries(script.eliEntries, stream);
            }
        }
        script.checkInOneButNotTheOther();
        script.checkCommonEntries();
        script.end();
        if (outputStream != null) {
            outputStream.close();
        }
        if (outputFile != null) {
            outputFile.close();
        }
    }

    /**
     * Displays help on the console
     */
    private static void showHelp() {
        System.out.println(getHelp());
        System.exit(0);
    }

    static String getHelp() {
        return "usage: java -cp build SyncEditorLayerIndex\n" +
        "-c,--encoding <encoding>           output encoding (defaults to UTF-8 or cp850 on Windows)\n" +
        "-e,--eli_input <eli_input>         Input file for the editor layer index (geojson). " +
                                            "Default is imagery_eli.geojson (current directory).\n" +
        "-h,--help                          show this help\n" +
        "-i,--ignore_input <ignore_input>   Input file for the ignore list. Default is imagery_josm.ignores.txt (current directory).\n" +
        "-j,--josm_input <josm_input>       Input file for the JOSM imagery list (xml). " +
                                            "Default is imagery_josm.imagery.xml (current directory).\n" +
        "-m,--noeli                         don't show output for ELI problems\n" +
        "-n,--noskip                        don't skip known entries\n" +
        "-o,--output <output>               Output file, - prints to stdout (default: -)\n" +
        "-p,--elixml <elixml>               ELI entries for use in JOSM as XML file (incomplete)\n" +
        "-q,--josmxml <josmxml>             JOSM entries reoutput as XML file (incomplete)\n" +
        "-s,--shorten                       shorten the output, so it is easier to read in a console window\n" +
        "-x,--xhtmlbody                     create XHTML body for display in a web page\n" +
        "-X,--xhtml                         create XHTML for display in a web page\n";
    }

    /**
     * Parse command line arguments.
     * @param args program arguments
     * @throws IOException in case of I/O error
     */
    static void parseCommandLineArguments(String[] args) throws IOException {
        new OptionParser("JOSM/ELI synchronization script")
                .addFlagParameter("help", SyncEditorLayerIndex::showHelp)
                .addShortAlias("help", "h")
                .addArgumentParameter("output", OptionCount.OPTIONAL, x -> optionOutput = x)
                .addShortAlias("output", "o")
                .addArgumentParameter("eli_input", OptionCount.OPTIONAL, x -> eliInputFile = x)
                .addShortAlias("eli_input", "e")
                .addArgumentParameter("josm_input", OptionCount.OPTIONAL, x -> josmInputFile = x)
                .addShortAlias("josm_input", "j")
                .addArgumentParameter("ignore_input", OptionCount.OPTIONAL, x -> ignoreInputFile = x)
                .addShortAlias("ignore_input", "i")
                .addFlagParameter("shorten", () -> optionShorten = true)
                .addShortAlias("shorten", "s")
                .addFlagParameter("noskip", () -> optionNoSkip = true)
                .addShortAlias("noskip", "n")
                .addFlagParameter("xhtmlbody", () -> optionXhtmlBody = true)
                .addShortAlias("xhtmlbody", "x")
                .addFlagParameter("xhtml", () -> optionXhtml = true)
                .addShortAlias("xhtml", "X")
                .addArgumentParameter("elixml", OptionCount.OPTIONAL, x -> optionEliXml = x)
                .addShortAlias("elixml", "p")
                .addArgumentParameter("josmxml", OptionCount.OPTIONAL, x -> optionJosmXml = x)
                .addShortAlias("josmxml", "q")
                .addFlagParameter("noeli", () -> optionNoEli = true)
                .addShortAlias("noeli", "m")
                .addArgumentParameter("encoding", OptionCount.OPTIONAL, x -> optionEncoding = x)
                .addShortAlias("encoding", "c")
                .parseOptionsOrExit(Arrays.asList(args));

        if (optionOutput != null && !"-".equals(optionOutput)) {
            outputFile = Files.newOutputStream(Paths.get(optionOutput));
            outputStream = new OutputStreamWriter(outputFile, optionEncoding != null ? optionEncoding : "UTF-8");
        } else if (optionEncoding != null) {
            outputStream = new OutputStreamWriter(System.out, optionEncoding);
        }
    }

    void setupProj() {
        oldproj.put("EPSG:3359", "EPSG:3404");
        oldproj.put("EPSG:3785", "EPSG:3857");
        oldproj.put("EPSG:31297", "EPGS:31287");
        oldproj.put("EPSG:31464", "EPSG:31468");
        oldproj.put("EPSG:54004", "EPSG:3857");
        oldproj.put("EPSG:102100", "EPSG:3857");
        oldproj.put("EPSG:102113", "EPSG:3857");
        oldproj.put("EPSG:900913", "EPGS:3857");
        ignoreproj.add("EPSG:4267");
        ignoreproj.add("EPSG:5221");
        ignoreproj.add("EPSG:5514");
        ignoreproj.add("EPSG:32019");
        ignoreproj.add("EPSG:102066");
        ignoreproj.add("EPSG:102067");
        ignoreproj.add("EPSG:102685");
        ignoreproj.add("EPSG:102711");
    }

    void loadSkip() throws IOException {
        final Pattern pattern = Pattern.compile("^\\|\\| *(ELI|Ignore) *\\|\\| *\\{\\{\\{(.+)\\}\\}\\} *\\|\\|");
        try (BufferedReader fr = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(ignoreInputFile)), UTF_8))) {
            String line;

            while ((line = fr.readLine()) != null) {
                Matcher res = pattern.matcher(line);
                if (res.matches()) {
                    String s = res.group(2);
                    if (s.endsWith("...")) {
                        s = s.substring(0, s.length() - 3);
                        if ("Ignore".equals(res.group(1))) {
                            skipStart.put(s, "green");
                        } else {
                            skipStart.put(s, "darkgoldenrod");
                        }
                    } else {
                        if ("Ignore".equals(res.group(1))) {
                            skip.put(s, "green");
                        } else {
                            skip.put(s, "darkgoldenrod");
                        }
                    }
                }
            }
        }
    }

    void myprintlnfinal(String s) {
        if (outputStream != null) {
            try {
                outputStream.write(s + System.getProperty("line.separator"));
            } catch (IOException e) {
                throw new JosmRuntimeException(e);
            }
        } else {
            System.out.println(s);
        }
    }

    String isSkipString(String s) {
        if (skip.containsKey(s))
            return skip.get(s);
        for (Entry<String, String> str : skipStart.entrySet()) {
            if (s.startsWith(str.getKey()))
                return str.getValue();
        }
        return null;
    }

    void myprintln(String s) {
        String color;
        if ((color = isSkipString(s)) != null) {
            skip.remove(s);
            if (optionXhtmlBody || optionXhtml) {
                s = "<pre style=\"margin:3px;color:"+color+"\">"
                        + s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")+"</pre>";
            }
            if (!optionNoSkip) {
                return;
            }
        } else if (optionXhtmlBody || optionXhtml) {
            color =
                    s.startsWith("***") ? "black" :
                        ((s.startsWith("+ ") || s.startsWith("+++ ELI")) ? "blue" :
                            (s.startsWith("#") ? "indigo" :
                                (s.startsWith("!") ? "orange" : "red")));
            s = "<pre style=\"margin:3px;color:"+color+"\">"+s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")+"</pre>";
        }
        if ((s.startsWith("+ ") || s.startsWith("+++ ELI") || s.startsWith("#")) && optionNoEli) {
            return;
        }
        myprintlnfinal(s);
    }

    void start() {
        if (optionXhtml) {
            myprintlnfinal(
                    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
            myprintlnfinal(
                    "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>"+
                    "<title>JOSM - ELI differences</title></head><body>\n");
        }
    }

    void end() {
        for (String s : skip.keySet()) {
            myprintln("+++ Obsolete skip entry: " + s);
        }
        if (optionXhtml) {
            myprintlnfinal("</body></html>\n");
        }
    }

    void loadELIEntries() throws IOException {
        try (JsonReader jr = Json.createReader(new InputStreamReader(Files.newInputStream(Paths.get(eliInputFile)), UTF_8))) {
            eliEntries = jr.readObject().getJsonArray("features");
        }

        for (JsonValue e : eliEntries) {
            String url = getUrlStripped(e);
            if (url.contains("{z}")) {
                myprintln("+++ ELI-URL uses {z} instead of {zoom}: "+url);
                url = url.replace("{z}", "{zoom}");
            }
            if (eliUrls.containsKey(url)) {
                myprintln("+++ ELI-URL is not unique: "+url);
            } else {
                eliUrls.put(url, e.asJsonObject());
            }
            JsonArray s = e.asJsonObject().get("properties").asJsonObject().getJsonArray("available_projections");
            if (s != null) {
                String urlLc = url.toLowerCase(Locale.ENGLISH);
                List<String> old = new LinkedList<>();
                for (JsonValue p : s) {
                    String proj = ((JsonString) p).getString();
                    if (oldproj.containsKey(proj) || ("CRS:84".equals(proj) && !urlLc.contains("version=1.3"))) {
                        old.add(proj);
                    }
                }
                if (!old.isEmpty()) {
                    myprintln("+ ELI Projections "+String.join(", ", old)+" not useful: "+getDescription(e));
                }
            }
        }
        myprintln("*** Loaded "+eliEntries.size()+" entries (ELI). ***");
    }

    String cdata(String s) {
        return cdata(s, false);
    }

    String cdata(String s, boolean escape) {
        if (escape) {
            return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        } else if (s.matches(".*[<>&].*"))
            return "<![CDATA["+s+"]]>";
        return s;
    }

    String maininfo(Object entry, String offset) {
        String t = getType(entry);
        String res = offset + "<type>"+t+"</type>\n";
        res += offset + "<url>"+cdata(getUrl(entry))+"</url>\n";
        if (getMinZoom(entry) != null)
            res += offset + "<min-zoom>"+getMinZoom(entry)+"</min-zoom>\n";
        if (getMaxZoom(entry) != null)
            res += offset + "<max-zoom>"+getMaxZoom(entry)+"</max-zoom>\n";
        if ("wms".equals(t)) {
            List<String> p = getProjections(entry);
            if (p != null) {
                res += offset + "<projections>\n";
                for (String c : p) {
                    res += offset + "    <code>"+c+"</code>\n";
                }
                res += offset + "</projections>\n";
            }
        }
        return res;
    }

    void printentries(List<?> entries, OutputStreamWriter stream) throws IOException {
        DecimalFormat df = new DecimalFormat("#.#######");
        df.setRoundingMode(java.math.RoundingMode.CEILING);
        stream.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        stream.write("<imagery xmlns=\"http://josm.openstreetmap.de/maps-1.0\">\n");
        for (Object e : entries) {
            stream.write("    <entry"
                + ("eli-best".equals(getQuality(e)) ? " eli-best=\"true\"" : "")
                + (getOverlay(e) ? " overlay=\"true\"" : "")
                + ">\n");
            String t;
            if (isNotBlank(t = getName(e)))
                stream.write("        <name>"+cdata(t, true)+"</name>\n");
            if (isNotBlank(t = getId(e)))
                stream.write("        <id>"+t+"</id>\n");
            if (isNotBlank(t = getCategory(e)))
                stream.write("        <category>"+t+"</category>\n");
            if (isNotBlank(t = getDate(e)))
                stream.write("        <date>"+t+"</date>\n");
            if (isNotBlank(t = getCountryCode(e)))
                stream.write("        <country-code>"+t+"</country-code>\n");
            if ((getDefault(e)))
                stream.write("        <default>true</default>\n");
            stream.write(maininfo(e, "        "));
            if (isNotBlank(t = getAttributionText(e)))
                stream.write("        <attribution-text mandatory=\"true\">"+cdata(t, true)+"</attribution-text>\n");
            if (isNotBlank(t = getAttributionUrl(e)))
                stream.write("        <attribution-url>"+cdata(t)+"</attribution-url>\n");
            if (isNotBlank(t = getLogoImage(e)))
                stream.write("        <logo-image>"+cdata(t, true)+"</logo-image>\n");
            if (isNotBlank(t = getLogoUrl(e)))
                stream.write("        <logo-url>"+cdata(t)+"</logo-url>\n");
            if (isNotBlank(t = getTermsOfUseText(e)))
                stream.write("        <terms-of-use-text>"+cdata(t, true)+"</terms-of-use-text>\n");
            if (isNotBlank(t = getTermsOfUseUrl(e)))
                stream.write("        <terms-of-use-url>"+cdata(t)+"</terms-of-use-url>\n");
            if (isNotBlank(t = getPermissionReferenceUrl(e)))
                stream.write("        <permission-ref>"+cdata(t)+"</permission-ref>\n");
            if ((getValidGeoreference(e)))
                stream.write("        <valid-georeference>true</valid-georeference>\n");
            if (isNotBlank(t = getIcon(e)))
                stream.write("        <icon>"+cdata(t)+"</icon>\n");
            for (Entry<String, String> d : getDescriptions(e).entrySet()) {
                stream.write("        <description lang=\""+d.getKey()+"\">"+d.getValue()+"</description>\n");
            }
            for (ImageryInfo m : getMirrors(e)) {
                stream.write("        <mirror>\n"+maininfo(m, "            ")+"        </mirror>\n");
            }
            double minlat = 1000;
            double minlon = 1000;
            double maxlat = -1000;
            double maxlon = -1000;
            String shapes = "";
            String sep = "\n            ";
            try {
                for (Shape s: getShapes(e)) {
                    shapes += "            <shape>";
                    int i = 0;
                    for (Coordinate p: s.getPoints()) {
                        double lat = p.getLat();
                        double lon = p.getLon();
                        if (lat > maxlat) maxlat = lat;
                        if (lon > maxlon) maxlon = lon;
                        if (lat < minlat) minlat = lat;
                        if (lon < minlon) minlon = lon;
                        if ((i++ % 3) == 0) {
                            shapes += sep + "    ";
                        }
                        shapes += "<point lat='"+df.format(lat)+"' lon='"+df.format(lon)+"'/>";
                    }
                    shapes += sep + "</shape>\n";
                }
            } catch (IllegalArgumentException ignored) {
                Logging.trace(ignored);
            }
            if (!shapes.isEmpty()) {
                stream.write("        <bounds min-lat='"+df.format(minlat)
                                          +"' min-lon='"+df.format(minlon)
                                          +"' max-lat='"+df.format(maxlat)
                                          +"' max-lon='"+df.format(maxlon)+"'>\n");
                stream.write(shapes + "        </bounds>\n");
            }
            stream.write("    </entry>\n");
        }
        stream.write("</imagery>\n");
        stream.close();
    }

    void loadJosmEntries() throws IOException, SAXException, ReflectiveOperationException {
        try (ImageryReader reader = new ImageryReader(josmInputFile)) {
            josmEntries = reader.parse();
        }

        for (ImageryInfo e : josmEntries) {
            if (isBlank(getUrl(e))) {
                myprintln("+++ JOSM-Entry without URL: " + getDescription(e));
                continue;
            }
            if (isBlank(getName(e))) {
                myprintln("+++ JOSM-Entry without Name: " + getDescription(e));
                continue;
            }
            String url = getUrlStripped(e);
            if (url.contains("{z}")) {
                myprintln("+++ JOSM-URL uses {z} instead of {zoom}: "+url);
                url = url.replace("{z}", "{zoom}");
            }
            if (josmUrls.containsKey(url)) {
                myprintln("+++ JOSM-URL is not unique: "+url);
            } else {
                josmUrls.put(url, e);
            }
            for (ImageryInfo m : e.getMirrors()) {
                url = getUrlStripped(m);
                Field origNameField = ImageryInfo.class.getDeclaredField("origName");
                ReflectionUtils.setObjectsAccessible(origNameField);
                origNameField.set(m, m.getOriginalName().replaceAll(" mirror server( \\d+)?", ""));
                if (josmUrls.containsKey(url)) {
                    myprintln("+++ JOSM-Mirror-URL is not unique: "+url);
                } else {
                    josmUrls.put(url, m);
                    josmMirrors.put(url, m);
                }
            }
        }
        myprintln("*** Loaded "+josmEntries.size()+" entries (JOSM). ***");
    }

    void checkInOneButNotTheOther() {
        List<String> le = new LinkedList<>(eliUrls.keySet());
        List<String> lj = new LinkedList<>(josmUrls.keySet());

        List<String> ke = new LinkedList<>(le);
        for (String url : ke) {
            if (lj.contains(url)) {
                le.remove(url);
                lj.remove(url);
            }
        }

        if (!le.isEmpty() && !lj.isEmpty()) {
            ke = new LinkedList<>(le);
            for (String urle : ke) {
                JsonObject e = eliUrls.get(urle);
                String ide = getId(e);
                String urlhttps = urle.replace("http:", "https:");
                if (lj.contains(urlhttps)) {
                    myprintln("+ Missing https: "+getDescription(e));
                    eliUrls.put(urlhttps, eliUrls.get(urle));
                    eliUrls.remove(urle);
                    le.remove(urle);
                    lj.remove(urlhttps);
                } else if (isNotBlank(ide)) {
                    List<String> kj = new LinkedList<>(lj);
                    for (String urlj : kj) {
                        ImageryInfo j = josmUrls.get(urlj);
                        String idj = getId(j);

                        if (ide.equals(idj) && Objects.equals(getType(j), getType(e))) {
                            myprintln("* URL for id "+idj+" differs ("+urle+"): "+getDescription(j));
                            le.remove(urle);
                            lj.remove(urlj);
                            // replace key for this entry with JOSM URL
                            eliUrls.remove(urle);
                            eliUrls.put(urlj, e);
                            break;
                        }
                    }
                }
            }
        }

        myprintln("*** URLs found in ELI but not in JOSM ("+le.size()+"): ***");
        Collections.sort(le);
        if (!le.isEmpty()) {
            for (String l : le) {
                myprintln("-  " + getDescription(eliUrls.get(l)));
            }
        }
        myprintln("*** URLs found in JOSM but not in ELI ("+lj.size()+"): ***");
        Collections.sort(lj);
        if (!lj.isEmpty()) {
            for (String l : lj) {
                myprintln("+  " + getDescription(josmUrls.get(l)));
            }
        }
    }

    void checkCommonEntries() {
        doSameUrlButDifferentName();
        doSameUrlButDifferentId();
        doSameUrlButDifferentType();
        doSameUrlButDifferentZoomBounds();
        doSameUrlButDifferentCountryCode();
        doSameUrlButDifferentQuality();
        doSameUrlButDifferentDates();
        doSameUrlButDifferentInformation();
        doMismatchingShapes();
        doMismatchingIcons();
        doMismatchingCategories();
        doMiscellaneousChecks();
    }

    void doSameUrlButDifferentName() {
        myprintln("*** Same URL, but different name: ***");
        for (String url : eliUrls.keySet()) {
            JsonObject e = eliUrls.get(url);
            if (!josmUrls.containsKey(url)) continue;
            ImageryInfo j = josmUrls.get(url);
            String ename = getName(e).replace("'", "\u2019");
            String jname = getName(j).replace("'", "\u2019");
            if (!ename.equals(jname)) {
                myprintln("* Name differs ('"+getName(e)+"' != '"+getName(j)+"'): "+getUrl(j));
            }
        }
    }

    void doSameUrlButDifferentId() {
        myprintln("*** Same URL, but different Id: ***");
        for (String url : eliUrls.keySet()) {
            JsonObject e = eliUrls.get(url);
            if (!josmUrls.containsKey(url)) continue;
            ImageryInfo j = josmUrls.get(url);
            String ename = getId(e);
            String jname = getId(j);
            if (!Objects.equals(ename, jname)) {
                myprintln("# Id differs ('"+getId(e)+"' != '"+getId(j)+"'): "+getUrl(j));
            }
        }
    }

    void doSameUrlButDifferentType() {
        myprintln("*** Same URL, but different type: ***");
        for (String url : eliUrls.keySet()) {
            JsonObject e = eliUrls.get(url);
            if (!josmUrls.containsKey(url)) continue;
            ImageryInfo j = josmUrls.get(url);
            if (!Objects.equals(getType(e), getType(j))) {
                myprintln("* Type differs ("+getType(e)+" != "+getType(j)+"): "+getName(j)+" - "+getUrl(j));
            }
        }
    }

    void doSameUrlButDifferentZoomBounds() {
        myprintln("*** Same URL, but different zoom bounds: ***");
        for (String url : eliUrls.keySet()) {
            JsonObject e = eliUrls.get(url);
            if (!josmUrls.containsKey(url)) continue;
            ImageryInfo j = josmUrls.get(url);

            Integer eMinZoom = getMinZoom(e);
            Integer jMinZoom = getMinZoom(j);
            /* dont warn for entries copied from the base of the mirror */
            if (eMinZoom == null && "wms".equals(getType(j)) && j.getName().contains(" mirror"))
                jMinZoom = null;
            if (!Objects.equals(eMinZoom, jMinZoom) && !(Objects.equals(eMinZoom, 0) && jMinZoom == null)) {
                myprintln("* Minzoom differs ("+eMinZoom+" != "+jMinZoom+"): "+getDescription(j));
            }
            Integer eMaxZoom = getMaxZoom(e);
            Integer jMaxZoom = getMaxZoom(j);
            /* dont warn for entries copied from the base of the mirror */
            if (eMaxZoom == null && "wms".equals(getType(j)) && j.getName().contains(" mirror"))
                jMaxZoom = null;
            if (!Objects.equals(eMaxZoom, jMaxZoom)) {
                myprintln("* Maxzoom differs ("+eMaxZoom+" != "+jMaxZoom+"): "+getDescription(j));
            }
        }
    }

    void doSameUrlButDifferentCountryCode() {
        myprintln("*** Same URL, but different country code: ***");
        for (String url : eliUrls.keySet()) {
            JsonObject e = eliUrls.get(url);
            if (!josmUrls.containsKey(url)) continue;
            ImageryInfo j = josmUrls.get(url);
            String cce = getCountryCode(e);
            if ("ZZ".equals(cce)) { /* special ELI country code */
                cce = null;
            }
            if (cce != null && !cce.equals(getCountryCode(j))) {
                myprintln("* Country code differs ("+getCountryCode(e)+" != "+getCountryCode(j)+"): "+getDescription(j));
            }
        }
    }

    void doSameUrlButDifferentQuality() {
        myprintln("*** Same URL, but different quality: ***");
        for (String url : eliUrls.keySet()) {
            JsonObject e = eliUrls.get(url);
            if (!josmUrls.containsKey(url)) {
              String q = getQuality(e);
              if ("eli-best".equals(q)) {
                  myprintln("- Quality best entry not in JOSM for "+getDescription(e));
              }
              continue;
            }
            ImageryInfo j = josmUrls.get(url);
            if (!Objects.equals(getQuality(e), getQuality(j))) {
                myprintln("* Quality differs ("+getQuality(e)+" != "+getQuality(j)+"): "+getDescription(j));
            }
        }
    }

    void doSameUrlButDifferentDates() {
        myprintln("*** Same URL, but different dates: ***");
        Pattern pattern = Pattern.compile("^(.*;)(\\d\\d\\d\\d)(-(\\d\\d)(-(\\d\\d))?)?$");
        for (String url : eliUrls.keySet()) {
            String ed = getDate(eliUrls.get(url));
            if (!josmUrls.containsKey(url)) continue;
            ImageryInfo j = josmUrls.get(url);
            String jd = getDate(j);
            // The forms 2015;- or -;2015 or 2015;2015 are handled equal to 2015
            String ef = ed.replaceAll("\\A-;", "").replaceAll(";-\\z", "").replaceAll("\\A([0-9-]+);\\1\\z", "$1");
            // ELI has a strange and inconsistent used end_date definition, so we try again with subtraction by one
            String ed2 = ed;
            Matcher m = pattern.matcher(ed);
            if (m.matches()) {
                Calendar cal = Calendar.getInstance();
                cal.set(Integer.valueOf(m.group(2)),
                        m.group(4) == null ? 0 : Integer.valueOf(m.group(4))-1,
                        m.group(6) == null ? 1 : Integer.valueOf(m.group(6)));
                cal.add(Calendar.DAY_OF_MONTH, -1);
                ed2 = m.group(1) + cal.get(Calendar.YEAR);
                if (m.group(4) != null)
                    ed2 += "-" + String.format("%02d", cal.get(Calendar.MONTH)+1);
                if (m.group(6) != null)
                    ed2 += "-" + String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
            }
            String ef2 = ed2.replaceAll("\\A-;", "").replaceAll(";-\\z", "").replaceAll("\\A([0-9-]+);\\1\\z", "$1");
            if (!ed.equals(jd) && !ef.equals(jd) && !ed2.equals(jd) && !ef2.equals(jd)) {
                String t = "'"+ed+"'";
                if (!ed.equals(ef)) {
                    t += " or '"+ef+"'";
                }
                if (jd.isEmpty()) {
                    myprintln("- Missing JOSM date ("+t+"): "+getDescription(j));
                } else if (!ed.isEmpty()) {
                    myprintln("* Date differs ('"+t+"' != '"+jd+"'): "+getDescription(j));
                } else if (!optionNoEli) {
                    myprintln("+ Missing ELI date ('"+jd+"'): "+getDescription(j));
                }
            }
        }
    }

    void doSameUrlButDifferentInformation() {
        myprintln("*** Same URL, but different information: ***");
        for (String url : eliUrls.keySet()) {
            if (!josmUrls.containsKey(url)) continue;
            JsonObject e = eliUrls.get(url);
            ImageryInfo j = josmUrls.get(url);

            compareDescriptions(e, j);
            comparePermissionReferenceUrls(e, j);
            compareAttributionUrls(e, j);
            compareAttributionTexts(e, j);
            compareProjections(e, j);
            compareDefaults(e, j);
            compareOverlays(e, j);
            compareNoTileHeaders(e, j);
        }
    }

    void compareDescriptions(JsonObject e, ImageryInfo j) {
        String et = getDescriptions(e).getOrDefault("en", "");
        String jt = getDescriptions(j).getOrDefault("en", "");
        if (!et.equals(jt)) {
            if (jt.isEmpty()) {
                myprintln("- Missing JOSM description ("+et+"): "+getDescription(j));
            } else if (!et.isEmpty()) {
                myprintln("* Description differs ('"+et+"' != '"+jt+"'): "+getDescription(j));
            } else if (!optionNoEli) {
                myprintln("+ Missing ELI description ('"+jt+"'): "+getDescription(j));
            }
        }
    }

    void comparePermissionReferenceUrls(JsonObject e, ImageryInfo j) {
        String et = getPermissionReferenceUrl(e);
        String jt = getPermissionReferenceUrl(j);
        String jt2 = getTermsOfUseUrl(j);
        if (isBlank(jt)) jt = jt2;
        if (!Objects.equals(et, jt)) {
            if (isBlank(jt)) {
                myprintln("- Missing JOSM license URL ("+et+"): "+getDescription(j));
            } else if (isNotBlank(et)) {
                String ethttps = et.replace("http:", "https:");
                if (isBlank(jt2) || !(jt2.equals(ethttps) || jt2.equals(et+"/") || jt2.equals(ethttps+"/"))) {
                    if (jt.equals(ethttps) || jt.equals(et+"/") || jt.equals(ethttps+"/")) {
                        myprintln("+ License URL differs ('"+et+"' != '"+jt+"'): "+getDescription(j));
                    } else {
                        String ja = getAttributionUrl(j);
                        if (ja != null && (ja.equals(et) || ja.equals(ethttps) || ja.equals(et+"/") || ja.equals(ethttps+"/"))) {
                           myprintln("+ ELI License URL in JOSM Attribution: "+getDescription(j));
                        } else {
                            myprintln("* License URL differs ('"+et+"' != '"+jt+"'): "+getDescription(j));
                        }
                    }
                }
            } else if (!optionNoEli) {
                myprintln("+ Missing ELI license URL ('"+jt+"'): "+getDescription(j));
            }
        }
    }

    void compareAttributionUrls(JsonObject e, ImageryInfo j) {
        String et = getAttributionUrl(e);
        String jt = getAttributionUrl(j);
        if (!Objects.equals(et, jt)) {
            if (isBlank(jt)) {
                myprintln("- Missing JOSM attribution URL ("+et+"): "+getDescription(j));
            } else if (isNotBlank(et)) {
                String ethttps = et.replace("http:", "https:");
                if (jt.equals(ethttps) || jt.equals(et+"/") || jt.equals(ethttps+"/")) {
                    myprintln("+ Attribution URL differs ('"+et+"' != '"+jt+"'): "+getDescription(j));
                } else {
                    myprintln("* Attribution URL differs ('"+et+"' != '"+jt+"'): "+getDescription(j));
                }
            } else if (!optionNoEli) {
                myprintln("+ Missing ELI attribution URL ('"+jt+"'): "+getDescription(j));
            }
        }
    }

    void compareAttributionTexts(JsonObject e, ImageryInfo j) {
        String et = getAttributionText(e);
        String jt = getAttributionText(j);
        if (!Objects.equals(et, jt)) {
            if (isBlank(jt)) {
                myprintln("- Missing JOSM attribution text ("+et+"): "+getDescription(j));
            } else if (isNotBlank(et)) {
                myprintln("* Attribution text differs ('"+et+"' != '"+jt+"'): "+getDescription(j));
            } else if (!optionNoEli) {
                myprintln("+ Missing ELI attribution text ('"+jt+"'): "+getDescription(j));
            }
        }
    }

    void compareProjections(JsonObject e, ImageryInfo j) {
        String et = getProjections(e).stream().sorted().collect(Collectors.joining(" "));
        String jt = getProjections(j).stream().sorted().collect(Collectors.joining(" "));
        if (!Objects.equals(et, jt)) {
            if (isBlank(jt)) {
                String t = getType(e);
                if ("wms_endpoint".equals(t) || "tms".equals(t)) {
                    myprintln("+ ELI projections for type "+t+": "+getDescription(j));
                } else {
                    myprintln("- Missing JOSM projections ("+et+"): "+getDescription(j));
                }
            } else if (isNotBlank(et)) {
                if ("EPSG:3857 EPSG:4326".equals(et) || "EPSG:3857".equals(et) || "EPSG:4326".equals(et)) {
                    myprintln("+ ELI has minimal projections ('"+et+"' != '"+jt+"'): "+getDescription(j));
                } else {
                    myprintln("* Projections differ ('"+et+"' != '"+jt+"'): "+getDescription(j));
                }
            } else if (!optionNoEli && !"tms".equals(getType(e))) {
                myprintln("+ Missing ELI projections ('"+jt+"'): "+getDescription(j));
            }
        }
    }

    void compareDefaults(JsonObject e, ImageryInfo j) {
        boolean ed = getDefault(e);
        boolean jd = getDefault(j);
        if (ed != jd) {
            if (!jd) {
                myprintln("- Missing JOSM default: "+getDescription(j));
            } else if (!optionNoEli) {
                myprintln("+ Missing ELI default: "+getDescription(j));
            }
        }
    }

    void compareOverlays(JsonObject e, ImageryInfo j) {
        boolean eo = getOverlay(e);
        boolean jo = getOverlay(j);
        if (eo != jo) {
            if (!jo) {
                myprintln("- Missing JOSM overlay flag: "+getDescription(j));
            } else if (!optionNoEli) {
                myprintln("+ Missing ELI overlay flag: "+getDescription(j));
            }
        }
    }

    void compareNoTileHeaders(JsonObject e, ImageryInfo j) {
        Map<String, Set<String>> eh = getNoTileHeader(e);
        Map<String, Set<String>> jh = getNoTileHeader(j);
        if (!Objects.equals(eh, jh)) {
            if (jh == null || jh.isEmpty()) {
                myprintln("- Missing JOSM no tile headers ("+eh+"): "+getDescription(j));
            } else if (eh != null && !eh.isEmpty()) {
                myprintln("* No tile headers differ ('"+eh+"' != '"+jh+"'): "+getDescription(j));
            } else if (!optionNoEli) {
                myprintln("+ Missing ELI no tile headers ('"+jh+"'): "+getDescription(j));
            }
        }
    }

    void doMismatchingShapes() {
        myprintln("*** Mismatching shapes: ***");
        for (String url : josmUrls.keySet()) {
            ImageryInfo j = josmUrls.get(url);
            int num = 1;
            for (Shape shape : getShapes(j)) {
                List<Coordinate> p = shape.getPoints();
                if (!p.get(0).equals(p.get(p.size()-1))) {
                    myprintln("+++ JOSM shape "+num+" unclosed: "+getDescription(j));
                }
                for (int nump = 1; nump < p.size(); ++nump) {
                    if (Objects.equals(p.get(nump-1), p.get(nump))) {
                        myprintln("+++ JOSM shape "+num+" double point at "+(nump-1)+": "+getDescription(j));
                    }
                }
                ++num;
            }
        }
        for (String url : eliUrls.keySet()) {
            JsonObject e = eliUrls.get(url);
            int num = 1;
            List<Shape> s = null;
            try {
                s = getShapes(e);
                for (Shape shape : s) {
                    List<Coordinate> p = shape.getPoints();
                    if (!p.get(0).equals(p.get(p.size()-1)) && !optionNoEli) {
                        myprintln("+++ ELI shape "+num+" unclosed: "+getDescription(e));
                    }
                    for (int nump = 1; nump < p.size(); ++nump) {
                        if (Objects.equals(p.get(nump-1), p.get(nump))) {
                            myprintln("+++ ELI shape "+num+" double point at "+(nump-1)+": "+getDescription(e));
                        }
                    }
                    ++num;
                }
            } catch (IllegalArgumentException err) {
                String desc = getDescription(e);
                myprintln("* Invalid data in ELI geometry for "+desc+": "+err.getMessage());
            }
            if (s == null || !josmUrls.containsKey(url)) {
                continue;
            }
            ImageryInfo j = josmUrls.get(url);
            List<Shape> js = getShapes(j);
            if (s.isEmpty() && !js.isEmpty()) {
                if (!optionNoEli) {
                    myprintln("+ No ELI shape: "+getDescription(j));
                }
            } else if (js.isEmpty() && !s.isEmpty()) {
                // don't report boundary like 5 point shapes as difference
                if (s.size() != 1 || s.get(0).getPoints().size() != 5) {
                    myprintln("- No JOSM shape: "+getDescription(j));
                }
            } else if (s.size() != js.size()) {
                myprintln("* Different number of shapes ("+s.size()+" != "+js.size()+"): "+getDescription(j));
            } else {
                boolean[] edone = new boolean[s.size()];
                boolean[] jdone = new boolean[js.size()];
                for (int enums = 0; enums < s.size(); ++enums) {
                    List<Coordinate> ep = s.get(enums).getPoints();
                    for (int jnums = 0; jnums < js.size() && !edone[enums]; ++jnums) {
                        List<Coordinate> jp = js.get(jnums).getPoints();
                        if (ep.size() == jp.size() && !jdone[jnums]) {
                            boolean err = false;
                            for (int nump = 0; nump < ep.size() && !err; ++nump) {
                                Coordinate ept = ep.get(nump);
                                Coordinate jpt = jp.get(nump);
                                if (Math.abs(ept.getLat()-jpt.getLat()) > 0.00001 || Math.abs(ept.getLon()-jpt.getLon()) > 0.00001)
                                    err = true;
                            }
                            if (!err) {
                                edone[enums] = true;
                                jdone[jnums] = true;
                                break;
                            }
                        }
                    }
                }
                for (int enums = 0; enums < s.size(); ++enums) {
                    List<Coordinate> ep = s.get(enums).getPoints();
                    for (int jnums = 0; jnums < js.size() && !edone[enums]; ++jnums) {
                        List<Coordinate> jp = js.get(jnums).getPoints();
                        if (ep.size() == jp.size() && !jdone[jnums]) {
                            boolean err = false;
                            for (int nump = 0; nump < ep.size() && !err; ++nump) {
                                Coordinate ept = ep.get(nump);
                                Coordinate jpt = jp.get(nump);
                                if (Math.abs(ept.getLat()-jpt.getLat()) > 0.00001 || Math.abs(ept.getLon()-jpt.getLon()) > 0.00001) {
                                    String numtxt = Integer.toString(enums+1);
                                    if (enums != jnums) {
                                        numtxt += '/' + Integer.toString(jnums+1);
                                    }
                                    myprintln("* Different coordinate for point "+(nump+1)+" of shape "+numtxt+": "+getDescription(j));
                                    break;
                                }
                            }
                            edone[enums] = true;
                            jdone[jnums] = true;
                            break;
                        }
                    }
                }
                for (int enums = 0; enums < s.size(); ++enums) {
                    List<Coordinate> ep = s.get(enums).getPoints();
                    for (int jnums = 0; jnums < js.size() && !edone[enums]; ++jnums) {
                        List<Coordinate> jp = js.get(jnums).getPoints();
                        if (!jdone[jnums]) {
                            String numtxt = Integer.toString(enums+1);
                            if (enums != jnums) {
                                numtxt += '/' + Integer.toString(jnums+1);
                            }
                            myprintln("* Different number of points for shape "+numtxt+" ("+ep.size()+" ! = "+jp.size()+"): "
                                    + getDescription(j));
                            edone[enums] = true;
                            jdone[jnums] = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    void doMismatchingIcons() {
        myprintln("*** Mismatching icons: ***");
        doMismatching(this::compareIcons);
    }

    void doMismatchingCategories() {
        myprintln("*** Mismatching categories: ***");
        doMismatching(this::compareCategories);
    }

    void doMismatching(BiConsumer<ImageryInfo, JsonObject> comparator) {
        for (String url : eliUrls.keySet()) {
            if (josmUrls.containsKey(url)) {
                comparator.accept(josmUrls.get(url), eliUrls.get(url));
            }
        }
    }

    void compareIcons(ImageryInfo j, JsonObject e) {
        String ij = getIcon(j);
        String ie = getIcon(e);
        boolean ijok = isNotBlank(ij);
        boolean ieok = isNotBlank(ie);
        if (ijok && !ieok) {
            if (!optionNoEli) {
                myprintln("+ No ELI icon: "+getDescription(j));
            }
        } else if (!ijok && ieok) {
            myprintln("- No JOSM icon: "+getDescription(j));
        } else if (ijok && ieok && !Objects.equals(ij, ie) && !(
          (ie.startsWith("https://osmlab.github.io/editor-layer-index/")
          || ie.startsWith("https://raw.githubusercontent.com/osmlab/editor-layer-index/")) &&
          ij.startsWith("data:"))) {
            String iehttps = ie.replace("http:", "https:");
            if (ij.equals(iehttps)) {
                myprintln("+ Different icons: "+getDescription(j));
            } else {
                myprintln("* Different icons: "+getDescription(j));
            }
        }
    }

    void compareCategories(ImageryInfo j, JsonObject e) {
        String cj = getCategory(j);
        String ce = getCategory(e);
        boolean cjok = isNotBlank(cj);
        boolean ceok = isNotBlank(ce);
        if (cjok && !ceok) {
            if (!optionNoEli) {
                myprintln("+ No ELI category: "+getDescription(j));
            }
        } else if (!cjok && ceok) {
            myprintln("- No JOSM category: "+getDescription(j));
        } else if (cjok && ceok && !Objects.equals(cj, ce)) {
            myprintln("* Different categories ('"+ce+"' != '"+cj+"'): "+getDescription(j));
        }
    }

    void doMiscellaneousChecks() {
        myprintln("*** Miscellaneous checks: ***");
        Map<String, ImageryInfo> josmIds = new HashMap<>();
        Collection<String> all = Projections.getAllProjectionCodes();
        DomainValidator dv = DomainValidator.getInstance();
        for (String url : josmUrls.keySet()) {
            ImageryInfo j = josmUrls.get(url);
            String id = getId(j);
            if ("wms".equals(getType(j))) {
                String urlLc = url.toLowerCase(Locale.ENGLISH);
                if (getProjections(j).isEmpty()) {
                    myprintln("* WMS without projections: "+getDescription(j));
                } else {
                    List<String> unsupported = new LinkedList<>();
                    List<String> old = new LinkedList<>();
                    for (String p : getProjectionsUnstripped(j)) {
                        if ("CRS:84".equals(p)) {
                            if (!urlLc.contains("version=1.3")) {
                                myprintln("* CRS:84 without WMS 1.3: "+getDescription(j));
                            }
                        } else if (oldproj.containsKey(p)) {
                            old.add(p);
                        } else if (!all.contains(p) && !ignoreproj.contains(p)) {
                            unsupported.add(p);
                        }
                    }
                    if (!unsupported.isEmpty()) {
                        myprintln("* Projections "+String.join(", ", unsupported)+" not supported by JOSM: "+getDescription(j));
                    }
                    for (String o : old) {
                        myprintln("* Projection "+o+" is an old unsupported code and has been replaced by "+oldproj.get(o)+": "
                                + getDescription(j));
                    }
                }
                if (urlLc.contains("version=1.3") && !urlLc.contains("crs={proj}")) {
                    myprintln("* WMS 1.3 with strange CRS specification: "+getDescription(j));
                } else if (urlLc.contains("version=1.1") && !urlLc.contains("srs={proj}")) {
                    myprintln("* WMS 1.1 with strange SRS specification: "+getDescription(j));
                }
            }
            List<String> urls = new LinkedList<>();
            if (!"scanex".equals(getType(j))) {
                urls.add(url);
            }
            String jt = getPermissionReferenceUrl(j);
            if (isNotBlank(jt) && !"Public Domain".equalsIgnoreCase(jt))
                urls.add(jt);
            jt = getTermsOfUseUrl(j);
            if (isNotBlank(jt))
                urls.add(jt);
            jt = getAttributionUrl(j);
            if (isNotBlank(jt))
                urls.add(jt);
            jt = getIcon(j);
            if (isNotBlank(jt)) {
                if (!jt.startsWith("data:image/"))
                    urls.add(jt);
                else {
                    try {
                        new ImageProvider(jt).get();
                    } catch (RuntimeException e) {
                        myprintln("* Strange Icon: "+getDescription(j));
                    }
                }
            }
            Pattern patternU = Pattern.compile("^https?://([^/]+?)(:\\d+)?(/.*)?");
            for (String u : urls) {
                if (!patternU.matcher(u).matches() || u.matches(".*[ \t]+$")) {
                    myprintln("* Strange URL '"+u+"': "+getDescription(j));
                } else {
                    try {
                        URL jurl = new URL(u.replaceAll("\\{switch:[^\\}]*\\}", "x"));
                        String domain = jurl.getHost();
                        int port = jurl.getPort();
                        if (!(domain.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) && !dv.isValid(domain))
                            myprintln("* Strange Domain '"+domain+"': "+getDescription(j));
                        else if (80 == port || 443 == port) {
                            myprintln("* Useless port '"+port+"': "+getDescription(j));
                        }
                    } catch (MalformedURLException e) {
                        myprintln("* Malformed URL '"+u+"': "+getDescription(j)+" => "+e.getMessage());
                    }
                }
            }

            if (josmMirrors.containsKey(url)) {
                continue;
            }
            if (isBlank(id)) {
                myprintln("* No JOSM-ID: "+getDescription(j));
            } else if (josmIds.containsKey(id)) {
                myprintln("* JOSM-ID "+id+" not unique: "+getDescription(j));
            } else {
                josmIds.put(id, j);
            }
            String d = getDate(j);
            if (isNotBlank(d)) {
                Pattern patternD = Pattern.compile("^(-|(\\d\\d\\d\\d)(-(\\d\\d)(-(\\d\\d))?)?)(;(-|(\\d\\d\\d\\d)(-(\\d\\d)(-(\\d\\d))?)?))?$");
                Matcher m = patternD.matcher(d);
                if (!m.matches()) {
                    myprintln("* JOSM-Date '"+d+"' is strange: "+getDescription(j));
                } else {
                    try {
                        Date first = verifyDate(m.group(2), m.group(4), m.group(6));
                        Date second = verifyDate(m.group(9), m.group(11), m.group(13));
                        if (second.compareTo(first) < 0) {
                            myprintln("* JOSM-Date '"+d+"' is strange (second earlier than first): "+getDescription(j));
                        }
                    } catch (Exception e) {
                        myprintln("* JOSM-Date '"+d+"' is strange ("+e.getMessage()+"): "+getDescription(j));
                    }
                }
            }
            if (isNotBlank(getAttributionUrl(j)) && isBlank(getAttributionText(j))) {
                myprintln("* Attribution link without text: "+getDescription(j));
            }
            if (isNotBlank(getLogoUrl(j)) && isBlank(getLogoImage(j))) {
                myprintln("* Logo link without image: "+getDescription(j));
            }
            if (isNotBlank(getTermsOfUseText(j)) && isBlank(getTermsOfUseUrl(j))) {
                myprintln("* Terms of Use text without link: "+getDescription(j));
            }
            List<Shape> js = getShapes(j);
            if (!js.isEmpty()) {
                double minlat = 1000;
                double minlon = 1000;
                double maxlat = -1000;
                double maxlon = -1000;
                for (Shape s: js) {
                    for (Coordinate p: s.getPoints()) {
                        double lat = p.getLat();
                        double lon = p.getLon();
                        if (lat > maxlat) maxlat = lat;
                        if (lon > maxlon) maxlon = lon;
                        if (lat < minlat) minlat = lat;
                        if (lon < minlon) minlon = lon;
                    }
                }
                ImageryBounds b = j.getBounds();
                if (b.getMinLat() != minlat || b.getMinLon() != minlon || b.getMaxLat() != maxlat || b.getMaxLon() != maxlon) {
                    myprintln("* Bounds do not match shape (is "+b.getMinLat()+","+b.getMinLon()+","+b.getMaxLat()+","+b.getMaxLon()
                        + ", calculated <bounds min-lat='"+minlat+"' min-lon='"+minlon+"' max-lat='"+maxlat+"' max-lon='"+maxlon+"'>): "
                        + getDescription(j));
                }
            }
            List<String> knownCategories = Arrays.asList(
                    "photo", "elevation", "map", "historicmap", "osmbasedmap", "historicphoto", "qa", "other");
            String cat = getCategory(j);
            if (isBlank(cat)) {
                myprintln("* No category: "+getDescription(j));
            } else if (!knownCategories.contains(cat)) {
                myprintln("* Strange category "+cat+": "+getDescription(j));
            }
        }
    }

    /*
     * Utility functions that allow uniform access for both ImageryInfo and JsonObject.
     */

    static String getUrl(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).getUrl();
        return ((Map<String, JsonObject>) e).get("properties").getString("url");
    }

    static String getUrlStripped(Object e) {
        return getUrl(e).replaceAll("\\?(apikey|access_token)=.*", "");
    }

    static String getDate(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).getDate() != null ? ((ImageryInfo) e).getDate() : "";
        JsonObject p = ((Map<String, JsonObject>) e).get("properties");
        String start = p.containsKey("start_date") ? p.getString("start_date") : "";
        String end = p.containsKey("end_date") ? p.getString("end_date") : "";
        if (!start.isEmpty() && !end.isEmpty())
            return start+";"+end;
        else if (!start.isEmpty())
            return start+";-";
        else if (!end.isEmpty())
            return "-;"+end;
        return "";
    }

    static Date verifyDate(String year, String month, String day) throws ParseException {
        String date;
        if (year == null) {
            date = "3000-01-01";
        } else {
            date = year + "-" + (month == null ? "01" : month) + "-" + (day == null ? "01" : day);
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setLenient(false);
        return df.parse(date);
    }

    static String getId(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).getId();
        return ((Map<String, JsonObject>) e).get("properties").getString("id");
    }

    static String getName(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).getOriginalName();
        return ((Map<String, JsonObject>) e).get("properties").getString("name");
    }

    static List<ImageryInfo> getMirrors(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).getMirrors();
        return Collections.emptyList();
    }

    static List<String> getProjections(Object e) {
        List<String> r = new ArrayList<>();
        List<String> u = getProjectionsUnstripped(e);
        if (u != null) {
            for (String p : u) {
                if (!oldproj.containsKey(p) && !("CRS:84".equals(p) && !(getUrlStripped(e).matches("(?i)version=1\\.3")))) {
                    r.add(p);
                }
            }
        }
        return r;
    }

    static List<String> getProjectionsUnstripped(Object e) {
        List<String> r = null;
        if (e instanceof ImageryInfo) {
            r = ((ImageryInfo) e).getServerProjections();
        } else {
            JsonValue s = ((Map<String, JsonObject>) e).get("properties").get("available_projections");
            if (s != null) {
                r = new ArrayList<>();
                for (JsonValue p : s.asJsonArray()) {
                    r.add(((JsonString) p).getString());
                }
            }
        }
        return r != null ? r : Collections.emptyList();
    }

    static List<Shape> getShapes(Object e) {
        if (e instanceof ImageryInfo) {
            ImageryBounds bounds = ((ImageryInfo) e).getBounds();
            if (bounds != null) {
                return bounds.getShapes();
            }
            return Collections.emptyList();
        }
        JsonValue ex = ((Map<String, JsonValue>) e).get("geometry");
        if (ex != null && !JsonValue.NULL.equals(ex) && !ex.asJsonObject().isNull("coordinates")) {
            JsonArray poly = ex.asJsonObject().getJsonArray("coordinates");
            List<Shape> l = new ArrayList<>();
            for (JsonValue shapes: poly) {
                Shape s = new Shape();
                for (JsonValue point: shapes.asJsonArray()) {
                    String lon = point.asJsonArray().getJsonNumber(0).toString();
                    String lat = point.asJsonArray().getJsonNumber(1).toString();
                    s.addPoint(lat, lon);
                }
                l.add(s);
            }
            return l;
        }
        return Collections.emptyList();
    }

    static String getType(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).getImageryType().getTypeString();
        return ((Map<String, JsonObject>) e).get("properties").getString("type");
    }

    static Integer getMinZoom(Object e) {
        if (e instanceof ImageryInfo) {
            int mz = ((ImageryInfo) e).getMinZoom();
            return mz == 0 ? null : mz;
        } else {
            JsonNumber num = ((Map<String, JsonObject>) e).get("properties").getJsonNumber("min_zoom");
            if (num == null) return null;
            return num.intValue();
        }
    }

    static Integer getMaxZoom(Object e) {
        if (e instanceof ImageryInfo) {
            int mz = ((ImageryInfo) e).getMaxZoom();
            return mz == 0 ? null : mz;
        } else {
            JsonNumber num = ((Map<String, JsonObject>) e).get("properties").getJsonNumber("max_zoom");
            if (num == null) return null;
            return num.intValue();
        }
    }

    static String getCountryCode(Object e) {
        if (e instanceof ImageryInfo) return "".equals(((ImageryInfo) e).getCountryCode()) ? null : ((ImageryInfo) e).getCountryCode();
        return ((Map<String, JsonObject>) e).get("properties").getString("country_code", null);
    }

    static String getQuality(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).isBestMarked() ? "eli-best" : null;
        return (((Map<String, JsonObject>) e).get("properties").containsKey("best")
            && ((Map<String, JsonObject>) e).get("properties").getBoolean("best")) ? "eli-best" : null;
    }

    static boolean getOverlay(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).isOverlay();
        return (((Map<String, JsonObject>) e).get("properties").containsKey("overlay")
            && ((Map<String, JsonObject>) e).get("properties").getBoolean("overlay"));
    }

    static String getIcon(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).getIcon();
        return ((Map<String, JsonObject>) e).get("properties").getString("icon", null);
    }

    static String getAttributionText(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).getAttributionText(0, null, null);
        try {
            return ((Map<String, JsonObject>) e).get("properties").getJsonObject("attribution").getString("text", null);
        } catch (NullPointerException ex) {
            return null;
        }
    }

    static String getAttributionUrl(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).getAttributionLinkURL();
        try {
            return ((Map<String, JsonObject>) e).get("properties").getJsonObject("attribution").getString("url", null);
        } catch (NullPointerException ex) {
            return null;
        }
    }

    static String getTermsOfUseText(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).getTermsOfUseText();
        return null;
    }

    static String getTermsOfUseUrl(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).getTermsOfUseURL();
        return null;
    }

    static String getCategory(Object e) {
        if (e instanceof ImageryInfo) {
            return ((ImageryInfo) e).getImageryCategoryOriginalString();
        }
        return ((Map<String, JsonObject>) e).get("properties").getString("category", null);
    }

    static String getLogoImage(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).getAttributionImageRaw();
        return null;
    }

    static String getLogoUrl(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).getAttributionImageURL();
        return null;
    }

    static String getPermissionReferenceUrl(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).getPermissionReferenceURL();
        return ((Map<String, JsonObject>) e).get("properties").getString("license_url", null);
    }

    static Map<String, Set<String>> getNoTileHeader(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).getNoTileHeaders();
        JsonObject nth = ((Map<String, JsonObject>) e).get("properties").getJsonObject("no_tile_header");
        return nth == null ? null : nth.keySet().stream().collect(Collectors.toMap(
                Function.identity(),
                k -> nth.getJsonArray(k).stream().map(x -> ((JsonString) x).getString()).collect(Collectors.toSet())));
    }

    static Map<String, String> getDescriptions(Object e) {
        Map<String, String> res = new HashMap<>();
        if (e instanceof ImageryInfo) {
            String a = ((ImageryInfo) e).getDescription();
            if (a != null) res.put("en", a);
        } else {
            String a = ((Map<String, JsonObject>) e).get("properties").getString("description", null);
            if (a != null) res.put("en", a.replaceAll("''", "'"));
        }
        return res;
    }

    static boolean getValidGeoreference(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).isGeoreferenceValid();
        return false;
    }

    static boolean getDefault(Object e) {
        if (e instanceof ImageryInfo) return ((ImageryInfo) e).isDefaultEntry();
        return ((Map<String, JsonObject>) e).get("properties").getBoolean("default", false);
    }

    String getDescription(Object o) {
        String url = getUrl(o);
        String cc = getCountryCode(o);
        if (cc == null) {
            ImageryInfo j = josmUrls.get(url);
            if (j != null) cc = getCountryCode(j);
            if (cc == null) {
                JsonObject e = eliUrls.get(url);
                if (e != null) cc = getCountryCode(e);
            }
        }
        if (cc == null) {
            cc = "";
        } else {
            cc = "["+cc+"] ";
        }
        String d = cc + getName(o) + " - " + getUrl(o);
        if (optionShorten) {
            if (d.length() > MAXLEN) d = d.substring(0, MAXLEN-1) + "...";
        }
        return d;
    }
}
