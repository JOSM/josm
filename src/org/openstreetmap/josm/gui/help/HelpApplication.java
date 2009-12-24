// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.I18n;

/**
 * The JOSM help browser wrapped in its own application. It is listening to commands
 * on standard in.
 *
 */
public class HelpApplication {
    static private final Logger logger = Logger.getLogger(HelpApplication.class.getName());
    private HelpBrowser browser;
    private HelpBrowserCommandProcessor commandProcessor;

    protected void setGeometry(HelpBrowser browser) {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        d.width = Math.min(800, d.width -50);
        Rectangle bounds = new Rectangle(
                new Point(
                        Toolkit.getDefaultToolkit().getScreenSize().width - d.width,
                        0
                ),
                d
        );
        browser.setBounds(bounds);
    }

    public void start() {
        browser = new HelpBrowser();
        setGeometry(browser);
        commandProcessor = new HelpBrowserCommandProcessor(browser);
        new Thread(commandProcessor).start();
    }

    static public void main(String argArray[]) {
        I18n.init();
        // initialize the plaform hook, and
        Main.determinePlatformHook();
        // call the really early hook before we anything else
        Main.platform.preStartupHook();

        // construct argument table
        final Map<String, Collection<String>> args = new HashMap<String, Collection<String>>();
        for (String arg : argArray) {
            if (!arg.startsWith("--")) {
                arg = "--download="+arg;
            }
            int i = arg.indexOf('=');
            String key = i == -1 ? arg.substring(2) : arg.substring(2,i);
            String value = i == -1 ? "" : arg.substring(i+1);
            Collection<String> v = args.get(key);
            if (v == null) {
                v = new LinkedList<String>();
            }
            v.add(value);
            args.put(key, v);
        }

        Main.pref.init(false /* don't reset preferences */);

        // Check if passed as parameter
        if (args.containsKey("language")) {
            I18n.set((String)(args.get("language").toArray()[0]));
        } else {
            I18n.set(Main.pref.get("language", null));
        }
        MainApplication.preConstructorInit(args);
        Thread.setDefaultUncaughtExceptionHandler(
                new UncaughtExceptionHandler() {
                    public void uncaughtException(Thread t, Throwable e) {
                        StringWriter sw = new StringWriter();
                        e.printStackTrace(new PrintWriter(sw));
                        logger.log(Level.SEVERE, sw.getBuffer().toString());
                    }
                }
        );

        new HelpApplication().start();
    }
}
