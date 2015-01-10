// License: GPL. For details, see LICENSE file.
/**
 * Extracts tag information for the taginfo project.
 *
 * Run from the base directory of a JOSM checkout:
 *
 * groovy -cp dist/josm-custom.jar taginfoextract.groovy
 */
import java.awt.image.BufferedImage

import javax.imageio.ImageIO

import org.openstreetmap.josm.Main
import org.openstreetmap.josm.data.Version
import org.openstreetmap.josm.data.coor.LatLon
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.Way
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer
import org.openstreetmap.josm.data.projection.Projections
import org.openstreetmap.josm.gui.NavigatableComponent
import org.openstreetmap.josm.gui.mappaint.AreaElemStyle
import org.openstreetmap.josm.gui.mappaint.Environment
import org.openstreetmap.josm.gui.mappaint.LineElemStyle
import org.openstreetmap.josm.gui.mappaint.MultiCascade
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.SimpleKeyValueCondition
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.GeneralSelector
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser
import org.openstreetmap.josm.io.CachedFile

class taginfoextract {

    static def options
    static String image_dir
    int josm_svn_revision
    String input_file
    MapCSSStyleSource style_source
    FileWriter output_file
    def base_dir = "."
    def tags = [] as Set

    private def cached_svnrev

    /**
     * Check if a certain tag is supported by the style as node / way / area.
     */
    abstract class Checker {

        def tag
        def osm

        Checker(tag) {
            this.tag = tag
        }

        def apply_stylesheet(osm) {
            osm.put(tag[0], tag[1])
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
            env.layer = "default"
            return env
        }

        /**
         * Determine full image url (can refer to JOSM or OSM repository).
         */
        def find_image_url(path) {
            def f = new File("${base_dir}/images/styles/standard/${path}")
            if (f.exists()) {
                def rev = osm_svn_revision()
                return "http://trac.openstreetmap.org/export/${rev}/subversion/applications/share/map-icons/classic.small/${path}"
            }
            f = new File("${base_dir}/images/${path}")
            if (f.exists()) {
                return "http://josm.openstreetmap.de/export/${josm_svn_revision}/josm/trunk/images/${path}"
            }
            assert false, "Cannot find image url for ${path}"
        }

        /**
         * Create image file from ElemStyle.
         * @return the URL
         */
        def create_image(elem_style, type, nc) {
            def img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
            def g = img.createGraphics()
            g.setClip(0, 0, 16, 16)
            def renderer = new StyledMapRenderer(g, nc, false)
            renderer.getSettings(false)
            elem_style.paintPrimitive(osm, MapPaintSettings.INSTANCE, renderer, false, false, false)
            def base_url = options.imgurlprefix ? options.imgurlprefix : image_dir
            def image_name = "${type}_${tag[0]}=${tag[1]}.png"
            ImageIO.write(img, "png", new File("${image_dir}/${image_name}"))
            return "${base_url}/${image_name}"
        }

        /**
         * Checks, if tag is supported and find URL for image icon in this case.
         * @param generate_image if true, create or find a suitable image icon and return URL,
         * if false, just check if tag is supported and return true or false
         */
        abstract def find_url(boolean generate_image)
    }

    class NodeChecker extends Checker {
        NodeChecker(tag) {
            super(tag)
        }

        def find_url(boolean generate_image) {
            osm = new Node(new LatLon(0,0))
            def env = apply_stylesheet(osm)
            def c = env.mc.getCascade("default")
            def image = c.get("icon-image")
            if (image) {
                if (image instanceof IconReference) {
                    if (image.iconName != "misc/deprecated.png")
                        return find_image_url(image.iconName)
                }
            }
        }
    }

    class WayChecker extends Checker {
        WayChecker(tag) {
            super(tag)
        }

        def find_url(boolean generate_image) {
            osm = new Way()
            def nc = new NavigatableComponent()
            def n1 = new Node(nc.getLatLon(2,8))
            def n2 = new Node(nc.getLatLon(14,8))
            osm.addNode(n1)
            osm.addNode(n2)
            def env = apply_stylesheet(osm)
            def les = LineElemStyle.createLine(env)
            if (les != null) {
                if (!generate_image) return true
                return create_image(les, 'way', nc)
            }
        }
    }

    class AreaChecker extends Checker {
        AreaChecker(tag) {
            super(tag)
        }

        def find_url(boolean generate_image) {
            osm = new Way()
            def nc = new NavigatableComponent()
            def n1 = new Node(nc.getLatLon(2,2))
            def n2 = new Node(nc.getLatLon(14,2))
            def n3 = new Node(nc.getLatLon(14,14))
            def n4 = new Node(nc.getLatLon(2,14))
            osm.addNode(n1)
            osm.addNode(n2)
            osm.addNode(n3)
            osm.addNode(n4)
            osm.addNode(n1)
            def env = apply_stylesheet(osm)
            def aes = AreaElemStyle.create(env)
            if (aes != null) {
                if (!generate_image) return true
                return create_image(aes, 'area', nc)
            }
        }
    }

    /**
     * Main method.
     */
    static main(def args) {
        parse_command_line_arguments(args)
        def script = new taginfoextract()
        script.run()
        System.exit(0)
    }

