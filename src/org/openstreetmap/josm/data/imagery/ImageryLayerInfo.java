// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryPreferenceEntry;
import org.openstreetmap.josm.io.imagery.ImageryReader;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * Manages the list of imagery entries that are shown in the imagery menu.
 */
public class ImageryLayerInfo {

    public static final ImageryLayerInfo instance = new ImageryLayerInfo();
    ArrayList<ImageryInfo> layers = new ArrayList<ImageryInfo>();
    static ArrayList<ImageryInfo> defaultLayers = new ArrayList<ImageryInfo>();

    private final static String[] DEFAULT_LAYER_SITES = {
        "http://josm.openstreetmap.de/maps"
    };

    private ImageryLayerInfo() {
    }

    public ImageryLayerInfo(ImageryLayerInfo info) {
        layers.addAll(info.layers);
    }

    public void clear() {
        layers.clear();
    }

    public void load() {
        boolean addedDefault = layers.size() != 0;
        List<ImageryPreferenceEntry> entries = Main.pref.getListOfStructs("imagery.entries", null, ImageryPreferenceEntry.class);
        if (entries == null) {
            /* FIXME: Remove old format ~ March 2012 */
            boolean hasOld = loadOld();
            if (hasOld) {
                save();
            }
        } else {
            for (ImageryPreferenceEntry prefEntry : entries) {
                ImageryInfo i = new ImageryInfo(prefEntry);
                add(i);
            }
            Collections.sort(layers);
        }
        if (addedDefault) {
            save();
        }
    }

    public boolean loadOld() {
        Collection<Collection<String>> entries = Main.pref.getArray("imagery.layers", null);
        if (entries != null) {
            for (Collection<String> c : Main.pref.getArray("imagery.layers",
                    Collections.<Collection<String>>emptySet())) {
                ImageryInfo i = new ImageryInfo(c);
                String url = i.getUrl();
                if(url != null) {
                    /* FIXME: Remove the attribution copy stuff end of 2011 */
                    if(!i.hasAttribution()) {
                        for(ImageryInfo d : defaultLayers) {
                            if(url.equals(d.getUrl())) {
                                i.copyAttribution(d);
                                i.setBounds(d.getBounds());
                                break;
                            }
                        }
                    }
                    add(i);
                }
            }
            Collections.sort(layers);
            return true;
        }
        return false;
    }

    public void loadDefaults(boolean clearCache) {
        defaultLayers.clear();
        for (String source : Main.pref.getCollection("imagery.layers.sites", Arrays.asList(DEFAULT_LAYER_SITES))) {
            if (clearCache) {
                MirroredInputStream.cleanup(source);
            }
            MirroredInputStream stream = null;
            try {
                ImageryReader reader = new ImageryReader(source);
                Collection<ImageryInfo> result = reader.parse();
                defaultLayers.addAll(result);
            } catch (IOException ex) {
                Utils.close(stream);
                ex.printStackTrace();
                continue;
            } catch (SAXException sex) {
                Utils.close(stream);
                sex.printStackTrace();
                continue;
            }
        }
        while (defaultLayers.remove(null)) {}

        Collection<String> defaults = Main.pref.getCollection("imagery.layers.default");
        ArrayList<String> defaultsSave = new ArrayList<String>();
        for (ImageryInfo def : defaultLayers) {
            if (def.isDefaultEntry()) {
                defaultsSave.add(def.getUrl());

                boolean isKnownDefault = false;
                for (String url : defaults) {
                    if (isSimilar(url, def.getUrl())) {
                        isKnownDefault = true;
                        break;
                    }
                }
                boolean isInUserList = false;
                if (!isKnownDefault) {
                    for (ImageryInfo i : layers) {
                        if (isSimilar(def.getUrl(), i.getUrl())) {
                            isInUserList = true;
                            break;
                        }
                    }
                }
                if (!isKnownDefault && !isInUserList) {
                    add(new ImageryInfo(def));
                }
            }
        }

        Collections.sort(defaultLayers);
        Main.pref.putCollection("imagery.layers.default", defaultsSave.size() > 0
                ? defaultsSave : defaults);
    }

    // some additional checks to respect extended URLs in preferences (legacy workaround)
    private boolean isSimilar(String a, String b) {
        return Utils.equal(a, b) || (a != null && b != null && !"".equals(a) && !"".equals(b) && (a.contains(b) || b.contains(a)));
    }

    public void add(ImageryInfo info) {
        layers.add(info);
    }

    public void remove(ImageryInfo info) {
        layers.remove(info);
    }

    public void save() {
        List<ImageryPreferenceEntry> entries = new ArrayList<ImageryPreferenceEntry>();
        for (ImageryInfo info : layers) {
            entries.add(new ImageryPreferenceEntry(info));
        }
        Main.pref.putListOfStructs("imagery.entries", entries, ImageryPreferenceEntry.class);
    }

    public List<ImageryInfo> getLayers() {
        return Collections.unmodifiableList(layers);
    }

    public List<ImageryInfo> getDefaultLayers() {
        return Collections.unmodifiableList(defaultLayers);
    }

    public static void addLayer(ImageryInfo info) {
        instance.add(info);
        instance.save();
        Main.main.menu.imageryMenu.refreshImageryMenu();
    }
}
