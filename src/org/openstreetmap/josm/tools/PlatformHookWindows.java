// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import org.openstreetmap.josm.Main;

/**
  * see PlatformHook.java
  */
public class PlatformHookWindows extends PlatformHookUnixoid implements PlatformHook {
    @Override
    public void openUrl(String url) throws IOException {
        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
    }

    @Override
    public void initSystemShortcuts() {
        // This list if far from complete!
        Shortcut.registerSystemShortcut("system:exit", tr("unused"), KeyEvent.VK_F4, KeyEvent.ALT_DOWN_MASK).setAutomatic(); // items with automatic shortcuts will not be added to the menu bar at all
        Shortcut.registerSystemShortcut("system:menuexit", tr("unused"), KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK);
        Shortcut.registerSystemShortcut("system:copy", tr("unused"), KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK);
        Shortcut.registerSystemShortcut("system:paste", tr("unused"), KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK);
        Shortcut.registerSystemShortcut("system:cut", tr("unused"), KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK);
        Shortcut.registerSystemShortcut("system:duplicate", tr("unused"), KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK); // not really system, but to avoid odd results
        Shortcut.registerSystemShortcut("system:help", tr("unused"), KeyEvent.VK_F1, 0);
    }

    @Override
    public String getDefaultStyle()
    {
        return "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
    }

    @Override
    public boolean rename(File from, File to)
    {
        if(to.exists())
            to.delete();
        return from.renameTo(to);
    }
}
