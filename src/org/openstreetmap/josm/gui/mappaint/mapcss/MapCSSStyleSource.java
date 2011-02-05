// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.Range;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.parser.MapCSSParser;
import org.openstreetmap.josm.gui.mappaint.mapcss.parser.ParseException;
import org.openstreetmap.josm.gui.mappaint.mapcss.parser.TokenMgrError;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Utils;

public class MapCSSStyleSource extends StyleSource {
    
    final public List<MapCSSRule> rules;

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
        rules.clear();
        hasError = false;
        try {
            MirroredInputStream in = new MirroredInputStream(url);
            InputStream zip = in.getZipEntry("mapcss", "style");
            InputStream input;
            if (zip != null) {
                input = zip;
                zipIcons = in.getFile();
            } else {
                input = in;
                zipIcons = null;
            }
            MapCSSParser parser = new MapCSSParser(input, "UTF-8");
            parser.sheet(this);
            loadMeta();
        } catch(IOException e) {
            System.err.println(tr("Warning: failed to load Mappaint styles from ''{0}''. Exception was: {1}", url, e.toString()));
            e.printStackTrace();
            hasError = true;
        } catch (TokenMgrError e) {
            System.err.println(tr("Warning: failed to parse Mappaint styles from ''{0}''. Error was: {1}", url, e.getMessage()));
            e.printStackTrace();
            hasError = true;
        } catch (ParseException e) {
            System.err.println(tr("Warning: failed to parse Mappaint styles from ''{0}''. Error was: {1}", url, e.getMessage()));
            e.printStackTrace();
            hasError = true;
        }
    }

    /**
     * load meta info from a selector "meta"
     */
    private void loadMeta() {
        MultiCascade mc = new MultiCascade();
        Node n = new Node();
        String code = LanguageInfo.getJOSMLocaleCode();
        n.put("lang", code);
        // create a fake environment to read the meta data block
        Environment env = new Environment(n, mc, "default");

        NEXT_RULE:
        for (MapCSSRule r : rules) {
            for (Selector s : r.selectors) {
                if (s.base.equals("meta")) {
                    for (Condition cnd : s.conds) {
                        if (!cnd.applies(env))
                            continue NEXT_RULE;
                    }
                    for (Instruction i : r.declaration) {
                        i.execute(env);
                    }
                }
            }
        }
        Cascade c = mc.getCascade("default");
        String sd = c.get("title", null, String.class);
        if (sd != null) {
            this.shortdescription = sd;
        }
    }

    @Override
    public void apply(MultiCascade mc, OsmPrimitive osm, double scale, OsmPrimitive multipolyOuterWay, boolean pretendWayIsClosed) {
        Environment env = new Environment(osm, mc, null);
        for (MapCSSRule r : rules) {
            for (Selector s : r.selectors) {
                if (s.applies(env)) {
                    if (s.range.contains(scale)) {
                        mc.range = Range.cut(mc.range, s.range);
                    } else {
                        mc.range = mc.range.reduceAround(scale, s.range);
                        continue;
                    }

                    String sub = s.subpart;
                    if (sub == null) {
                        sub = "default";
                    }

                    Cascade c = mc.get(sub);
                    if (c == null) {
                        if (mc.containsKey("*")) {
                            c = mc.get("*").clone();
                        } else {
                            c = new Cascade();
                        }
                        mc.put(sub, c);
                    }

                    if (sub.equals("*")) { // fixme: proper subparts handling
                        for (Entry<String, Cascade> entry : mc.entrySet()) {
                            env.layer = entry.getKey();
                            for (Instruction i : r.declaration) {
                                i.execute(env);
                            }
                        }
                    } else {
                        env.layer = sub;
                        for (Instruction i : r.declaration) {
                            i.execute(env);
                        }
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
