// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.applet.AppletContext;
import java.applet.AppletStub;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.ServerSidePreferences;
import org.openstreetmap.josm.gui.MainApplication.Option;
import org.openstreetmap.josm.gui.widgets.JosmPasswordField;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

public class MainApplet extends JApplet {

    final static JFrame frame = new JFrame("Java OpenStreetMap Editor");

    public static final class UploadPreferencesAction extends JosmAction {
        /**
         * Constructs a new {@code UploadPreferencesAction}.
         */
        public UploadPreferencesAction() {
            super(tr("Upload Preferences"), "upload-preferences", tr("Upload the current preferences to the server"),
                    Shortcut.registerShortcut("applet:uploadprefs", tr("Upload Preferences"), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            ((ServerSidePreferences)Main.pref).upload();
        }
    }

    private final class MainCaller extends Main {
        private MainCaller(Map<Option, Collection<String>> mapargs) {
            addListener();
            setContentPane(contentPanePrivate);
            setJMenuBar(menu);
            postConstructorProcessCmdLine(mapargs);
        }
    }

    private final static String[][] paramInfo = {
        {"username", tr("string"), tr("Name of the user.")},
        {"password", tr("string"), tr("OSM Password.")},
        {"geometry", tr("string"), tr("Resize the applet to the given geometry (format: WIDTHxHEIGHT)")},
        {"download", tr("string;string;..."), tr("Download each. Can be x1,y1,x2,y2 an URL containing lat=y&lon=x&zoom=z or a filename")},
        {"downloadgps", tr("string;string;..."), tr("Download each as raw gps. Can be x1,y1,x2,y2 an URL containing lat=y&lon=x&zoom=z or a filename")},
        {"selection", tr("string;string;..."), tr("Add each to the initial selection. Can be a google-like search string or an URL which returns osm-xml")},
        {"reset-preferences", tr("any"),tr("If specified, reset the configuration instead of reading it.")}
    };

    private Map<String, Collection<String>> args = new HashMap<String, Collection<String>>();

    @Override public String[][] getParameterInfo() {
        return paramInfo;
    }

    @Override public void init() {
        for (String[] s : paramInfo) {
            Collection<String> p = readParameter(s[0], args.get(s[0]));
            if (p != null) {
                args.put(s[0], p);
            }
        }
        if (!args.containsKey("geometry") && getParameter("width") != null && getParameter("height") != null) {
            args.put("geometry", Arrays.asList(new String[]{getParameter("width")+"x"+getParameter("height")}));
        }
    }

    @Override public void start() {
        I18n.init();
        Main.checkJava6();

        String url = getParameter("load_url");
        if(url != null)
            args.put("download", Arrays.asList(new String[]{url}));

        // initialize the platform hook, and
        Main.determinePlatformHook();
        // call the really early hook before we do anything else
        Main.platform.preStartupHook();

        Main.initAppletPreferences(getCodeBase());

        String lang = getParameter("language");
        I18n.set(lang != null ? lang : Main.pref.get("language", null));
        Main.pref.updateSystemProperties();

        try
        {
            ((ServerSidePreferences)Main.pref).download();
        } catch (ServerSidePreferences.MissingPassword e) {
            String username = args.containsKey("username") ? args.get("username").iterator().next() : null;
            String password = args.containsKey("password") ? args.get("password").iterator().next() : null;
            if (username == null || password == null) {
                JPanel p = new JPanel(new GridBagLayout());
                p.add(new JLabel(tr(e.realm)), GBC.eol().fill(GBC.HORIZONTAL));
                p.add(new JLabel(tr("Username")), GBC.std().insets(0,0,20,0));
                JosmTextField user = new JosmTextField(username == null ? "" : username);
                p.add(user, GBC.eol().fill(GBC.HORIZONTAL));
                p.add(new JLabel(tr("Password")), GBC.std().insets(0,0,20,0));
                JosmPasswordField pass = new JosmPasswordField(password == null ? "" : password);
                p.add(pass, GBC.eol().fill(GBC.HORIZONTAL));
                JOptionPane.showMessageDialog(null, p);
                username = user.getText();
                if("".equals(username))
                    username = null;
                password = new String(pass.getPassword());
                if("".equals(password))
                    password = null;
            }
            if (username != null && password != null) {
                ((ServerSidePreferences)Main.pref).download(username, password);
            }
        }

        Main.preConstructorInit(Option.fromStringMap(args));
        Main.pref.updateSystemProperties();
        Main.parent = frame;

        new MainCaller(Option.fromStringMap(args));

        MainMenu m = Main.main.menu; // shortcut

        // remove offending stuff from JOSM (that would break the SecurityManager)
        m.editMenu.add(new UploadPreferencesAction());
        m.openFile.setEnabled(false);
        m.exit.setEnabled(false);
        m.save.setEnabled(false);
        m.saveAs.setEnabled(false);
        m.gpxExport.setEnabled(false);
    }

    private Collection<String> readParameter(String s, Collection<String> v) {
        String param = getParameter(s);
        if (param != null) {
            if (v == null) {
                v = new LinkedList<String>();
            }
            v.addAll(Arrays.asList(param.split(";")));
        }
        return v;
    }

    public static void main(String[] args) {
        Main.applet = true;
        MainApplet applet = new MainApplet();
        Main.initAppletPreferences(applet.getCodeBase());
        applet.setStub(new AppletStub() {
            @Override
            public void appletResize(int w, int h) {
                frame.setSize(w, h);
            }

            @Override
            public AppletContext getAppletContext() {
                return null;
            }

            @Override
            public URL getCodeBase() {
                return Utils.fileToURL(new File("."));
            }

            @Override
            public URL getDocumentBase() {
                return getCodeBase();
            }

            @Override
            public String getParameter(String k) {
                return null;
            }

            @Override
            public boolean isActive() {
                return true;
            }
        });
        applet.init();
        applet.start();
        frame.setContentPane(applet);
        frame.setVisible(true);
    }
}
