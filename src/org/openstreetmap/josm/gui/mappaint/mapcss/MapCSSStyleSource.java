// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.Range;
import org.openstreetmap.josm.gui.mappaint.StyleKeys;
import org.openstreetmap.josm.gui.mappaint.StyleSetting;
import org.openstreetmap.josm.gui.mappaint.StyleSetting.StyleSettingGroup;
import org.openstreetmap.josm.gui.mappaint.StyleSettingFactory;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.GeneralSelector;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.TokenMgrError;
import org.openstreetmap.josm.gui.mappaint.styleelement.LineElement;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * This is a mappaint style that is based on MapCSS rules.
 */
public class MapCSSStyleSource extends StyleSource {

    /**
     * The accepted MIME types sent in the HTTP Accept header.
     * @since 6867
     */
    public static final String MAPCSS_STYLE_MIME_TYPES =
            "text/x-mapcss, text/mapcss, text/css; q=0.9, text/plain; q=0.8, application/zip, application/octet-stream; q=0.5";

    /**
     * all rules in this style file
     */
    public final List<MapCSSRule> rules = new ArrayList<>();
    /**
     * Index of rules in this style file
     */
    private final MapCSSStyleIndex ruleIndex = new MapCSSStyleIndex();

    private Color backgroundColorOverride;
    private String css;
    private ZipFile zipFile;

    private boolean removeAreaStylePseudoClass;

    /**
     * This lock prevents concurrent execution of {@link MapCSSRuleIndex#clear() } /
     * {@link MapCSSRuleIndex#initIndex()} and {@link MapCSSRuleIndex#getRuleCandidates }.
     *
     * For efficiency reasons, these methods are synchronized higher up the
     * stack trace.
     */
    public static final ReadWriteLock STYLE_SOURCE_LOCK = new ReentrantReadWriteLock();

