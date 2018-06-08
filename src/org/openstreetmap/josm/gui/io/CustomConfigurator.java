// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.PreferencesUtils;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.PluginDownloadTask;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.ReadLocalPluginInformationTask;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.Setting;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class to process configuration changes stored in XML
 * can be used to modify preferences, store/delete files in .josm folders etc
 */
public final class CustomConfigurator {

    private CustomConfigurator() {
        // Hide default constructor for utils classes
    }

    /**
     * Read configuration script from XML file, modifying main preferences
     * @param dir - directory
     * @param fileName - XML file name
     */
    public static void readXML(String dir, String fileName) {
        readXML(new File(dir, fileName));
    }

    /**
     * Read configuration script from XML file, modifying given preferences object
     * @param file - file to open for reading XML
     * @param prefs - arbitrary Preferences object to modify by script
     */
    public static void readXML(final File file, final Preferences prefs) {
        synchronized (CustomConfigurator.class) {
            busy = true;
        }
        new XMLCommandProcessor(prefs).openAndReadXML(file);
        synchronized (CustomConfigurator.class) {
            CustomConfigurator.class.notifyAll();
            busy = false;
        }
    }

    /**
     * Read configuration script from XML file, modifying main preferences
     * @param file - file to open for reading XML
     */
    public static void readXML(File file) {
        readXML(file, Main.pref);
    }

    /**
     * Downloads file to one of JOSM standard folders
     * @param address - URL to download
     * @param path - file path relative to base where to put downloaded file
     * @param base - only "prefs", "cache" and "plugins" allowed for standard folders
     */
    public static void downloadFile(String address, String path, String base) {
        processDownloadOperation(address, path, getDirectoryByAbbr(base), true, false);
    }

    /**
     * Downloads file to one of JOSM standard folders and unpack it as ZIP/JAR file
     * @param address - URL to download
     * @param path - file path relative to base where to put downloaded file
     * @param base - only "prefs", "cache" and "plugins" allowed for standard folders
     */
    public static void downloadAndUnpackFile(String address, String path, String base) {
        processDownloadOperation(address, path, getDirectoryByAbbr(base), true, true);
    }

    /**
     * Downloads file to arbitrary folder
     * @param address - URL to download
     * @param path - file path relative to parentDir where to put downloaded file
     * @param parentDir - folder where to put file
     * @param mkdir - if true, non-existing directories will be created
     * @param unzip - if true file wil be unzipped and deleted after download
     */
    public static void processDownloadOperation(String address, String path, String parentDir, boolean mkdir, boolean unzip) {
        String dir = parentDir;
        if (path.contains("..") || path.startsWith("/") || path.contains(":")) {
            return; // some basic protection
        }
        File fOut = new File(dir, path);
        DownloadFileTask downloadFileTask = new DownloadFileTask(Main.parent, address, fOut, mkdir, unzip);

        MainApplication.worker.submit(downloadFileTask);
        PreferencesUtils.log("Info: downloading file from %s to %s in background ", parentDir, fOut.getAbsolutePath());
        if (unzip) PreferencesUtils.log("and unpacking it"); else PreferencesUtils.log("");

    }

    /**
     * Simple function to show messageBox, may be used from JS API and from other code
     * @param type - 'i','w','e','q','p' for Information, Warning, Error, Question, Message
     * @param text - message to display, HTML allowed
     */
    public static void messageBox(String type, String text) {
        char c = (type == null || type.isEmpty() ? "plain" : type).charAt(0);
        switch (c) {
            case 'i': JOptionPane.showMessageDialog(Main.parent, text, tr("Information"), JOptionPane.INFORMATION_MESSAGE); break;
            case 'w': JOptionPane.showMessageDialog(Main.parent, text, tr("Warning"), JOptionPane.WARNING_MESSAGE); break;
            case 'e': JOptionPane.showMessageDialog(Main.parent, text, tr("Error"), JOptionPane.ERROR_MESSAGE); break;
            case 'q': JOptionPane.showMessageDialog(Main.parent, text, tr("Question"), JOptionPane.QUESTION_MESSAGE); break;
            case 'p': JOptionPane.showMessageDialog(Main.parent, text, tr("Message"), JOptionPane.PLAIN_MESSAGE); break;
            default: Logging.warn("Unsupported messageBox type: " + c);
        }
    }

