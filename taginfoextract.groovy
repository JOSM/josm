// License: GPL. For details, see LICENSE file.
/**
 * Extracts tag information for the taginfo project.
 *
 * Run from the base directory of a JOSM checkout:
 *
 * groovy -cp dist/josm-custom.jar taginfoextract.groovy
 */

import java.io.BufferedReader
import java.util.ArrayList

import org.openstreetmap.josm.Main
import org.openstreetmap.josm.data.coor.LatLon
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.projection.Projections
import org.openstreetmap.josm.data.Version
import org.openstreetmap.josm.gui.mappaint.Cascade
import org.openstreetmap.josm.gui.mappaint.Environment
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.SimpleKeyValueCondition
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.GeneralSelector
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference
import org.openstreetmap.josm.gui.mappaint.MultiCascade
import org.openstreetmap.josm.io.CachedFile

basedir = "."

def cli = new CliBuilder(usage:'taginfoextract.groovy [options] [inputfile]',
    header:"Options:",
    footer:"[inputfile]  the file to process (optional, default is 'resource://styles/standard/elemstyles.mapcss')")
cli.o(args:1, argName: "file", "output file, - prints to stdout (default: -)")
cli._(longOpt:'svnrev', args:1, argName:"revision", "corresponding revision of the repository http://svn.openstreetmap.org/ (optional, current revision is fetched from the web if not given)")
cli.h(longOpt:'help', "show this help")
options = cli.parse(args)

if (options.h) {
    cli.usage()
    System.exit(0)
}
if (options.arguments().size() > 1) {
    System.err.println "Error: More than one input file given!"
    cli.usage()
    System.exit(-1)
}
if (options.svnrev) {
    assert Integer.parseInt(options.svnrev) > 0
}

Main.initApplicationPreferences()
Main.pref.enableSaveOnPut(false)
Main.setProjection(Projections.getProjectionByCode("EPSG:3857"))

josm_svn_revsion = Version.getInstance().getVersion()
assert josm_svn_revsion != Version.JOSM_UNKNOWN_VERSION

cached_svnrev = null
/**
 * Get revision for the repository http://svn.openstreetmap.org.
 */
def osm_svn_revision() {
    if (cached_svnrev != null) return cached_svnrev
    if (options.svnrev) {
        cached_svnrev = Integer.parseInt(options.svnrev)
        return cached_svnrev
    }
    //xml = "svn info --xml http://svn.openstreetmap.org/applications/share/map-icons/classic.small".execute().text
    xml = ("svn info --xml ${basedir}/images/styles/standard/").execute().text 
	
	def svninfo = new XmlParser().parseText(xml)
	def rev = svninfo.entry.'@revision'[0]
	cached_svnrev = Integer.parseInt(rev)
	assert cached_svnrev > 0
	return cached_svnrev
}

/**
 * Determine full image url (can refer to JOSM or OSM repository).
 */
def find_image_url(path) {
    def f = new File("${basedir}/images/styles/standard/${path}")
    if (f.exists()) {
        def rev = osm_svn_revision()
        return "http://trac.openstreetmap.org/export/${rev}/subversion/applications/share/map-icons/classic.small/${path}"
    }
    f = new File("${basedir}/images/${path}")
    if (f.exists()) {
        return "https://josm.openstreetmap.de/export/${josm_svn_revsion}/josm/trunk/images/${path}"
    }
    assert false, "Cannot find image url for ${path}"
}

def input_file
if (options.arguments().size() == 0) {
    input_file = "resource://styles/standard/elemstyles.mapcss"
} else {
    input_file = options.arguments()[0]
}


def file = new CachedFile(input_file)
def stream = file.getInputStream()
def parser = new MapCSSParser(stream, "UTF-8", MapCSSParser.LexicalState.DEFAULT)
def style_source = new MapCSSStyleSource("")
style_source.url = ""
parser.sheet(style_source)

def tags = [] as Set

for (rule in style_source.rules) {
    def selector = rule.selector
    if (selector instanceof GeneralSelector) {
        def conditions = selector.getConditions()
        for (cond in conditions) {
            if (cond instanceof SimpleKeyValueCondition) {
                if (selector.base == "node") {
                    tags.add([cond.k, cond.v])
                }
            }
        }
    }
}

output_file = null
if (options.o && options.o != "-") {
    output_file = new FileWriter(options.o)
}

def output(x) {
    if (output_file != null) {
        output_file.write(x)
    } else {
        print x
    }
}

datetime = new Date().format("yyyyMMdd'T'hhmmssZ")

output """{
  "data_format": 1,
  "data_url": "FIXME",
  "data_updated": "${datetime}",
  "project": {
    "name": "JOSM main mappaint style",
    "description": "Tags supported by the main mappaint style in the OSM editor JOSM",
    "project_url": "http://josm.openstreetmap.de/",
    "icon_url": "http://josm.openstreetmap.de/export/7543/josm/trunk/images/logo_16x16x8.png",
    "contact_name": "JOSM developer team",
    "contact_email": "josm-dev@openstreetmap.org"
  },
  "tags": [
"""

sep = ""
for (tag in tags) {
    def k = tag[0]
    def v = tag[1]
    def osm = new Node(new LatLon(0,0))
    osm.put(k, v)
    def mc = new MultiCascade()
    
    def env = new Environment(osm, mc, null, style_source)
    for (def r in style_source.rules) {
        env.clearSelectorMatchingInformation()
        env.layer = r.selector.getSubpart()
        if (r.selector.matches(env)) {
            // ignore selector range
            if (env.layer == null) {
                env.layer = "default"
            }
            r.execute(env)
        }
    }
    def c = mc.getCascade("default")
    def image = c.get("icon-image")
    if (image) {
        if (!(image instanceof IconReference)) continue
        def image_url = find_image_url(image.iconName)

        output """${sep}    {
                 |      "key": "${k}",
                 |      "value": "${v}",
                 |      "object_types": ["node"],
                 |      "icon_url": "${image_url}"
                 |    }""".stripMargin()
    sep = ",\n"
    }
}
output """
  ]
}
"""

if (output_file != null) {
    output_file.close()
}

System.exit(0)

