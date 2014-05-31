// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.Range;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.SimpleKeyValueCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.ChildOrParentSelector;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.GeneralSelector;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.OptimizedGeneralSelector;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.TokenMgrError;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Utils;

public class MapCSSStyleSource extends StyleSource {

    /**
     * The accepted MIME types sent in the HTTP Accept header.
     * @since 6867
     */
    public static final String MAPCSS_STYLE_MIME_TYPES = "text/x-mapcss, text/mapcss, text/css; q=0.9, text/plain; q=0.8, application/zip, application/octet-stream; q=0.5";

    // all rules
    public final List<MapCSSRule> rules = new ArrayList<>();
    // rule indices, filtered by primitive type
    public final MapCSSRuleIndex nodeRules = new MapCSSRuleIndex();         // nodes
    public final MapCSSRuleIndex wayRules = new MapCSSRuleIndex();          // ways without tag area=no
    public final MapCSSRuleIndex wayNoAreaRules = new MapCSSRuleIndex();    // ways with tag area=no
    public final MapCSSRuleIndex relationRules = new MapCSSRuleIndex();     // relations that are not multipolygon relations
    public final MapCSSRuleIndex multipolygonRules = new MapCSSRuleIndex(); // multipolygon relations
    public final MapCSSRuleIndex canvasRules = new MapCSSRuleIndex();       // rules to apply canvas properties

    private Color backgroundColorOverride;
    private String css = null;
    private ZipFile zipFile;

    /**
     * A collection of {@link MapCSSRule}s, that are indexed by tag key and value.
     * 
     * Speeds up the process of finding all rules that match a certain primitive.
     * 
     * Rules with a {@link SimpleKeyValueCondition} [key=value] are indexed by
     * key and value in a HashMap. Now you only need to loop the tags of a
     * primitive to retrieve the possibly matching rules.
     * 
     * Rules with no SimpleKeyValueCondition in the selector have to be
     * checked separately.
     * 
     * The order of rules gets mixed up by this and needs to be sorted later.
     */
    public static class MapCSSRuleIndex {
        /* all rules for this index */
        public final List<MapCSSRule> rules = new ArrayList<>();
        /* tag based index */
        public final Map<String,Map<String,Set<MapCSSRule>>> index = new HashMap<>();
        /* rules without SimpleKeyValueCondition */
        public final Set<MapCSSRule> remaining = new HashSet<>();
        
        public void add(MapCSSRule rule) {
            rules.add(rule);
        }

        /**
         * Initialize the index.
         */
        public void initIndex() {
            for (MapCSSRule r: rules) {
                // find the rightmost selector, this must be a GeneralSelector
                Selector selRightmost = r.selector;
                while (selRightmost instanceof ChildOrParentSelector) {
                    selRightmost = ((ChildOrParentSelector) selRightmost).right;
                }
                OptimizedGeneralSelector s = (OptimizedGeneralSelector) selRightmost;
                if (s.conds == null) {
                    remaining.add(r);
                    continue;
                }
                List<SimpleKeyValueCondition> sk = new ArrayList<>(Utils.filteredCollection(s.conds, SimpleKeyValueCondition.class));
                if (sk.isEmpty()) {
                    remaining.add(r);
                    continue;
                }
                SimpleKeyValueCondition c = sk.get(sk.size() - 1);
                Map<String,Set<MapCSSRule>> rulesWithMatchingKey = index.get(c.k);
                if (rulesWithMatchingKey == null) {
                    rulesWithMatchingKey = new HashMap<>();
                    index.put(c.k, rulesWithMatchingKey);
                }
                Set<MapCSSRule> rulesWithMatchingKeyValue = rulesWithMatchingKey.get(c.v);
                if (rulesWithMatchingKeyValue == null) {
                    rulesWithMatchingKeyValue = new HashSet<>();
                    rulesWithMatchingKey.put(c.v, rulesWithMatchingKeyValue);
                }
                rulesWithMatchingKeyValue.add(r);
            }
        }
        
        /**
         * Get a subset of all rules that might match the primitive.
         * @param osm the primitive to match
         * @return a Collection of rules that filters out most of the rules
         * that cannot match, based on the tags of the primitive
         */
        public Collection<MapCSSRule> getRuleCandidates(OsmPrimitive osm) {
            List<MapCSSRule> ruleCandidates = new ArrayList<>(remaining);
            for (Map.Entry<String,String> e : osm.getKeys().entrySet()) {
                Map<String,Set<MapCSSRule>> v = index.get(e.getKey());
                if (v != null) {
                    Set<MapCSSRule> rs = v.get(e.getValue());
                    if (rs != null)  {
                        ruleCandidates.addAll(rs);
                    }
                }
            }
            Collections.sort(ruleCandidates);
            return ruleCandidates;
        } 

