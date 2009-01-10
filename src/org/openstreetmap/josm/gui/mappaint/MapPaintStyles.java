package org.openstreetmap.josm.gui.mappaint;

import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.ImageProvider;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class MapPaintStyles {

    private static ElemStyles styles = new ElemStyles();
    private static String iconDirs;

    public static ElemStyles getStyles()
    {
        return styles;
    }

    public static ImageIcon getIcon(String name, String styleName)
    {
        List<String> dirs = new LinkedList<String>();
        for(String fileset : iconDirs.split(";"))
        {
            String[] a;
            if(fileset.indexOf("=") >= 0)
                a = fileset.split("=", 2);
            else
                a = new String[] {"", fileset};

            /* non-prefixed path is generic path, always take it */
            if(a[0].length() == 0 || styleName.equals(a[0]))
                dirs.add(a[1]);
        }
        ImageIcon i = ImageProvider.getIfAvailable(dirs, "mappaint."+styleName, null, name);
        if(i == null)
        {
            System.out.println("Mappaint-Style \""+styleName+"\" icon \"" + name + "\" not found.");
            i = ImageProvider.getIfAvailable(dirs, "mappaint."+styleName, null, "misc/no_icon.png");
        }
        return i;
    }

    public static void readFromPreferences() {
        String[] a = null;
        
        /* don't prefix icon path, as it should be generic */
        String internalicon = "resource://images/styles/standard/;resource://images/styles/";
        String internalfile = "standard=resource://styles/standard/elemstyles.xml";

        iconDirs = Main.pref.get("mappaint.icon.sources");
        if(Main.pref.getBoolean("mappaint.icon.enable-defaults", true))
            iconDirs = iconDirs == null || iconDirs.length() == 0 ? internalicon : iconDirs + ";" + internalicon;

        String file = Main.pref.get("mappaint.style.sources");
        if(Main.pref.getBoolean("mappaint.style.enable-defaults", true))
            file = (file == null || file.length() == 0) ? internalfile : internalfile + ";" + file;

        for(String fileset : file.split(";"))
        {
            try
            {
                if(fileset.indexOf("=") >= 0)
                    a = fileset.split("=", 2);
                else
                    a = new String[] {"standard", fileset};
                XMLReader xmlReader = XMLReaderFactory.createXMLReader();
                ElemStyleHandler handler = new ElemStyleHandler(a[0]);
                xmlReader.setContentHandler(handler);
                xmlReader.setErrorHandler(handler);
                xmlReader.parse(new InputSource(new MirroredInputStream(a[1])));
            }
            catch (Exception e)
            {
                System.out.println("Mappaint-Style \"" + a[0] + "\" file \"" + a[1] + "\"");
                System.out.println("Mappaint-Style problems: " + e);
            }
        }
        iconDirs = null;
    }

}
