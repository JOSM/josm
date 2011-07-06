// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.io.MirroredInputStream;

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

    public void load() {
        layers.clear();
        for(Collection<String> c : Main.pref.getArray("imagery.layers",
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
    }

    public void loadDefaults(boolean clearCache) {
        defaultLayers.clear();
        Collection<String> defaults = Main.pref.getCollection(
                "imagery.layers.default", Collections.<String>emptySet());
        ArrayList<String> defaultsSave = new ArrayList<String>();
        for(String source : Main.pref.getCollection("imagery.layers.sites", Arrays.asList(DEFAULT_LAYER_SITES)))
        {
            try
            {
                if (clearCache) {
                    MirroredInputStream.cleanup(source);
                }
                MirroredInputStream s = new MirroredInputStream(source, -1);
                try {
                    InputStreamReader r;
                    try
                    {
                        r = new InputStreamReader(s, "UTF-8");
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        r = new InputStreamReader(s);
                    }
                    BufferedReader reader = new BufferedReader(r);
                    String line;
                    while((line = reader.readLine()) != null)
                    {
                        String val[] = line.split(";");
                        if(!line.startsWith("#") && val.length >= 3) {
                            boolean force = "true".equals(val[0]);
                            String name = tr(val[1]);
                            String url = val[2];
                            String eulaAcceptanceRequired = null;

                            if (val.length >= 4 && !val[3].isEmpty()) {
                                // 4th parameter optional for license agreement (EULA)
                                eulaAcceptanceRequired = val[3];
                            }

                            ImageryInfo info = new ImageryInfo(name, url, eulaAcceptanceRequired);

                            if (val.length >= 5 && !val[4].isEmpty()) {
                                // 5th parameter optional for bounds
                                try {
                                    info.setBounds(new Bounds(val[4], ","));
                                } catch (IllegalArgumentException e) {
                                    Main.warn(e.toString());
                                }
                            }
                            if (val.length >= 6 && !val[5].isEmpty()) {
                                info.setAttributionText(val[5]);
                            }
                            if (val.length >= 7 && !val[6].isEmpty()) {
                                info.setAttributionLinkURL(val[6]);
                            }
                            if (val.length >= 8 && !val[7].isEmpty()) {
                                info.setTermsOfUseURL(val[7]);
                            }
                            if (val.length >= 9 && !val[8].isEmpty()) {
                                info.setAttributionImage(val[8]);
                            }

                            defaultLayers.add(info);

                            if (force) {
                                defaultsSave.add(url);
                                if (!defaults.contains(url)) {
                                    for (ImageryInfo i : layers) {
                                        if ((i.getImageryType() == ImageryType.WMS && url.equals(i.getUrl()))
                                                || url.equals(i.getFullUrl())) {
                                            force = false;
                                        }
                                    }
                                    if (force) {
                                        add(new ImageryInfo(name, url));
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    s.close();
                }
            }
            catch (IOException e)
            {
            }
        }

        Collections.sort(defaultLayers);
        Main.pref.putCollection("imagery.layers.default", defaultsSave.size() > 0
                ? defaultsSave : defaults);
    }

    public void add(ImageryInfo info) {
        layers.add(info);
    }

    public void remove(ImageryInfo info) {
        layers.remove(info);
    }

    public void save() {
        LinkedList<Collection<String>> coll = new LinkedList<Collection<String>>();
        for (ImageryInfo info : layers) {
            coll.add(info.getInfoArray());
        }
        Main.pref.putArray("imagery.layers", coll);
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
