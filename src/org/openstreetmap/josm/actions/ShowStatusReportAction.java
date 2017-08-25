// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
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
import java.util.stream.Collectors;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DatasetConsistencyTest;
import org.openstreetmap.josm.data.preferences.Setting;
import org.openstreetmap.josm.data.preferences.sources.MapPaintPrefHelper;
import org.openstreetmap.josm.data.preferences.sources.PresetPrefHelper;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.preferences.sources.SourcePrefHelper;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.bugreport.DebugTextDisplay;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.PlatformHookUnixoid;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReportSender;

/**
 * Opens a dialog with useful status information like version numbers for Java, JOSM and plugins
 * Also includes preferences with stripped username and password.
 *
 * @author xeen
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
        MainApplication.getToolbar().register(this);
    }

    private static boolean isRunningJavaWebStart() {
        try {
            // See http://stackoverflow.com/a/16200769/2257172
            return Class.forName("javax.jnlp.ServiceManager") != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Replies the report header (software and system info)
     * @return The report header (software and system info)
     */
    public static String getReportHeader() {
        StringBuilder text = new StringBuilder(256);
        String runtimeVersion = System.getProperty("java.runtime.version");
        text.append(Version.getInstance().getReleaseAttributes())
            .append("\nIdentification: ").append(Version.getInstance().getAgentString());
        String buildNumber = Main.platform.getOSBuildNumber();
        if (!buildNumber.isEmpty()) {
            text.append("\nOS Build number: ").append(buildNumber);
        }
        text.append("\nMemory Usage: ")
            .append(Runtime.getRuntime().totalMemory()/1024/1024)
            .append(" MB / ")
            .append(Runtime.getRuntime().maxMemory()/1024/1024)
            .append(" MB (")
            .append(Runtime.getRuntime().freeMemory()/1024/1024)
            .append(" MB allocated, but free)\nJava version: ")
            .append(runtimeVersion != null ? runtimeVersion : System.getProperty("java.version")).append(", ")
            .append(System.getProperty("java.vendor")).append(", ")
            .append(System.getProperty("java.vm.name"))
            .append("\nScreen: ");
        if (!GraphicsEnvironment.isHeadless()) {
            text.append(Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()).map(gd -> {
                        StringBuilder b = new StringBuilder(gd.getIDstring());
                        DisplayMode dm = gd.getDisplayMode();
                        if (dm != null) {
                            b.append(' ').append(dm.getWidth()).append('x').append(dm.getHeight());
                        }
                        return b.toString();
                    }).collect(Collectors.joining(", ")));
        }
        Dimension maxScreenSize = GuiHelper.getMaximumScreenSize();
        text.append("\nMaximum Screen Size: ")
            .append((int) maxScreenSize.getWidth()).append('x')
            .append((int) maxScreenSize.getHeight()).append('\n');

        if (Main.platform instanceof PlatformHookUnixoid) {
            // Add Java package details
            String packageDetails = ((PlatformHookUnixoid) Main.platform).getJavaPackageDetails();
            if (packageDetails != null) {
                text.append("Java package: ")
                    .append(packageDetails)
                    .append('\n');
            }
            // Add WebStart package details if run from JNLP
            if (isRunningJavaWebStart()) {
                String webStartDetails = ((PlatformHookUnixoid) Main.platform).getWebStartPackageDetails();
                if (webStartDetails != null) {
                    text.append("WebStart package: ")
                        .append(webStartDetails)
                        .append('\n');
                }
            }
            // Add Gnome Atk wrapper details if found
            String atkWrapperDetails = ((PlatformHookUnixoid) Main.platform).getAtkWrapperPackageDetails();
            if (atkWrapperDetails != null) {
                text.append("Java ATK Wrapper package: ")
                    .append(atkWrapperDetails)
                    .append('\n');
            }
        }
        try {
            // Build a new list of VM parameters to modify it below if needed (default implementation returns an UnmodifiableList instance)
            List<String> vmArguments = new ArrayList<>(ManagementFactory.getRuntimeMXBean().getInputArguments());
            for (ListIterator<String> it = vmArguments.listIterator(); it.hasNext();) {
                String value = it.next();
                if (value.contains("=")) {
                    String[] param = value.split("=");
                    // Hide some parameters for privacy concerns
                    if (param[0].toLowerCase(Locale.ENGLISH).startsWith("-dproxy")) {
                        it.set(param[0]+"=xxx");
                    } else if ("-Djnlpx.vmargs".equals(param[0])) {
                        // Remove jnlpx.vmargs (base64 encoded copy of VM arguments already included in clear)
                        it.remove();
                    } else {
                        // Replace some paths for readability and privacy concerns
                        String val = paramCleanup(param[1]);
                        if (!val.equals(param[1])) {
                            it.set(param[0] + '=' + val);
                        }
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
            Logging.trace(e);
        }
        List<String> commandLineArgs = MainApplication.getCommandLineArgs();
        if (!commandLineArgs.isEmpty()) {
            text.append("Program arguments: ").append(Arrays.toString(paramCleanup(commandLineArgs).toArray())).append('\n');
        }
        if (Main.main != null) {
            DataSet dataset = MainApplication.getLayerManager().getEditDataSet();
            if (dataset != null) {
                String result = DatasetConsistencyTest.runTests(dataset);
                if (result.isEmpty()) {
                    text.append("Dataset consistency test: No problems found\n");
                } else {
                    text.append("\nDataset consistency test:\n").append(result).append('\n');
                }
            }
        }
        text.append('\n');
        appendCollection(text, "Plugins", Utils.transform(PluginHandler.getBugReportInformation(), i -> "+ " + i));
        appendCollection(text, "Tagging presets", getCustomUrls(PresetPrefHelper.INSTANCE));
        appendCollection(text, "Map paint styles", getCustomUrls(MapPaintPrefHelper.INSTANCE));
        appendCollection(text, "Validator rules", getCustomUrls(ValidatorPrefHelper.INSTANCE));
        appendCollection(text, "Last errors/warnings", Utils.transform(Logging.getLastErrorAndWarnings(), i -> "- " + i));

        String osmApi = OsmApi.getOsmApi().getServerUrl();
        if (!OsmApi.DEFAULT_API_URL.equals(osmApi.trim())) {
            text.append("OSM API: ").append(osmApi).append("\n\n");
        }

        return text.toString();
    }

    private static Collection<String> getCustomUrls(SourcePrefHelper helper) {
        final Set<String> defaultUrls = helper.getDefault().stream()
                .map(i -> i.url)
                .collect(Collectors.toSet());
        return helper.get().stream()
                .filter(i -> !defaultUrls.contains(i.url))
                .map(i -> (i.active ? "+ " : "- ") + i.url)
                .collect(Collectors.toList());
    }

    private static List<String> paramCleanup(Collection<String> params) {
        List<String> result = new ArrayList<>(params.size());
        for (String param : params) {
            result.add(paramCleanup(param));
        }
        return result;
    }

    /**
     * Shortens and removes private informations from a parameter used for status report.
     * @param param parameter to cleanup
     * @return shortened/anonymized parameter
     */
    private static String paramCleanup(String param) {
        final String envJavaHome = System.getenv("JAVA_HOME");
        final String envJavaHomeAlt = Main.isPlatformWindows() ? "%JAVA_HOME%" : "${JAVA_HOME}";
        final String propJavaHome = System.getProperty("java.home");
        final String propJavaHomeAlt = "<java.home>";
        final String prefDir = Main.pref.getPreferencesDirectory().toString();
        final String prefDirAlt = "<josm.pref>";
        final String userDataDir = Main.pref.getUserDataDirectory().toString();
        final String userDataDirAlt = "<josm.userdata>";
        final String userCacheDir = Main.pref.getCacheDirectory().toString();
        final String userCacheDirAlt = "<josm.cache>";
        final String userHomeDir = System.getProperty("user.home");
        final String userHomeDirAlt = Main.isPlatformWindows() ? "%UserProfile%" : "${HOME}";
        final String userName = System.getProperty("user.name");
        final String userNameAlt = "<user.name>";

        String val = param;
        val = paramReplace(val, envJavaHome, envJavaHomeAlt);
        val = paramReplace(val, envJavaHome, envJavaHomeAlt);
        val = paramReplace(val, propJavaHome, propJavaHomeAlt);
        val = paramReplace(val, prefDir, prefDirAlt);
        val = paramReplace(val, userDataDir, userDataDirAlt);
        val = paramReplace(val, userCacheDir, userCacheDirAlt);
        val = paramReplace(val, userHomeDir, userHomeDirAlt);
        if (userName.length() >= 3) {
            val = paramReplace(val, userName, userNameAlt);
        }
        return val;
    }

    private static String paramReplace(String str, String target, String replacement) {
        return target == null ? str : str.replace(target, replacement);
    }

    private static void appendCollection(StringBuilder text, String label, Collection<String> col) {
        if (!col.isEmpty()) {
            text.append(label).append(":\n");
            for (String o : col) {
                text.append(paramCleanup(o)).append('\n');
            }
            text.append('\n');
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        StringBuilder text = new StringBuilder();
        String reportHeader = getReportHeader();
        text.append(reportHeader);
        Map<String, Setting<?>> settings = Main.pref.getAllSettings();
        Set<String> keys = new HashSet<>(settings.keySet());
        for (String key : keys) {
            // Remove sensitive information from status report
            if (key.startsWith("marker.show") || key.contains("username") || key.contains("password") || key.contains("access-token")) {
                settings.remove(key);
            }
        }
        for (Entry<String, Setting<?>> entry : settings.entrySet()) {
            text.append(paramCleanup(entry.getKey()))
                .append('=')
                .append(paramCleanup(entry.getValue().getValue().toString())).append('\n');
        }

        DebugTextDisplay ta = new DebugTextDisplay(text.toString());

        ExtendedDialog ed = new ExtendedDialog(Main.parent,
                tr("Status Report"),
                tr("Copy to clipboard and close"), tr("Report bug"), tr("Close"));
        ed.setButtonIcons("copy", "bug", "cancel");
        ed.setContent(ta, false);
        ed.setMinimumSize(new Dimension(380, 200));
        ed.setPreferredSize(new Dimension(700, Main.parent.getHeight()-50));

        switch (ed.showDialog().getValue()) {
            case 1: ta.copyToClipboard(); break;
            case 2: BugReportSender.reportBug(reportHeader); break;
            default: // Do nothing
        }
    }
}
