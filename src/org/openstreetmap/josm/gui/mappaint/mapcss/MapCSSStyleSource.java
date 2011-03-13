// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.Range;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.GeneralSelector;
import org.openstreetmap.josm.gui.mappaint.mapcss.parser.MapCSSParser;
import org.openstreetmap.josm.gui.mappaint.mapcss.parser.ParseException;
import org.openstreetmap.josm.gui.mappaint.mapcss.parser.TokenMgrError;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Utils;

public class MapCSSStyleSource extends StyleSource {
    //static private final Logger logger = Logger.getLogger(MapCSSStyleSource.class.getName());

    final public List<MapCSSRule> rules;
    private Color backgroundColorOverride;

    public MapCSSStyleSource(String url, String name, String shortdescription) {
        super(url, name, shortdescription);
        rules = new ArrayList<MapCSSRule>();
    }

    public MapCSSStyleSource(SourceEntry entry) {
        super(entry);
        rules = new ArrayList<MapCSSRule>();
    }

    @Override
    public void loadStyleSource() {
        init();
        rules.clear();
        try {
            MapCSSParser parser = new MapCSSParser(getSourceInputStream(), "UTF-8");
            parser.sheet(this);
            loadMeta();
            loadCanvas();
        } catch(IOException e) {
            System.err.println(tr("Warning: failed to load Mappaint styles from ''{0}''. Exception was: {1}", url, e.toString()));
            e.printStackTrace();
            logError(e);
        } catch (TokenMgrError e) {
            System.err.println(tr("Warning: failed to parse Mappaint styles from ''{0}''. Error was: {1}", url, e.getMessage()));
            e.printStackTrace();
            logError(e);
        } catch (ParseException e) {
            System.err.println(tr("Warning: failed to parse Mappaint styles from ''{0}''. Error was: {1}", url, e.getMessage()));
            e.printStackTrace();
            logError(new ParseException(e.getMessage())); // allow e to be garbage collected, it links to the entire token stream
        }
    }

    @Override
    public InputStream getSourceInputStream() throws IOException {
        MirroredInputStream in = new MirroredInputStream(url);
        InputStream zip = in.getZipEntry("mapcss", "style");
        if (zip != null) {
            zipIcons = in.getFile();
            return zip;
        } else {
            zipIcons = null;
            return in;
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
                    if (gs.base.equals(type))
                     {
                        for (Condition cnd : gs.conds) {
                            if (!cnd.applies(env))
                                continue NEXT_RULE;
                        }
                        for (Instruction i : r.declaration) {
                            i.execute(env);
                        }
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
        for (MapCSSRule r : rules) {
            for (Selector s : r.selectors) {
                if (s.applies(env)) {
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

                    if (sub.equals("*")) {
                        for (Entry<String, Cascade> entry : mc.getLayers()) {
                            env.layer = entry.getKey();
                            if (Utils.equal(env.layer, "*")) {
                                continue;
                            }
                            for (Instruction i : r.declaration) {
                                i.execute(env);
                            }
                        }
                    }
                    env.layer = sub;
                    for (Instruction i : r.declaration) {
                        i.execute(env);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return Utils.join("\n", rules);
    }
}
