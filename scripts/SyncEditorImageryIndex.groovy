// License: GPL. For details, see LICENSE file.
/**
 * Compare and analyse the differences of the editor imagery index and the JOSM imagery list.
 * The goal is to keep both lists in sync.
 *
 * The editor imagery index project (https://github.com/osmlab/editor-imagery-index)
 * provides also a version in the JOSM format, but the JSON is the original source
 * format, so we read that.
 *
 * How to run:
 * -----------
 *
 * Main JOSM binary needs to be in classpath, e.g.
 *
 * $ groovy -cp ../dist/josm-custom.jar SyncEditorImageryIndex.groovy
 *
 * Add option "-h" to show the available command line flags.
 */
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonObject
import javax.json.JsonReader
import javax.json.JsonValue

import org.openstreetmap.josm.data.imagery.ImageryInfo
import org.openstreetmap.josm.data.imagery.Shape
import org.openstreetmap.josm.io.imagery.ImageryReader

class SyncEditorImageryIndex {

    List<ImageryInfo> josmEntries;
    JsonArray eiiEntries;

    def eiiUrls = new HashMap<String, JsonObject>()
    def josmUrls = new HashMap<String, ImageryInfo>()

    static String eiiInputFile = 'imagery.geojson'
    static String josmInputFile = 'maps.xml'
    static String ignoreInputFile = 'maps_ignores.txt'
    static FileWriter outputFile = null
    static BufferedWriter outputStream = null
    int skipCount = 0;
    String skipColor = "greenyellow" // should never be visible
    def skipEntries = [:]
    def skipColors = [:]

    static def options

    /**
     * Main method.
     */
    static main(def args) {
        parse_command_line_arguments(args)
        def script = new SyncEditorImageryIndex()
        script.loadSkip()
        script.start()
        script.loadJosmEntries()
        script.loadEIIEntries()
        script.checkInOneButNotTheOther()
        script.checkCommonEntries()
        script.end()
        if(outputStream != null) {
            outputStream.close();
        }
        if(outputFile != null) {
            outputFile.close();
        }
    }

    /**
     * Parse command line arguments.
     */
    static void parse_command_line_arguments(args) {
        def cli = new CliBuilder(width: 160)
        cli.o(longOpt:'output', args:1, argName: "output", "Output file, - prints to stdout (default: -)")
        cli.e(longOpt:'eii_input', args:1, argName:"eii_input", "Input file for the editor imagery index (json). Default is $eiiInputFile (current directory).")
        cli.j(longOpt:'josm_input', args:1, argName:"josm_input", "Input file for the JOSM imagery list (xml). Default is $josmInputFile (current directory).")
        cli.i(longOpt:'ignore_input', args:1, argName:"ignore_input", "Input file for the ignore list. Default is $ignoreInputFile (current directory).")
        cli.s(longOpt:'shorten', "shorten the output, so it is easier to read in a console window")
        cli.n(longOpt:'noskip', argName:"noskip", "don't skip known entries")
        cli.x(longOpt:'xhtmlbody', argName:"xhtmlbody", "create XHTML body for display in a web page")
        cli.X(longOpt:'xhtml', argName:"xhtml", "create XHTML for display in a web page")
        cli.m(longOpt:'nomissingeii', argName:"nomissingeii", "don't show missing editor imagery index entries")
        cli.h(longOpt:'help', "show this help")
        options = cli.parse(args)

        if (options.h) {
            cli.usage()
            System.exit(0)
        }
        if (options.eii_input) {
            eiiInputFile = options.eii_input
        }
        if (options.josm_input) {
            josmInputFile = options.josm_input
        }
        if (options.ignore_input) {
            ignoreInputFile = options.ignore_input
        }
        if (options.output && options.output != "-") {
            outputFile = new FileWriter(options.output)
            outputStream = new BufferedWriter(outputFile)
        }
    }

    void loadSkip() {
        FileReader fr = new FileReader(ignoreInputFile)
        def line

        while((line = fr.readLine()) != null) {
            def res = (line =~ /^\|\| *(\d) *\|\| *(EII|Ignore) *\|\| *\{\{\{(.+)\}\}\} *\|\|/)
            if(res.count)
            {
                skipEntries[res[0][3]] = res[0][1] as int
                if(res[0][2].equals("Ignore")) {
                    skipColors[res[0][3]] = "green"
                } else {
                    skipColors[res[0][3]] = "darkgoldenrod"
                }
            }
        }
    }

