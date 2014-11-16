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
 * $ groovy -cp ../dist/josm-custom.jar sync_editor-imagery-index.groovy
 * 
 * Add option "-h" to show the available command line flags.
 */
import java.io.FileReader
import java.util.List

import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonObject
import javax.json.JsonReader

import org.openstreetmap.josm.io.imagery.ImageryReader
import org.openstreetmap.josm.data.imagery.ImageryInfo
import org.openstreetmap.josm.tools.Utils

class sync_editor_imagery_index {

    List<ImageryInfo> josmEntries;
    JsonArray eiiEntries;
    
    final static def EII_KNOWN_DUPLICATES = ["http://geolittoral.application.equipement.gouv.fr/wms/metropole?FORMAT=image/jpeg&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&Layers=ortholittorale&SRS={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}"]
    final static def JOSM_KNOWN_DUPLICATES = ["http://geolittoral.application.equipement.gouv.fr/wms/metropole?FORMAT=image/jpeg&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&Layers=ortholittorale&SRS={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}"]

    def eiiUrls = new HashMap<String, JsonObject>()
    def josmUrls = new HashMap<String, ImageryInfo>()
    
    static String eiiInputFile = 'imagery.json'
    static String josmInputFile = 'maps.xml'
    
    static def options
    
    /**
     * Main method.
     */
    static main(def args) {
        parse_command_line_arguments(args)
        def script = new sync_editor_imagery_index()
        script.loadJosmEntries()
        println "*** Loaded ${script.josmEntries.size()} entries (JOSM). ***"
        script.loadEIIEntries()
        println "*** Loaded ${script.eiiEntries.size()} entries (EII). ***"
        script.checkInOneButNotTheOther()
        script.checkCommonEntries()
    }
    
    /**
     * Parse command line arguments.
     */
    static void parse_command_line_arguments(args) {
        def cli = new CliBuilder()
        cli._(longOpt:'eii_input', args:1, argName:"eii_input", "Input file for the editor imagery index (json). Default is $eiiInputFile (current directory).")
        cli._(longOpt:'josm_input', args:1, argName:"josm_input", "Input file for the JOSM imagery list (xml). Default is $josmInputFile (current directory).")
        cli.s(longOpt:'shorten', "shorten the output, so it is easier to read in a console window")
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
    }

    void loadEIIEntries() {
        FileReader fr = new FileReader(eiiInputFile)
        JsonReader jr = Json.createReader(fr)
        eiiEntries = jr.readArray()
        jr.close()
        
        for (def e : eiiEntries) {
            def url = getUrl(e)
            if (eiiUrls.containsKey(url) && !EII_KNOWN_DUPLICATES.contains(url))
                throw new Exception("URL is not unique: "+url)
            eiiUrls.put(url, e)
        }
    }

    void loadJosmEntries() {
        def reader = new ImageryReader(josmInputFile)
        josmEntries = reader.parse()
        
        for (def e : josmEntries) {
            def url = getUrl(e)
            if (josmUrls.containsKey(url) && !JOSM_KNOWN_DUPLICATES.contains(url)) {
                throw new Exception("URL is not unique: "+url)
            }
            josmUrls.put(url, e)
        }
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
        println "*** URLs found in EII but not in JOSM (${l1.size()}): ***"
        if (l1.isEmpty()) {
            println "  -"
        } else {
            println Utils.join("\n", l1)
        }

        def l2 = inOneButNotTheOther(josmUrls, eiiUrls)
        println "*** URLs found in JOSM but not in EII (${l2.size()}): ***"
        if (l2.isEmpty()) {
            println "  -"
        } else {
            println Utils.join("\n", l2)
        }
    }
    
    void checkCommonEntries() {
        println "*** Same URL, but different name: ***"
        for (def url : eiiUrls.keySet()) {
            def e = eiiUrls.get(url)
            if (!josmUrls.containsKey(url)) continue
            def j = josmUrls.get(url)
            if (!getName(e).equals(getName(j))) {
                println "  name differs: $url"
                println "     (IEE):     ${getName(e)}"
                println "     (JOSM):    ${getName(j)}"
            }
        }
        
        println "*** Same URL, but different type: ***"
        for (def url : eiiUrls.keySet()) {
            def e = eiiUrls.get(url)
            if (!josmUrls.containsKey(url)) continue
            def j = josmUrls.get(url)
            if (!getType(e).equals(getType(j))) {
                println "  type differs: ${getName(j)} - $url"
                println "     (IEE):     ${getType(e)}"
                println "     (JOSM):    ${getType(j)}"
            }
        }
        
        println "*** Same URL, but different zoom bounds: ***"
        for (def url : eiiUrls.keySet()) {
            def e = eiiUrls.get(url)
            if (!josmUrls.containsKey(url)) continue
            def j = josmUrls.get(url)

            Integer eMinZoom = getMinZoom(e)
            Integer jMinZoom = getMinZoom(j)
            if (eMinZoom != jMinZoom) {
                println "  minzoom differs: ${getDescription(j)}"
                println "     (IEE):     ${eMinZoom}"
                println "     (JOSM):    ${jMinZoom}"
            }
            Integer eMaxZoom = getMaxZoom(e)
            Integer jMaxZoom = getMaxZoom(j)
            if (eMaxZoom != jMaxZoom) {
                println "  maxzoom differs: ${getDescription(j)}"
                println "     (IEE):     ${eMaxZoom}"
                println "     (JOSM):    ${jMaxZoom}"
            }
        }
        
        println "*** Same URL, but different country code: ***"
        for (def url : eiiUrls.keySet()) {
            def e = eiiUrls.get(url)
            if (!josmUrls.containsKey(url)) continue
            def j = josmUrls.get(url)
            if (!getCountryCode(e).equals(getCountryCode(j))) {
                println "  country code differs: ${getDescription(j)}"
                println "     (IEE):     ${getCountryCode(e)}"
                println "     (JOSM):    ${getCountryCode(j)}"
            }
        }
    }
    
    /**
     * Utility functions that allow uniform access for both ImageryInfo and JsonObject.
     */
    static String getUrl(Object e) {
        if (e instanceof ImageryInfo) return e.url
        return e.getString("url")
    }
    static String getName(Object e) {
        if (e instanceof ImageryInfo) return e.name
        return e.getString("name")
    }
    static String getType(Object e) {
        if (e instanceof ImageryInfo) return e.getImageryType().getTypeString()
        return e.getString("type")
    }
    static Integer getMinZoom(Object e) {
        if (e instanceof ImageryInfo) {
            int mz = e.getMinZoom()
            return mz == 0 ? null : mz
        } else {
            def ext = e.getJsonObject("extent")
            if (ext == null) return null
            def num = ext.getJsonNumber("min_zoom")
            if (num == null) return null
            return num.intValue()
        }
    }
    static Integer getMaxZoom(Object e) {
        if (e instanceof ImageryInfo) {
            int mz = e.getMaxZoom()
            return mz == 0 ? null : mz
        } else {
            def ext = e.getJsonObject("extent")
            if (ext == null) return null
            def num = ext.getJsonNumber("max_zoom")
            if (num == null) return null
            return num.intValue()
        }
    }
    static String getCountryCode(Object e) {
        if (e instanceof ImageryInfo) return "".equals(e.getCountryCode()) ? null : e.getCountryCode()
        return e.getString("country_code", null)
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
