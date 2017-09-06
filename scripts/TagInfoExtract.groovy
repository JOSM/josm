// License: GPL. For details, see LICENSE file.
/**
 * Extracts tag information for the taginfo project.
 *
 * Run from the base directory of a JOSM checkout:
 *
 * groovy -cp dist/josm-custom.jar scripts/taginfoextract.groovy -t mappaint
 * groovy -cp dist/josm-custom.jar scripts/taginfoextract.groovy -t presets
 * groovy -cp dist/josm-custom.jar scripts/taginfoextract.groovy -t external_presets
 */
import java.awt.image.BufferedImage
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

import javax.imageio.ImageIO

import org.openstreetmap.josm.Main
import org.openstreetmap.josm.actions.DeleteAction
import org.openstreetmap.josm.command.DeleteCommand
import org.openstreetmap.josm.data.Version
import org.openstreetmap.josm.data.coor.LatLon
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.OsmPrimitive
import org.openstreetmap.josm.data.osm.Way
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer
import org.openstreetmap.josm.data.projection.Projections
import org.openstreetmap.josm.gui.NavigatableComponent
import org.openstreetmap.josm.gui.mappaint.Environment
import org.openstreetmap.josm.gui.mappaint.MultiCascade
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.SimpleKeyValueCondition
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.GeneralSelector
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser
import org.openstreetmap.josm.gui.mappaint.styleelement.AreaElement
import org.openstreetmap.josm.gui.mappaint.styleelement.LineElement
import org.openstreetmap.josm.gui.mappaint.styleelement.StyleElement
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType
import org.openstreetmap.josm.gui.tagging.presets.items.KeyedItem
import org.openstreetmap.josm.gui.tagging.presets.items.KeyedItem.MatchType
import org.openstreetmap.josm.io.CachedFile
import org.openstreetmap.josm.tools.Logging
import org.openstreetmap.josm.tools.RightAndLefthandTraffic
import org.openstreetmap.josm.tools.Territories
import org.openstreetmap.josm.tools.Utils

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import groovy.json.JsonBuilder

class TagInfoExtract {

    static def options
    static String image_dir
    int josm_svn_revision
    String input_file
    MapCSSStyleSource style_source
    FileWriter output_file
    String base_dir = "."
    Set tags = []

    private def cached_svnrev

    /**
     * Check if a certain tag is supported by the style as node / way / area.
     */
    abstract class Checker {

        def tag
        OsmPrimitive osm

        Checker(tag) {
            this.tag = tag
        }

