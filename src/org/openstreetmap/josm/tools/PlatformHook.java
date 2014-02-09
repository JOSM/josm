// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.io.File;
import java.io.IOException;

/**
 * This interface allows platform (operating system) dependent code
 * to be bundled into self-contained classes.
 * @since 1023
 */
public interface PlatformHook {

    /**
      * The preStartupHook will be called extremly early. It is
      * guaranteed to be called before the GUI setup has started.
      *
      * Reason: On OSX we need to inform the Swing libraries
      * that we want to be integrated with the OS before we setup our GUI.
      */
    public void preStartupHook();

    /**
      * The startupHook will be called early, but after the GUI
      * setup has started.
      *
      * Reason: On OSX we need to register some callbacks with the
      * OS, so we'll receive events from the system menu.
      */
    public void startupHook();

    /**
      * The openURL hook will be used to open an URL in the
      * default web browser.
     * @param url The URL to open
     * @throws IOException if any I/O error occurs
      */
    public void openUrl(String url) throws IOException;

    /**
      * The initSystemShortcuts hook will be called by the
      * Shortcut class after the modifier groups have been read
      * from the config, but before any shortcuts are read from
      * it or registered from within the application.
      *
      * Plese note that you are not allowed to register any
      * shortuts from this hook, but only "systemCuts"!
      *
      * BTW: SystemCuts should be named "system:&lt;whatever&gt;",
      * and it'd be best if sou'd recycle the names already used
      * by the Windows and OSX hooks. Especially the later has
      * really many of them.
      *
      * You should also register any and all shortcuts that the
      * operation system handles itself to block JOSM from trying
      * to use them---as that would just not work. Call setAutomatic
      * on them to prevent the keyboard preferences from allowing the
      * user to change them.
      */
    public void initSystemShortcuts();

    /**
      * The makeTooltip hook will be called whenever a tooltip for
      * a menu or button is created.
      *
      * Tooltips are usually not system dependent, unless the
      * JVM is too dumb to provide correct names for all the keys.
      *
      * Another reason not to use the implementation in the *nix
      * hook are LAFs that don't understand HTML, such as the OSX LAFs.
      *
     * @param name Tooltip text to display
     * @param sc Shortcut associated (to display accelerator between parenthesis)
     * @return Full tooltip text (name + accelerator)
      */
    public String makeTooltip(String name, Shortcut sc);

    /**
     * Returns the default LAF to be used on this platform to look almost as a native application.
     * @return The default native LAF for this platform
     */
    public String getDefaultStyle();

    /**
     * Determines if the platform allows full-screen.
     * @return {@code true} if full screen is allowed, {@code false} otherwise
     */
    public boolean canFullscreen();

    /**
     * Renames a file.
     * @param from Source file
     * @param to Target file
     * @return {@code true} if the file has been renamed, {@code false} otherwise
     */
    public boolean rename(File from, File to);

    /**
     * Returns a detailed OS description (at least family + version).
     * @return A detailed OS description.
     * @since 5850
     */
    public String getOSDescription();
}
