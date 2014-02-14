// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.Setting;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DatasetConsistencyTest;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.BugReportExceptionHandler;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.PlatformHookUnixoid;
import org.openstreetmap.josm.tools.PlatformHookWindows;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * @author xeen
 *
 * Opens a dialog with useful status information like version numbers for Java, JOSM and plugins
 * Also includes preferences with stripped username and password
 */
public final class ShowStatusReportAction extends JosmAction {

    /**
     * Constructs a new {@code ShowStatusReportAction}
     */
    public ShowStatusReportAction() {
        super(
                tr("Show Status Report"),
                "clock",
                tr("Show status report with useful information that can be attached to bugs"),
                Shortcut.registerShortcut("help:showstatusreport", tr("Help: {0}",
                        tr("Show Status Report")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), false);

        putValue("help", ht("/Action/ShowStatusReport"));
        putValue("toolbar", "help/showstatusreport");
        Main.toolbar.register(this);
    }

    private static void shortenParam(ListIterator<String> it, String[] param, String source, String target) {
        if (source != null && target.length() < source.length() && param[1].startsWith(source)) {
            it.set(param[0] + "=" + param[1].replace(source, target));
        }
    }

    /**
     * Replies the report header (software and system info)
     * @return The report header (software and system info)
     */
    public static String getReportHeader() {
        StringBuilder text = new StringBuilder();
        text.append(Version.getInstance().getReleaseAttributes());
        text.append("\n");
        text.append("Identification: " + Version.getInstance().getAgentString());
        text.append("\n");
        text.append("Memory Usage: ");
        text.append(Runtime.getRuntime().totalMemory()/1024/1024);
        text.append(" MB / ");
        text.append(Runtime.getRuntime().maxMemory()/1024/1024);
        text.append(" MB (");
        text.append(Runtime.getRuntime().freeMemory()/1024/1024);
        text.append(" MB allocated, but free)");
        text.append("\n");
        text.append("Java version: " + System.getProperty("java.version") + ", " + System.getProperty("java.vendor") + ", " + System.getProperty("java.vm.name"));
        text.append("\n");
        if (Main.platform.getClass() == PlatformHookUnixoid.class) {
            // Add Java package details for Debian/Ubuntu 
            String packageDetails = ((PlatformHookUnixoid) Main.platform).getJavaPackageDetails();
            if (packageDetails != null) {
                text.append("Java package: ");
                text.append(packageDetails);
                text.append("\n");
            }
            // Add WebStart package details for Debian/Ubuntu, if run from JNLP 
            if (Package.getPackage("javax.jnlp") != null) {
                String webStartDetails = ((PlatformHookUnixoid) Main.platform).getWebStartPackageDetails();
                if (webStartDetails != null) {
                    text.append("WebStart package: ");
                    text.append(webStartDetails);
                    text.append("\n");
                }
            }
        }
        try {
            final String env_java_home = System.getenv("JAVA_HOME");
            final String env_java_home_alt = Main.platform instanceof PlatformHookWindows ? "%JAVA_HOME%" : "${JAVA_HOME}";
            final String prop_java_home = System.getProperty("java.home");
            final String prop_java_home_alt = "<java.home>";
            // Build a new list of VM parameters to modify it below if needed (default implementation returns an UnmodifiableList instance)
            List<String> vmArguments = new ArrayList<String>(ManagementFactory.getRuntimeMXBean().getInputArguments());
            for (ListIterator<String> it = vmArguments.listIterator(); it.hasNext(); ) {
                String value = it.next();
                if (value.contains("=")) {
                    String[] param = value.split("=");
                    // Hide some parameters for privacy concerns
                    if (param[0].toLowerCase().startsWith("-dproxy")) {
                        it.set(param[0]+"=xxx");
                    // Shorten some parameters for readability concerns
                    } else {
                        shortenParam(it, param, env_java_home, env_java_home_alt);
                        shortenParam(it, param, prop_java_home, prop_java_home_alt);
                    }
                }
            }
            if (!vmArguments.isEmpty()) {
                text.append("VM arguments: "+ vmArguments.toString().replace("\\\\", "\\"));
                text.append("\n");
            }
        } catch (SecurityException e) {
            // Ignore exception
        }
        if (Main.commandLineArgs.length > 0) {
            text.append("Program arguments: "+ Arrays.toString(Main.commandLineArgs));
            text.append("\n");
        }
        if (Main.main != null) {
            DataSet dataset = Main.main.getCurrentDataSet();
            if (dataset != null) {
                String result = DatasetConsistencyTest.runTests(dataset);
                if (result.length() == 0) {
                    text.append("Dataset consistency test: No problems found\n");
                } else {
                    text.append("\nDataset consistency test:\n"+result+"\n");
                }
            }
        }
        text.append("\n");
        text.append(PluginHandler.getBugReportText());
        text.append("\n");

        return text.toString();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        StringBuilder text = new StringBuilder();
        String reportHeader = getReportHeader();
        text.append(reportHeader);
        try {
            Map<String, Setting> settings = Main.pref.getAllSettings();
            settings.remove("osm-server.username");
            settings.remove("osm-server.password");
            settings.remove("oauth.access-token.key");
            settings.remove("oauth.access-token.secret");
            Set<String> keys = new HashSet<String>(settings.keySet());
            for (String key : keys) {
                if (key.startsWith("marker.show")) {
                    settings.remove(key);
                }
            }
            for (Entry<String, Setting> entry : settings.entrySet()) {
                text.append(entry.getKey()).append("=").append(entry.getValue().getValue().toString()).append("\n");
            }
        } catch (Exception x) {
            Main.error(x);
        }

        JosmTextArea ta = new JosmTextArea(text.toString());
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        ta.setEditable(false);
        JScrollPane sp = new JScrollPane(ta);

        ExtendedDialog ed = new ExtendedDialog(Main.parent,
                tr("Status Report"),
                new String[] {tr("Copy to clipboard and close"), tr("Report bug"), tr("Close") });
        ed.setButtonIcons(new String[] {"copy.png", "bug.png", "cancel.png" });
        ed.setContent(sp, false);
        ed.setMinimumSize(new Dimension(380, 200));
        ed.setPreferredSize(new Dimension(700, Main.parent.getHeight()-50));

        switch (ed.showDialog().getValue()) {
            case 1: Utils.copyToClipboard(text.toString()); break;
            case 2: OpenBrowser.displayUrl(BugReportExceptionHandler.getBugReportUrl(
                        Utils.strip(reportHeader)).toExternalForm()) ; break;
        }
    }
}
