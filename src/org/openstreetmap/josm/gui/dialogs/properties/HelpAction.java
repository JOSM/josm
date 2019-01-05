// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;

import javax.swing.AbstractAction;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Mediawiki;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * Launch browser with wiki help for selected object.
 * @since 13521
 */
public class HelpAction extends AbstractAction {
    private final JTable tagTable;
    private final IntFunction<String> tagKeySupplier;
    private final IntFunction<Map<String, Integer>> tagValuesSupplier;

    private final JTable membershipTable;
    private final IntFunction<IRelation<?>> memberValueSupplier;

    /**
     * Constructs a new {@code HelpAction}.
     * @param tagTable The tag table. Cannot be null
     * @param tagKeySupplier Finds the key from given row of tag table. Cannot be null
     * @param tagValuesSupplier Finds the values from given row of tag table (map of values and number of occurrences). Cannot be null
     * @param membershipTable The membership table. Can be null
     * @param memberValueSupplier Finds the parent relation from given row of membership table. Can be null
     * @since 13959 (signature)
     */
    public HelpAction(JTable tagTable, IntFunction<String> tagKeySupplier, IntFunction<Map<String, Integer>> tagValuesSupplier,
            JTable membershipTable, IntFunction<IRelation<?>> memberValueSupplier) {
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
        if (tagTable.getSelectedRowCount() == 1) {
            int row = tagTable.getSelectedRow();
            String key = Utils.encodeUrl(tagKeySupplier.apply(row));
            Map<String, Integer> m = tagValuesSupplier.apply(row);
            if (!m.isEmpty()) {
                String val = Utils.encodeUrl(m.entrySet().iterator().next().getKey());
                MainApplication.worker.execute(() -> displayTagHelp(key, val));
            }
        } else if (membershipTable != null && membershipTable.getSelectedRowCount() == 1) {
            int row = membershipTable.getSelectedRow();
            final IRelation<?> relation = memberValueSupplier.apply(row);
            MainApplication.worker.execute(() -> displayRelationHelp(relation));
        } else {
            // give the generic help page, if more than one element is selected
            MainApplication.worker.execute(HelpAction::displayGenericHelp);
        }
    }

    /**
     * Displays the most specific wiki page for the given key/value.
     * @param key Key
     * @param val Value
     * @since 14208
     */
    public static void displayTagHelp(String key, String val) {
        final String lang = LanguageInfo.getWikiLanguagePrefix(LanguageInfo.LocaleType.OSM_WIKI);
        final List<String> pages = Arrays.asList(
                String.format("%sTag:%s=%s", lang, key, val),
                String.format("Tag:%s=%s", key, val),
                String.format("%sKey:%s", lang, key),
                String.format("Key:%s", key),
                String.format("%sMap_Features", lang),
                "Map_Features"
        );
        displayHelp(pages);
    }

    /**
     * Displays the most specific wiki page for the given relation.
     * @param rel Relation
     * @since 14208
     */
    public static void displayRelationHelp(IRelation<?> rel) {
        final String lang = LanguageInfo.getWikiLanguagePrefix(LanguageInfo.LocaleType.OSM_WIKI);
        final List<String> pages = new ArrayList<>();
        String type = rel.get("type");
        if (type != null) {
            type = Utils.encodeUrl(type);
        }

        if (type != null && !type.isEmpty()) {
            pages.add(String.format("%sRelation:%s", lang, type));
            pages.add(String.format("Relation:%s", type));
        }

        pages.add(String.format("%sRelations", lang));
        pages.add("Relations");
        displayHelp(pages);
    }

    /**
     * Displays the localized Map Features.
     * @since 14208
     */
    public static void displayGenericHelp() {
        final String lang = LanguageInfo.getWikiLanguagePrefix(LanguageInfo.LocaleType.OSM_WIKI);
        final List<String> pages = Arrays.asList(
                String.format("%sMap_Features", lang),
                "Map_Features"
        );
        displayHelp(pages);
    }

    /**
     * Display help by opening the first existing wiki page in the given list.
     * @param pages list of wiki page names to test
     * @since 14208
     */
    public static void displayHelp(final List<String> pages) {
        try {
            new Mediawiki(Config.getUrls().getOSMWiki())
                    .findExistingPage(pages)
                    .ifPresent(page -> OpenBrowser.displayUrl(Config.getUrls().getOSMWiki() + "/wiki/" + page));
        } catch (IOException | ParserConfigurationException | XPathExpressionException | SAXException e1) {
            Logging.error(e1);
        }
    }
}
