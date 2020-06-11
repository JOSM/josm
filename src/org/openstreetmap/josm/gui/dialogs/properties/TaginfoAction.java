// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Utils;

/**
 * Launch browser with Taginfo statistics for selected object.
 * @since 13521
 */
public class TaginfoAction extends AbstractAction {

    private static final StringProperty TAGINFO_URL_PROP = new StringProperty("taginfo.url", "https://taginfo.openstreetmap.org/");
    private static final StringProperty TAG_HISTORY_URL_PROP = new StringProperty("taghistory.url", "https://taghistory.raifer.tech/#***");

    private final Supplier<Tag> tagSupplier;
    private final Supplier<String> relationTypeSupplier;
    protected final String taginfoUrl;

    private TaginfoAction(String name, Supplier<Tag> tagSupplier, Supplier<String> relationTypeSupplier, String taginfoUrl) {
        super(name);
        this.tagSupplier = Objects.requireNonNull(tagSupplier);
        this.relationTypeSupplier = Objects.requireNonNull(relationTypeSupplier);
        this.taginfoUrl = withoutTrailingSlash(Objects.requireNonNull(taginfoUrl));
    }

    /**
     * Constructs a new {@code TaginfoAction}.
     * @param tagSupplier Supplies the tag for which Taginfo should be opened
     * @param relationTypeSupplier Supplies a relation type for which Taginfo should be opened
     * @since 16275
     */
    public TaginfoAction(Supplier<Tag> tagSupplier, Supplier<String> relationTypeSupplier) {
        this(tr("Go to Taginfo"), tagSupplier, relationTypeSupplier, TAGINFO_URL_PROP.get());
        new ImageProvider("dialogs/taginfo").getResource().attachImageIcon(this, true);
    }

    /**
     * Constructs a new {@code TaginfoAction} with a given URL and optional name suffix.
     * @param tagTable The tag table. Cannot be null
     * @param tagKeySupplier Finds the key from given row of tag table. Cannot be null
     * @param tagValuesSupplier Finds the values from given row of tag table (map of values and number of occurrences). Cannot be null
     * @param membershipTable The membership table. Can be null
     * @param memberValueSupplier Finds the parent relation from given row of membership table. Can be null
     * @since 16597
     */
    public TaginfoAction(JTable tagTable, IntFunction<String> tagKeySupplier, IntFunction<Map<String, Integer>> tagValuesSupplier,
                         JTable membershipTable, IntFunction<IRelation<?>> memberValueSupplier) {
        this(getTagSupplier(tagTable, tagKeySupplier, tagValuesSupplier),
                getRelationTypeSupplier(membershipTable, memberValueSupplier));
    }

    private static Supplier<Tag> getTagSupplier(JTable tagTable, IntFunction<String> tagKeySupplier,
                                                IntFunction<Map<String, Integer>> tagValuesSupplier) {
        Objects.requireNonNull(tagTable);
        Objects.requireNonNull(tagKeySupplier);
        Objects.requireNonNull(tagValuesSupplier);
        return () -> {
            if (tagTable.getSelectedRowCount() == 1) {
                final int row = tagTable.getSelectedRow();
                final String key = tagKeySupplier.apply(row);
                Map<String, Integer> values = tagValuesSupplier.apply(row);
                String value = values.size() == 1 ? values.keySet().iterator().next() : null;
                return new Tag(key, value);
            }
            return null;
        };
    }

    private static Supplier<String> getRelationTypeSupplier(JTable membershipTable, IntFunction<IRelation<?>> memberValueSupplier) {
        return () -> membershipTable != null && membershipTable.getSelectedRowCount() == 1
                ? memberValueSupplier.apply(membershipTable.getSelectedRow()).get("type") : null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Tag tag = tagSupplier.get();
        if (tag != null) {
            OpenBrowser.displayUrl(getTaginfoUrlForTag(tag));
            return;
        }
        String type = relationTypeSupplier.get();
        if (type != null) {
            OpenBrowser.displayUrl(getTaginfoUrlForRelationType(type));
        }
    }

    private static String withoutTrailingSlash(String url) {
        return Utils.strip(url, "/");
    }

    /**
     * Opens Taginfo for the given tag or key (if the tag value is null)
     * @param tag the tag
     * @since 16596
     */
    public String getTaginfoUrlForTag(Tag tag) {
        if (tag.getValue().isEmpty()) {
            return taginfoUrl + "/keys/" + encodeKeyValue(tag.getKey());
        } else {
            return taginfoUrl + "/tags/" + encodeKeyValue(tag.getKey()) + '=' + encodeKeyValue(tag.getValue());
        }
    }

    private static String encodeKeyValue(String string) {
        return Utils.encodeUrl(string).replaceAll("\\+", "%20");
    }

    /**
     * Opens Taginfo for the given relation type
     * @param type the relation type
     * @since 16596
     */
    public String getTaginfoUrlForRelationType(String type) {
        return taginfoUrl + "/relations/" + type;
    }

    /**
     * Returns a new action which launches the Taginfo instance from the given URL
     * @param name the action's text as displayed on the menu (if it is added to a menu)
     * @param taginfoUrl Taginfo URL
     * @since 16597
     */
    public TaginfoAction withTaginfoUrl(String name, String taginfoUrl) {
        TaginfoAction action = new TaginfoAction(name, tagSupplier, relationTypeSupplier, taginfoUrl);
        new ImageProvider("dialogs/taginfo").getResource().attachImageIcon(action, true);
        return action;
    }

    /**
     * Returns a new action which launches https://taghistory.raifer.tech/ for the given tag
     * @return a new action
     * @since 16596
     */
    public TaginfoAction toTagHistoryAction() {
        String url = TAG_HISTORY_URL_PROP.get();
        return new TaginfoAction(tr("Go to OSM Tag History"), tagSupplier, relationTypeSupplier, url) {
            @Override
            public String getTaginfoUrlForTag(Tag tag) {
                return String.join("/", taginfoUrl, tag.getKey(), tag.getValue());
            }

            @Override
            public String getTaginfoUrlForRelationType(String type) {
                return null;
            }
        };
    }
}
