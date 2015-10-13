// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryPreferenceEntry;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.OfflineAccessException;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.io.imagery.ImageryReader;
import org.xml.sax.SAXException;

/**
 * Manages the list of imagery entries that are shown in the imagery menu.
 */
public class ImageryLayerInfo {

    public static final ImageryLayerInfo instance = new ImageryLayerInfo();
    private final List<ImageryInfo> layers = new ArrayList<>();
    private final Map<String, ImageryInfo> layerIds = new HashMap<>();
    private static final List<ImageryInfo> defaultLayers = new ArrayList<>();
    private static final Map<String, ImageryInfo> defaultLayerIds = new HashMap<>();

    private static final String[] DEFAULT_LAYER_SITES = {
        Main.getJOSMWebsite()+"/maps"
    };

    /**
     * Returns the list of imagery layers sites.
     * @return the list of imagery layers sites
     * @since 7434
     */
    public static Collection<String> getImageryLayersSites() {
        return Main.pref.getCollection("imagery.layers.sites", Arrays.asList(DEFAULT_LAYER_SITES));
    }

    private ImageryLayerInfo() {
    }

    public ImageryLayerInfo(ImageryLayerInfo info) {
        layers.addAll(info.layers);
    }

    public void clear() {
        layers.clear();
        layerIds.clear();
    }

    public void load() {
        clear();
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
        loadDefaults(false);
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
        defaultLayerIds.clear();
        for (String source : getImageryLayersSites()) {
            boolean online = true;
            try {
                OnlineResource.JOSM_WEBSITE.checkOfflineAccess(source, Main.getJOSMWebsite());
            } catch (OfflineAccessException e) {
                Main.warn(e.getMessage());
                online = false;
            }
            if (clearCache && online) {
                CachedFile.cleanup(source);
            }
            try {
                ImageryReader reader = new ImageryReader(source);
                Collection<ImageryInfo> result = reader.parse();
                defaultLayers.addAll(result);
            } catch (IOException ex) {
                Main.error(ex, false);
            } catch (SAXException ex) {
                Main.error(ex);
            }
        }
        while (defaultLayers.remove(null)) {
            // Do nothing
        }
        Collections.sort(defaultLayers);
        buildIdMap(defaultLayers, defaultLayerIds);
        updateEntriesFromDefaults();
        buildIdMap(layers, layerIds);
    }

    /**
     * Build the mapping of unique ids to {@link ImageryInfo}s.
     * @param lst input list
     * @param idMap output map
     */
    private static void buildIdMap(List<ImageryInfo> lst, Map<String, ImageryInfo> idMap) {
        idMap.clear();
        Set<String> notUnique = new HashSet<>();
        for (ImageryInfo i : lst) {
            if (i.getId() != null) {
                if (idMap.containsKey(i.getId())) {
                    notUnique.add(i.getId());
                    Main.error("Id ''{0}'' is not unique - used by ''{1}'' and ''{2}''!",
                            i.getId(), i.getName(), idMap.get(i.getId()).getName());
                    continue;
                }
                idMap.put(i.getId(), i);
            }
        }
        for (String i : notUnique) {
            idMap.remove(i);
        }
    }

