// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
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
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.ChildOrParentSelector;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.GeneralSelector;
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
    // rules filtered by primitive type
    public final List<MapCSSRule> nodeRules = new ArrayList<>();
    public final List<MapCSSRule> wayRules = new ArrayList<>();
    public final List<MapCSSRule> relationRules = new ArrayList<>();
    public final List<MapCSSRule> multipolygonRules = new ArrayList<>();
    public final List<MapCSSRule> canvasRules = new ArrayList<>();
    
    private Color backgroundColorOverride;
    private String css = null;
    private ZipFile zipFile;

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
        relationRules.clear();
        multipolygonRules.clear();
        canvasRules.clear();
        try (InputStream in = getSourceInputStream()) {
            try {
                // evaluate @media { ... } blocks
                MapCSSParser preprocessor = new MapCSSParser(in, "UTF-8", MapCSSParser.LexicalState.PREPROCESSOR);
                String mapcss = preprocessor.pp_root(this);
                
                // do the actual mapcss parsing
                InputStream in2 = new ByteArrayInputStream(mapcss.getBytes(Utils.UTF_8));
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
            switch (((GeneralSelector) selRightmost).getBase()) {
                case "node":
                    nodeRules.add(optRule);
                    break;
                case "way":
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
                    relationRules.add(optRule);
                    multipolygonRules.add(optRule);
                    break;
                case "canvas":
                    canvasRules.add(r);
            }
        }
    }
    
    @Override
    public InputStream getSourceInputStream() throws IOException {
        if (css != null) {
            return new ByteArrayInputStream(css.getBytes(Utils.UTF_8));
        }
        MirroredInputStream in = new MirroredInputStream(url, null, MAPCSS_STYLE_MIME_TYPES);
        if (isZip) {
            File file = in.getFile();
            Utils.close(in);
            zipFile = new ZipFile(file);
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
        backgroundColorOverride = c.get("background-color", null, Color.class);
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
        List<MapCSSRule> matchingRules;
        if (osm instanceof Node) {
            matchingRules = nodeRules;
        } else if (osm instanceof Way) {
            matchingRules = wayRules;
        } else {
            if (((Relation) osm).isMultipolygon()) {
                matchingRules = multipolygonRules;
            } else if (osm.hasKey("#canvas")) {
                matchingRules = canvasRules;
            } else {
                matchingRules = relationRules;
            }
        }
        
        // the declaration indices are sorted, so it suffices to save the
        // last used index
        int lastDeclUsed = -1;
        
        for (MapCSSRule r : matchingRules) {
            env.clearSelectorMatchingInformation();
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
                        if (Utils.equal(env.layer, "*")) {
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
