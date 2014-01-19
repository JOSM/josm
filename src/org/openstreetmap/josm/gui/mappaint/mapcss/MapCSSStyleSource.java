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
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.Range;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
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
    public final List<MapCSSRule> rules;
    private Color backgroundColorOverride;
    private String css = null;
    private ZipFile zipFile;

    public MapCSSStyleSource(String url, String name, String shortdescription) {
        super(url, name, shortdescription);
        rules = new ArrayList<MapCSSRule>();
    }

    public MapCSSStyleSource(SourceEntry entry) {
        super(entry);
        rules = new ArrayList<MapCSSRule>();
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
        rules = new ArrayList<MapCSSRule>();
    }

    @Override
    public void loadStyleSource() {
        init();
        rules.clear();
        try {
            InputStream in = getSourceInputStream();
            try {
                MapCSSParser parser = new MapCSSParser(in, "UTF-8");
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
    }

    @Override
    public InputStream getSourceInputStream() throws IOException {
        if (css != null) {
            return new ByteArrayInputStream(css.getBytes(Utils.UTF_8));
        }
        MirroredInputStream in = new MirroredInputStream(url);
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

        NEXT_RULE:
        for (MapCSSRule r : rules) {
            for (Selector s : r.selectors) {
                if ((s instanceof GeneralSelector)) {
                    GeneralSelector gs = (GeneralSelector) s;
                    if (gs.getBase().equals(type)) {
                        if (!gs.matchesConditions(env)) {
                            continue NEXT_RULE;
                        }
                        r.execute(env);
                    }
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
        RULE: for (MapCSSRule r : rules) {
            for (Selector s : r.selectors) {
                env.clearSelectorMatchingInformation();
                if (s.matches(env)) { // as side effect env.parent will be set (if s is a child selector)
                    if (s.getRange().contains(scale)) {
                        mc.range = Range.cut(mc.range, s.getRange());
                    } else {
                        mc.range = mc.range.reduceAround(scale, s.getRange());
                        continue;
                    }

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
                    continue RULE;
                }
            }
        }
    }

    @Override
    public String toString() {
        return Utils.join("\n", rules);
    }
}
