// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.Setting;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DatasetConsistencyTest;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;


/**
 * @author xeen
 *
 * Opens a dialog with useful status information like version numbers for Java, JOSM and plugins
 * Also includes preferences with stripped username and password
 */
public final class ShowStatusReportAction extends JosmAction {
    public ShowStatusReportAction() {
        super(
                tr("Show Status Report"),
                "clock",
                tr("Show status report with useful information that can be attached to bugs"),
                Shortcut.registerShortcut("help:showstatusreport", tr("Help: {0}",
                        tr("Show Status Report")), KeyEvent.VK_R, Shortcut.GROUP_NONE), false);

        putValue("help", ht("/Action/ShowStatusReport"));
        putValue("toolbar", "help/showstatusreport");
        Main.toolbar.register(this);
    }

    public static String getReportHeader()
    {
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
        text.append("Operating system: "+ System.getProperty("os.name"));
        text.append("\n");
        DataSet dataset = Main.main.getCurrentDataSet();
        if (dataset != null) {
            String result = DatasetConsistencyTest.runTests(dataset);
            if (result.length() == 0) {
                text.append("Dataset consistency test: No problems found\n");
            } else {
                text.append("\nDataset consistency test:\n"+result+"\n");
            }
        }
        text.append("\n");
        text.append(PluginHandler.getBugReportText());
        text.append("\n");

        return text.toString();
    }

    public void actionPerformed(ActionEvent e) {
        StringBuilder text = new StringBuilder();
        text.append(getReportHeader());
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
            x.printStackTrace();
        }

        JTextArea ta = new JTextArea(text.toString());
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        ta.setEditable(false);
        JScrollPane sp = new JScrollPane(ta);

        ExtendedDialog ed = new ExtendedDialog(Main.parent,
                tr("Status Report"),
                new String[] {tr("Copy to clipboard and close"), tr("Close") });
        ed.setButtonIcons(new String[] {"copy.png", "cancel.png" });
        ed.setContent(sp, false);
        ed.setMinimumSize(new Dimension(500, 0));
        ed.showDialog();

        if(ed.getValue() != 1) return;
        Utils.copyToClipboard(text.toString());
    }
}