        public void clear() {
            rules.clear();
            index.clear();
            remaining.clear();
        }
    }

    public MapCSSStyleSource(String url, String name, String shortdescription) {
        super(url, name, shortdescription);
    }

    public MapCSSStyleSource(SourceEntry entry) {
        super(entry);
    }

    /**
     * <p>Creates a new style source from the MapCSS styles supplied in
     * {@code css}</p>
     *
     * @param css the MapCSS style declaration. Must not be null.
     * @throws IllegalArgumentException thrown if {@code css} is null
     */
    public MapCSSStyleSource(String css) throws IllegalArgumentException{
        super(null, null, null);
        CheckParameterUtil.ensureParameterNotNull(css);
        this.css = css;
    }

    @Override
    public void loadStyleSource() {
        init();
        rules.clear();
        nodeRules.clear();
        wayRules.clear();
        wayNoAreaRules.clear();
        relationRules.clear();
        multipolygonRules.clear();
        canvasRules.clear();
        try (InputStream in = getSourceInputStream()) {
            try {
                // evaluate @media { ... } blocks
                MapCSSParser preprocessor = new MapCSSParser(in, "UTF-8", MapCSSParser.LexicalState.PREPROCESSOR);
                String mapcss = preprocessor.pp_root(this);

                // do the actual mapcss parsing
                InputStream in2 = new ByteArrayInputStream(mapcss.getBytes(StandardCharsets.UTF_8));
                MapCSSParser parser = new MapCSSParser(in2, "UTF-8", MapCSSParser.LexicalState.DEFAULT);
                parser.sheet(this);

                loadMeta();
                loadCanvas();
            } finally {
                closeSourceInputStream(in);
            }
        } catch (IOException e) {
            Main.warn(tr("Failed to load Mappaint styles from ''{0}''. Exception was: {1}", url, e.toString()));
            Main.error(e);
            logError(e);
        } catch (TokenMgrError e) {
            Main.warn(tr("Failed to parse Mappaint styles from ''{0}''. Error was: {1}", url, e.getMessage()));
            Main.error(e);
            logError(e);
        } catch (ParseException e) {
            Main.warn(tr("Failed to parse Mappaint styles from ''{0}''. Error was: {1}", url, e.getMessage()));
            Main.error(e);
            logError(new ParseException(e.getMessage())); // allow e to be garbage collected, it links to the entire token stream
        }
        // optimization: filter rules for different primitive types
        for (MapCSSRule r: rules) {
            // find the rightmost selector, this must be a GeneralSelector
            Selector selRightmost = r.selector;
            while (selRightmost instanceof ChildOrParentSelector) {
                selRightmost = ((ChildOrParentSelector) selRightmost).right;
            }
            MapCSSRule optRule = new MapCSSRule(r.selector.optimizedBaseCheck(), r.declaration);
            final String base = ((GeneralSelector) selRightmost).getBase();
            switch (base) {
                case "node":
                    nodeRules.add(optRule);
                    break;
                case "way":
                    wayNoAreaRules.add(optRule);
                    wayRules.add(optRule);
                    break;
                case "area":
                    wayRules.add(optRule);
                    multipolygonRules.add(optRule);
                    break;
                case "relation":
                    relationRules.add(optRule);
                    multipolygonRules.add(optRule);
                    break;
                case "*":
                    nodeRules.add(optRule);
                    wayRules.add(optRule);
                    wayNoAreaRules.add(optRule);
                    relationRules.add(optRule);
                    multipolygonRules.add(optRule);
                    break;
                case "canvas":
                    canvasRules.add(r);
                    break;
                case "meta":
                    break;
                default:
                    final RuntimeException e = new RuntimeException(MessageFormat.format("Unknown MapCSS base selector {0}", base));
                    Main.warn(tr("Failed to parse Mappaint styles from ''{0}''. Error was: {1}", url, e.getMessage()));
                    Main.error(e);
                    logError(e);
            }
        }
        nodeRules.initIndex();
        wayRules.initIndex();
        wayNoAreaRules.initIndex();
        relationRules.initIndex();
        multipolygonRules.initIndex();
        canvasRules.initIndex();
    }
    
