// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.xml.XmlStyleSource;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.gui.preferences.MapPaintPreference.MapPaintPrefMigration;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.ImageProvider;

public class MapPaintStyles {

    private static ElemStyles styles = new ElemStyles();
    private static Collection<String> iconDirs;

    public static ElemStyles getStyles()
    {
        return styles;
    }
    
    public static class IconReference {

        public String iconName;
        public XmlStyleSource source;

        public IconReference(String iconName, XmlStyleSource source) {
            this.iconName = iconName;
            this.source = source;
        }
    }

    public static ImageIcon getIcon(IconReference ref)
    {
        String styleName = ref.source.getPrefName();
        List<String> dirs = new LinkedList<String>();
        for(String fileset : iconDirs)
        {
            String[] a;
            if(fileset.indexOf("=") >= 0) {
                a = fileset.split("=", 2);
            } else {
                a = new String[] {"", fileset};
            }

            /* non-prefixed path is generic path, always take it */
            if(a[0].length() == 0 || styleName.equals(a[0])) {
                dirs.add(a[1]);
            }
        }
        ImageIcon i = ImageProvider.getIfAvailable(dirs, "mappaint."+styleName, null, ref.iconName, ref.source.zipIcons);
        if(i == null)
        {
            System.out.println("Mappaint style \""+styleName+"\" icon \"" + ref.iconName + "\" not found.");
            i = ImageProvider.getIfAvailable(dirs, "mappaint."+styleName, null, "misc/no_icon.png");
        }
        return i;
    }

    @SuppressWarnings("null")
    public static void readFromPreferences() {
        iconDirs = Main.pref.getCollection("mappaint.icon.sources", Collections.<String>emptySet());
        if(Main.pref.getBoolean("mappaint.icon.enable-defaults", true))
        {
            LinkedList<String> f = new LinkedList<String>(iconDirs);
            /* don't prefix icon path, as it should be generic */
            f.add("resource://images/styles/standard/");
            f.add("resource://images/styles/");
            iconDirs = f;
        }

        Collection<? extends SourceEntry> sourceEntries = (new MapPaintPrefMigration()).get();

        for (SourceEntry entry : sourceEntries) {
            StyleSource style = null;
            try {
                MirroredInputStream in = new MirroredInputStream(entry.url);
                InputStream zip = in.getZipEntry("xml","style");
                if (zip != null) {
                    style = new XmlStyleSource(entry);
                    continue;
                }
                zip = in.getZipEntry("mapcss","style");
                if (zip != null) {
                    style = new MapCSSStyleSource(entry);
                    continue;
                }
                if (entry.url.toLowerCase().endsWith(".mapcss")) {
                    style = new MapCSSStyleSource(entry);
                } else {
                    style = new XmlStyleSource(entry);
                }
            } catch(IOException e) {
                System.err.println(tr("Warning: failed to load Mappaint styles from ''{0}''. Exception was: {1}", entry.url, e.toString()));
                e.printStackTrace();
                if (style != null) {
                    style.hasError = true;
                }
            }
            if (style != null) {
                styles.add(style);
            }
        }
        for (StyleSource s : styles.getStyleSources()) {
            s.loadStyleSource();
        }
    }

    public static class MapPaintStyleLoader extends PleaseWaitRunnable {
        private boolean canceled;
        private List<StyleSource> sources;

        public MapPaintStyleLoader(List<StyleSource> sources) {
            super(tr("Reloading style sources"));
            this.sources = sources;
        }

        @Override
        protected void cancel() {
            canceled = true;
        }

        @Override
        protected void finish() {
        }

        @Override
        protected void realRun() {
            ProgressMonitor monitor = getProgressMonitor();
            monitor.setTicksCount(sources.size());
            for (StyleSource s : sources) {
                if (canceled)
                    return;
                monitor.subTask(tr("loading style ''{0}''...", s.getDisplayString()));
                s.loadStyleSource();
                monitor.worked(1);
            }
        }
    }

}
