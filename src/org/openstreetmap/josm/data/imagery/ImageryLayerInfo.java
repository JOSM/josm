// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryPreferenceEntry;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.io.imagery.ImageryReader;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * Manages the list of imagery entries that are shown in the imagery menu.
 */
public class ImageryLayerInfo {

    public static final ImageryLayerInfo instance = new ImageryLayerInfo();
    List<ImageryInfo> layers = new ArrayList<ImageryInfo>();
    static List<ImageryInfo> defaultLayers = new ArrayList<ImageryInfo>();

    private final static String[] DEFAULT_LAYER_SITES = {
        Main.JOSM_WEBSITE+"/maps"
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
        boolean addedDefault = !layers.isEmpty();
        List<ImageryPreferenceEntry> entries = Main.pref.getListOfStructs("imagery.entries", null, ImageryPreferenceEntry.class);
        if (entries != null) {
            for (ImageryPreferenceEntry prefEntry : entries) {
                try {
                    ImageryInfo i = new ImageryInfo(prefEntry);
                    add(i);
                } catch (IllegalArgumentException e) {
                    Main.warn("Unable to load imagery preference entry:"+e);
                }
            }
            Collections.sort(layers);
        }
        if (addedDefault) {
            save();
        }
    }

    /**
     * Loads the available imagery entries.
     *
     * The data is downloaded from the JOSM website (or loaded from cache).
     * Entries marked as "default" are added to the user selection, if not
     * already present.
     *
     * @param clearCache if true, clear the cache and start a fresh download.
     */
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
                Main.error(ex, false);
                continue;
            } catch (SAXException ex) {
                Utils.close(stream);
                Main.error(ex);
                continue;
            }
        }
        while (defaultLayers.remove(null));

        Collection<String> defaults = Main.pref.getCollection("imagery.layers.default");
        List<String> defaultsSave = new ArrayList<String>();
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
        Main.pref.putCollection("imagery.layers.default", defaultsSave.isEmpty() ? defaults : defaultsSave);
    }

    // some additional checks to respect extended URLs in preferences (legacy workaround)
    private boolean isSimilar(String a, String b) {
        return Utils.equal(a, b) || (a != null && b != null && !a.isEmpty() && !b.isEmpty() && (a.contains(b) || b.contains(a)));
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
    }

    public static void addLayers(Collection<ImageryInfo> infos) {
        for (ImageryInfo i : infos) {
            instance.add(i);
        }
        instance.save();
        Collections.sort(instance.layers);
    }
}
