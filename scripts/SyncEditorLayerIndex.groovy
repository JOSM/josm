// License: GPL. For details, see LICENSE file.
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
 * $ groovy -cp ../dist/josm-custom.jar SyncEditorLayerIndex.groovy
 *
 * Add option "-h" to show the available command line flags.
 */
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonObject
import javax.json.JsonReader

import org.openstreetmap.josm.data.imagery.ImageryInfo
import org.openstreetmap.josm.data.imagery.Shape
import org.openstreetmap.josm.io.imagery.ImageryReader

class SyncEditorLayerIndex {

    List<ImageryInfo> josmEntries;
    JsonArray eliEntries;

    def eliUrls = new HashMap<String, JsonObject>()
    def josmUrls = new HashMap<String, ImageryInfo>()
    def josmMirrors = new HashMap<String, ImageryInfo>()

    static String eliInputFile = 'imagery.geojson'
    static String josmInputFile = 'maps.xml'
    static String ignoreInputFile = 'maps_ignores.txt'
    static FileWriter outputFile = null
    static BufferedWriter outputStream = null
    def skip = [:]

    static def options

    /**
     * Main method.
     */
    static main(def args) {
        parse_command_line_arguments(args)
        def script = new SyncEditorLayerIndex()
        script.loadSkip()
        script.start()
        script.loadJosmEntries()
        script.loadELIEntries()
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
        cli.e(longOpt:'eli_input', args:1, argName:"eli_input", "Input file for the editor layer index (geojson). Default is $eliInputFile (current directory).")
        cli.j(longOpt:'josm_input', args:1, argName:"josm_input", "Input file for the JOSM imagery list (xml). Default is $josmInputFile (current directory).")
        cli.i(longOpt:'ignore_input', args:1, argName:"ignore_input", "Input file for the ignore list. Default is $ignoreInputFile (current directory).")
        cli.s(longOpt:'shorten', "shorten the output, so it is easier to read in a console window")
        cli.n(longOpt:'noskip', argName:"noskip", "don't skip known entries")
        cli.x(longOpt:'xhtmlbody', argName:"xhtmlbody", "create XHTML body for display in a web page")
        cli.X(longOpt:'xhtml', argName:"xhtml", "create XHTML for display in a web page")
        cli.m(longOpt:'nomissingeli', argName:"nomissingeli", "don't show missing editor layer index entries")
        cli.h(longOpt:'help', "show this help")
        options = cli.parse(args)

        if (options.h) {
            cli.usage()
            System.exit(0)
        }
        if (options.eli_input) {
            eliInputFile = options.eli_input
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
            def res = (line =~ /^\|\| *(ELI|Ignore) *\|\| *\{\{\{(.+)\}\}\} *\|\|/)
            if(res.count)
            {
                if(res[0][1].equals("Ignore")) {
                    skip[res[0][2]] = "green"
                } else {
                    skip[res[0][2]] = "darkgoldenrod"
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
        if(skip.containsKey(s)) {
            String color = skip.get(s)
            skip.remove(s)
            if(options.xhtmlbody || options.xhtml) {
                s = "<pre style=\"margin:3px;color:"+color+"\">"+s.replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;")+"</pre>"
            }
            if (!options.noskip) {
                return;
            }
        } else if(options.xhtmlbody || options.xhtml) {
            String color = s.startsWith("***") ? "black" : ((s.startsWith("+ ") || s.startsWith("+++ ELI")) ? "blue" : "red")
            s = "<pre style=\"margin:3px;color:"+color+"\">"+s.replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;")+"</pre>"
        }
        myprintlnfinal(s)
    }

    void start() {
        if (options.xhtml) {
            myprintlnfinal "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
            myprintlnfinal "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/><title>JOSM - ELI differences</title></head><body>\n"
        }
    }

    void end() {
        for (def s: skip.keySet()) {
            myprintln "+++ Obsolete skip entry: " + s
        }
        if (options.xhtml) {
            myprintlnfinal "</body></html>\n"
        }
    }

    void loadELIEntries() {
        FileReader fr = new FileReader(eliInputFile)
        JsonReader jr = Json.createReader(fr)
        eliEntries = jr.readObject().get("features")
        jr.close()

        for (def e : eliEntries) {
            def url = getUrl(e)
            if (url.contains("{z}")) {
                myprintln "+++ ELI-URL uses {z} instead of {zoom}: "+url
                url = url.replace("{z}","{zoom}")
            }
            if (eliUrls.containsKey(url)) {
                myprintln "+++ ELI-URL is not unique: "+url
            } else {
                eliUrls.put(url, e)
            }
        }
        myprintln "*** Loaded ${eliEntries.size()} entries (ELI). ***"
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
                m.origName = m.getOriginalName().replaceAll(" mirror server( \\d+)?","")
                if (josmUrls.containsKey(url)) {
                    myprintln "+++ JOSM-Mirror-URL is not unique: "+url
                } else {
                    josmUrls.put(url, m)
                    josmMirrors.put(url, m)
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
        def l1 = inOneButNotTheOther(eliUrls, josmUrls)
        myprintln "*** URLs found in ELI but not in JOSM (${l1.size()}): ***"
        if (!l1.isEmpty()) {
            for (def l : l1) {
                myprintln "-" + l
            }
        }

        if (options.nomissingeli)
            return
        def l2 = inOneButNotTheOther(josmUrls, eliUrls)
        myprintln "*** URLs found in JOSM but not in ELI (${l2.size()}): ***"
        if (!l2.isEmpty()) {
            for (def l : l2) {
                myprintln "+" + l
            }
        }
    }

    void checkCommonEntries() {
        myprintln "*** Same URL, but different name: ***"
        for (def url : eliUrls.keySet()) {
            def e = eliUrls.get(url)
            if (!josmUrls.containsKey(url)) continue
            def j = josmUrls.get(url)
            if (!getName(e).equals(getName(j))) {
                myprintln "* Name differs ('${getName(e)}' != '${getName(j)}'): $url"
            }
        }

        myprintln "*** Same URL, but different type: ***"
        for (def url : eliUrls.keySet()) {
            def e = eliUrls.get(url)
            if (!josmUrls.containsKey(url)) continue
            def j = josmUrls.get(url)
            if (!getType(e).equals(getType(j))) {
                myprintln "* Type differs (${getType(e)} != ${getType(j)}): ${getName(j)} - $url"
            }
        }

        myprintln "*** Same URL, but different zoom bounds: ***"
        for (def url : eliUrls.keySet()) {
            def e = eliUrls.get(url)
            if (!josmUrls.containsKey(url)) continue
            def j = josmUrls.get(url)

            Integer eMinZoom = getMinZoom(e)
            Integer jMinZoom = getMinZoom(j)
            if (eMinZoom != jMinZoom  && !(eMinZoom == 0 && jMinZoom == null)) {
                myprintln "* Minzoom differs (${eMinZoom} != ${jMinZoom}): ${getDescription(j)}"
            }
            Integer eMaxZoom = getMaxZoom(e)
            Integer jMaxZoom = getMaxZoom(j)
            if (eMaxZoom != jMaxZoom) {
                myprintln "* Maxzoom differs (${eMaxZoom} != ${jMaxZoom}): ${getDescription(j)}"
            }
        }

        myprintln "*** Same URL, but different country code: ***"
        for (def url : eliUrls.keySet()) {
            def e = eliUrls.get(url)
            if (!josmUrls.containsKey(url)) continue
            def j = josmUrls.get(url)
            if (!getCountryCode(e).equals(getCountryCode(j))) {
                myprintln "* Country code differs (${getCountryCode(e)} != ${getCountryCode(j)}): ${getDescription(j)}"
            }
        }
        myprintln "*** Same URL, but different quality: ***"
        for (def url : eliUrls.keySet()) {
            def e = eliUrls.get(url)
            if (!josmUrls.containsKey(url)) {
              def q = getQuality(e)
              if("eli-best".equals(q)) {
                myprintln "- Quality best entry not in JOSM for ${getDescription(e)}"
              }
              continue
            }
            def j = josmUrls.get(url)
            if (!getQuality(e).equals(getQuality(j))) {
                myprintln "* Quality differs (${getQuality(e)} != ${getQuality(j)}): ${getDescription(j)}"
            }
        }
        myprintln "*** Same URL, but different dates: ***"
        for (def url : eliUrls.keySet()) {
            def ed = getDate(eliUrls.get(url))
            if (!josmUrls.containsKey(url)) continue
            def j = josmUrls.get(url)
            def jd = getDate(j)
            // The forms 2015;- or -;2015 or 2015;2015 are handled equal to 2015
            String ef = ed.replaceAll("\\A-;","").replaceAll(";-\\z","").replaceAll("\\A([0-9-]+);\\1\\z","\$1");
            // ELI has a strange and inconsistent used end_date definition, so we try again with subtraction by one
            String ed2 = ed;
            def reg = (ed =~ /^(.*;)(\d\d\d\d)(-(\d\d)(-(\d\d))?)?$/)
            if(reg != null && reg.count == 1) {
                Calendar cal = Calendar.getInstance();
                cal.set(reg[0][2] as Integer, reg[0][4] == null ? 0 : (reg[0][4] as Integer)-1, reg[0][6] == null ? 1 : reg[0][6] as Integer)
                cal.add(Calendar.DAY_OF_MONTH, -1)
                ed2 = reg[0][1] + cal.get(Calendar.YEAR)
                if (reg[0][4] != null)
                    ed2 += "-" + String.format("%02d", cal.get(Calendar.MONTH)+1)
                if (reg[0][6] != null)
                    ed2 += "-" + String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
            }
            String ef2 = ed2.replaceAll("\\A-;","").replaceAll(";-\\z","").replaceAll("\\A([0-9-]+);\\1\\z","\$1");
            if (!ed.equals(jd) && !ef.equals(jd) && !ed2.equals(jd) && !ef2.equals(jd)) {
                String t = "'${ed}'";
                if (!ed.equals(ef)) {
                    t += " or '${ef}'";
                }
                if (jd.isEmpty()) {
                    myprintln "- Missing JOSM date (${t}): ${getDescription(j)}"
                } else if (!ed.isEmpty()) {
                    myprintln "* Date differs (${t} != '${jd}'): ${getDescription(j)}"
                } else if (!options.nomissingeli) {
                    myprintln "+ Missing ELI date ('${jd}'): ${getDescription(j)}"
                }
            }
        }
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
        for (def url : eliUrls.keySet()) {
            def e = eliUrls.get(url)
            def num = 1
            def s = getShapes(e)
            for (def shape : s) {
                def p = shape.getPoints()
                if(!p[0].equals(p[p.size()-1]) && !options.nomissingeli) {
                    myprintln "+++ ELI shape $num unclosed: ${getDescription(e)}"
                }
                ++num
            }
            if (!josmUrls.containsKey(url)) {
                continue
            }
            def j = josmUrls.get(url)
            def js = getShapes(j)
            if(!s.size() && js.size()) {
                if(!options.nomissingeli) {
                    myprintln "+ No ELI shape: ${getDescription(j)}"
                }
            } else if(!js.size() && s.size()) {
                // don't report boundary like 5 point shapes as difference
                if (s.size() != 1 || s[0].getPoints().size() != 5) {
                    myprintln "- No JOSM shape: ${getDescription(j)}"
                }
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
        myprintln "*** Mismatching icons: ***"
        for (def url : eliUrls.keySet()) {
            def e = eliUrls.get(url)
            if (!josmUrls.containsKey(url)) {
                continue
            }
            def j = josmUrls.get(url)
            def ij = getIcon(j)
            def ie = getIcon(e)
            if(ij != null && ie == null) {
                if(!options.nomissingeli) {
                    myprintln "+ No ELI icon: ${getDescription(j)}"
                }
            } else if(ij == null && ie != null) {
                myprintln "- No JOSM icon: ${getDescription(j)}"
            } else if(!ij.equals(ie)) {
                myprintln "* Different icons: ${getDescription(j)}"
            }
        }
        myprintln "*** Miscellaneous checks: ***"
        def josmIds = new HashMap<String, ImageryInfo>()
        for (def url : josmUrls.keySet()) {
            def j = josmUrls.get(url)
            def id = getId(j)
            if(josmMirrors.containsKey(url)) {
                continue;
            }
            if(id == null) {
                myprintln "* No JOSM-ID: ${getDescription(j)}"
            } else if(josmIds.containsKey(id)) {
                myprintln "* JOSM-ID ${id} not unique: ${getDescription(j)}"
            } else {
                josmIds.put(id, j);
            }
            def d = getDate(j)
            if(!d.isEmpty()) {
                def reg = (d =~ /^(-|(\d\d\d\d)(-(\d\d)(-(\d\d))?)?)(;(-|(\d\d\d\d)(-(\d\d)(-(\d\d))?)?))?$/)
                if(reg == null || reg.count != 1) {
                    myprintln "* JOSM-Date '${d}' is strange: ${getDescription(j)}"
                } else {
                    try {
                        def first = verifyDate(reg[0][2],reg[0][4],reg[0][6]);
                        def second = verifyDate(reg[0][9],reg[0][11],reg[0][13]);
                        if(second.compareTo(first) < 0) {
                            myprintln "* JOSM-Date '${d}' is strange (second earlier than first): ${getDescription(j)}"
                        }
                    }
                    catch (Exception e) {
                        myprintln "* JOSM-Date '${d}' is strange (${e.getMessage()}): ${getDescription(j)}"
                    }
                }
            }
            def js = getShapes(j)
            if(js.size()) {
                def minlat = 1000;
                def minlon = 1000;
                def maxlat = -1000;
                def maxlon = -1000;
                for(def s: js) {
                    for(def p: s.getPoints()) {
                        def lat = p.getLat();
                        def lon = p.getLon();
                        if(lat > maxlat) maxlat = lat;
                        if(lon > maxlon) maxlon = lon;
                        if(lat < minlat) minlat = lat;
                        if(lon < minlon) minlon = lon;
                    }
                }
                def b = j.getBounds();
                if(b.getMinLat() != minlat || b.getMinLon() != minlon || b.getMaxLat() != maxlat || b.getMaxLon() != maxlon) {
                    myprintln "* Bounds do not match shape (is ${b.getMinLat()},${b.getMinLon()},${b.getMaxLat()},${b.getMaxLon()}, calculated <bounds min-lat='${minlat}' min-lon='${minlon}' max-lat='${maxlat}' max-lon='${maxlon}'>): ${getDescription(j)}"
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
    static String getDate(Object e) {
        if (e instanceof ImageryInfo) return e.date ? e.date : ""
        def p = e.get("properties")
        def start = p.containsKey("start_date") ? p.getString("start_date") : ""
        def end = p.containsKey("end_date") ? p.getString("end_date") : ""
        if(!start.isEmpty() && !end.isEmpty())
            return start+";"+end
        else if(!start.isEmpty())
            return start+";-"
        else if(!end.isEmpty())
            return "-;"+end
        return "";
    }
    static Date verifyDate(String year, String month, String day) {
        def date
        if(year == null) {
            date = "3000-01-01"
        } else {
            date = year + "-" + (month == null ? "01" : month) + "-" + (day == null ? "01" : day)
        }
        def df = new java.text.SimpleDateFormat("yyyy-MM-dd")
        df.setLenient(false)
        return df.parse(date)
    }
    static String getId(Object e) {
        if (e instanceof ImageryInfo) return e.getId()
        return e.get("properties").getString("id")
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
        if (e instanceof ImageryInfo) return e.isBestMarked() ? "eli-best" : null
        return (e.get("properties").containsKey("best")
            && e.get("properties").getBoolean("best")) ? "eli-best" : null
    }
    static String getIcon(Object e) {
        if (e instanceof ImageryInfo) return e.getIcon()
        return e.get("properties").getString("icon", null)
    }
    String getDescription(Object o) {
        def url = getUrl(o)
        def cc = getCountryCode(o)
        if (cc == null) {
            def j = josmUrls.get(url)
            if (j != null) cc = getCountryCode(j)
            if (cc == null) {
                def e = eliUrls.get(url)
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