    /**
     * Set of all supported MapCSS keys.
     */
    static final Set<String> SUPPORTED_KEYS = new HashSet<>();
    static {
        Field[] declaredFields = StyleKeys.class.getDeclaredFields();
        for (Field f : declaredFields) {
            try {
                SUPPORTED_KEYS.add((String) f.get(null));
                if (!f.getName().toLowerCase(Locale.ENGLISH).replace('_', '-').equals(f.get(null))) {
                    throw new JosmRuntimeException(f.getName());
                }
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                throw new JosmRuntimeException(ex);
            }
        }
        for (LineElement.LineType lt : LineElement.LineType.values()) {
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.COLOR);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.DASHES);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.DASHES_BACKGROUND_COLOR);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.DASHES_BACKGROUND_OPACITY);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.DASHES_OFFSET);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.LINECAP);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.LINEJOIN);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.MITERLIMIT);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.OFFSET);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.OPACITY);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.REAL_WIDTH);
            SUPPORTED_KEYS.add(lt.prefix + StyleKeys.WIDTH);
        }
    }

    /**
     * Constructs a new, active {@link MapCSSStyleSource}.
     * @param url URL that {@link org.openstreetmap.josm.io.CachedFile} understands
     * @param name The name for this StyleSource
     * @param shortdescription The title for that source.
     */
    public MapCSSStyleSource(String url, String name, String shortdescription) {
        super(url, name, shortdescription);
    }

    /**
     * Constructs a new {@link MapCSSStyleSource}
     * @param entry The entry to copy the data (url, name, ...) from.
     */
    public MapCSSStyleSource(SourceEntry entry) {
        super(entry);
    }

    /**
     * <p>Creates a new style source from the MapCSS styles supplied in
     * {@code css}</p>
     *
     * @param css the MapCSS style declaration. Must not be null.
     * @throws IllegalArgumentException if {@code css} is null
     */
    public MapCSSStyleSource(String css) {
        super(null, null, null);
        CheckParameterUtil.ensureParameterNotNull(css);
        this.css = css;
    }

    @Override
    public void loadStyleSource(boolean metadataOnly) {
        STYLE_SOURCE_LOCK.writeLock().lock();
        try {
            init();
            rules.clear();
            ruleIndex.clear();
            // remove "areaStyle" pseudo classes intended only for validator (causes StackOverflowError otherwise), see #16183
            removeAreaStylePseudoClass = url == null || !url.contains("validator"); // resource://data/validator/ or xxx.validator.mapcss
            try (InputStream in = getSourceInputStream()) {
                try (Reader reader = new BufferedReader(UTFInputStreamReader.create(in))) {
                    // evaluate @media { ... } blocks
                    MapCSSParser preprocessor = new MapCSSParser(reader, MapCSSParser.LexicalState.PREPROCESSOR);

                    // do the actual mapcss parsing
                    try (Reader in2 = new StringReader(preprocessor.pp_root(this))) {
                        new MapCSSParser(in2, MapCSSParser.LexicalState.DEFAULT).sheet(this);
                    }

                    loadMeta();
                    if (!metadataOnly) {
                        loadCanvas();
                        loadSettings();
                    }
                } finally {
                    closeSourceInputStream(in);
                }
            } catch (IOException | IllegalArgumentException e) {
                Logging.warn(tr("Failed to load Mappaint styles from ''{0}''. Exception was: {1}", url, e.toString()));
                Logging.log(Logging.LEVEL_ERROR, e);
                logError(e);
            } catch (TokenMgrError e) {
                Logging.warn(tr("Failed to parse Mappaint styles from ''{0}''. Error was: {1}", url, e.getMessage()));
                Logging.error(e);
                logError(e);
            } catch (ParseException e) {
                Logging.warn(tr("Failed to parse Mappaint styles from ''{0}''. Error was: {1}", url, e.getMessage()));
                Logging.error(e);
                logError(new ParseException(e.getMessage())); // allow e to be garbage collected, it links to the entire token stream
            }
            if (metadataOnly) {
                return;
            }
            // optimization: filter rules for different primitive types
            ruleIndex.buildIndex(rules.stream());
            loaded = true;
        } finally {
            STYLE_SOURCE_LOCK.writeLock().unlock();
        }
    }

    @Override
    public InputStream getSourceInputStream() throws IOException {
        if (css != null) {
            return new ByteArrayInputStream(css.getBytes(StandardCharsets.UTF_8));
        }
        CachedFile cf = getCachedFile();
        if (isZip) {
            File file = cf.getFile();
            zipFile = new ZipFile(file, StandardCharsets.UTF_8);
            zipIcons = file;
            I18n.addTexts(zipIcons);
            ZipEntry zipEntry = zipFile.getEntry(zipEntryPath);
            return zipFile.getInputStream(zipEntry);
        } else {
            zipFile = null;
            zipIcons = null;
            return cf.getInputStream();
        }
    }

    @Override
    @SuppressWarnings("resource")
    public CachedFile getCachedFile() throws IOException {
        return new CachedFile(url).setHttpAccept(MAPCSS_STYLE_MIME_TYPES); // NOSONAR
    }

    @Override
    public void closeSourceInputStream(InputStream is) {
        super.closeSourceInputStream(is);
        if (isZip) {
            Utils.close(zipFile);
        }
    }

    /**
     * load meta info from a selector "meta"
     */
    private void loadMeta() {
        Cascade c = constructSpecial(Selector.BASE_META);
        String pTitle = c.get("title", null, String.class);
        if (title == null) {
            title = pTitle;
        }
        String pIcon = c.get("icon", null, String.class);
        if (icon == null) {
            icon = pIcon;
        }
    }

    private void loadCanvas() {
        Cascade c = constructSpecial(Selector.BASE_CANVAS);
        backgroundColorOverride = c.get("fill-color", null, Color.class);
    }

    private static void loadSettings(MapCSSRule r, GeneralSelector gs, Environment env) {
        if (gs.matchesConditions(env)) {
            env.layer = null;
            env.layer = gs.getSubpart().getId(env);
            r.execute(env);
        }
    }

    private void loadSettings() {
        settings.clear();
        settingValues.clear();
        settingGroups.clear();
        MultiCascade mc = new MultiCascade();
        MultiCascade mcGroups = new MultiCascade();
        Node n = new Node();
        n.put("lang", LanguageInfo.getJOSMLocaleCode());
        // create a fake environment to read the meta data block
        Environment env = new Environment(n, mc, "default", this);
        Environment envGroups = new Environment(n, mcGroups, "default", this);

        // Parse rules
        for (MapCSSRule r : rules) {
            final Selector gs = r.selectors.get(0);
            if (gs instanceof GeneralSelector) {
                if (Selector.BASE_SETTING.equals(gs.getBase())) {
                    loadSettings(r, ((GeneralSelector) gs), env);
                } else if (Selector.BASE_SETTINGS.equals(gs.getBase())) {
                    loadSettings(r, ((GeneralSelector) gs), envGroups);
                }
            }
        }
        // Load groups
        for (Entry<String, Cascade> e : mcGroups.getLayers()) {
            if ("default".equals(e.getKey())) {
                Logging.warn("settings requires layer identifier e.g. 'settings::settings_group {...}'");
                continue;
            }
            settingGroups.put(StyleSettingGroup.create(e.getValue(), this, e.getKey()), new ArrayList<>());
        }
        // Load settings
        for (Entry<String, Cascade> e : mc.getLayers()) {
            if ("default".equals(e.getKey())) {
                Logging.warn("setting requires layer identifier e.g. 'setting::my_setting {...}'");
                continue;
            }
            Cascade c = e.getValue();
            StyleSetting set = StyleSettingFactory.create(c, this, e.getKey());
            if (set != null) {
                settings.add(set);
                settingValues.put(e.getKey(), set.getValue());
                String groupId = c.get("group", null, String.class);
                if (groupId != null) {
                    final StyleSettingGroup group = settingGroups.keySet().stream()
                            .filter(g -> g.key.equals(groupId))
                            .findAny()
                            .orElseThrow(() -> new IllegalArgumentException("Unknown settings group: " + groupId));
                    settingGroups.get(group).add(set);
                }
            }
        }
        settings.sort(null);
    }

    private Cascade constructSpecial(String type) {

        MultiCascade mc = new MultiCascade();
        Node n = new Node();
        String code = LanguageInfo.getJOSMLocaleCode();
        n.put("lang", code);
        // create a fake environment to read the meta data block
        Environment env = new Environment(n, mc, "default", this);

        for (MapCSSRule r : rules) {
            final boolean matches = r.selectors.stream().anyMatch(gs -> gs instanceof GeneralSelector
                    && gs.getBase().equals(type)
                    && ((GeneralSelector) gs).matchesConditions(env));
            if (matches) {
                r.execute(env);
            }
        }
        return mc.getCascade("default");
    }

    @Override
    public Color getBackgroundColorOverride() {
        return backgroundColorOverride;
    }

    @Override
    public void apply(MultiCascade mc, IPrimitive osm, double scale, boolean pretendWayIsClosed) {

        Environment env = new Environment(osm, mc, null, this);
        // the declaration indices are sorted, so it suffices to save the last used index
        int lastDeclUsed = -1;

        Iterator<MapCSSRule> candidates = ruleIndex.getRuleCandidates(osm);
        while (candidates.hasNext()) {
            MapCSSRule r = candidates.next();
            for (Selector s : r.selectors) {
                env.clearSelectorMatchingInformation();
                env.layer = s.getSubpart().getId(env);
                String sub = env.layer;
                if (!s.matches(env)) { // as side effect env.parent will be set (if s is a child selector)
                    continue;
                }
                if (s.getRange().contains(scale)) {
                    mc.range = Range.cut(mc.range, s.getRange());
                } else {
                    mc.range = mc.range.reduceAround(scale, s.getRange());
                    continue;
                }

                if (r.declaration.idx == lastDeclUsed)
                    continue; // don't apply one declaration more than once
                lastDeclUsed = r.declaration.idx;
                if ("*".equals(sub)) {
                    for (Entry<String, Cascade> entry : mc.getLayers()) {
                        env.layer = entry.getKey();
                        if ("*".equals(env.layer)) {
                            continue;
                        }
                        r.execute(env);
                    }
                }
                env.layer = sub;
                r.execute(env);
            }
        }
    }

    /**
     * Evaluate a supports condition
     * @param feature The feature to evaluate for
     * @param val The additional parameter passed to evaluate
     * @return <code>true</code> if JSOM supports that feature
     */
    public boolean evalSupportsDeclCondition(String feature, Object val) {
        if (feature == null) return false;
        if (SUPPORTED_KEYS.contains(feature)) return true;
        switch (feature) {
            case "user-agent":
                String s = Cascade.convertTo(val, String.class);
                return "josm".equals(s);
            case "min-josm-version":
                Float min = Cascade.convertTo(val, Float.class);
                return min != null && Math.round(min) <= Version.getInstance().getVersion();
            case "max-josm-version":
                Float max = Cascade.convertTo(val, Float.class);
                return max != null && Math.round(max) >= Version.getInstance().getVersion();
            default:
                return false;
        }
    }

    /**
     * Removes "meta" rules. Not needed for validator.
     * @since 13633
     */
    public void removeMetaRules() {
        rules.removeIf(x -> x.selectors.get(0) instanceof GeneralSelector && Selector.BASE_META.equals(x.selectors.get(0).getBase()));
    }

    /**
     * Whether to remove "areaStyle" pseudo classes. Only for use in MapCSSParser!
     * @return whether to remove "areaStyle" pseudo classes
     */
    public boolean isRemoveAreaStylePseudoClass() {
        return removeAreaStylePseudoClass;
    }

    @Override
    public String toString() {
        return rules.stream().map(MapCSSRule::toString).collect(Collectors.joining("\n"));
    }
}
