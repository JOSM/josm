// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.ListListSetting;
import org.openstreetmap.josm.data.Preferences.ListSetting;
import org.openstreetmap.josm.data.Preferences.MapListSetting;
import org.openstreetmap.josm.data.Preferences.Setting;
import org.openstreetmap.josm.data.Preferences.StringSetting;
import org.openstreetmap.josm.gui.io.DownloadFileTask;
import org.openstreetmap.josm.plugins.PluginDownloadTask;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.ReadLocalPluginInformationTask;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class to process configuration changes stored in XML
 * can be used to modify preferences, store/delete files in .josm folders etc
 */
public final class CustomConfigurator {
    
    private CustomConfigurator() {
        // Hide default constructor for utils classes
    }
    
    private static StringBuilder summary = new StringBuilder();

    public static void log(String fmt, Object... vars) {
        summary.append(String.format(fmt, vars));
    }

    public static void log(String s) {
        summary.append(s);
        summary.append("\n");
    }

    public static String getLog() {
        return summary.toString();
    }

    public static void readXML(String dir, String fileName) {
        readXML(new File(dir, fileName));
    }

    /**
     * Read configuration script from XML file, modifying given preferences object
     * @param file - file to open for reading XML
     * @param prefs - arbitrary Preferences object to modify by script
     */
    public static void readXML(final File file, final Preferences prefs) {
        synchronized(CustomConfigurator.class) {
            busy=true;
        }
        new XMLCommandProcessor(prefs).openAndReadXML(file);
        synchronized(CustomConfigurator.class) {
            CustomConfigurator.class.notifyAll();
            busy=false;
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
     * Downloads file to one of JOSM standard folders nad unpack it as ZIP/JAR file
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
        if (type==null || type.length()==0) type="plain";

        switch (type.charAt(0)) {
            case 'i': JOptionPane.showMessageDialog(Main.parent, text, tr("Information"), JOptionPane.INFORMATION_MESSAGE); break;
            case 'w': JOptionPane.showMessageDialog(Main.parent, text, tr("Warning"), JOptionPane.WARNING_MESSAGE); break;
            case 'e': JOptionPane.showMessageDialog(Main.parent, text, tr("Error"), JOptionPane.ERROR_MESSAGE); break;
            case 'q': JOptionPane.showMessageDialog(Main.parent, text, tr("Question"), JOptionPane.QUESTION_MESSAGE); break;
            case 'p': JOptionPane.showMessageDialog(Main.parent, text, tr("Message"), JOptionPane.PLAIN_MESSAGE); break;
        }
    }

    /**
     * Simple function for choose window, may be used from JS API and from other code
     * @param text - message to show, HTML allowed
     * @param opts -
     * @return number of pressed button, -1 if cancelled
     */
    public static int askForOption(String text, String opts) {
        Integer answer;
        if (opts.length()>0) {
            String[] options = opts.split(";");
            answer = JOptionPane.showOptionDialog(Main.parent, text, "Question", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, 0);
        } else {
            answer = JOptionPane.showOptionDialog(Main.parent, text, "Question", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, 2);
        }
        if (answer==null) return -1; else return answer;
    }

    public static String askForText(String text) {
        String s = JOptionPane.showInputDialog(Main.parent, text, tr("Enter text"), JOptionPane.QUESTION_MESSAGE);
        if (s!=null && (s=s.trim()).length()>0) {
            return s;
        } else {
            return "";
        }
    }

    /**
     * This function exports part of user preferences to specified file.
     * Default values are not saved.
     * @param filename - where to export
     * @param append - if true, resulting file cause appending to exuisting preferences
     * @param keys - which preferences keys you need to export ("imagery.entries", for example)
     */
    public static void exportPreferencesKeysToFile(String filename, boolean append, String... keys) {
        HashSet<String> keySet = new HashSet<String>();
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
        List<String> keySet = new ArrayList<String>();
        Map<String, Setting> allSettings = Main.pref.getAllSettings();
        for (String key: allSettings.keySet()) {
            if (key.matches(pattern)) keySet.add(key);
        }
        exportPreferencesKeysToFile(fileName, append, keySet);
    }

    /**
     * Export specified preferences keys to configuration file
     * @param filename - name of file
     * @param append - will the preferences be appended to existing ones when file is imported later. Elsewhere preferences from file will replace existing keys.
     * @param keys - collection of preferences key names to save
     */
    public static void exportPreferencesKeysToFile(String filename, boolean append, Collection<String> keys) {
        Element root = null;
        Document document = null;
        Document exportDocument = null;

        try {
            String toXML = Main.pref.toXML(true);
            InputStream is = new ByteArrayInputStream(toXML.getBytes(Utils.UTF_8));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setValidating(false);
            builderFactory.setNamespaceAware(false);
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            document = builder.parse(is);
            exportDocument = builder.newDocument();
            root = document.getDocumentElement();
        } catch (Exception ex) {
            Main.warn("Error getting preferences to save:" +ex.getMessage());
        }
        if (root==null) return;
        try {

            Element newRoot = exportDocument.createElement("config");
            exportDocument.appendChild(newRoot);

            Element prefElem = exportDocument.createElement("preferences");
            prefElem.setAttribute("operation", append?"append":"replace");
            newRoot.appendChild(prefElem);

            NodeList childNodes = root.getChildNodes();
            int n = childNodes.getLength();
            for (int i = 0; i < n ; i++) {
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
        } catch (Exception ex) {
            Main.warn("Error saving preferences part:");
            Main.error(ex);
        }
    }


    public static void deleteFile(String path, String base) {
        String dir = getDirectoryByAbbr(base);
        if (dir==null) {
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

    public static void deleteFileOrDirectory(String path) {
        deleteFileOrDirectory(new File(path));
    }

    public static void deleteFileOrDirectory(File f) {
        if (f.isDirectory()) {
            for (File f1: f.listFiles()) {
                deleteFileOrDirectory(f1);
            }
        }
        try {
            f.delete();
        } catch (Exception e) {
            log("Warning: Can not delete file "+f.getPath());
        }
    }

    private static boolean busy=false;


    public static void pluginOperation(String install, String uninstall, String delete)  {
        final List<String> installList = new ArrayList<String>();
        final List<String> removeList = new ArrayList<String>();
        final List<String> deleteList = new ArrayList<String>();
        Collections.addAll(installList, install.toLowerCase().split(";"));
        Collections.addAll(removeList, uninstall.toLowerCase().split(";"));
        Collections.addAll(deleteList, delete.toLowerCase().split(";"));
        installList.remove("");removeList.remove("");deleteList.remove("");

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
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (task.isCanceled()) return;
                synchronized (CustomConfigurator.class) {
                try { // proceed only after all other tasks were finished
                    while (busy) CustomConfigurator.class.wait();
                } catch (InterruptedException ex) {
                    Main.warn("InterruptedException while reading local plugin information");
                }

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        List<PluginInformation> availablePlugins = task.getAvailablePlugins();
                        List<PluginInformation> toInstallPlugins = new ArrayList<PluginInformation>();
                        List<PluginInformation> toRemovePlugins = new ArrayList<PluginInformation>();
                        List<PluginInformation> toDeletePlugins = new ArrayList<PluginInformation>();
                        for (PluginInformation pi: availablePlugins) {
                            String name = pi.name.toLowerCase();
                            if (installList.contains(name)) toInstallPlugins.add(pi);
                            if (removeList.contains(name)) toRemovePlugins.add(pi);
                            if (deleteList.contains(name)) toDeletePlugins.add(pi);
                        }
                        if (!installList.isEmpty()) {
                            PluginDownloadTask pluginDownloadTask = new PluginDownloadTask(Main.parent, toInstallPlugins, tr ("Installing plugins"));
                            Main.worker.submit(pluginDownloadTask);
                        }
                        Collection<String> pls = new ArrayList<String>(Main.pref.getCollection("plugins"));
                        for (PluginInformation pi: toInstallPlugins) {
                            if (!pls.contains(pi.name)) {
                                pls.add(pi.name);
                            }
                        }
                        for (PluginInformation pi: toRemovePlugins) {
                            pls.remove(pi.name);
                        }
                        for (PluginInformation pi: toDeletePlugins) {
                            pls.remove(pi.name);
                            new File(Main.pref.getPluginsDirectory(), pi.name+".jar").deleteOnExit();
                        }
                        Main.pref.putCollection("plugins",pls);
                    }
                });
            }
            }

        };
        Main.worker.submit(task);
        Main.worker.submit(r);
    }

    private static String getDirectoryByAbbr(String base) {
            String dir;
            if ("prefs".equals(base) || base.length()==0) {
                dir = Main.pref.getPreferencesDir();
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
        tmp.colornames.putAll( pref.colornames );

        return tmp;
    }


    public static class XMLCommandProcessor {

        Preferences mainPrefs;
        Map<String,Element> tasksMap = new HashMap<String,Element>();

        private boolean lastV; // last If condition result


        ScriptEngine engine ;

        public void openAndReadXML(File file) {
            log("-- Reading custom preferences from " + file.getAbsolutePath() + " --");
            try {
                String fileDir = file.getParentFile().getAbsolutePath();
                if (fileDir!=null) engine.eval("scriptDir='"+normalizeDirName(fileDir) +"';");
                openAndReadXML(new BufferedInputStream(new FileInputStream(file)));
            } catch (Exception ex) {
                log("Error reading custom preferences: " + ex.getMessage());
            }
        }

        public void openAndReadXML(InputStream is) {
            try {
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                builderFactory.setValidating(false);
                builderFactory.setNamespaceAware(true);
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document document = builder.parse(is);
                synchronized (CustomConfigurator.class) {
                    processXML(document);
                }
            } catch (Exception ex) {
                log("Error reading custom preferences: "+ex.getMessage());
            } finally {
                Utils.close(is);
            }
            log("-- Reading complete --");
        }

        public XMLCommandProcessor(Preferences mainPrefs) {
            try {
                this.mainPrefs = mainPrefs;
                CustomConfigurator.summary = new StringBuilder();
                engine = new ScriptEngineManager().getEngineByName("rhino");
                engine.eval("API={}; API.pref={}; API.fragments={};");

                engine.eval("homeDir='"+normalizeDirName(Main.pref.getPreferencesDir()) +"';");
                engine.eval("josmVersion="+Version.getInstance().getVersion()+";");
                String className =  CustomConfigurator.class.getName();
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
            } catch (Exception ex) {
                log("Error: initializing script engine: "+ex.getMessage());
            }
        }

        private void processXML(Document document) {
            Element root = document.getDocumentElement();
            processXmlFragment(root);
        }

        private void processXmlFragment(Element root) {
            NodeList childNodes = root.getChildNodes();
            int nops = childNodes.getLength();
            for (int i = 0; i < nops; i++) {
                Node item = childNodes.item(i);
                if (item.getNodeType() != Node.ELEMENT_NODE) continue;
                String elementName = item.getNodeName();
                Element elem = (Element) item;

                if ("var".equals(elementName)) {
                    setVar(elem.getAttribute("name"), evalVars(elem.getAttribute("value")));
                } else if ("task".equals(elementName)) {
                    tasksMap.put(elem.getAttribute("name"), elem);
                } else if ("runtask".equals(elementName)) {
                    if (processRunTaskElement(elem)) return;
                } else if ("ask".equals(elementName)) {
                    processAskElement(elem);
                } else if ("if".equals(elementName)) {
                    processIfElement(elem);
                } else if ("else".equals(elementName)) {
                    processElseElement(elem);
                } else if ("break".equals(elementName)) {
                    return;
                } else if ("plugin".equals(elementName)) {
                    processPluginInstallElement(elem);
                } else if ("messagebox".equals(elementName)){
                    processMsgBoxElement(elem);
                } else if ("preferences".equals(elementName)) {
                    processPreferencesElement(elem);
                } else if ("download".equals(elementName)) {
                    processDownloadElement(elem);
                } else if ("delete".equals(elementName)) {
                    processDeleteElement(elem);
                } else if ("script".equals(elementName)) {
                    processScriptElement(elem);
                } else {
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
                if (key != null) {
                    PreferencesUtils.deletePreferenceKey(key, mainPrefs);
                }
                if (pattern != null) {
                    PreferencesUtils.deletePreferenceKeyByPattern(pattern, mainPrefs);
                }
                return;
            }

            Preferences tmpPref = readPreferencesFromDOMElement(item);
            PreferencesUtils.showPrefs(tmpPref);

            if (id.length()>0) {
                try {
                    String fragmentVar = "API.fragments['"+id+"']";
                    engine.eval(fragmentVar+"={};");
                    PreferencesUtils.loadPrefsToJS(engine, tmpPref, fragmentVar, false);
                    // we store this fragment as API.fragments['id']
                } catch (ScriptException ex) {
                    log("Error: can not load preferences fragment : "+ex.getMessage());
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
            }  else if ("delete-values".equals(oper)) {
                PreferencesUtils.deletePreferenceValues(tmpPref, mainPrefs);
            }
        }

         private void processDeleteElement(Element item) {
            String path = evalVars(item.getAttribute("path"));
            String base = evalVars(item.getAttribute("base"));
            deleteFile(base, path);
        }

        private void processDownloadElement(Element item) {
            String address = evalVars(item.getAttribute("url"));
            String path = evalVars(item.getAttribute("path"));
            String unzip = evalVars(item.getAttribute("unzip"));
            String mkdir = evalVars(item.getAttribute("mkdir"));

            String base = evalVars(item.getAttribute("base"));
            String dir = getDirectoryByAbbr(base);
            if (dir==null) {
                log("Error: Can not find directory to place file, use base=cache, base=prefs or base=plugins attribute.");
                return;
            }

            if (path.contains("..") || path.startsWith("/") || path.contains(":")) {
                return; // some basic protection
            }
            if (address == null || path == null || address.length() == 0 || path.length() == 0) {
                log("Error: Please specify url=\"where to get file\" and path=\"where to place it\"");
                return;
            }
            processDownloadOperation(address, path, dir, "true".equals(mkdir), "true".equals(unzip));
        }

        private void processPluginInstallElement(Element elem) {
            String install = elem.getAttribute("install");
            String uninstall = elem.getAttribute("remove");
            String delete = elem.getAttribute("delete");
            pluginOperation(install, uninstall, delete);
        }

        private void processMsgBoxElement(Element elem) {
            String text = evalVars(elem.getAttribute("text"));
            String locText = evalVars(elem.getAttribute(LanguageInfo.getJOSMLocaleCode()+".text"));
            if (locText!=null && locText.length()>0) text=locText;

            String type = evalVars(elem.getAttribute("type"));
            messageBox(type, text);
        }


        private void processAskElement(Element elem) {
            String text = evalVars(elem.getAttribute("text"));
            String locText = evalVars(elem.getAttribute(LanguageInfo.getJOSMLocaleCode()+".text"));
            if (locText.length()>0) text=locText;
            String var = elem.getAttribute("var");
            if (var.length()==0) var="result";

            String input = evalVars(elem.getAttribute("input"));
            if ("true".equals(input)) {
                setVar(var, askForText(text));
            } else {
                String opts = evalVars(elem.getAttribute("options"));
                String locOpts = evalVars(elem.getAttribute(LanguageInfo.getJOSMLocaleCode()+".options"));
                if (locOpts.length()>0) opts=locOpts;
                setVar(var, String.valueOf(askForOption(text, opts)));
            }
        }

        public void setVar(String name, String value) {
            try {
                engine.eval(name+"='"+value+"';");
            } catch (ScriptException ex) {
                log("Error: Can not assign variable: %s=%s  : %s\n", name, value, ex.getMessage());
            }
        }

        private void processIfElement(Element elem) {
            String realValue = evalVars(elem.getAttribute("test"));
            boolean v=false;
            if ("true".equals(realValue)) v=true; else
            if ("fales".equals(realValue)) v=true; else
            {
                log("Error: Illegal test expression in if: %s=%s\n", elem.getAttribute("test"), realValue);
            }

            if (v) processXmlFragment(elem);
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
            if (task!=null) {
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
                log("JS error: "+ex.getMessage());
            }
            log("Script finished");
        }

        /**
         * substitute ${expression} = expression evaluated by JavaScript
         */
        private String evalVars(String s) {
            Pattern p = Pattern.compile("\\$\\{([^\\}]*)\\}");
            Matcher mr =  p.matcher(s);
            StringBuffer sb = new StringBuffer();
            while (mr.find()) {
                try {
                    String result = engine.eval(mr.group(1)).toString();
                    mr.appendReplacement(sb, result);
                } catch (ScriptException ex) {
                    log("Error: Can not evaluate expression %s : %s",  mr.group(1), ex.getMessage());
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

                String fragmentWithReplacedVars= evalVars(outputWriter.toString());

                CharArrayReader reader = new CharArrayReader(fragmentWithReplacedVars.toCharArray());
                tmpPref.fromXML(reader);
            } catch (Exception ex) {
                log("Error: can not read XML fragment :" + ex.getMessage());
            }

            return tmpPref;
        }

        private String normalizeDirName(String dir) {
            String s = dir.replace("\\", "/");
            if (s.endsWith("/")) s=s.substring(0,s.length()-1);
            return s;
        }


    }

    /**
     * Helper class to do specific Preferences operation - appending, replacing,
     * deletion by key and by value
     * Also contains functions that convert preferences object to JavaScript object and back
     */
    public static class PreferencesUtils {

        private static void replacePreferences(Preferences fragment, Preferences mainpref) {
            for (Entry<String, Setting> entry: fragment.settingsMap.entrySet()) {
                mainpref.putSetting(entry.getKey(), entry.getValue());
            }
        }

        private static void appendPreferences(Preferences fragment, Preferences mainpref) {
            for (Entry<String, Setting> entry: fragment.settingsMap.entrySet()) {
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
        * Delete items from @param mainpref collections that match items from @param fragment collections
        */
        private static void deletePreferenceValues(Preferences fragment, Preferences mainpref) {

            for (Entry<String, Setting> entry : fragment.settingsMap.entrySet()) {
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
        Map<String, Setting> allSettings = pref.getAllSettings();
        for (Entry<String, Setting> entry : allSettings.entrySet()) {
            String key = entry.getKey();
            if (key.matches(pattern)) {
                log("Deleting preferences: deleting key from preferences: " + key);
                pref.putSetting(key, null);
            }
        }
    }

    private static void deletePreferenceKey(String key, Preferences pref) {
        Map<String, Setting> allSettings = pref.getAllSettings();
        if (allSettings.containsKey(key)) {
            log("Deleting preferences: deleting key from preferences: " + key);
            pref.putSetting(key, null);
        }
    }

    private static Collection<String> getCollection(Preferences mainpref, String key, boolean warnUnknownDefault)  {
        ListSetting existing = Utils.cast(mainpref.settingsMap.get(key), ListSetting.class);
        ListSetting defaults = Utils.cast(mainpref.defaultsMap.get(key), ListSetting.class);
        if (existing == null && defaults == null) {
            if (warnUnknownDefault) defaultUnknownWarning(key);
            return null;
        }
        if (existing != null)
            return new ArrayList<String>(existing.getValue());
        else
            return defaults.getValue() == null ? null : new ArrayList<String>(defaults.getValue());
    }

    private static Collection<Collection<String>> getArray(Preferences mainpref, String key, boolean warnUnknownDefault)  {
        ListListSetting existing = Utils.cast(mainpref.settingsMap.get(key), ListListSetting.class);
        ListListSetting defaults = Utils.cast(mainpref.defaultsMap.get(key), ListListSetting.class);

        if (existing == null && defaults == null) {
            if (warnUnknownDefault) defaultUnknownWarning(key);
            return null;
        }
        if (existing != null)
            return new ArrayList<Collection<String>>(existing.getValue());
        else
            return defaults.getValue() == null ? null : new ArrayList<Collection<String>>(defaults.getValue());
    }

    private static List<Map<String, String>> getListOfStructs(Preferences mainpref, String key, boolean warnUnknownDefault)  {
        MapListSetting existing = Utils.cast(mainpref.settingsMap.get(key), MapListSetting.class);
        MapListSetting defaults = Utils.cast(mainpref.settingsMap.get(key), MapListSetting.class);

        if (existing == null && defaults == null) {
            if (warnUnknownDefault) defaultUnknownWarning(key);
            return null;
        }

        if (existing != null)
            return new ArrayList<Map<String, String>>(existing.getValue());
        else
            return defaults.getValue() == null ? null : new ArrayList<Map<String, String>>(defaults.getValue());
    }

    private static void defaultUnknownWarning(String key) {
        log("Warning: Unknown default value of %s , skipped\n", key);
        JOptionPane.showMessageDialog(
                Main.parent,
                tr("<html>Settings file asks to append preferences to <b>{0}</b>,<br/> but its default value is unknown at this moment.<br/> Please activate corresponding function manually and retry importing.", key),
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE);
    }

    private static void showPrefs(Preferences tmpPref) {
        Main.info("properties: " + tmpPref.settingsMap);
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
     * @throws ScriptException
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
        Map<String, String> stringMap =  (Map<String, String>) engine.get("stringMap");
        @SuppressWarnings("unchecked")
        Map<String, List<String>> listMap = (SortedMap<String, List<String>> ) engine.get("listMap");
        @SuppressWarnings("unchecked")
        Map<String, List<Collection<String>>> listlistMap = (SortedMap<String, List<Collection<String>>>) engine.get("listlistMap");
        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, String>>> listmapMap = (SortedMap<String, List<Map<String,String>>>) engine.get("listmapMap");

        tmpPref.settingsMap.clear();

        Map<String, Setting> tmp = new HashMap<String, Setting>();
        for (Entry<String, String> e : stringMap.entrySet()) {
            tmp.put(e.getKey(), new StringSetting(e.getValue()));
        }
        for (Entry<String, List<String>> e : listMap.entrySet()) {
            tmp.put(e.getKey(), new ListSetting(e.getValue()));
        }

        for (Entry<String, List<Collection<String>>> e : listlistMap.entrySet()) {
            @SuppressWarnings("unchecked") List<List<String>> value = (List)e.getValue();
            tmp.put(e.getKey(), new ListListSetting(value));
        }
        for (Entry<String, List<Map<String, String>>> e : listmapMap.entrySet()) {
            tmp.put(e.getKey(), new MapListSetting(e.getValue()));
        }
        for (Entry<String, Setting> e : tmp.entrySet()) {
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
     * @throws ScriptException
     */
    public static void loadPrefsToJS(ScriptEngine engine, Preferences tmpPref, String whereToPutInJS, boolean includeDefaults) throws ScriptException {
        Map<String, String> stringMap =  new TreeMap<String, String>();
        Map<String, List<String>> listMap = new TreeMap<String, List<String>>();
        Map<String, List<List<String>>> listlistMap = new TreeMap<String, List<List<String>>>();
        Map<String, List<Map<String, String>>> listmapMap = new TreeMap<String, List<Map<String, String>>>();

        if (includeDefaults) {
            for (Map.Entry<String, Setting> e: tmpPref.defaultsMap.entrySet()) {
                Setting setting = e.getValue();
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
        Iterator<Map.Entry<String, Setting>> it = tmpPref.settingsMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Setting> e = it.next();
            if (e.getValue().getValue() == null) {
                it.remove();
            }
        }

        for (Map.Entry<String, Setting> e: tmpPref.settingsMap.entrySet()) {
            Setting setting = e.getValue();
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
