// License: GPL. For details, see LICENSE file.

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

import org.openstreetmap.josm.actions.DeleteAction;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.data.preferences.JosmUrls;
import org.openstreetmap.josm.data.preferences.sources.ExtendedSourceEntry;
import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSRule;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.gui.mappaint.styleelement.AreaElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.LineElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.StyleElement;
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType;
import org.openstreetmap.josm.gui.tagging.presets.items.CheckGroup;
import org.openstreetmap.josm.gui.tagging.presets.items.KeyedItem;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Http1Client;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OptionParser;
import org.openstreetmap.josm.tools.Territories;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * Extracts tag information for the taginfo project.
 * <p>
 * Run from the base directory of a JOSM checkout:
 * <p>
 * java -cp dist/josm-custom.jar TagInfoExtract --type mappaint
 * java -cp dist/josm-custom.jar TagInfoExtract --type presets
 * java -cp dist/josm-custom.jar TagInfoExtract --type external_presets
 */
public class TagInfoExtract {

    /**
     * Main method.
     * @param args Main program arguments
     * @throws Exception if any error occurs
     */
    public static void main(String[] args) throws Exception {
        HttpClient.setFactory(Http1Client::new);
        TagInfoExtract script = new TagInfoExtract();
        script.parseCommandLineArguments(args);
        script.init();
        switch (script.options.mode) {
            case MAPPAINT:
                script.new StyleSheet().run();
                break;
            case PRESETS:
                script.new Presets().run();
                break;
            case EXTERNAL_PRESETS:
                script.new ExternalPresets().run();
                break;
            default:
                throw new IllegalStateException("Invalid type " + script.options.mode);
        }
        if (!script.options.noexit) {
            System.exit(0);
        }
    }

    enum Mode {
        MAPPAINT, PRESETS, EXTERNAL_PRESETS
    }

    private final Options options = new Options();

    /**
     * Parse command line arguments.
     * @param args command line arguments
     */
    private void parseCommandLineArguments(String[] args) {
        if (args.length == 1 && "--help".equals(args[0])) {
            this.usage();
        }
        final OptionParser parser = new OptionParser(getClass().getName());
        parser.addArgumentParameter("type", OptionParser.OptionCount.REQUIRED, options::setMode);
        parser.addArgumentParameter("input", OptionParser.OptionCount.OPTIONAL, options::setInputFile);
        parser.addArgumentParameter("output", OptionParser.OptionCount.OPTIONAL, options::setOutputFile);
        parser.addArgumentParameter("imgdir", OptionParser.OptionCount.OPTIONAL, options::setImageDir);
        parser.addArgumentParameter("imgurlprefix", OptionParser.OptionCount.OPTIONAL, options::setImageUrlPrefix);
        parser.addFlagParameter("noexit", options::setNoExit);
        parser.addFlagParameter("help", this::usage);
        parser.parseOptionsOrExit(Arrays.asList(args));
    }

    private void usage() {
        System.out.println("java " + getClass().getName());
        System.out.println("  --type TYPE\tthe project type to be generated: " + Arrays.toString(Mode.values()));
        System.out.println("  --input FILE\tthe input file to use (overrides defaults for types mappaint, presets)");
        System.out.println("  --output FILE\tthe output file to use (defaults to STDOUT)");
        System.out.println("  --imgdir DIRECTORY\tthe directory to put the generated images in (default: " + options.imageDir + ")");
        System.out.println("  --imgurlprefix STRING\timage URLs prefix for generated image files (public path on webserver)");
        System.out.println("  --noexit\tdo not call System.exit(), for use from Ant script");
        System.out.println("  --help\tshow this help");
        System.exit(0);
    }

    private static class Options {
        Mode mode;
        int josmSvnRevision = Version.getInstance().getVersion();
        Path baseDir = Paths.get("");
        Path imageDir = Paths.get("taginfo-img");
        String imageUrlPrefix;
        CachedFile inputFile;
        Path outputFile;
        boolean noexit;