    void myprintlnfinal(String s) {
        if(outputStream != null) {
            outputStream.write(s);
            outputStream.newLine();
        } else {
            println s;
        }
    }

    void myprintln(String s) {
        if(skipEntries.containsKey(s)) {
            skipCount = skipEntries.get(s)
            skipEntries.remove(s)
            if(skipColors.containsKey(s)) {
                skipColor = skipColors.get(s)
            } else {
                skipColor = "greenyellow"
            }
        }
        if(skipCount) {
            skipCount -= 1;
            if(options.xhtmlbody || options.xhtml) {
                s = "<pre style=\"margin:3px;color:"+skipColor+"\">"+s.replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;")+"</pre>"
            }
            if (!options.noskip) {
                return;
            }
        } else if(options.xhtmlbody || options.xhtml) {
            String color = s.startsWith("***") ? "black" : ((s.startsWith("+ ") || s.startsWith("+++ EII")) ? "blue" : "red")
            s = "<pre style=\"margin:3px;color:"+color+"\">"+s.replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;")+"</pre>"
        }
        myprintlnfinal(s)
    }

    void start() {
        if (options.xhtml) {
            myprintlnfinal "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
            myprintlnfinal "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/><title>JOSM - EII differences</title></head><body>\n"
        }
    }

    void end() {
        for (def s: skipEntries.keySet()) {
            myprintln "+++ Obsolete skip entry: " + s
        }
        if (options.xhtml) {
            myprintlnfinal "</body></html>\n"
        }
    }

    void loadEIIEntries() {
        FileReader fr = new FileReader(eiiInputFile)
        JsonReader jr = Json.createReader(fr)
        eiiEntries = jr.readObject().get("features")
        jr.close()

        for (def e : eiiEntries) {
            def url = getUrl(e)
            if (url.contains("{z}")) {
                myprintln "+++ EII-URL uses {z} instead of {zoom}: "+url
                url = url.replace("{z}","{zoom}")
            }
            if (eiiUrls.containsKey(url)) {
                myprintln "+++ EII-URL is not unique: "+url
            } else {
                eiiUrls.put(url, e)
            }
        }
        myprintln "*** Loaded ${eiiEntries.size()} entries (EII). ***"
    }

    void loadJosmEntries() {
        def reader = new ImageryReader(josmInputFile)
        josmEntries = reader.parse()

        for (def e : josmEntries) {
            def url = getUrl(e)
            if (url.contains("{z}")) {
                myprintln "+++ JOSM-URL uses {z} instead of {zoom}: "+url
                url = url.replace("{z}","{zoom}")
            }
            if (josmUrls.containsKey(url)) {
                myprintln "+++ JOSM-URL is not unique: "+url
            } else {
              josmUrls.put(url, e)
            }
            for (def m : e.getMirrors()) {
                url = getUrl(m)
                if (josmUrls.containsKey(url)) {
                    myprintln "+++ JOSM-Mirror-URL is not unique: "+url
                } else {
                  josmUrls.put(url, m)
                }
            }
        }
        myprintln "*** Loaded ${josmEntries.size()} entries (JOSM). ***"
    }

    List inOneButNotTheOther(Map m1, Map m2) {
        def l = []
        for (def url : m1.keySet()) {
            if (!m2.containsKey(url)) {
                def name = getName(m1.get(url))
                l += "  "+getDescription(m1.get(url))
            }
        }
        l.sort()
    }

    void checkInOneButNotTheOther() {
        def l1 = inOneButNotTheOther(eiiUrls, josmUrls)
        myprintln "*** URLs found in EII but not in JOSM (${l1.size()}): ***"
        if (!l1.isEmpty()) {
            for (def l : l1) {
                myprintln "-" + l
            }
        }

        if (options.nomissingeii)
            return
        def l2 = inOneButNotTheOther(josmUrls, eiiUrls)
        myprintln "*** URLs found in JOSM but not in EII (${l2.size()}): ***"
        if (!l2.isEmpty()) {
            for (def l : l2) {
                myprintln "+" + l
            }
        }
    }