    /**
     * Simple function for choose window, may be used from JS API and from other code
     * @param text - message to show, HTML allowed
     * @param opts -
     * @return number of pressed button, -1 if cancelled
     */
    public static int askForOption(String text, String opts) {
        if (!opts.isEmpty()) {
            return JOptionPane.showOptionDialog(Main.parent, text, "Question",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, opts.split(";"), 0);
        } else {
            return JOptionPane.showOptionDialog(Main.parent, text, "Question",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, 2);
        }
    }

    public static String askForText(String text) {
        String s = JOptionPane.showInputDialog(Main.parent, text, tr("Enter text"), JOptionPane.QUESTION_MESSAGE);
        return s != null ? s.trim() : null;
    }

    /**
     * This function exports part of user preferences to specified file.
     * Default values are not saved.
     * @param filename - where to export
     * @param append - if true, resulting file cause appending to exuisting preferences
     * @param keys - which preferences keys you need to export ("imagery.entries", for example)
     */
    public static void exportPreferencesKeysToFile(String filename, boolean append, String... keys) {
        Set<String> keySet = new HashSet<>();
        Collections.addAll(keySet, keys);
        exportPreferencesKeysToFile(filename, append, keySet);
    }

    /**
     * This function exports part of user preferences to specified file.
     * Default values are not saved.
     * Preference keys matching specified pattern are saved
     * @param fileName - where to export
     * @param append - if true, resulting file cause appending to exuisting preferences
     * @param pattern - Regexp pattern forh preferences keys you need to export (".*imagery.*", for example)
     */
    public static void exportPreferencesKeysByPatternToFile(String fileName, boolean append, String pattern) {
        List<String> keySet = new ArrayList<>();
        Map<String, Setting<?>> allSettings = Main.pref.getAllSettings();
        for (String key: allSettings.keySet()) {
            if (key.matches(pattern))
                keySet.add(key);
        }
        exportPreferencesKeysToFile(fileName, append, keySet);
    }

    /**
     * Export specified preferences keys to configuration file
     * @param filename - name of file
     * @param append - will the preferences be appended to existing ones when file is imported later.
     * Elsewhere preferences from file will replace existing keys.
     * @param keys - collection of preferences key names to save
     */
    public static void exportPreferencesKeysToFile(String filename, boolean append, Collection<String> keys) {
        Element root = null;
        Document document = null;
        Document exportDocument = null;

        try {
            String toXML = Main.pref.toXML(true);
            DocumentBuilder builder = XmlUtils.newSafeDOMBuilder();
            document = builder.parse(new ByteArrayInputStream(toXML.getBytes(StandardCharsets.UTF_8)));
            exportDocument = builder.newDocument();
            root = document.getDocumentElement();
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            Logging.log(Logging.LEVEL_WARN, "Error getting preferences to save:", ex);
        }
        if (root == null || exportDocument == null)
            return;
        try {
            Element newRoot = exportDocument.createElement("config");
            exportDocument.appendChild(newRoot);

            Element prefElem = exportDocument.createElement("preferences");
            prefElem.setAttribute("operation", append ? "append" : "replace");
            newRoot.appendChild(prefElem);

            NodeList childNodes = root.getChildNodes();
            int n = childNodes.getLength();
            for (int i = 0; i < n; i++) {
                Node item = childNodes.item(i);
                if (item.getNodeType() == Node.ELEMENT_NODE) {
                    String currentKey = ((Element) item).getAttribute("key");
                    if (keys.contains(currentKey)) {
                        Node imported = exportDocument.importNode(item, true);
                        prefElem.appendChild(imported);
                    }
                }
            }
            File f = new File(filename);
            Transformer ts = XmlUtils.newSafeTransformerFactory().newTransformer();
            ts.setOutputProperty(OutputKeys.INDENT, "yes");
            ts.transform(new DOMSource(exportDocument), new StreamResult(f.toURI().getPath()));
        } catch (DOMException | TransformerFactoryConfigurationError | TransformerException ex) {
            Logging.warn("Error saving preferences part:");
            Logging.error(ex);
        }
    }

