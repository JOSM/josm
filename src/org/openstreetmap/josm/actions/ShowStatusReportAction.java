// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.Utils.getSystemEnv;
import static org.openstreetmap.josm.tools.Utils.getSystemProperty;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.UIManager;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DatasetConsistencyTest;
import org.openstreetmap.josm.data.preferences.sources.MapPaintPrefHelper;
import org.openstreetmap.josm.data.preferences.sources.PresetPrefHelper;
import org.openstreetmap.josm.data.preferences.sources.SourcePrefHelper;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.bugreport.DebugTextDisplay;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.PlatformHookUnixoid;
import org.openstreetmap.josm.tools.PlatformManager;
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
     * Localized description text for this action
     */
    public static final String ACTION_DESCRIPTION = tr("Show status report with useful information that can be attached to bugs");

    /**
     * Constructs a new {@code ShowStatusReportAction}
     */
    public ShowStatusReportAction() {
        super(
                tr("Show Status Report"),
                "misc/statusreport",
                ACTION_DESCRIPTION,
                Shortcut.registerShortcut("help:showstatusreport", tr("Help: {0}",
                        tr("Show Status Report")), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE), true, "help/showstatusreport", false);

        setHelpId(ht("/Action/ShowStatusReport"));
    }

    /**
     * Replies the report header (software and system info)
     * @return The report header (software and system info)
     */
    public static String getReportHeader() {
        StringWriter stringWriter = new StringWriter(256);
        PrintWriter text = new PrintWriter(stringWriter);
        String runtimeVersion = getSystemProperty("java.runtime.version");
        text.println(Version.getInstance().getReleaseAttributes());
        text.format("Identification: %s%n", Version.getInstance().getAgentString());
        String buildNumber = PlatformManager.getPlatform().getOSBuildNumber();
        if (!buildNumber.isEmpty()) {
            text.format("OS Build number: %s%n", buildNumber);
        }
        text.format(Locale.ROOT, "Memory Usage: %d MB / %d MB (%d MB allocated, but free)%n",
                Runtime.getRuntime().totalMemory() / 1024 / 1024,
                Runtime.getRuntime().maxMemory() / 1024 / 1024,
                Runtime.getRuntime().freeMemory() / 1024 / 1024);
        text.format("Java version: %s, %s, %s%n",
                runtimeVersion != null ? runtimeVersion : getSystemProperty("java.version"),
                getSystemProperty("java.vendor"),
                getSystemProperty("java.vm.name"));
        text.format("Look and Feel: %s%n",
                Optional.ofNullable(UIManager.getLookAndFeel()).map(laf -> laf.getClass().getName()).orElse("null"));
        if (!GraphicsEnvironment.isHeadless()) {
            text.append("Screen:");
            for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
                text.append(" ").append(gd.getIDstring());
                DisplayMode dm = gd.getDisplayMode();
                if (dm != null) {
                    AffineTransform transform = gd.getDefaultConfiguration().getDefaultTransform();
                    // Java 11: use DisplayMode#toString
                    text.format(Locale.ROOT, " %d\u00D7%d (scaling %.2f\u00D7%.2f)",
                            dm.getWidth(), dm.getHeight(), transform.getScaleX(), transform.getScaleY());
                }
            }
            text.println();
        }
        text.format("Maximum Screen Size: %s%n", toString(GuiHelper.getMaximumScreenSize()));
        if (!GraphicsEnvironment.isHeadless()) {
            Dimension bestCursorSize16 = Toolkit.getDefaultToolkit().getBestCursorSize(16, 16);
            Dimension bestCursorSize32 = Toolkit.getDefaultToolkit().getBestCursorSize(32, 32);
            text.format("Best cursor sizes: %s→%s, %s→%s%n",
                    toString(new Dimension(16, 16)), toString(bestCursorSize16),
                    toString(new Dimension(32, 32)), toString(bestCursorSize32));
        }

        for (String name : Arrays.asList("LANG", "LC_ALL")) {
            String value = getSystemEnv(name);
            if (value != null) {
                text.format("Environment variable %s: %s%n", name, value);
            }
        }
        for (String name : Arrays.asList("file.encoding", "sun.jnu.encoding")) {
            String value = getSystemProperty(name);
            if (value != null) {
                text.format("System property %s: %s%n", name, value);
            }
        }
        text.format("Locale info: %s%n", Locale.getDefault().toString());
        text.format("Numbers with default locale: %s -> %d%n", Integer.toString(1_234_567_890), 1_234_567_890);

        if (PlatformManager.isPlatformUnixoid()) {
            PlatformHookUnixoid platform = (PlatformHookUnixoid) PlatformManager.getPlatform();
            // Add desktop environment
            platform.getDesktopEnvironment().ifPresent(desktop -> text.format("Desktop environment: %s%n", desktop));
            // Add Java package details
            String packageDetails = platform.getJavaPackageDetails();
            if (packageDetails != null) {
                text.format("Java package: %s%n", packageDetails);
            }
            // Add WebStart package details if run from JNLP
            if (Utils.isRunningWebStart()) {
                String webStartDetails = platform.getWebStartPackageDetails();
                if (webStartDetails != null) {
                    text.format("WebStart package: %s%n", webStartDetails);
                }
            }
            // Add Gnome Atk wrapper details if found
            String atkWrapperDetails = platform.getAtkWrapperPackageDetails();
            if (atkWrapperDetails != null) {
                text.format("Java ATK Wrapper package: %s%n", atkWrapperDetails);
            }
            // Add dependencies details if found
            for (String p : new String[] {
                    "apache-commons-compress", "libcommons-compress-java",
                    "apache-commons-jcs-core",
                    "apache-commons-logging", "libcommons-logging-java",
                    "fonts-noto",
                    "jsonp",
                    "metadata-extractor2",
                    "signpost-core", "liboauth-signpost-java",
                    "svgsalamander"
            }) {
                String details = PlatformHookUnixoid.getPackageDetails(p);
                if (details != null) {
                    text.format("%s: %s%n", p, details);
                }
            }
        }
        try {
            // Build a new list of VM parameters to modify it below if needed (default implementation returns an UnmodifiableList instance)
            List<String> vmArguments = new ArrayList<>(ManagementFactory.getRuntimeMXBean().getInputArguments());
            for (ListIterator<String> it = vmArguments.listIterator(); it.hasNext();) {
                String value = it.next();
                if (value.contains("=")) {
                    String[] param = value.split("=", 2);
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
                text.format("VM arguments: %s%n", vmArguments.toString().replace("\\\\", "\\"));
            }
        } catch (SecurityException e) {
            Logging.trace(e);
        }
        List<String> commandLineArgs = MainApplication.getCommandLineArgs();
        if (!commandLineArgs.isEmpty()) {
            text.format("Program arguments: %s%n", Arrays.toString(paramCleanup(commandLineArgs).toArray()));
        }
        DataSet dataset = MainApplication.getLayerManager().getActiveDataSet();
        if (dataset != null) {
            String result = DatasetConsistencyTest.runTests(dataset);
            if (result.isEmpty()) {
                text.println("Dataset consistency test: No problems found");
            } else {
                text.println();
                text.println("Dataset consistency test:");
                text.println(result);
            }
        }
        text.println();
        appendCollection(text, "Plugins", Utils.transform(PluginHandler.getBugReportInformation(), i -> "+ " + i));
        appendCollection(text, "Tagging presets", getCustomUrls(PresetPrefHelper.INSTANCE));
        appendCollection(text, "Map paint styles", getCustomUrls(MapPaintPrefHelper.INSTANCE));
        appendCollection(text, "Validator rules", getCustomUrls(ValidatorPrefHelper.INSTANCE));
        appendCollection(text, "Last errors/warnings", Utils.transform(Logging.getLastErrorAndWarnings(), i -> "- " + i));

        String osmApi = OsmApi.getOsmApi().getServerUrl();
        if (!Config.getUrls().getDefaultOsmApiUrl().equals(osmApi.trim())) {
            text.format("OSM API: %s%n", osmApi);
        }

        text.println();
        return stringWriter.toString();
    }

    private static String toString(Dimension dimension) {
        return dimension.width + "\u00D7" + dimension.height;
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
        return params.stream()
                .map(ShowStatusReportAction::paramCleanup)
                .collect(Collectors.toList());
    }

    /**
     * Fill map with anonymized name to the actual used path.
     * @return map that maps shortened name to full directory path
     */
    static Map<String, String> getAnonimicDirectorySymbolMap() {
        /** maps the anonymized name to the actual used path */
        Map<String, String> map = new LinkedHashMap<>();
        map.put(PlatformManager.isPlatformWindows() ? "%JAVA_HOME%" : "${JAVA_HOME}", getSystemEnv("JAVA_HOME"));
        map.put("<java.home>", getSystemProperty("java.home"));
        map.put("<josm.pref>", Config.getDirs().getPreferencesDirectory(false).toString());
        map.put("<josm.userdata>", Config.getDirs().getUserDataDirectory(false).toString());
        map.put("<josm.cache>", Config.getDirs().getCacheDirectory(false).toString());
        map.put(PlatformManager.isPlatformWindows() ? "%UserProfile%" : "${HOME}", getSystemProperty("user.home"));
        return map;
    }

    /**
     * Shortens and removes private informations from a parameter used for status report.
     * @param param parameter to cleanup
     * @return shortened/anonymized parameter
     */
    static String paramCleanup(String param) {
        final String userName = getSystemProperty("user.name");
        final String userNameAlt = "<user.name>";

        String val = param;
        for (Entry<String, String> entry : getAnonimicDirectorySymbolMap().entrySet()) {
            val = paramReplace(val, entry.getValue(), entry.getKey());
        }
        if (userName != null && userName.length() >= 3) {
            val = paramReplace(val, userName, userNameAlt);
        }
        return val;
    }

    private static String paramReplace(String str, String target, String replacement) {
        return target == null ? str : str.replace(target, replacement);
    }

    private static void appendCollection(PrintWriter text, String label, Collection<String> col) {
        if (!col.isEmpty()) {
            text.append(col.stream().map(o -> paramCleanup(o) + '\n')
                    .collect(Collectors.joining("", label + ":\n", "\n")));
        }
    }

    private static String valueCleanup(Object value) {
        String valueString = value.toString();
        if (valueString.length() > 512 && value instanceof Collection<?>) {
            valueString = ((Collection<?>) value).stream().map(v -> {
                if (v instanceof Map<?, ?>) {
                    LinkedHashMap<Object, Object> map = new LinkedHashMap<>(((Map<?, ?>) v));
                    map.computeIfPresent("icon", (k, icon) -> Utils.shortenString(icon.toString(), 32)); // see #19058
                    return map.toString();
                } else {
                    return String.valueOf(v);
                }
            }).collect(Collectors.joining(",\n  ", "[", "\n]"));
        }
        return paramCleanup(valueString);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        StringBuilder text = new StringBuilder();
        String reportHeader = getReportHeader();
        text.append(reportHeader);

        Preferences.main().getAllSettings().forEach((key, setting) -> {
            if ("file-open.history".equals(key)
                    || "download.overpass.query".equals(key)
                    || "download.overpass.queries".equals(key)
                    || key.contains("username")
                    || key.contains("password")
                    || key.contains("access-token")) {
                // Remove sensitive information from status report
                return;
            }
            text.append(paramCleanup(key))
                    .append('=')
                    .append(valueCleanup(setting.getValue()))
                    .append('\n');
        });

        DebugTextDisplay ta = new DebugTextDisplay(text.toString());

        ExtendedDialog ed = new ExtendedDialog(MainApplication.getMainFrame(),
                tr("Status Report"),
                tr("Copy to clipboard and close"), tr("Report bug"), tr("Close"));
        ed.setButtonIcons("copy", "bug", "cancel");
        ed.configureContextsensitiveHelp("/Action/ShowStatusReport", true);
        ed.setContent(ta, false);
        ed.setMinimumSize(new Dimension(380, 200));
        ed.setPreferredSize(new Dimension(700, MainApplication.getMainFrame().getHeight()-50));

        switch (ed.showDialog().getValue()) {
            case 1: ta.copyToClipboard(); break;
            case 2: BugReportSender.reportBug(reportHeader); break;
            default: // do nothing
        }
        GuiHelper.destroyComponents(ed, false);
    }
}