    /**
     * Update user entries according to the list of default entries.
     */
    public void updateEntriesFromDefaults() {
        // add new default entries to the user selection
        boolean changed = false;
        Collection<String> knownDefaults = Main.pref.getCollection("imagery.layers.default");
        Collection<String> newKnownDefaults = new TreeSet<>(knownDefaults);
        for (ImageryInfo def : defaultLayers) {
            // temporary migration code, so all user preferences will get updated with new settings from JOSM site (can be removed ~Dez. 2015)
            if (def.getNoTileHeaders() != null || def.getTileSize() > 0 || def.getMetadataHeaders() != null) {
                for (ImageryInfo i: layers) {
                    if (isSimilar(def,  i)) {
                        if (def.getNoTileHeaders() != null) {
                            i.setNoTileHeaders(def.getNoTileHeaders());
                        }
                        if (def.getTileSize() > 0) {
                            i.setTileSize(def.getTileSize());
                        }
                        if (def.getMetadataHeaders() != null && def.getMetadataHeaders().size() > 0) {
                            i.setMetadataHeaders(def.getMetadataHeaders());
                        }
                        changed = true;
                    }
                }
            }

            if (def.isDefaultEntry()) {
                boolean isKnownDefault = false;
                for (String url : knownDefaults) {
                    if (isSimilar(url, def.getUrl())) {
                        isKnownDefault = true;
                        break;
                    }
                }
                boolean isInUserList = false;
                if (!isKnownDefault) {
                    newKnownDefaults.add(def.getUrl());
                    for (ImageryInfo i : layers) {
                        if (isSimilar(def, i)) {
                            isInUserList = true;
                            break;
                        }
                    }
                }
                if (!isKnownDefault && !isInUserList) {
                    add(new ImageryInfo(def));
                    changed = true;
                }
            }
        }
        Main.pref.putCollection("imagery.layers.default", newKnownDefaults);

        // Add ids to user entries without id.
        // Only do this the first time for each id, so the user can have
        // custom entries that don't get updated automatically
        Collection<String> addedIds = Main.pref.getCollection("imagery.layers.addedIds");
        Collection<String> newAddedIds = new TreeSet<>(addedIds);
        for (ImageryInfo info : layers) {
            for (ImageryInfo def : defaultLayers) {
                if (isSimilar(def, info)) {
                    if (def.getId() != null && !addedIds.contains(def.getId())) {
                        if (!defaultLayerIds.containsKey(def.getId())) {
                            // ignore ids used more than once (have been purged from the map)
                            continue;
                        }
                        newAddedIds.add(def.getId());
                        if (info.getId() == null) {
                            info.setId(def.getId());
                            changed = true;
                        }
                    }
                }
            }
        }
        Main.pref.putCollection("imagery.layers.addedIds", newAddedIds);

        // automatically update user entries with same id as a default entry
        for (int i = 0; i < layers.size(); i++) {
            ImageryInfo info = layers.get(i);
            if (info.getId() == null) {
                continue;
            }
            ImageryInfo matchingDefault = defaultLayerIds.get(info.getId());
            if (matchingDefault != null && !matchingDefault.equalsPref(info)) {
                layers.set(i, matchingDefault);
                changed = true;
            }
        }

        if (changed) {
            save();
        }
    }

    private boolean isSimilar(ImageryInfo iiA, ImageryInfo iiB) {
        if (iiA == null)
            return false;
        if (!iiA.getImageryType().equals(iiB.getImageryType()))
            return false;
        if (iiA.getId() != null && iiB.getId() != null) return iiA.getId().equals(iiB.getId());
        return isSimilar(iiA.getUrl(), iiB.getUrl());
    }

    // some additional checks to respect extended URLs in preferences (legacy workaround)
    private static boolean isSimilar(String a, String b) {
        return Objects.equals(a, b) || (a != null && b != null && !a.isEmpty() && !b.isEmpty() && (a.contains(b) || b.contains(a)));
    }

    public void add(ImageryInfo info) {
        layers.add(info);
    }

    public void remove(ImageryInfo info) {
        layers.remove(info);
    }

    public void save() {
        List<ImageryPreferenceEntry> entries = new ArrayList<>();
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

    /**
     * Get unique id for ImageryInfo.
     *
     * This takes care, that no id is used twice (due to a user error)
     * @param info the ImageryInfo to look up
     * @return null, if there is no id or the id is used twice,
     * the corresponding id otherwise
     */
    public String getUniqueId(ImageryInfo info) {
        if (info.getId() != null && layerIds.get(info.getId()) == info) {
            return info.getId();
        }
        return null;
    }
}