    /**
     * Parse command line arguments.
     */
    static void parse_command_line_arguments(args) {
        def cli = new CliBuilder(usage:'taginfoextract.groovy [options] [inputfile]',
            header:"Options:",
            footer:"[inputfile]           the file to process (optional, default is 'resource://styles/standard/elemstyles.mapcss')")
        cli.o(args:1, argName: "file", "output file (json), - prints to stdout (default: -)")
        cli._(longOpt:'svnrev', args:1, argName:"revision", "corresponding revision of the repository http://svn.openstreetmap.org/ (optional, current revision is read from the local checkout or from the web if not given, see --svnweb)")
        cli._(longOpt:'imgdir', args:1, argName:"directory", "directory to put the generated images in (default: ./taginfo-img)")
        cli._(longOpt:'svnweb', 'fetch revision of the repository http://svn.openstreetmap.org/ from web and not from the local repository')
        cli._(longOpt:'imgurlprefix', args:1, argName:'prefix', 'image URLs prefix for generated image files')
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
        image_dir = 'taginfo-img'
        if (options.imgdir) {
            image_dir = options.imgdir
        }
        def image_dir_file = new File(image_dir)
        if (!image_dir_file.exists()) {
            image_dir_file.mkdirs()
        }
    }

    void run() {
        init()
        parse_style_sheet()
        collect_tags()

        def datetime = new Date().format("yyyyMMdd'T'hhmmssZ")
        output """{
                |  "data_format": 1,
                |  "data_updated": "${datetime}",
                |  "project": {
                |    "name": "JOSM main mappaint style",
                |    "description": "Tags supported by the main mappaint style in the OSM editor JOSM",
                |    "project_url": "http://josm.openstreetmap.de/",
                |    "icon_url": "http://josm.openstreetmap.de/export/7770/josm/trunk/images/logo_16x16x8.png",
                |    "contact_name": "JOSM developer team",
                |    "contact_email": "josm-dev@openstreetmap.org"
                |  },
                |  "tags": [
                |""".stripMargin()
        // another optional field is "data_url": ...

        def sep = ""
        for (tag in tags) {
            def types = []
            def final_url = null

            def node_url = new NodeChecker(tag).find_url(true)
            if (node_url) {
                types += '"node"'
                final_url = node_url
            }
            def way_url = new WayChecker(tag).find_url(final_url == null)
            if (way_url) {
                types += '"way"'
                if (!final_url) {
                    final_url = way_url
                }
            }
            def area_url = new AreaChecker(tag).find_url(final_url == null)
            if (area_url) {
                types += '"area"'
                if (!final_url) {
                    final_url = area_url
                }
            }

            output """${sep}    {
                     |      "key": "${tag[0]}",
                     |      "value": "${tag[1]}",
                     |      "object_types": ${types}""".stripMargin()
            if (final_url != null) {
                output """,
                     |      "icon_url": "${final_url}"
                     |    }""".stripMargin()
            } else {
                output """
                     |    }""".stripMargin()
            }
            sep = ",\n"
        }
        output """
        |  ]
        |}
        |""".stripMargin()

        if (output_file != null) {
            output_file.close()
        }
    }

    /**
     * Initialize the script.
     */
    def init() {
        Main.initApplicationPreferences()
        Main.pref.enableSaveOnPut(false)
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857"))

        josm_svn_revision = Version.getInstance().getVersion()
        assert josm_svn_revision != Version.JOSM_UNKNOWN_VERSION

        if (options.arguments().size() == 0) {
            input_file = "resource://styles/standard/elemstyles.mapcss"
        } else {
            input_file = options.arguments()[0]
        }

        output_file = null
        if (options.o && options.o != "-") {
            output_file = new FileWriter(options.o)
        }
    }

    /**
     * Get revision for the repository http://svn.openstreetmap.org.
     */
    def osm_svn_revision() {
        if (cached_svnrev != null) return cached_svnrev
        if (options.svnrev) {
            cached_svnrev = Integer.parseInt(options.svnrev)
            return cached_svnrev
        }
        def xml
        if (options.svnweb) {
            xml = "svn info --xml http://svn.openstreetmap.org/applications/share/map-icons/classic.small".execute().text
        } else {
            xml = "svn info --xml ${base_dir}/images/styles/standard/".execute().text
        }

        def svninfo = new XmlParser().parseText(xml)
        def rev = svninfo.entry.'@revision'[0]
        cached_svnrev = Integer.parseInt(rev)
        assert cached_svnrev > 0
        return cached_svnrev
    }

    /**
     * Read the style sheet file and parse the MapCSS code.
     */
    def parse_style_sheet() {
        def file = new CachedFile(input_file)
        def stream = file.getInputStream()
        def parser = new MapCSSParser(stream, "UTF-8", MapCSSParser.LexicalState.DEFAULT)
        style_source = new MapCSSStyleSource("")
        style_source.url = ""
        parser.sheet(style_source)
    }

    /**
     * Collect all the tag from the style sheet.
     */
    def collect_tags() {
        for (rule in style_source.rules) {
            def selector = rule.selector
            if (selector instanceof GeneralSelector) {
                def conditions = selector.getConditions()
                for (cond in conditions) {
                    if (cond instanceof SimpleKeyValueCondition) {
                        tags.add([cond.k, cond.v])
                    }
                }
            }
        }
    }

    /**
     * Write the JSON output (either to file or to command line).
     */
    def output(x) {
        if (output_file != null) {
            output_file.write(x)
        } else {
            print x
        }
    }

    static def err_println(s) {
        System.err.println(s);
    }

    static def err_print(s) {
        System.err.print(s);
    }

}