        void setMode(String value) {
            mode = Mode.valueOf(value.toUpperCase(Locale.ENGLISH));
            switch (mode) {
                case MAPPAINT:
                    inputFile = new CachedFile("resource://styles/standard/elemstyles.mapcss");
                    break;
                case PRESETS:
                    inputFile = new CachedFile("resource://data/defaultpresets.xml");
                    break;
                default:
                    inputFile = null;
            }
        }

        void setInputFile(String value) {
            inputFile = new CachedFile(value);
        }

        void setOutputFile(String value) {
            outputFile = Paths.get(value);
        }

        void setImageDir(String value) {
            imageDir = Paths.get(value);
        }

        void setImageUrlPrefix(String value) {
            imageUrlPrefix = value;
        }

        void setNoExit() {
            noexit = true;
        }

        /**
         * Determine full image url (can refer to JOSM or OSM repository).
         * @param path the image path
         * @return full image url
         */
        private String findImageUrl(String path) {
            final Path f = baseDir.resolve("resources").resolve("images").resolve(path);
            if (Files.exists(f)) {
                return "https://josm.openstreetmap.de/export/" + josmSvnRevision + "/josm/trunk/resources/images/" + path;
            }
            throw new IllegalStateException("Cannot find image url for " + path);
        }
    }

    private abstract class Extractor {
        abstract void run() throws Exception;

