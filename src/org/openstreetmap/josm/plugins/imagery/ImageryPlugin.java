package org.openstreetmap.josm.plugins.imagery;

import java.io.File;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

public class ImageryPlugin extends Plugin  {


    public ImageryLayerInfo info = new ImageryLayerInfo();
    // remember state of menu item to restore on changed preferences

    public ImageryPlugin(PluginInformation info) {
        super(info);
        this.info.load();
        OffsetBookmark.loadBookmarks();

    }

    @Override
    public String getPluginDir()
    {
        return new File(Main.pref.getPluginsDirectory(), "imagery").getPath();
    }

}
