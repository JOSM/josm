// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;

import org.openstreetmap.josm.tools.Logging;

/**
 * A parser for the plugin list provided by a JOSM Plugin Download Site.
 *
 * See <a href="https://josm.openstreetmap.de/plugin">https://josm.openstreetmap.de/plugin</a>
 * for a sample of the document. The format is a custom format, kind of mix of CSV and RFC822 style
 * name/value-pairs.
 *
 */
public class PluginListParser {

    /**
     * Creates the plugin information object
     *
     * @param name the plugin name
     * @param url the plugin download url
     * @param manifest the plugin manifest attributes
     * @return a plugin information object
     * @throws PluginListParseException if plugin manifest cannot be parsed
     */
    public static PluginInformation createInfo(String name, String url, Attributes manifest) throws PluginListParseException {
        try {
            return new PluginInformation(
                    manifest,
                    name.substring(0, name.length() - 4),
                    url
                    );
        } catch (PluginException e) {
            throw new PluginListParseException(tr("Failed to create plugin information from manifest for plugin ''{0}''", name), e);
        }
    }

    /**
     * Parses a plugin information document and replies a list of plugin information objects.
     *
     * See <a href="https://josm.openstreetmap.de/plugin">https://josm.openstreetmap.de/plugin</a>
     * for a sample of the document. The format is a custom format, kind of mix of CSV and RFC822 style
     * name/value-pairs.
     *
     * @param in the input stream from which to parse
     * @return the list of plugin information objects
     * @throws PluginListParseException if something goes wrong while parsing
     */
    public List<PluginInformation> parse(InputStream in) throws PluginListParseException {
        List<PluginInformation> ret = new LinkedList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String name = null;
            String url = null;
            Attributes manifest = new Attributes();
            for (String line = r.readLine(); line != null; line = r.readLine()) {
                if (line.startsWith("\t")) {
                    final String[] keyValue = line.split("\\s*:\\s*", 2);
                    if (keyValue.length >= 2)
                        manifest.put(new Attributes.Name(keyValue[0].substring(1)), keyValue[1]);
                    continue;
                }
                addPluginInformation(ret, name, url, manifest);
                String[] x = line.split(";", -1);
                if (x.length != 2)
                    throw new IOException(tr("Illegal entry in plugin list.") + " " + line);
                name = x[0];
                url = x[1];
                manifest = new Attributes();
            }
            addPluginInformation(ret, name, url, manifest);
            return ret;
        } catch (IOException e) {
            throw new PluginListParseException(e);
        }
    }

    private static void addPluginInformation(List<PluginInformation> ret, String name, String url, Attributes manifest) {
        try {
            if (name != null) {
                PluginInformation info = createInfo(name, url, manifest);
                for (PluginProxy plugin : PluginHandler.pluginList) {
                    if (plugin.getPluginInformation().name.equals(info.getName())) {
                        info.localversion = plugin.getPluginInformation().localversion;
                    }
                }
                ret.add(info);
            }
        } catch (PluginListParseException ex) {
            Logging.error(ex);
        }
    }

}