        void writeJson(String name, String description, Iterable<TagInfoTag> tags) throws IOException {
            try (Writer writer = options.outputFile != null ? Files.newBufferedWriter(options.outputFile) : new StringWriter();
                 JsonWriter json = Json
                         .createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true))
                         .createWriter(writer)) {
                JsonObjectBuilder project = Json.createObjectBuilder()
                        .add("name", name)
                        .add("description", description)
                        .add("project_url", JosmUrls.getInstance().getJOSMWebsite())
                        .add("icon_url", options.findImageUrl("logo_16x16x8.png"))
                        .add("contact_name", "JOSM developer team")
                        .add("contact_email", "josm-dev@openstreetmap.org");
                final JsonArrayBuilder jsonTags = Json.createArrayBuilder();
                for (TagInfoTag t : tags) {
                    jsonTags.add(t.toJson());
                }
                json.writeObject(Json.createObjectBuilder()
                        .add("data_format", 1)
                        .add("data_updated", DateTimeFormatter.ofPattern("yyyyMMdd'T'hhmmss'Z'").withZone(ZoneId.of("Z")).format(Instant.now()))
                        .add("project", project)
                        .add("tags", jsonTags)
                        .build());
                if (options.outputFile == null) {
                    System.out.println(writer.toString());
                }
            }
        }

    }

    private class Presets extends Extractor {

        @Override
        void run() throws IOException, OsmTransferException, SAXException {
            try (BufferedReader reader = options.inputFile.getContentReader()) {
                Collection<TaggingPreset> presets = TaggingPresetReader.readAll(reader, true);
                List<TagInfoTag> tags = convertPresets(presets, "", true);
                Logging.info("Converting {0} internal presets", tags.size());
                writeJson("JOSM main presets", "Tags supported by the default presets in the OSM editor JOSM", tags);
            }
        }

        List<TagInfoTag> convertPresets(Iterable<TaggingPreset> presets, String descriptionPrefix, boolean addImages) {
            final List<TagInfoTag> tags = new ArrayList<>();
            final Map<Tag, TagInfoTag> optionalTags = new LinkedHashMap<>();
            for (TaggingPreset preset : presets) {
                preset.data.stream()
                        .flatMap(item -> item instanceof KeyedItem
                                ? Stream.of(((KeyedItem) item))
                                : item instanceof CheckGroup
                                ? ((CheckGroup) item).checks.stream()
                                : Stream.empty())
                        .forEach(item -> {
                            for (String value : item.getValues()) {
                                Set<TagInfoTag.Type> types = TagInfoTag.Type.forPresetTypes(preset.types);
                                if (item.isKeyRequired()) {
                                    tags.add(new TagInfoTag(descriptionPrefix + preset.getName(), item.key, value, types,
                                            addImages && preset.iconName != null ? options.findImageUrl(preset.iconName) : null));
                                } else {
                                    optionalTags.compute(new Tag(item.key, value), (osmTag, tagInfoTag) -> {
                                        if (tagInfoTag == null) {
                                            String description = descriptionPrefix + "Optional for: " + preset.getName();
                                            return new TagInfoTag(description, item.key, value, types, null);
                                        } else {
                                            tagInfoTag.descriptions.add(preset.getName());
                                            tagInfoTag.objectTypes.addAll(types);
                                            return tagInfoTag;
                                        }
                                    });
                                }
                            }
                        });
            }
            tags.addAll(optionalTags.values());
            return tags;
        }

    }

    private class ExternalPresets extends Presets {

        @Override
        void run() throws IOException, OsmTransferException, SAXException {
            TaggingPresetReader.setLoadIcons(false);
            final Collection<ExtendedSourceEntry> sources = new TaggingPresetPreference.TaggingPresetSourceEditor().loadAndGetAvailableSources();
            final List<TagInfoTag> tags = new ArrayList<>();
            for (SourceEntry source : sources) {
                if (source.url.startsWith("resource")) {
                    // default presets
                    continue;
                }
                try {
                    Logging.info("Loading {0}", source.url);
                    Collection<TaggingPreset> presets = TaggingPresetReader.readAll(source.url, false);
                    final List<TagInfoTag> t = convertPresets(presets, source.title + " ", false);
                    Logging.info("Converting {0} presets of {1}", t.size(), source.title);
                    tags.addAll(t);
                } catch (Exception ex) {
                    Logging.warn("Skipping {0} due to error", source.url);
                    Logging.warn(ex);
                }

            }
            writeJson("JOSM user presets", "Tags supported by the user contributed presets in the OSM editor JOSM", tags);
        }
    }

    private class StyleSheet extends Extractor {
        private MapCSSStyleSource styleSource;

        @Override
        void run() throws IOException, ParseException {
            init();
            parseStyleSheet();
            final List<TagInfoTag> tags = convertStyleSheet();
            writeJson("JOSM main mappaint style", "Tags supported by the main mappaint style in the OSM editor JOSM", tags);
        }

        /**
         * Read the style sheet file and parse the MapCSS code.
         * @throws IOException if any I/O error occurs
         * @throws ParseException in case of parsing error
         */
        private void parseStyleSheet() throws IOException, ParseException {
            try (BufferedReader reader = options.inputFile.getContentReader()) {
                MapCSSParser parser = new MapCSSParser(reader, MapCSSParser.LexicalState.DEFAULT);
                styleSource = new MapCSSStyleSource("");
                styleSource.url = "";
                parser.sheet(styleSource);
            }
        }

        /**
         * Collect all the tag from the style sheet.
         * @return list of taginfo tags
         */
        private List<TagInfoTag> convertStyleSheet() {
            return styleSource.rules.stream()
                    .flatMap(rule -> rule.selectors.stream())
                    .flatMap(selector -> selector.getConditions().stream())
                    .filter(ConditionFactory.SimpleKeyValueCondition.class::isInstance)
                    .map(ConditionFactory.SimpleKeyValueCondition.class::cast)
                    .map(condition -> condition.asTag(null))
                    .distinct()
                    .map(tag -> {
                        String iconUrl = null;
                        final EnumSet<TagInfoTag.Type> types = EnumSet.noneOf(TagInfoTag.Type.class);
                        Optional<String> nodeUrl = new NodeChecker(tag).findUrl(true);
                        if (nodeUrl.isPresent()) {
                            iconUrl = nodeUrl.get();
                            types.add(TagInfoTag.Type.NODE);
                        }
                        Optional<String> wayUrl = new WayChecker(tag).findUrl(iconUrl == null);
                        if (wayUrl.isPresent()) {
                            if (iconUrl == null) {
                                iconUrl = wayUrl.get();
                            }
                            types.add(TagInfoTag.Type.WAY);
                        }
                        Optional<String> areaUrl = new AreaChecker(tag).findUrl(iconUrl == null);
                        if (areaUrl.isPresent()) {
                            if (iconUrl == null) {
                                iconUrl = areaUrl.get();
                            }
                            types.add(TagInfoTag.Type.AREA);
                        }
                        return new TagInfoTag(null, tag.getKey(), tag.getValue(), types, iconUrl);
                    })
                    .collect(Collectors.toList());
        }

        /**
         * Check if a certain tag is supported by the style as node / way / area.
         */
        private abstract class Checker {
            private final Pattern reservedChars = Pattern.compile("[<>:\"|\\?\\*]");

            Checker(Tag tag) {
                this.tag = tag;
            }

            Environment applyStylesheet(OsmPrimitive osm) {
                osm.put(tag);
                MultiCascade mc = new MultiCascade();

                Environment env = new Environment(osm, mc, null, styleSource);
                for (MapCSSRule r : styleSource.rules) {
                    env.clearSelectorMatchingInformation();
                    if (r.matches(env)) {
                        // ignore selector range
                        if (env.layer == null) {
                            env.layer = "default";
                        }
                        r.execute(env);
                    }
                }
                env.layer = "default";
                return env;
            }

            /**
             * Create image file from StyleElement.
             * @param element style element
             * @param type object type
             * @param nc navigatable component
             *
             * @return the URL
             */
            String createImage(StyleElement element, final String type, NavigatableComponent nc) {
                BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.setClip(0, 0, 16, 16);
                StyledMapRenderer renderer = new StyledMapRenderer(g, nc, false);
                renderer.getSettings(false);
                element.paintPrimitive(osm, MapPaintSettings.INSTANCE, renderer, false, false, false);
                final String imageName = type + "_" + normalize(tag.toString()) + ".png";
                try (OutputStream out = Files.newOutputStream(options.imageDir.resolve(imageName))) {
                    ImageIO.write(img, "png", out);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                final String baseUrl = options.imageUrlPrefix != null ? options.imageUrlPrefix : options.imageDir.toString();
                return baseUrl + "/" + imageName;
            }

            /**
             * Normalizes tag so that it can used as a filename on all platforms, including Windows.
             * @param tag OSM tag, that can contain illegal path characters
             * @return OSM tag with all illegal path characters replaced by underscore ('_')
             */
            String normalize(String tag) {
                Matcher m = reservedChars.matcher(tag);
                return m.find() ? m.replaceAll("_") : tag;
            }

            /**
             * Checks, if tag is supported and find URL for image icon in this case.
             *
             * @param generateImage if true, create or find a suitable image icon and return URL,
             *                       if false, just check if tag is supported and return true or false
             * @return URL for image icon if tag is supported
             */
            abstract Optional<String> findUrl(boolean generateImage);

            protected Tag tag;
            protected OsmPrimitive osm;
        }

        private class NodeChecker extends Checker {
            NodeChecker(Tag tag) {
                super(tag);
            }

            @Override
            Optional<String> findUrl(boolean generateImage) {
                this.osm = new Node(LatLon.ZERO);
                Environment env = applyStylesheet(osm);
                Cascade c = env.mc.getCascade("default");
                Object image = c.get("icon-image");
                if (image instanceof MapPaintStyles.IconReference && !((MapPaintStyles.IconReference) image).isDeprecatedIcon()) {
                    return Optional.of(options.findImageUrl(((MapPaintStyles.IconReference) image).iconName));
                }
                return Optional.empty();
            }

        }

        private class WayChecker extends Checker {
            WayChecker(Tag tag) {
                super(tag);
            }

            @Override
            Optional<String> findUrl(boolean generateImage) {
                this.osm = new Way();
                NavigatableComponent nc = new NavigatableComponent();
                Node n1 = new Node(nc.getLatLon(2, 8));
                Node n2 = new Node(nc.getLatLon(14, 8));
                ((Way) osm).addNode(n1);
                ((Way) osm).addNode(n2);
                Environment env = applyStylesheet(osm);
                LineElement les = LineElement.createLine(env);
                if (les != null) {
                    if (!generateImage) return Optional.of("");
                    return Optional.of(createImage(les, "way", nc));
                }
                return Optional.empty();
            }

        }

        private class AreaChecker extends Checker {
            AreaChecker(Tag tag) {
                super(tag);
            }

            @Override
            Optional<String> findUrl(boolean generateImage) {
                this.osm = new Way();
                NavigatableComponent nc = new NavigatableComponent();
                Node n1 = new Node(nc.getLatLon(2, 2));
                Node n2 = new Node(nc.getLatLon(14, 2));
                Node n3 = new Node(nc.getLatLon(14, 14));
                Node n4 = new Node(nc.getLatLon(2, 14));
                ((Way) osm).addNode(n1);
                ((Way) osm).addNode(n2);
                ((Way) osm).addNode(n3);
                ((Way) osm).addNode(n4);
                ((Way) osm).addNode(n1);
                Environment env = applyStylesheet(osm);
                AreaElement aes = AreaElement.create(env);
                if (aes != null) {
                    if (!generateImage) return Optional.of("");
                    return Optional.of(createImage(aes, "area", nc));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * POJO representing a <a href="https://wiki.openstreetmap.org/wiki/Taginfo/Projects">Taginfo tag</a>.
     */
    private static class TagInfoTag {
        final Collection<String> descriptions = new ArrayList<>();
        final String key;
        final String value;
        final Set<Type> objectTypes;
        final String iconURL;

        TagInfoTag(String description, String key, String value, Set<Type> objectTypes, String iconURL) {
            if (description != null) {
                this.descriptions.add(description);
            }
            this.key = key;
            this.value = value;
            this.objectTypes = objectTypes;
            this.iconURL = iconURL;
        }

        JsonObjectBuilder toJson() {
            final JsonObjectBuilder object = Json.createObjectBuilder();
            if (!descriptions.isEmpty()) {
                object.add("description", String.join(", ", Utils.limit(descriptions, 8, "...")));
            }
            object.add("key", key);
            object.add("value", value);
            if ((!objectTypes.isEmpty())) {
                final JsonArrayBuilder types = Json.createArrayBuilder();
                objectTypes.stream().map(Enum::name).map(String::toLowerCase).forEach(types::add);
                object.add("object_types", types);
            }
            if (iconURL != null) {
                object.add("icon_url", iconURL);
            }
            return object;
        }

        enum Type {
            NODE, WAY, AREA, RELATION;

            static TagInfoTag.Type forPresetType(TaggingPresetType type) {
                switch (type) {
                    case CLOSEDWAY:
                        return AREA;
                    case MULTIPOLYGON:
                        return RELATION;
                    default:
                        return valueOf(type.toString());
                }
            }

            static Set<TagInfoTag.Type> forPresetTypes(Set<TaggingPresetType> types) {
                if (types == null) {
                    return Collections.emptySet();
                }
                return types.stream()
                        .map(Type::forPresetType)
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(Type.class)));
            }
        }
    }

    /**
     * Initialize the script.
     * @throws IOException if any I/O error occurs
     */
    private void init() throws IOException {
        Logging.setLogLevel(Logging.LEVEL_INFO);
        Preferences.main().enableSaveOnPut(false);
        Config.setPreferencesInstance(Preferences.main());
        Config.setBaseDirectoriesProvider(JosmBaseDirectories.getInstance());
        Config.setUrlsProvider(JosmUrls.getInstance());
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        Path tmpdir = Files.createTempDirectory(options.baseDir, "pref");
        tmpdir.toFile().deleteOnExit();
        System.setProperty("josm.home", tmpdir.toString());
        DeleteCommand.setDeletionCallback(DeleteAction.defaultDeletionCallback);
        Territories.initializeInternalData();
        Files.createDirectories(options.imageDir);
    }
}