        Environment apply_stylesheet(OsmPrimitive osm) {
            osm.put(tag[0], tag[1])
            MultiCascade mc = new MultiCascade()

            Environment env = new Environment(osm, mc, null, style_source)
            for (def r in style_source.rules) {
                env.clearSelectorMatchingInformation()
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
         * Create image file from StyleElement.
         * @return the URL
         */
        def create_image(StyleElement elem_style, type, nc) {
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

    @SuppressFBWarnings(value = "MF_CLASS_MASKS_FIELD")
    class NodeChecker extends Checker {
        NodeChecker(tag) {
            super(tag)
        }

        @Override
        def find_url(boolean generate_image) {
            osm = new Node(LatLon.ZERO)
            def env = apply_stylesheet(osm)
            def c = env.mc.getCascade("default")
            def image = c.get("icon-image")
            if (image) {
                if (image instanceof IconReference && !image.isDeprecatedIcon()) {
                    return find_image_url(image.iconName)
                }
            }
        }
    }

    @SuppressFBWarnings(value = "MF_CLASS_MASKS_FIELD")
    class WayChecker extends Checker {
        WayChecker(tag) {
            super(tag)
        }

        @Override
        def find_url(boolean generate_image) {
            osm = new Way()
            def nc = new NavigatableComponent()
            def n1 = new Node(nc.getLatLon(2,8))
            def n2 = new Node(nc.getLatLon(14,8))
            ((Way)osm).addNode(n1)
            ((Way)osm).addNode(n2)
            def env = apply_stylesheet(osm)
            def les = LineElement.createLine(env)
            if (les != null) {
                if (!generate_image) return true
                return create_image(les, 'way', nc)
            }
        }
    }

    @SuppressFBWarnings(value = "MF_CLASS_MASKS_FIELD")
    class AreaChecker extends Checker {
        AreaChecker(tag) {
            super(tag)
        }

        @Override
        def find_url(boolean generate_image) {
            osm = new Way()
            def nc = new NavigatableComponent()
            def n1 = new Node(nc.getLatLon(2,2))
            def n2 = new Node(nc.getLatLon(14,2))
            def n3 = new Node(nc.getLatLon(14,14))
            def n4 = new Node(nc.getLatLon(2,14))
            ((Way)osm).addNode(n1)
            ((Way)osm).addNode(n2)
            ((Way)osm).addNode(n3)
            ((Way)osm).addNode(n4)
            ((Way)osm).addNode(n1)
            def env = apply_stylesheet(osm)
            def aes = AreaElement.create(env)
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
        def script = new TagInfoExtract()
        if (!options.t || options.t == 'mappaint') {
            script.run()
        } else if (options.t == 'presets') {
            script.run_presets()
        } else if (options.t == 'external_presets') {
            script.run_external_presets()
        } else {
            System.err.println 'Invalid type ' + options.t
            if (!options.noexit) {
                System.exit(1)
            }
        }

        if (!options.noexit) {
            System.exit(0)
        }
    }

    /**
     * Parse command line arguments.
     */
    static void parse_command_line_arguments(args) {
        def cli = new CliBuilder(usage:'taginfoextract.groovy [options] [inputfile]',
            header:"Options:",
            footer:"[inputfile]           the file to process (optional, default is 'resource://styles/standard/elemstyles.mapcss')")
        cli.o(args:1, argName: "file", "output file (json), - prints to stdout (default: -)")
        cli.t(args:1, argName: "type", "the project type to be generated")
        cli._(longOpt:'svnrev', args:1, argName:"revision", "corresponding revision of the repository https://svn.openstreetmap.org/ (optional, current revision is read from the local checkout or from the web if not given, see --svnweb)")
        cli._(longOpt:'imgdir', args:1, argName:"directory", "directory to put the generated images in (default: ./taginfo-img)")
        cli._(longOpt:'noexit', "don't call System.exit(), for use from Ant script")
        cli._(longOpt:'svnweb', 'fetch revision of the repository https://svn.openstreetmap.org/ from web and not from the local repository')
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

    void run_presets() {
        init()
        def presets = TaggingPresetReader.readAll(input_file, true)
        def tags = convert_presets(presets, "", true)
        write_json("JOSM main presets", "Tags supported by the default presets in the OSM editor JOSM", tags)
    }

    def convert_presets(Iterable<TaggingPreset> presets, String descriptionPrefix, boolean addImages) {
        def tags = []
        for (TaggingPreset preset : presets) {
            for (KeyedItem item : Utils.filteredCollection(preset.data, KeyedItem.class)) {
                def values
                switch (MatchType.ofString(item.match)) {
                    case MatchType.KEY_REQUIRED: values = item.getValues(); break;
                    case MatchType.KEY_VALUE_REQUIRED: values = item.getValues(); break;
                    default: values = [];
                }
                for (String value : values) {
                    def tag = [
                            description: descriptionPrefix + preset.name,
                            key: item.key,
                            value: value,
                    ]
                    def otypes = preset.types.collect {
                        it == TaggingPresetType.CLOSEDWAY ? "area" :
                            (it == TaggingPresetType.MULTIPOLYGON ? "relation" : it.toString().toLowerCase(Locale.ENGLISH))
                    }
                    if (!otypes.isEmpty()) tag += [object_types: otypes]
                    if (addImages && preset.iconName) tag += [icon_url: find_image_url(preset.iconName)]
                    tags += tag
                }
            }
        }
        return tags
    }

    void run_external_presets() {
        init()
        TaggingPresetReader.setLoadIcons(false)
        def sources = new TaggingPresetPreference.TaggingPresetSourceEditor().loadAndGetAvailableSources()
        def tags = []
        for (def source : sources) {
            if (source.url.startsWith("resource")) {
                // default presets
                continue;
            }
            try {
                println "Loading ${source.url}"
                def presets = TaggingPresetReader.readAll(source.url, false)
                def t = convert_presets(presets, source.title + " ", false)
                println "Converting ${t.size()} presets of ${source.title}"
                tags += t
            } catch (Exception ex) {
                System.err.println("Skipping ${source.url} due to error")
                ex.printStackTrace()
            }
        }
        write_json("JOSM user presets", "Tags supported by the user contributed presets in the OSM editor JOSM", tags)
    }

    void run() {
        init()
        parse_style_sheet()
        collect_tags()

        def tags = tags.collect {
            def tag = it
            def types = []
            def final_url = null

            def node_url = new NodeChecker(tag).find_url(true)
            if (node_url) {
                types += 'node'
                final_url = node_url
            }
            def way_url = new WayChecker(tag).find_url(final_url == null)
            if (way_url) {
                types += 'way'
                if (!final_url) {
                    final_url = way_url
                }
            }
            def area_url = new AreaChecker(tag).find_url(final_url == null)
            if (area_url) {
                types += 'area'
                if (!final_url) {
                    final_url = area_url
                }
            }

            def obj = [key: tag[0], value: tag[1]]
            if (types) obj += [object_types: types]
            if (final_url) obj += [icon_url: final_url]
            obj
        }

        write_json("JOSM main mappaint style", "Tags supported by the main mappaint style in the OSM editor JOSM", tags)
    }

    void write_json(name, description, tags) {
        def json = new JsonBuilder()
        def project = [
                name: name,
                description: description,
                project_url: "https://josm.openstreetmap.de/",
                icon_url: "https://josm.openstreetmap.de/export/7770/josm/trunk/images/logo_16x16x8.png",
                contact_name: "JOSM developer team",
                contact_email: "josm-dev@openstreetmap.org",
        ]
        json data_format: 1, data_updated: new Date().format("yyyyMMdd'T'hhmmss'Z'", TimeZone.getTimeZone('UTC')), project: project, tags: tags

        if (output_file != null) {
            json.writeTo(output_file)
            output_file.close()
        } else {
            print json.toPrettyString()
        }
    }

    /**
     * Initialize the script.
     */
    def init() {
        Main.determinePlatformHook()
        Logging.setLogLevel(Logging.LEVEL_INFO)
        Main.pref.enableSaveOnPut(false)
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857"))
        Path tmpdir = Files.createTempDirectory(FileSystems.getDefault().getPath(base_dir), "pref")
        tmpdir.toFile().deleteOnExit()
        System.setProperty("josm.home", tmpdir.toString())
        DeleteCommand.setDeletionCallback(DeleteAction.&checkAndConfirmOutlyingDelete)
        Territories.initialize()
        RightAndLefthandTraffic.initialize()

        josm_svn_revision = Version.getInstance().getVersion()
        assert josm_svn_revision != Version.JOSM_UNKNOWN_VERSION

        if (options.arguments().size() == 0 && (!options.t || options.t == 'mappaint')) {
            input_file = "resource://styles/standard/elemstyles.mapcss"
        } else if (options.arguments().size() == 0 && options.t == 'presets') {
            input_file = "resource://data/defaultpresets.xml"
        } else {
            input_file = options.arguments()[0]
        }

        output_file = null
        if (options.o && options.o != "-") {
            output_file = new FileWriter(options.o)
        }
    }

    /**
     * Determine full image url (can refer to JOSM or OSM repository).
     */
    def find_image_url(String path) {
        def f = new File("${base_dir}/images/styles/standard/${path}")
        if (f.exists()) {
            def rev = osm_svn_revision()
            return "https://trac.openstreetmap.org/export/${rev}/subversion/applications/share/map-icons/classic.small/${path}"
        }
        f = new File("${base_dir}/images/${path}")
        if (f.exists()) {
            if (path.startsWith("images/styles/standard/")) {
                path = path.substring("images/styles/standard/".length())
                def rev = osm_svn_revision()
                return "https://trac.openstreetmap.org/export/${rev}/subversion/applications/share/map-icons/classic.small/${path}"
            } else if (path.startsWith("styles/standard/")) {
                path = path.substring("styles/standard/".length())
                def rev = osm_svn_revision()
                return "https://trac.openstreetmap.org/export/${rev}/subversion/applications/share/map-icons/classic.small/${path}"
            } else {
                return "https://josm.openstreetmap.de/export/${josm_svn_revision}/josm/trunk/images/${path}"
            }
        }
        assert false, "Cannot find image url for ${path}"
    }

    /**
     * Get revision for the repository https://svn.openstreetmap.org.
     */
    def osm_svn_revision() {
        if (cached_svnrev != null) return cached_svnrev
        if (options.svnrev) {
            cached_svnrev = Integer.parseInt(options.svnrev)
            return cached_svnrev
        }
        def xml
        if (options.svnweb) {
            xml = "svn info --xml https://svn.openstreetmap.org/applications/share/map-icons/classic.small".execute().text
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
}
