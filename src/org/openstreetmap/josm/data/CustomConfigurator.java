// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.ListListSetting;
import org.openstreetmap.josm.data.preferences.ListSetting;
import org.openstreetmap.josm.data.preferences.MapListSetting;
import org.openstreetmap.josm.data.preferences.Setting;
import org.openstreetmap.josm.data.preferences.StringSetting;
import org.openstreetmap.josm.gui.io.DownloadFileTask;
import org.openstreetmap.josm.plugins.PluginDownloadTask;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.ReadLocalPluginInformationTask;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
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

    private static StringBuilder summary = new StringBuilder();

    private CustomConfigurator() {
        // Hide default constructor for utils classes
    }

    /**
     * Log a formatted message.
     * @param fmt format
     * @param vars arguments
     * @see String#format
     */
    public static void log(String fmt, Object... vars) {
        summary.append(String.format(fmt, vars));
    }

    /**
     * Log a message.
     * @param s message to log
     */
    public static void log(String s) {
        summary.append(s).append('\n');
    }

    /**
     * Log an exception.
     * @param e exception to log
     * @param s message prefix
     * @since 10469
     */
    public static void log(Exception e, String s) {
        summary.append(s).append(' ').append(Logging.getErrorMessage(e)).append('\n');
    }

    /**
     * Returns the log.
     * @return the log
     */
    public static String getLog() {
        return summary.toString();
    }

    /**
     * Resets the log.
     */
    public static void resetLog() {
        summary = new StringBuilder();
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

        Main.worker.submit(downloadFileTask);
        log("Info: downloading file from %s to %s in background ", parentDir, fOut.getAbsolutePath());
        if (unzip) log("and unpacking it"); else log("");

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
            DocumentBuilder builder = Utils.newSafeDOMBuilder();
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
            Transformer ts = TransformerFactory.newInstance().newTransformer();
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
            log("Error: Can not find base, use base=cache, base=prefs or base=plugins attribute.");
            return;
        }
        log("Delete file: %s\n", path);
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
            log("Warning: Can not delete file "+f.getPath());
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
            log("Plugins install: "+installList);
        }
        if (!removeList.isEmpty()) {
            log("Plugins turn off: "+removeList);
        }
        if (!deleteList.isEmpty()) {
            log("Plugins delete: "+deleteList);
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
                        Main.worker.submit(pluginDownloadTask);
                    }
                    Collection<String> pls = new ArrayList<>(Main.pref.getCollection("plugins"));
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
                    Main.pref.putCollection("plugins", pls);
                });
            }
        };
        Main.worker.submit(task);
        Main.worker.submit(r);
    }

    private static String getDirectoryByAbbr(String base) {
        String dir;
        if ("prefs".equals(base) || base.isEmpty()) {
            dir = Main.pref.getPreferencesDirectory().getAbsolutePath();
        } else if ("cache".equals(base)) {
            dir = Main.pref.getCacheDirectory().getAbsolutePath();
        } else if ("plugins".equals(base)) {
            dir = Main.pref.getPluginsDirectory().getAbsolutePath();
        } else {
            dir = null;
        }
        return dir;
    }

    public static Preferences clonePreferences(Preferences pref) {
        Preferences tmp = new Preferences();
        tmp.settingsMap.putAll(pref.settingsMap);
        tmp.defaultsMap.putAll(pref.defaultsMap);
        tmp.colornames.putAll(pref.colornames);

        return tmp;
    }

    public static class XMLCommandProcessor {

        private Preferences mainPrefs;
        private final Map<String, Element> tasksMap = new HashMap<>();

        private boolean lastV; // last If condition result

        private ScriptEngine engine;

        public void openAndReadXML(File file) {
            log("-- Reading custom preferences from " + file.getAbsolutePath() + " --");
            try {
                String fileDir = file.getParentFile().getAbsolutePath();
                if (fileDir != null) engine.eval("scriptDir='"+normalizeDirName(fileDir) +"';");
                try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                    openAndReadXML(is);
                }
            } catch (ScriptException | IOException | SecurityException ex) {
                log(ex, "Error reading custom preferences:");
            }
        }

        public void openAndReadXML(InputStream is) {
            try {
                Document document = Utils.parseSafeDOM(is);
                synchronized (CustomConfigurator.class) {
                    processXML(document);
                }
            } catch (SAXException | IOException | ParserConfigurationException ex) {
                log(ex, "Error reading custom preferences:");
            }
            log("-- Reading complete --");
        }

        public XMLCommandProcessor(Preferences mainPrefs) {
            try {
                this.mainPrefs = mainPrefs;
                resetLog();
                engine = new ScriptEngineManager().getEngineByName("JavaScript");
                engine.eval("API={}; API.pref={}; API.fragments={};");

                engine.eval("homeDir='"+normalizeDirName(Main.pref.getPreferencesDirectory().getAbsolutePath()) +"';");
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
                log("Error: initializing script engine: "+ex.getMessage());
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
                    log("Error: Unknown element " + elementName);
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
                    log(ex, "Error: can not load preferences fragment:");
                }
            }

            if ("replace".equals(oper)) {
                log("Preferences replace: %d keys: %s\n",
                   tmpPref.getAllSettings().size(), tmpPref.getAllSettings().keySet().toString());
                PreferencesUtils.replacePreferences(tmpPref, mainPrefs);
            } else if ("append".equals(oper)) {
                log("Preferences append: %d keys: %s\n",
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
                log("Error: Can not find directory to place file, use base=cache, base=prefs or base=plugins attribute.");
                return;
            }

            String path = evalVars(item.getAttribute("path"));
            if (path.contains("..") || path.startsWith("/") || path.contains(":")) {
                return; // some basic protection
            }

            String address = evalVars(item.getAttribute("url"));
            if (address.isEmpty() || path.isEmpty()) {
                log("Error: Please specify url=\"where to get file\" and path=\"where to place it\"");
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
                log(ex, String.format("Error: Can not assign variable: %s=%s :", name, value));
            }
        }

        private void processIfElement(Element elem) {
            String realValue = evalVars(elem.getAttribute("test"));
            boolean v = false;
            if ("true".equals(realValue) || "false".equals(realValue)) {
                processXmlFragment(elem);
                v = true;
            } else {
                log("Error: Illegal test expression in if: %s=%s\n", elem.getAttribute("test"), realValue);
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
                log("EXECUTING TASK "+taskName);
                processXmlFragment(task); // process task recursively
            } else {
                log("Error: Can not execute task "+taskName);
                return true;
            }
            return false;
        }

        private void processScriptElement(Element elem) {
            String js = elem.getChildNodes().item(0).getTextContent();
            log("Processing script...");
            try {
                PreferencesUtils.modifyPreferencesByScript(engine, mainPrefs, js);
            } catch (ScriptException ex) {
                messageBox("e", ex.getMessage());
                log(ex, "JS error:");
            }
            log("Script finished");
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
                    log(ex, String.format("Error: Can not evaluate expression %s :", mr.group(1)));
                }
            }
            mr.appendTail(sb);
            return sb.toString();
        }

        private Preferences readPreferencesFromDOMElement(Element item) {
            Preferences tmpPref = new Preferences();
            try {
                Transformer xformer = TransformerFactory.newInstance().newTransformer();
                CharArrayWriter outputWriter = new CharArrayWriter(8192);
                StreamResult out = new StreamResult(outputWriter);

                xformer.transform(new DOMSource(item), out);

                String fragmentWithReplacedVars = evalVars(outputWriter.toString());

                CharArrayReader reader = new CharArrayReader(fragmentWithReplacedVars.toCharArray());
                tmpPref.fromXML(reader);
            } catch (TransformerException | XMLStreamException | IOException ex) {
                log(ex, "Error: can not read XML fragment:");
            }

            return tmpPref;
        }

        private static String normalizeDirName(String dir) {
            String s = dir.replace('\\', '/');
            if (s.endsWith("/")) s = s.substring(0, s.length()-1);
            return s;
        }
    }

    /**
     * Helper class to do specific Preferences operation - appending, replacing,
     * deletion by key and by value
     * Also contains functions that convert preferences object to JavaScript object and back
     */
    public static final class PreferencesUtils {

        private PreferencesUtils() {
            // Hide implicit public constructor for utility class
        }

        private static void replacePreferences(Preferences fragment, Preferences mainpref) {
            for (Entry<String, Setting<?>> entry: fragment.settingsMap.entrySet()) {
                mainpref.putSetting(entry.getKey(), entry.getValue());
            }
        }

        private static void appendPreferences(Preferences fragment, Preferences mainpref) {
            for (Entry<String, Setting<?>> entry: fragment.settingsMap.entrySet()) {
                String key = entry.getKey();
                if (entry.getValue() instanceof StringSetting) {
                    mainpref.putSetting(key, entry.getValue());
                } else if (entry.getValue() instanceof ListSetting) {
                    ListSetting lSetting = (ListSetting) entry.getValue();
                    Collection<String> newItems = getCollection(mainpref, key, true);
                    if (newItems == null) continue;
                    for (String item : lSetting.getValue()) {
                        // add nonexisting elements to then list
                        if (!newItems.contains(item)) {
                            newItems.add(item);
                        }
                    }
                    mainpref.putCollection(key, newItems);
                } else if (entry.getValue() instanceof ListListSetting) {
                    ListListSetting llSetting = (ListListSetting) entry.getValue();
                    Collection<Collection<String>> newLists = getArray(mainpref, key, true);
                    if (newLists == null) continue;

                    for (Collection<String> list : llSetting.getValue()) {
                        // add nonexisting list (equals comparison for lists is used implicitly)
                        if (!newLists.contains(list)) {
                            newLists.add(list);
                        }
                    }
                    mainpref.putArray(key, newLists);
                } else if (entry.getValue() instanceof MapListSetting) {
                    MapListSetting mlSetting = (MapListSetting) entry.getValue();
                    List<Map<String, String>> newMaps = getListOfStructs(mainpref, key, true);
                    if (newMaps == null) continue;

                    // get existing properties as list of maps

                    for (Map<String, String> map : mlSetting.getValue()) {
                        // add nonexisting map (equals comparison for maps is used implicitly)
                        if (!newMaps.contains(map)) {
                            newMaps.add(map);
                        }
                    }
                    mainpref.putListOfStructs(entry.getKey(), newMaps);
                }
            }
        }

        /**
         * Delete items from {@code mainpref} collections that match items from {@code fragment} collections.
         * @param fragment preferences
         * @param mainpref main preferences
         */
        private static void deletePreferenceValues(Preferences fragment, Preferences mainpref) {

            for (Entry<String, Setting<?>> entry : fragment.settingsMap.entrySet()) {
                String key = entry.getKey();
                if (entry.getValue() instanceof StringSetting) {
                    StringSetting sSetting = (StringSetting) entry.getValue();
                    // if mentioned value found, delete it
                    if (sSetting.equals(mainpref.settingsMap.get(key))) {
                        mainpref.put(key, null);
                    }
                } else if (entry.getValue() instanceof ListSetting) {
                    ListSetting lSetting = (ListSetting) entry.getValue();
                    Collection<String> newItems = getCollection(mainpref, key, true);
                    if (newItems == null) continue;

                    // remove mentioned items from collection
                    for (String item : lSetting.getValue()) {
                        log("Deleting preferences: from list %s: %s\n", key, item);
                        newItems.remove(item);
                    }
                    mainpref.putCollection(entry.getKey(), newItems);
                } else if (entry.getValue() instanceof ListListSetting) {
                    ListListSetting llSetting = (ListListSetting) entry.getValue();
                    Collection<Collection<String>> newLists = getArray(mainpref, key, true);
                    if (newLists == null) continue;

                    // if items are found in one of lists, remove that list!
                    Iterator<Collection<String>> listIterator = newLists.iterator();
                    while (listIterator.hasNext()) {
                        Collection<String> list = listIterator.next();
                        for (Collection<String> removeList : llSetting.getValue()) {
                            if (list.containsAll(removeList)) {
                                // remove current list, because it matches search criteria
                                log("Deleting preferences: list from lists %s: %s\n", key, list);
                                listIterator.remove();
                            }
                        }
                    }

                    mainpref.putArray(key, newLists);
                } else if (entry.getValue() instanceof MapListSetting) {
                    MapListSetting mlSetting = (MapListSetting) entry.getValue();
                    List<Map<String, String>> newMaps = getListOfStructs(mainpref, key, true);
                    if (newMaps == null) continue;

                    Iterator<Map<String, String>> mapIterator = newMaps.iterator();
                    while (mapIterator.hasNext()) {
                        Map<String, String> map = mapIterator.next();
                        for (Map<String, String> removeMap : mlSetting.getValue()) {
                            if (map.entrySet().containsAll(removeMap.entrySet())) {
                                // the map contain all mentioned key-value pair, so it should be deleted from "maps"
                                log("Deleting preferences: deleting map from maps %s: %s\n", key, map);
                                mapIterator.remove();
                            }
                        }
                    }
                    mainpref.putListOfStructs(entry.getKey(), newMaps);
                }
            }
        }

    private static void deletePreferenceKeyByPattern(String pattern, Preferences pref) {
        Map<String, Setting<?>> allSettings = pref.getAllSettings();
        for (Entry<String, Setting<?>> entry : allSettings.entrySet()) {
            String key = entry.getKey();
            if (key.matches(pattern)) {
                log("Deleting preferences: deleting key from preferences: " + key);
                pref.putSetting(key, null);
            }
        }
    }

    private static void deletePreferenceKey(String key, Preferences pref) {
        Map<String, Setting<?>> allSettings = pref.getAllSettings();
        if (allSettings.containsKey(key)) {
            log("Deleting preferences: deleting key from preferences: " + key);
            pref.putSetting(key, null);
        }
    }

    private static Collection<String> getCollection(Preferences mainpref, String key, boolean warnUnknownDefault) {
        ListSetting existing = Utils.cast(mainpref.settingsMap.get(key), ListSetting.class);
        ListSetting defaults = Utils.cast(mainpref.defaultsMap.get(key), ListSetting.class);
        if (existing == null && defaults == null) {
            if (warnUnknownDefault) defaultUnknownWarning(key);
            return null;
        }
        if (existing != null)
            return new ArrayList<>(existing.getValue());
        else
            return defaults.getValue() == null ? null : new ArrayList<>(defaults.getValue());
    }

    private static Collection<Collection<String>> getArray(Preferences mainpref, String key, boolean warnUnknownDefault) {
        ListListSetting existing = Utils.cast(mainpref.settingsMap.get(key), ListListSetting.class);
        ListListSetting defaults = Utils.cast(mainpref.defaultsMap.get(key), ListListSetting.class);

        if (existing == null && defaults == null) {
            if (warnUnknownDefault) defaultUnknownWarning(key);
            return null;
        }
        if (existing != null)
            return new ArrayList<>(existing.getValue());
        else
            return defaults.getValue() == null ? null : new ArrayList<>(defaults.getValue());
    }

    private static List<Map<String, String>> getListOfStructs(Preferences mainpref, String key, boolean warnUnknownDefault) {
        MapListSetting existing = Utils.cast(mainpref.settingsMap.get(key), MapListSetting.class);
        MapListSetting defaults = Utils.cast(mainpref.settingsMap.get(key), MapListSetting.class);

        if (existing == null && defaults == null) {
            if (warnUnknownDefault) defaultUnknownWarning(key);
            return null;
        }

        if (existing != null)
            return new ArrayList<>(existing.getValue());
        else
            return defaults.getValue() == null ? null : new ArrayList<>(defaults.getValue());
    }

    private static void defaultUnknownWarning(String key) {
        log("Warning: Unknown default value of %s , skipped\n", key);
        JOptionPane.showMessageDialog(
                Main.parent,
                tr("<html>Settings file asks to append preferences to <b>{0}</b>,<br/> "+
                        "but its default value is unknown at this moment.<br/> " +
                        "Please activate corresponding function manually and retry importing.", key),
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE);
    }

    private static void showPrefs(Preferences tmpPref) {
        Logging.info("properties: " + tmpPref.settingsMap);
    }

    private static void modifyPreferencesByScript(ScriptEngine engine, Preferences tmpPref, String js) throws ScriptException {
        loadPrefsToJS(engine, tmpPref, "API.pref", true);
        engine.eval(js);
        readPrefsFromJS(engine, tmpPref, "API.pref");
    }

    /**
     * Convert JavaScript preferences object to preferences data structures
     * @param engine - JS engine to put object
     * @param tmpPref - preferences to fill from JS
     * @param varInJS - JS variable name, where preferences are stored
     * @throws ScriptException if the evaluation fails
     */
    public static void readPrefsFromJS(ScriptEngine engine, Preferences tmpPref, String varInJS) throws ScriptException {
        String finish =
            "stringMap = new java.util.TreeMap ;"+
            "listMap =  new java.util.TreeMap ;"+
            "listlistMap = new java.util.TreeMap ;"+
            "listmapMap =  new java.util.TreeMap ;"+
            "for (key in "+varInJS+") {"+
            "  val = "+varInJS+"[key];"+
            "  type = typeof val == 'string' ? 'string' : val.type;"+
            "  if (type == 'string') {"+
            "    stringMap.put(key, val);"+
            "  } else if (type == 'list') {"+
            "    l = new java.util.ArrayList;"+
            "    for (i=0; i<val.length; i++) {"+
            "      l.add(java.lang.String.valueOf(val[i]));"+
            "    }"+
            "    listMap.put(key, l);"+
            "  } else if (type == 'listlist') {"+
            "    l = new java.util.ArrayList;"+
            "    for (i=0; i<val.length; i++) {"+
            "      list=val[i];"+
            "      jlist=new java.util.ArrayList;"+
            "      for (j=0; j<list.length; j++) {"+
            "         jlist.add(java.lang.String.valueOf(list[j]));"+
            "      }"+
            "      l.add(jlist);"+
            "    }"+
            "    listlistMap.put(key, l);"+
            "  } else if (type == 'listmap') {"+
            "    l = new java.util.ArrayList;"+
            "    for (i=0; i<val.length; i++) {"+
            "      map=val[i];"+
            "      jmap=new java.util.TreeMap;"+
            "      for (var key2 in map) {"+
            "         jmap.put(key2,java.lang.String.valueOf(map[key2]));"+
            "      }"+
            "      l.add(jmap);"+
            "    }"+
            "    listmapMap.put(key, l);"+
            "  }  else {" +
            "   org.openstreetmap.josm.data.CustomConfigurator.log('Unknown type:'+val.type+ '- use list, listlist or listmap'); }"+
            "  }";
        engine.eval(finish);

        @SuppressWarnings("unchecked")
        Map<String, String> stringMap = (Map<String, String>) engine.get("stringMap");
        @SuppressWarnings("unchecked")
        Map<String, List<String>> listMap = (Map<String, List<String>>) engine.get("listMap");
        @SuppressWarnings("unchecked")
        Map<String, List<Collection<String>>> listlistMap = (Map<String, List<Collection<String>>>) engine.get("listlistMap");
        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, String>>> listmapMap = (Map<String, List<Map<String, String>>>) engine.get("listmapMap");

        tmpPref.settingsMap.clear();

        Map<String, Setting<?>> tmp = new HashMap<>();
        for (Entry<String, String> e : stringMap.entrySet()) {
            tmp.put(e.getKey(), new StringSetting(e.getValue()));
        }
        for (Entry<String, List<String>> e : listMap.entrySet()) {
            tmp.put(e.getKey(), new ListSetting(e.getValue()));
        }

        for (Entry<String, List<Collection<String>>> e : listlistMap.entrySet()) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            List<List<String>> value = (List) e.getValue();
            tmp.put(e.getKey(), new ListListSetting(value));
        }
        for (Entry<String, List<Map<String, String>>> e : listmapMap.entrySet()) {
            tmp.put(e.getKey(), new MapListSetting(e.getValue()));
        }
        for (Entry<String, Setting<?>> e : tmp.entrySet()) {
            if (e.getValue().equals(tmpPref.defaultsMap.get(e.getKey()))) continue;
            tmpPref.settingsMap.put(e.getKey(), e.getValue());
        }
    }

    /**
     * Convert preferences data structures to JavaScript object
     * @param engine - JS engine to put object
     * @param tmpPref - preferences to convert
     * @param whereToPutInJS - variable name to store preferences in JS
     * @param includeDefaults - include known default values to JS objects
     * @throws ScriptException if the evaluation fails
     */
    public static void loadPrefsToJS(ScriptEngine engine, Preferences tmpPref, String whereToPutInJS, boolean includeDefaults)
            throws ScriptException {
        Map<String, String> stringMap = new TreeMap<>();
        Map<String, List<String>> listMap = new TreeMap<>();
        Map<String, List<List<String>>> listlistMap = new TreeMap<>();
        Map<String, List<Map<String, String>>> listmapMap = new TreeMap<>();

        if (includeDefaults) {
            for (Map.Entry<String, Setting<?>> e: tmpPref.defaultsMap.entrySet()) {
                Setting<?> setting = e.getValue();
                if (setting instanceof StringSetting) {
                    stringMap.put(e.getKey(), ((StringSetting) setting).getValue());
                } else if (setting instanceof ListSetting) {
                    listMap.put(e.getKey(), ((ListSetting) setting).getValue());
                } else if (setting instanceof ListListSetting) {
                    listlistMap.put(e.getKey(), ((ListListSetting) setting).getValue());
                } else if (setting instanceof MapListSetting) {
                    listmapMap.put(e.getKey(), ((MapListSetting) setting).getValue());
                }
            }
        }
        tmpPref.settingsMap.entrySet().removeIf(e -> e.getValue().getValue() == null);

        for (Map.Entry<String, Setting<?>> e: tmpPref.settingsMap.entrySet()) {
            Setting<?> setting = e.getValue();
            if (setting instanceof StringSetting) {
                stringMap.put(e.getKey(), ((StringSetting) setting).getValue());
            } else if (setting instanceof ListSetting) {
                listMap.put(e.getKey(), ((ListSetting) setting).getValue());
            } else if (setting instanceof ListListSetting) {
                listlistMap.put(e.getKey(), ((ListListSetting) setting).getValue());
            } else if (setting instanceof MapListSetting) {
                listmapMap.put(e.getKey(), ((MapListSetting) setting).getValue());
            }
        }

        engine.put("stringMap", stringMap);
        engine.put("listMap", listMap);
        engine.put("listlistMap", listlistMap);
        engine.put("listmapMap", listmapMap);

        String init =
            "function getJSList( javaList ) {"+
            " var jsList; var i; "+
            " if (javaList == null) return null;"+
            "jsList = [];"+
            "  for (i = 0; i < javaList.size(); i++) {"+
            "    jsList.push(String(list.get(i)));"+
            "  }"+
            "return jsList;"+
            "}"+
            "function getJSMap( javaMap ) {"+
            " var jsMap; var it; var e; "+
            " if (javaMap == null) return null;"+
            " jsMap = {};"+
            " for (it = javaMap.entrySet().iterator(); it.hasNext();) {"+
            "    e = it.next();"+
            "    jsMap[ String(e.getKey()) ] = String(e.getValue()); "+
            "  }"+
            "  return jsMap;"+
            "}"+
            "for (it = stringMap.entrySet().iterator(); it.hasNext();) {"+
            "  e = it.next();"+
            whereToPutInJS+"[String(e.getKey())] = String(e.getValue());"+
            "}\n"+
            "for (it = listMap.entrySet().iterator(); it.hasNext();) {"+
            "  e = it.next();"+
            "  list = e.getValue();"+
            "  jslist = getJSList(list);"+
            "  jslist.type = 'list';"+
            whereToPutInJS+"[String(e.getKey())] = jslist;"+
            "}\n"+
            "for (it = listlistMap.entrySet().iterator(); it.hasNext(); ) {"+
            "  e = it.next();"+
            "  listlist = e.getValue();"+
            "  jslistlist = [];"+
            "  for (it2 = listlist.iterator(); it2.hasNext(); ) {"+
            "    list = it2.next(); "+
            "    jslistlist.push(getJSList(list));"+
            "    }"+
            "  jslistlist.type = 'listlist';"+
            whereToPutInJS+"[String(e.getKey())] = jslistlist;"+
            "}\n"+
            "for (it = listmapMap.entrySet().iterator(); it.hasNext();) {"+
            "  e = it.next();"+
            "  listmap = e.getValue();"+
            "  jslistmap = [];"+
            "  for (it2 = listmap.iterator(); it2.hasNext();) {"+
            "    map = it2.next();"+
            "    jslistmap.push(getJSMap(map));"+
            "    }"+
            "  jslistmap.type = 'listmap';"+
            whereToPutInJS+"[String(e.getKey())] = jslistmap;"+
            "}\n";

            // Execute conversion script
            engine.eval(init);
        }
    }
}
