// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;

import javax.swing.AbstractAction;
import javax.swing.JTable;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Utils;

/**
 * Launch browser with wiki help for selected object.
 * @since 13521
 */
public class HelpAction extends AbstractAction {
    private final JTable tagTable;
    private final IntFunction<String> tagKeySupplier;
    private final IntFunction<Map<String, Integer>> tagValuesSupplier;

    private final JTable membershipTable;
    private final IntFunction<Relation> memberValueSupplier;

    /**
     * Constructs a new {@code HelpAction}.
     * @param tagTable The tag table. Cannot be null
     * @param tagKeySupplier Finds the key from given row of tag table. Cannot be null
     * @param tagValuesSupplier Finds the values from given row of tag table (map of values and number of occurrences). Cannot be null
     * @param membershipTable The membership table. Can be null
     * @param memberValueSupplier Finds the parent relation from given row of membership table. Can be null
     */
    public HelpAction(JTable tagTable, IntFunction<String> tagKeySupplier, IntFunction<Map<String, Integer>> tagValuesSupplier,
            JTable membershipTable, IntFunction<Relation> memberValueSupplier) {
        this.tagTable = Objects.requireNonNull(tagTable);
        this.tagKeySupplier = Objects.requireNonNull(tagKeySupplier);
        this.tagValuesSupplier = Objects.requireNonNull(tagValuesSupplier);
        this.membershipTable = membershipTable;
        this.memberValueSupplier = memberValueSupplier;
        putValue(NAME, tr("Go to OSM wiki for tag help"));
        putValue(SHORT_DESCRIPTION, tr("Launch browser with wiki help for selected object"));
        new ImageProvider("dialogs", "search").getResource().attachImageIcon(this, true);
        putValue(ACCELERATOR_KEY, getKeyStroke());
    }

    /**
     * Returns the keystroke launching this action (F1).
     * @return the keystroke launching this action
     */
    public KeyStroke getKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            String base = Config.getPref().get("url.openstreetmap-wiki", "https://wiki.openstreetmap.org/wiki/");
            String lang = LanguageInfo.getWikiLanguagePrefix();
            final List<URI> uris = new ArrayList<>();
            if (tagTable.getSelectedRowCount() == 1) {
                int row = tagTable.getSelectedRow();
                String key = Utils.encodeUrl(tagKeySupplier.apply(row));
                Map<String, Integer> m = tagValuesSupplier.apply(row);
                if (!m.isEmpty()) {
                    String val = Utils.encodeUrl(m.entrySet().iterator().next().getKey());
                    uris.addAll(getTagURIs(base, lang, key, val));
                }
            } else if (membershipTable != null && membershipTable.getSelectedRowCount() == 1) {
                int row = membershipTable.getSelectedRow();
                uris.addAll(getRelationURIs(base, lang, memberValueSupplier.apply(row)));
            } else {
                // give the generic help page, if more than one element is selected
                uris.addAll(getGenericURIs(base, lang));
            }

            MainApplication.worker.execute(() -> displayHelp(uris));
        } catch (URISyntaxException e1) {
            Logging.error(e1);
        }
    }

    /**
     * Returns a list of URIs for the given key/value.
     * @param base OSM wiki base URL
     * @param lang Language prefix
     * @param key Key
     * @param val Value
     * @return a list of URIs for the given key/value by order of relevance
     * @throws URISyntaxException in case of internal error
     * @since 13522
     */
    public static List<URI> getTagURIs(String base, String lang, String key, String val) throws URISyntaxException {
        return Arrays.asList(
            new URI(String.format("%s%sTag:%s=%s", base, lang, key, val)),
            new URI(String.format("%sTag:%s=%s", base, key, val)),
            new URI(String.format("%s%sKey:%s", base, lang, key)),
            new URI(String.format("%sKey:%s", base, key)),
            new URI(String.format("%s%sMap_Features", base, lang)),
            new URI(String.format("%sMap_Features", base))
        );
    }

    /**
     * Returns a list of URIs for the given relation.
     * @param base OSM wiki base URL
     * @param lang Language prefix
     * @param rel Relation
     * @return a list of URIs for the given relation by order of relevance
     * @throws URISyntaxException in case of internal error
     * @since 13522
     */
    public static List<URI> getRelationURIs(String base, String lang, Relation rel) throws URISyntaxException {
        List<URI> uris = new ArrayList<>();
        String type = rel.get("type");
        if (type != null) {
            type = Utils.encodeUrl(type);
        }

        if (type != null && !type.isEmpty()) {
            uris.add(new URI(String.format("%s%sRelation:%s", base, lang, type)));
            uris.add(new URI(String.format("%sRelation:%s", base, type)));
        }

        uris.add(new URI(String.format("%s%sRelations", base, lang)));
        uris.add(new URI(String.format("%sRelations", base)));
        return uris;
    }

    /**
     * Returns a list of generic URIs (Map Features).
     * @param base OSM wiki base URL
     * @param lang Language prefix
     * @return a list of generic URIs (Map Features)
     * @throws URISyntaxException in case of internal error
     * @since 13522
     */
    public static List<URI> getGenericURIs(String base, String lang) throws URISyntaxException {
        return Arrays.asList(
            new URI(String.format("%s%sMap_Features", base, lang)),
            new URI(String.format("%sMap_Features", base))
        );
    }

    /**
     * Display help by searching the forst valid URI in the given list.
     * @param uris list of URIs to test
     * @since 13522
     */
    public static void displayHelp(final List<URI> uris) {
        try {
            // find a page that actually exists in the wiki
            HttpClient.Response conn;
            for (URI u : uris) {
                conn = HttpClient.create(u.toURL(), "HEAD").connect();

                if (conn.getResponseCode() != 200) {
                    conn.disconnect();
                } else {
                    long osize = conn.getContentLength();
                    if (osize > -1) {
                        conn.disconnect();

                        final URI newURI = new URI(u.toString()
                                .replace("=", "%3D") /* do not URLencode whole string! */
                                .replaceFirst("/wiki/", "/w/index.php?redirect=no&title=")
                        );
                        conn = HttpClient.create(newURI.toURL(), "HEAD").connect();
                    }

                    /* redirect pages have different content length, but retrieving a "nonredirect"
                     *  page using index.php and the direct-link method gives slightly different
                     *  content lengths, so we have to be fuzzy.. (this is UGLY, recode if u know better)
                     */
                    if (osize > -1 && conn.getContentLength() != -1 && Math.abs(conn.getContentLength() - osize) > 200) {
                        Logging.info("{0} is a mediawiki redirect", u);
                        conn.disconnect();
                    } else {
                        conn.disconnect();

                        OpenBrowser.displayUrl(u.toString());
                        break;
                    }
                }
            }
        } catch (URISyntaxException | IOException e1) {
            Logging.error(e1);
        }
    }
}