    public static void deleteFile(String path, String base) {
        String dir = getDirectoryByAbbr(base);
        if (dir == null) {
            PreferencesUtils.log("Error: Can not find base, use base=cache, base=prefs or base=plugins attribute.");
            return;
        }
        PreferencesUtils.log("Delete file: %s\n", path);
        if (path.contains("..") || path.startsWith("/") || path.contains(":")) {
            return; // some basic protection
        }
        File fOut = new File(dir, path);
        if (fOut.exists()) {
            deleteFileOrDirectory(fOut);
        }
    }

    public static void deleteFileOrDirectory(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File f1: files) {
                    deleteFileOrDirectory(f1);
                }
            }
        }
        if (!Utils.deleteFile(f)) {
            PreferencesUtils.log("Warning: Can not delete file "+f.getPath());
        }
    }

    private static boolean busy;

    public static void pluginOperation(String install, String uninstall, String delete) {
        final List<String> installList = new ArrayList<>();
        final List<String> removeList = new ArrayList<>();
        final List<String> deleteList = new ArrayList<>();
        Collections.addAll(installList, install.toLowerCase(Locale.ENGLISH).split(";"));
        Collections.addAll(removeList, uninstall.toLowerCase(Locale.ENGLISH).split(";"));
        Collections.addAll(deleteList, delete.toLowerCase(Locale.ENGLISH).split(";"));
        installList.remove("");
        removeList.remove("");
        deleteList.remove("");

        if (!installList.isEmpty()) {
            PreferencesUtils.log("Plugins install: "+installList);
        }
        if (!removeList.isEmpty()) {
            PreferencesUtils.log("Plugins turn off: "+removeList);
        }
        if (!deleteList.isEmpty()) {
            PreferencesUtils.log("Plugins delete: "+deleteList);
        }

        final ReadLocalPluginInformationTask task = new ReadLocalPluginInformationTask();
        Runnable r = () -> {
            if (task.isCanceled()) return;
            synchronized (CustomConfigurator.class) {
                try { // proceed only after all other tasks were finished
                    while (busy) CustomConfigurator.class.wait();
                } catch (InterruptedException ex) {
                    Logging.log(Logging.LEVEL_WARN, "InterruptedException while reading local plugin information", ex);
                    Thread.currentThread().interrupt();
                }

                SwingUtilities.invokeLater(() -> {
                    List<PluginInformation> availablePlugins = task.getAvailablePlugins();
                    List<PluginInformation> toInstallPlugins = new ArrayList<>();
                    List<PluginInformation> toRemovePlugins = new ArrayList<>();
                    List<PluginInformation> toDeletePlugins = new ArrayList<>();
                    for (PluginInformation pi1: availablePlugins) {
                        String name = pi1.name.toLowerCase(Locale.ENGLISH);
                        if (installList.contains(name)) toInstallPlugins.add(pi1);
                        if (removeList.contains(name)) toRemovePlugins.add(pi1);
                        if (deleteList.contains(name)) toDeletePlugins.add(pi1);
                    }
                    if (!installList.isEmpty()) {
                        PluginDownloadTask pluginDownloadTask =
                                new PluginDownloadTask(Main.parent, toInstallPlugins, tr("Installing plugins"));
                        MainApplication.worker.submit(pluginDownloadTask);
                    }
                    List<String> pls = new ArrayList<>(Config.getPref().getList("plugins"));
                    for (PluginInformation pi2: toInstallPlugins) {
                        if (!pls.contains(pi2.name)) {
                            pls.add(pi2.name);
                        }
                    }
                    for (PluginInformation pi3: toRemovePlugins) {
                        pls.remove(pi3.name);
                    }
                    for (PluginInformation pi4: toDeletePlugins) {
                        pls.remove(pi4.name);
                        new File(Main.pref.getPluginsDirectory(), pi4.name+".jar").deleteOnExit();
                    }
                    Config.getPref().putList("plugins", pls);
                });
            }
        };
        MainApplication.worker.submit(task);
        MainApplication.worker.submit(r);
    }

    private static String getDirectoryByAbbr(String base) {
        String dir;
        if ("prefs".equals(base) || base.isEmpty()) {
            dir = Config.getDirs().getPreferencesDirectory(false).getAbsolutePath();
        } else if ("cache".equals(base)) {
            dir = Config.getDirs().getCacheDirectory(false).getAbsolutePath();
        } else if ("plugins".equals(base)) {
            dir = Main.pref.getPluginsDirectory().getAbsolutePath();
        } else {
            dir = null;
        }
        return dir;
    }

    public static class XMLCommandProcessor {

        private Preferences mainPrefs;
        private final Map<String, Element> tasksMap = new HashMap<>();

        private boolean lastV; // last If condition result

        private ScriptEngine engine;

        public void openAndReadXML(File file) {
            PreferencesUtils.log("-- Reading custom preferences from " + file.getAbsolutePath() + " --");
            try {
                String fileDir = file.getParentFile().getAbsolutePath();
                if (fileDir != null) engine.eval("scriptDir='"+normalizeDirName(fileDir) +"';");
                try (InputStream is = Files.newInputStream(file.toPath())) {
                    openAndReadXML(is);
                }
            } catch (ScriptException | IOException | SecurityException | InvalidPathException ex) {
                PreferencesUtils.log(ex, "Error reading custom preferences:");
            }
        }

        public void openAndReadXML(InputStream is) {
            try {
                Document document = XmlUtils.parseSafeDOM(is);
                synchronized (CustomConfigurator.class) {
                    processXML(document);
                }
            } catch (SAXException | IOException | ParserConfigurationException ex) {
                PreferencesUtils.log(ex, "Error reading custom preferences:");
            }
            PreferencesUtils.log("-- Reading complete --");
        }

        public XMLCommandProcessor(Preferences mainPrefs) {
            try {
                this.mainPrefs = mainPrefs;
                PreferencesUtils.resetLog();
                engine = Utils.getJavaScriptEngine();
                if (engine == null) {
                    throw new ScriptException("Failed to retrieve JavaScript engine");
                }
                engine.eval("API={}; API.pref={}; API.fragments={};");

                engine.eval("homeDir='"+normalizeDirName(Config.getDirs().getPreferencesDirectory(false).getAbsolutePath()) +"';");
                engine.eval("josmVersion="+Version.getInstance().getVersion()+';');
                String className = CustomConfigurator.class.getName();
                engine.eval("API.messageBox="+className+".messageBox");
                engine.eval("API.askText=function(text) { return String("+className+".askForText(text));}");
                engine.eval("API.askOption="+className+".askForOption");
                engine.eval("API.downloadFile="+className+".downloadFile");
                engine.eval("API.downloadAndUnpackFile="+className+".downloadAndUnpackFile");
                engine.eval("API.deleteFile="+className+".deleteFile");
                engine.eval("API.plugin ="+className+".pluginOperation");
                engine.eval("API.pluginInstall = function(names) { "+className+".pluginOperation(names,'','');}");
                engine.eval("API.pluginUninstall = function(names) { "+className+".pluginOperation('',names,'');}");
                engine.eval("API.pluginDelete = function(names) { "+className+".pluginOperation('','',names);}");
            } catch (ScriptException ex) {
                PreferencesUtils.log("Error: initializing script engine: "+ex.getMessage());
                Logging.error(ex);
            }
        }

        private void processXML(Document document) {
            processXmlFragment(document.getDocumentElement());
        }

        private void processXmlFragment(Element root) {
            NodeList childNodes = root.getChildNodes();
            int nops = childNodes.getLength();
            for (int i = 0; i < nops; i++) {
                Node item = childNodes.item(i);
                if (item.getNodeType() != Node.ELEMENT_NODE) continue;
                String elementName = item.getNodeName();
                Element elem = (Element) item;

                switch(elementName) {
                case "var":
                    setVar(elem.getAttribute("name"), evalVars(elem.getAttribute("value")));
                    break;
                case "task":
                    tasksMap.put(elem.getAttribute("name"), elem);
                    break;
                case "runtask":
                    if (processRunTaskElement(elem)) return;
                    break;
                case "ask":
                    processAskElement(elem);
                    break;
                case "if":
                    processIfElement(elem);
                    break;
                case "else":
                    processElseElement(elem);
                    break;
                case "break":
                    return;
                case "plugin":
                    processPluginInstallElement(elem);
                    break;
                case "messagebox":
                    processMsgBoxElement(elem);
                    break;
                case "preferences":
                    processPreferencesElement(elem);
                    break;
                case "download":
                    processDownloadElement(elem);
                    break;
                case "delete":
                    processDeleteElement(elem);
                    break;
                case "script":
                    processScriptElement(elem);
                    break;
                default:
                    PreferencesUtils.log("Error: Unknown element " + elementName);
                }
            }
        }

        private void processPreferencesElement(Element item) {
            String oper = evalVars(item.getAttribute("operation"));
            String id = evalVars(item.getAttribute("id"));

            if ("delete-keys".equals(oper)) {
                String pattern = evalVars(item.getAttribute("pattern"));
                String key = evalVars(item.getAttribute("key"));
                PreferencesUtils.deletePreferenceKey(key, mainPrefs);
                PreferencesUtils.deletePreferenceKeyByPattern(pattern, mainPrefs);
                return;
            }

            Preferences tmpPref = readPreferencesFromDOMElement(item);
            PreferencesUtils.showPrefs(tmpPref);

            if (!id.isEmpty()) {
                try {
                    String fragmentVar = "API.fragments['"+id+"']";
                    engine.eval(fragmentVar+"={};");
                    PreferencesUtils.loadPrefsToJS(engine, tmpPref, fragmentVar, false);
                    // we store this fragment as API.fragments['id']
                } catch (ScriptException ex) {
                    PreferencesUtils.log(ex, "Error: can not load preferences fragment:");
                }
            }

            if ("replace".equals(oper)) {
                PreferencesUtils.log("Preferences replace: %d keys: %s\n",
                   tmpPref.getAllSettings().size(), tmpPref.getAllSettings().keySet().toString());
                PreferencesUtils.replacePreferences(tmpPref, mainPrefs);
            } else if ("append".equals(oper)) {
                PreferencesUtils.log("Preferences append: %d keys: %s\n",
                   tmpPref.getAllSettings().size(), tmpPref.getAllSettings().keySet().toString());
                PreferencesUtils.appendPreferences(tmpPref, mainPrefs);
            } else if ("delete-values".equals(oper)) {
                PreferencesUtils.deletePreferenceValues(tmpPref, mainPrefs);
            }
        }

         private void processDeleteElement(Element item) {
            String path = evalVars(item.getAttribute("path"));
            String base = evalVars(item.getAttribute("base"));
            deleteFile(path, base);
        }

        private void processDownloadElement(Element item) {
            String base = evalVars(item.getAttribute("base"));
            String dir = getDirectoryByAbbr(base);
            if (dir == null) {
                PreferencesUtils.log("Error: Can not find directory to place file, use base=cache, base=prefs or base=plugins attribute.");
                return;
            }

            String path = evalVars(item.getAttribute("path"));
            if (path.contains("..") || path.startsWith("/") || path.contains(":")) {
                return; // some basic protection
            }

            String address = evalVars(item.getAttribute("url"));
            if (address.isEmpty() || path.isEmpty()) {
                PreferencesUtils.log("Error: Please specify url=\"where to get file\" and path=\"where to place it\"");
                return;
            }

            String unzip = evalVars(item.getAttribute("unzip"));
            String mkdir = evalVars(item.getAttribute("mkdir"));
            processDownloadOperation(address, path, dir, "true".equals(mkdir), "true".equals(unzip));
        }

        private static void processPluginInstallElement(Element elem) {
            String install = elem.getAttribute("install");
            String uninstall = elem.getAttribute("remove");
            String delete = elem.getAttribute("delete");
            pluginOperation(install, uninstall, delete);
        }

        private void processMsgBoxElement(Element elem) {
            String text = evalVars(elem.getAttribute("text"));
            String locText = evalVars(elem.getAttribute(LanguageInfo.getJOSMLocaleCode()+".text"));
            if (!locText.isEmpty()) text = locText;

            String type = evalVars(elem.getAttribute("type"));
            messageBox(type, text);
        }

        private void processAskElement(Element elem) {
            String text = evalVars(elem.getAttribute("text"));
            String locText = evalVars(elem.getAttribute(LanguageInfo.getJOSMLocaleCode()+".text"));
            if (!locText.isEmpty()) text = locText;
            String var = elem.getAttribute("var");
            if (var.isEmpty()) var = "result";

            String input = evalVars(elem.getAttribute("input"));
            if ("true".equals(input)) {
                setVar(var, askForText(text));
            } else {
                String opts = evalVars(elem.getAttribute("options"));
                String locOpts = evalVars(elem.getAttribute(LanguageInfo.getJOSMLocaleCode()+".options"));
                if (!locOpts.isEmpty()) opts = locOpts;
                setVar(var, String.valueOf(askForOption(text, opts)));
            }
        }

        public void setVar(String name, String value) {
            try {
                engine.eval(name+"='"+value+"';");
            } catch (ScriptException ex) {
                PreferencesUtils.log(ex, String.format("Error: Can not assign variable: %s=%s :", name, value));
            }
        }

        private void processIfElement(Element elem) {
            String realValue = evalVars(elem.getAttribute("test"));
            boolean v = false;
            if ("true".equals(realValue) || "false".equals(realValue)) {
                processXmlFragment(elem);
                v = true;
            } else {
                PreferencesUtils.log("Error: Illegal test expression in if: %s=%s\n", elem.getAttribute("test"), realValue);
            }

            lastV = v;
        }

        private void processElseElement(Element elem) {
            if (!lastV) {
                processXmlFragment(elem);
            }
        }

        private boolean processRunTaskElement(Element elem) {
            String taskName = elem.getAttribute("name");
            Element task = tasksMap.get(taskName);
            if (task != null) {
                PreferencesUtils.log("EXECUTING TASK "+taskName);
                processXmlFragment(task); // process task recursively
            } else {
                PreferencesUtils.log("Error: Can not execute task "+taskName);
                return true;
            }
            return false;
        }

        private void processScriptElement(Element elem) {
            String js = elem.getChildNodes().item(0).getTextContent();
            PreferencesUtils.log("Processing script...");
            try {
                PreferencesUtils.modifyPreferencesByScript(engine, mainPrefs, js);
            } catch (ScriptException ex) {
                messageBox("e", ex.getMessage());
                PreferencesUtils.log(ex, "JS error:");
            }
            PreferencesUtils.log("Script finished");
        }

        /**
         * substitute ${expression} = expression evaluated by JavaScript
         * @param s string
         * @return evaluation result
         */
        private String evalVars(String s) {
            Matcher mr = Pattern.compile("\\$\\{([^\\}]*)\\}").matcher(s);
            StringBuffer sb = new StringBuffer();
            while (mr.find()) {
                try {
                    String result = engine.eval(mr.group(1)).toString();
                    mr.appendReplacement(sb, result);
                } catch (ScriptException ex) {
                    PreferencesUtils.log(ex, String.format("Error: Can not evaluate expression %s :", mr.group(1)));
                }
            }
            mr.appendTail(sb);
            return sb.toString();
        }

        private Preferences readPreferencesFromDOMElement(Element item) {
            Preferences tmpPref = new Preferences();
            try {
                Transformer xformer = XmlUtils.newSafeTransformerFactory().newTransformer();
                CharArrayWriter outputWriter = new CharArrayWriter(8192);
                StreamResult out = new StreamResult(outputWriter);

                xformer.transform(new DOMSource(item), out);

                String fragmentWithReplacedVars = evalVars(outputWriter.toString());

                CharArrayReader reader = new CharArrayReader(fragmentWithReplacedVars.toCharArray());
                tmpPref.fromXML(reader);
            } catch (TransformerException | XMLStreamException | IOException ex) {
                PreferencesUtils.log(ex, "Error: can not read XML fragment:");
            }

            return tmpPref;
        }

        private static String normalizeDirName(String dir) {
            String s = dir.replace('\\', '/');
            if (s.endsWith("/")) s = s.substring(0, s.length()-1);
            return s;
        }
    }
}
