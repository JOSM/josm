// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * see PlatformHook.java
 *
 * BTW: THIS IS A STUB. See comments below for details.
 *
 * Don't write (Main.platform instanceof PlatformHookUnixoid) because other platform
 * hooks are subclasses of this class.
 */
public class PlatformHookUnixoid implements PlatformHook {
    @Override
    public void preStartupHook(){
    }

    @Override
    public void startupHook() {
    }

    @Override
    public void openUrl(String url) throws IOException {
        String[] programs = {"gnome-open", "kfmclient openURL", "firefox"};
        for (String program : programs) {
            try {
                Runtime.getRuntime().exec(program+" "+url);
                return;
            } catch (IOException e) {
            }
        }
    }

    @Override
    public void initSystemShortcuts() {
        // TODO: Insert system shortcuts here. See Windows and especially OSX to see how to.
        for(int i = KeyEvent.VK_F1; i <= KeyEvent.VK_F12; ++i)
            Shortcut.registerSystemShortcut("screen:toogle"+i, tr("reserved"), i, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic();
        Shortcut.registerSystemShortcut("system:reset", tr("reserved"), KeyEvent.VK_DELETE, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic();
        Shortcut.registerSystemShortcut("system:resetX", tr("reserved"), KeyEvent.VK_BACK_SPACE, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK).setAutomatic();
    }
    /**
     * This should work for all platforms. Yeah, should.
     * See PlatformHook.java for a list of reasons why
     * this is implemented here...
     */
    @Override
    public String makeTooltip(String name, Shortcut sc) {
        String result = "";
        result += "<html>";
        result += name;
        if (sc != null && sc.getKeyText().length() != 0) {
            result += " ";
            result += "<font size='-2'>";
            result += "("+sc.getKeyText()+")";
            result += "</font>";
        }
        result += "&nbsp;</html>";
        return result;
    }

    @Override
    public String getDefaultStyle() {
        return "javax.swing.plaf.metal.MetalLookAndFeel";
    }

    @Override
    public boolean canFullscreen()
    {
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice().isFullScreenSupported();
    }

    @Override
    public boolean rename(File from, File to)
    {
        return from.renameTo(to);
    }

    @Override
    public String getOSDescription() {
        String osName = System.getProperty("os.name");
        if ("Linux".equalsIgnoreCase(osName)) {
            try {
                Process p = Runtime.getRuntime().exec("lsb_release -ds");
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = Utils.strip(input.readLine());
                input.close();
                if (line != null && !line.isEmpty()) {
                    return line; 
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return osName;
    }
}