    @Override
    public InputStream getSourceInputStream() throws IOException {
        if (css != null) {
            return new ByteArrayInputStream(css.getBytes(StandardCharsets.UTF_8));
        }
        MirroredInputStream in = getMirroredInputStream();
        if (isZip) {
            File file = in.getFile();
            Utils.close(in);
            zipFile = new ZipFile(file, StandardCharsets.UTF_8);
            zipIcons = file;
            ZipEntry zipEntry = zipFile.getEntry(zipEntryPath);
            return zipFile.getInputStream(zipEntry);
        } else {
            zipFile = null;
            zipIcons = null;
            return in;
        }
    }

    @Override
    public MirroredInputStream getMirroredInputStream() throws IOException {
        return new MirroredInputStream(url, null, MAPCSS_STYLE_MIME_TYPES);
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
        Cascade c = constructSpecial("meta");
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
        Cascade c = constructSpecial("canvas");
        backgroundColorOverride = c.get("fill-color", null, Color.class);
        if (backgroundColorOverride == null) {
            backgroundColorOverride = c.get("background-color", null, Color.class);
            if (backgroundColorOverride != null) {
                Main.warn(tr("Detected deprecated ''{0}'' in ''{1}'' which will be removed shortly. Use ''{2}'' instead.", "canvas{background-color}", url, "fill-color"));
            }
        }
    }

    private Cascade constructSpecial(String type) {

        MultiCascade mc = new MultiCascade();
        Node n = new Node();
        String code = LanguageInfo.getJOSMLocaleCode();
        n.put("lang", code);
        // create a fake environment to read the meta data block
        Environment env = new Environment(n, mc, "default", this);

        for (MapCSSRule r : rules) {
            if ((r.selector instanceof GeneralSelector)) {
                GeneralSelector gs = (GeneralSelector) r.selector;
                if (gs.getBase().equals(type)) {
                    if (!gs.matchesConditions(env)) {
                        continue;
                    }
                    r.execute(env);
                }
            }
        }
        return mc.getCascade("default");
    }

    @Override
    public Color getBackgroundColorOverride() {
        return backgroundColorOverride;
    }

    @Override
    public void apply(MultiCascade mc, OsmPrimitive osm, double scale, OsmPrimitive multipolyOuterWay, boolean pretendWayIsClosed) {
        Environment env = new Environment(osm, mc, null, this);
        MapCSSRuleIndex matchingRuleIndex;
        if (osm instanceof Node) {
            matchingRuleIndex = nodeRules;
        } else if (osm instanceof Way) {
            if (osm.isKeyFalse("area")) {
                matchingRuleIndex = wayNoAreaRules;
            } else {
                matchingRuleIndex = wayRules;
            }
        } else {
            if (((Relation) osm).isMultipolygon()) {
                matchingRuleIndex = multipolygonRules;
            } else if (osm.hasKey("#canvas")) {
                matchingRuleIndex = canvasRules;
            } else {
                matchingRuleIndex = relationRules;
            }
        }
        
        // the declaration indices are sorted, so it suffices to save the
        // last used index
        int lastDeclUsed = -1;

        for (MapCSSRule r : matchingRuleIndex.getRuleCandidates(osm)) {
            env.clearSelectorMatchingInformation();
            env.layer = r.selector.getSubpart();
            if (r.selector.matches(env)) { // as side effect env.parent will be set (if s is a child selector)
                Selector s = r.selector;
                if (s.getRange().contains(scale)) {
                    mc.range = Range.cut(mc.range, s.getRange());
                } else {
                    mc.range = mc.range.reduceAround(scale, s.getRange());
                    continue;
                }

                if (r.declaration.idx == lastDeclUsed) continue; // don't apply one declaration more than once
                lastDeclUsed = r.declaration.idx;
                String sub = s.getSubpart();
                if (sub == null) {
                    sub = "default";
                }
                else if ("*".equals(sub)) {
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

    public boolean evalMediaExpression(String feature, Object val) {
        if ("user-agent".equals(feature)) {
            String s = Cascade.convertTo(val, String.class);
            if ("josm".equals(s)) return true;
        }
        if ("min-josm-version".equals(feature)) {
            Float v = Cascade.convertTo(val, Float.class);
            if (v != null) return Math.round(v) <= Version.getInstance().getVersion();
        }
        if ("max-josm-version".equals(feature)) {
            Float v = Cascade.convertTo(val, Float.class);
            if (v != null) return Math.round(v) >= Version.getInstance().getVersion();
        }
        return false;
    }

    @Override
    public String toString() {
        return Utils.join("\n", rules);
    }
}