    void checkCommonEntries() {
        myprintln "*** Same URL, but different name: ***"
        for (def url : eiiUrls.keySet()) {
            def e = eiiUrls.get(url)
            if (!josmUrls.containsKey(url)) continue
            def j = josmUrls.get(url)
            if (!getName(e).equals(getName(j))) {
                myprintln "  name differs: $url"
                myprintln "     (EII):     ${getName(e)}"
                myprintln "     (JOSM):    ${getName(j)}"
            }
        }

        myprintln "*** Same URL, but different type: ***"
        for (def url : eiiUrls.keySet()) {
            def e = eiiUrls.get(url)
            if (!josmUrls.containsKey(url)) continue
            def j = josmUrls.get(url)
            if (!getType(e).equals(getType(j))) {
                myprintln "  type differs: ${getName(j)} - $url"
                myprintln "     (EII):     ${getType(e)}"
                myprintln "     (JOSM):    ${getType(j)}"
            }
        }

        myprintln "*** Same URL, but different zoom bounds: ***"
        for (def url : eiiUrls.keySet()) {
            def e = eiiUrls.get(url)
            if (!josmUrls.containsKey(url)) continue
            def j = josmUrls.get(url)

            Integer eMinZoom = getMinZoom(e)
            Integer jMinZoom = getMinZoom(j)
            if (eMinZoom != jMinZoom  && !(eMinZoom == 0 && jMinZoom == null)) {
                myprintln "  minzoom differs: ${getDescription(j)}"
                myprintln "     (EII):     ${eMinZoom}"
                myprintln "     (JOSM):    ${jMinZoom}"
            }
            Integer eMaxZoom = getMaxZoom(e)
            Integer jMaxZoom = getMaxZoom(j)
            if (eMaxZoom != jMaxZoom) {
                myprintln "  maxzoom differs: ${getDescription(j)}"
                myprintln "     (EII):     ${eMaxZoom}"
                myprintln "     (JOSM):    ${jMaxZoom}"
            }
        }

        myprintln "*** Same URL, but different country code: ***"
        for (def url : eiiUrls.keySet()) {
            def e = eiiUrls.get(url)
            if (!josmUrls.containsKey(url)) continue
            def j = josmUrls.get(url)
            if (!getCountryCode(e).equals(getCountryCode(j))) {
                myprintln "  country code differs: ${getDescription(j)}"
                myprintln "     (EII):     ${getCountryCode(e)}"
                myprintln "     (JOSM):    ${getCountryCode(j)}"
            }
        }
        /*myprintln "*** Same URL, but different quality: ***"
        for (def url : eiiUrls.keySet()) {
            def e = eiiUrls.get(url)
            if (!josmUrls.containsKey(url)) {
              def q = getQuality(e)
              if("best".equals(q)) {
                myprintln "  quality best entry not in JOSM for ${getDescription(e)}"
              }
              continue
            }
            def j = josmUrls.get(url)
            if (!getQuality(e).equals(getQuality(j))) {
                myprintln "  quality differs: ${getDescription(j)}"
                myprintln "     (EII):     ${getQuality(e)}"
                myprintln "     (JOSM):    ${getQuality(j)}"
            }
        }*/
        myprintln "*** Mismatching shapes: ***"
        for (def url : josmUrls.keySet()) {
            def j = josmUrls.get(url)
            def num = 1
            for (def shape : getShapes(j)) {
                def p = shape.getPoints()
                if(!p[0].equals(p[p.size()-1])) {
                    myprintln "+++ JOSM shape $num unclosed: ${getDescription(j)}"
                }
                ++num
            }
        }
        for (def url : eiiUrls.keySet()) {
            def e = eiiUrls.get(url)
            def num = 1
            def s = getShapes(e)
            for (def shape : s) {
                def p = shape.getPoints()
                if(!p[0].equals(p[p.size()-1]) && !options.nomissingeii) {
                    myprintln "+++ EII shape $num unclosed: ${getDescription(e)}"
                }
                ++num
            }
            if (!josmUrls.containsKey(url)) {
                continue
            }
            def j = josmUrls.get(url)
            def js = getShapes(j)
            if(!s.size() && js.size()) {
                if(!options.nomissingeii) {
                    myprintln "+ No EII shape: ${getDescription(j)}"
                }
            } else if(!js.size() && s.size()) {
                myprintln "- No JOSM shape: ${getDescription(j)}"
            } else if(s.size() != js.size()) {
                myprintln "* Different number of shapes (${s.size()} != ${js.size()}): ${getDescription(j)}"
            } else {
                for(def nums = 0; nums < s.size(); ++nums) {
                    def ep = s[nums].getPoints()
                    def jp = js[nums].getPoints()
                    if(ep.size() != jp.size()) {
                        myprintln "* Different number of points for shape ${nums+1} (${ep.size()} ! = ${jp.size()})): ${getDescription(j)}"
                    } else {
                        for(def nump = 0; nump < ep.size(); ++nump) {
                            def ept = ep[nump]
                            def jpt = jp[nump]
                            if(Math.abs(ept.getLat()-jpt.getLat()) > 0.000001 || Math.abs(ept.getLon()-jpt.getLon()) > 0.000001) {
                                myprintln "* Different coordinate for point ${nump+1} of shape ${nums+1}: ${getDescription(j)}"
                                nump = ep.size()
                                num = s.size()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Utility functions that allow uniform access for both ImageryInfo and JsonObject.
     */
    static String getUrl(Object e) {
        if (e instanceof ImageryInfo) return e.url
        return e.get("properties").getString("url")
    }
    static String getName(Object e) {
        if (e instanceof ImageryInfo) return e.getOriginalName()
        return e.get("properties").getString("name")
    }
    static List<Shape> getShapes(Object e) {
        if (e instanceof ImageryInfo) {
            def bounds = e.getBounds();
            if(bounds != null) {
                return bounds.getShapes();
            }
            return []
        }
        if(!e.isNull("geometry")) {
            def ex = e.get("geometry")
            if(ex != null && !ex.isNull("coordinates")) {
                def poly = ex.get("coordinates")
                List<Shape> l = []
                for(def shapes: poly) {
                    def s = new Shape()
                    for(def point: shapes) {
                        def lon = point[0].toString()
                        def lat = point[1].toString()
                        s.addPoint(lat, lon)
                    }
                    l.add(s)
                }
                if (l.size() == 1 && l[0].getPoints().size() == 5) {
                    return [] // ignore a bounds equivalent shape
                }
                return l
            }
        }
        return []
    }
    static String getType(Object e) {
        if (e instanceof ImageryInfo) return e.getImageryType().getTypeString()
        return e.get("properties").getString("type")
    }
    static Integer getMinZoom(Object e) {
        if (e instanceof ImageryInfo) {
            int mz = e.getMinZoom()
            return mz == 0 ? null : mz
        } else {
            def num = e.get("properties").getJsonNumber("min_zoom")
            if (num == null) return null
            return num.intValue()
        }
    }
    static Integer getMaxZoom(Object e) {
        if (e instanceof ImageryInfo) {
            int mz = e.getMaxZoom()
            return mz == 0 ? null : mz
        } else {
            def num = e.get("properties").getJsonNumber("max_zoom")
            if (num == null) return null
            return num.intValue()
        }
    }
    static String getCountryCode(Object e) {
        if (e instanceof ImageryInfo) return "".equals(e.getCountryCode()) ? null : e.getCountryCode()
        return e.get("properties").getString("country_code", null)
    }
    static String getQuality(Object e) {
        //if (e instanceof ImageryInfo) return "".equals(e.getQuality()) ? null : e.getQuality()
        if (e instanceof ImageryInfo) return null
        return e.get("properties").get("best") ? "best" : null
    }
    String getDescription(Object o) {
        def url = getUrl(o)
        def cc = getCountryCode(o)
        if (cc == null) {
            def j = josmUrls.get(url)
            if (j != null) cc = getCountryCode(j)
            if (cc == null) {
                def e = eiiUrls.get(url)
                if (e != null) cc = getCountryCode(e)
            }
        }
        if (cc == null) {
            cc = ''
        } else {
            cc = "[$cc] "
        }
        def d = cc + getName(o) + " - " + getUrl(o)
        if (options.shorten) {
            def MAXLEN = 140
            if (d.length() > MAXLEN) d = d.substring(0, MAXLEN-1) + "..."
        }
        return d
    }
}
