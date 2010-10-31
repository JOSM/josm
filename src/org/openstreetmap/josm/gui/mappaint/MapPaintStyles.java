package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.XmlObjectParser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class MapPaintStyles {

    private static ElemStyles styles = new ElemStyles();
    private static Collection<String> iconDirs;
    private static File zipIcons;

    public static ElemStyles getStyles()
    {
        return styles;
    }

    public static ImageIcon getIcon(String name, String styleName)
    {
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
        ImageIcon i = ImageProvider.getIfAvailable(dirs, "mappaint."+styleName, null, name, zipIcons);
        if(i == null)
        {
            System.out.println("Mappaint style \""+styleName+"\" icon \"" + name + "\" not found.");
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

        Collection<String> files = Main.pref.getCollection("mappaint.style.sources", Collections.<String>emptySet());
        if (Main.pref.getBoolean("mappaint.style.enable-defaults", true)) {
            LinkedList<String> f = new LinkedList<String>();
            f.add("resource://data/elemstyles.xml");
            f.addAll(files);
            files = f;
        }

        for (String file : files) {
            String[] a = null;
            try {
                if (file.indexOf("=") >= 0) {
                    a = file.split("=", 2);
                } else {
                    a = new String[] { null, file };
                }
                XmlObjectParser parser = new XmlObjectParser(new ElemStyleHandler(a[0]));
                MirroredInputStream in = new MirroredInputStream(a[1]);
                InputStream zip = in.getZipEntry("xml","style");
                InputStreamReader ins;
                if(zip != null)
                {
                    zipIcons = in.getFile();
                    ins = new InputStreamReader(zip);
                } else {
                    ins = new InputStreamReader(in);
                }
                parser.startWithValidation(ins, "http://josm.openstreetmap.de/mappaint-style-1.0",
                "resource://data/mappaint-style.xsd");
                while(parser.hasNext()) {
                }
            } catch(IOException e) {
                System.err.println(tr("Warning: failed to load Mappaint styles from ''{0}''. Exception was: {1}", a[1], e.toString()));
                e.printStackTrace();
            } catch(SAXParseException e) {
                System.err.println(tr("Warning: failed to parse Mappaint styles from ''{0}''. Error was: [{1}:{2}] {3}", a[1], e.getLineNumber(), e.getColumnNumber(), e.getMessage()));
                e.printStackTrace();
            } catch(SAXException e) {
                System.err.println(tr("Warning: failed to parse Mappaint styles from ''{0}''. Error was: {1}", a[1], e.getMessage()));
                e.printStackTrace();
            }
        }
        iconDirs = null;
        zipIcons = null;
    }
}
