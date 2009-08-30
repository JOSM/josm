// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.io.IOException;

/**
 * This interface allows platfrom (operating system) dependent code
 * to be bundled into self-contained classes.
 *
 * For plugin authors:
 * To implement your own PlatformHook class, implement this interface,
 * then create the class when your plugin is loaded and store it in
 * Main.platform. Please not that the two "startup" hooks will be
 * called _before_ your plugin is loaded. If you need to hook there,
 * split your class into two (one containing only the startup hooks,
 * and one with the remainder) and send the startup class, together
 * with propper OS detection code (see Main) for inclusion with
 * JOSM to the JOSM team.
 *
 * Also, it might be a good idea to extend PlatformHookUnixoid.
 * That class has a more or less neutral behaviour, that should
 * work on all platforms supported by J2SE.
 *
 * Attention: At this time this interface is not to be considered
 * complete.
 */
public interface PlatformHook {
    /**
      * The preStartupHook will be called extremly early. It is
      * guaranteed to be called before the GUI setup has started.
      *
      * Reason: On OSX we need to inform the Swing libraries
      * that we want to be integrated with the OS before we setup
      * our GUI.
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
      * default webbrowser.
      */
    public void openUrl(String url) throws IOException;

    /**
      * The initShortcutGroups hook will be called by the
      * Shortcut class if it detects that there are no
      * groups in teh config file. So that will happen
      * once on each JOSM installation only.
      *
      * Please note that ShorCut will load its config on demand,
      * that is, at the moment the first shortcut is registered.
      *
      * In this hook, you have to fill the preferences with
      * data, not the internal structures! Also, do not try
      * to register any shortcuts from within.
      */
    public void initShortcutGroups();

    /**
      * The initSystemShortcuts hook will be called by the
      * Shortcut class after the modifier groups have been read
      * from the config, but before any shortcuts are read from
      * it or registered from within the application.
      *
      * Plese note that you are not allowed to register any
      * shortuts from this hook, but only "systemCuts"!
      *
      * BTW: SystemCuts should be named "system:<whatever>",
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
      * JVM is to dumb to provide correct names for all the keys.
      *
      * Another reason not to use the implementation in the *nix
      * hook are LAFs that don't understand HTML, such as the OSX
      * LAFs.
      */
    public String makeTooltip(String name, Shortcut sc);
}
