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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DatasetConsistencyTest;
import org.openstreetmap.josm.data.preferences.Setting;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.PlatformHookUnixoid;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.bugreport.BugReportSender;
import org.openstreetmap.josm.tools.bugreport.DebugTextDisplay;

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
            it.set(param[0] + '=' + param[1].replace(source, target));
        }
    }

    /**
     * Replies the report header (software and system info)
     * @return The report header (software and system info)
     */
    public static String getReportHeader() {
        StringBuilder text = new StringBuilder();
        String runtimeVersion = System.getProperty("java.runtime.version");
        text.append(Version.getInstance().getReleaseAttributes())
            .append("\nIdentification: ").append(Version.getInstance().getAgentString())
            .append("\nMemory Usage: ")
            .append(Runtime.getRuntime().totalMemory()/1024/1024)
            .append(" MB / ")
            .append(Runtime.getRuntime().maxMemory()/1024/1024)
            .append(" MB (")
            .append(Runtime.getRuntime().freeMemory()/1024/1024)
            .append(" MB allocated, but free)\nJava version: ")
            .append(runtimeVersion != null ? runtimeVersion : System.getProperty("java.version")).append(", ")
            .append(System.getProperty("java.vendor")).append(", ")
            .append(System.getProperty("java.vm.name")).append('\n');
        if (Main.platform.getClass() == PlatformHookUnixoid.class) {
            // Add Java package details
            String packageDetails = ((PlatformHookUnixoid) Main.platform).getJavaPackageDetails();
            if (packageDetails != null) {
                text.append("Java package: ")
                    .append(packageDetails)
                    .append('\n');
            }
            // Add WebStart package details if run from JNLP
            if (Package.getPackage("javax.jnlp") != null) {
                String webStartDetails = ((PlatformHookUnixoid) Main.platform).getWebStartPackageDetails();
                if (webStartDetails != null) {
                    text.append("WebStart package: ")
                        .append(webStartDetails)
                        .append('\n');
                }
            }
        }
        try {
            final String envJavaHome = System.getenv("JAVA_HOME");
            final String envJavaHomeAlt = Main.isPlatformWindows() ? "%JAVA_HOME%" : "${JAVA_HOME}";
            final String propJavaHome = System.getProperty("java.home");
            final String propJavaHomeAlt = "<java.home>";
            // Build a new list of VM parameters to modify it below if needed (default implementation returns an UnmodifiableList instance)
            List<String> vmArguments = new ArrayList<>(ManagementFactory.getRuntimeMXBean().getInputArguments());
            for (ListIterator<String> it = vmArguments.listIterator(); it.hasNext();) {
                String value = it.next();
                if (value.contains("=")) {
                    String[] param = value.split("=");
                    // Hide some parameters for privacy concerns
                    if (param[0].toLowerCase(Locale.ENGLISH).startsWith("-dproxy")) {
                        it.set(param[0]+"=xxx");
                    } else {
                        // Shorten some parameters for readability concerns
                        shortenParam(it, param, envJavaHome, envJavaHomeAlt);
                        shortenParam(it, param, propJavaHome, propJavaHomeAlt);
                    }
                } else if (value.startsWith("-X")) {
                    // Remove arguments like -Xbootclasspath/a, -Xverify:remote, that can be very long and unhelpful
                    it.remove();
                }
            }
            if (!vmArguments.isEmpty()) {
                text.append("VM arguments: ").append(vmArguments.toString().replace("\\\\", "\\")).append('\n');
            }
        } catch (SecurityException e) {
            // Ignore exception
            if (Main.isTraceEnabled()) {
                Main.trace(e.getMessage());
            }
        }
        List<String> commandLineArgs = Main.getCommandLineArgs();
        if (!commandLineArgs.isEmpty()) {
            text.append("Program arguments: ").append(Arrays.toString(commandLineArgs.toArray())).append('\n');
        }
        if (Main.main != null) {
            DataSet dataset = Main.main.getCurrentDataSet();
            if (dataset != null) {
                String result = DatasetConsistencyTest.runTests(dataset);
                if (result.isEmpty()) {
                    text.append("Dataset consistency test: No problems found\n");
                } else {
                    text.append("\nDataset consistency test:\n").append(result).append('\n');
                }
            }
        }
        text.append('\n').append(PluginHandler.getBugReportText()).append('\n');

        Collection<String> errorsWarnings = Main.getLastErrorAndWarnings();
        if (!errorsWarnings.isEmpty()) {
            text.append("Last errors/warnings:\n");
            for (String s : errorsWarnings) {
                text.append("- ").append(s).append('\n');
            }
            text.append('\n');
        }

        return text.toString();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        StringBuilder text = new StringBuilder();
        String reportHeader = getReportHeader();
        text.append(reportHeader);
        try {
            Map<String, Setting<?>> settings = Main.pref.getAllSettings();
            Set<String> keys = new HashSet<>(settings.keySet());
            for (String key : keys) {
                // Remove sensitive information from status report
                if (key.startsWith("marker.show") || key.contains("username") || key.contains("password") || key.contains("access-token")) {
                    settings.remove(key);
                }
            }
            for (Entry<String, Setting<?>> entry : settings.entrySet()) {
                text.append(entry.getKey()).append('=').append(entry.getValue().getValue()).append('\n');
            }
        } catch (Exception x) {
            Main.error(x);
        }

        DebugTextDisplay ta = new DebugTextDisplay(text.toString());

        ExtendedDialog ed = new ExtendedDialog(Main.parent,
                tr("Status Report"),
                new String[] {tr("Copy to clipboard and close"), tr("Report bug"), tr("Close") });
        ed.setButtonIcons(new String[] {"copy", "bug", "cancel" });
        ed.setContent(ta, false);
        ed.setMinimumSize(new Dimension(380, 200));
        ed.setPreferredSize(new Dimension(700, Main.parent.getHeight()-50));

        switch (ed.showDialog().getValue()) {
            case 1: ta.copyToClippboard(); break;
            case 2: BugReportSender.reportBug(reportHeader); break;
        }
    }
}
