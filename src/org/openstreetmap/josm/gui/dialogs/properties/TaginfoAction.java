// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import javax.swing.JTable;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Utils;

/**
 * Launch browser with Taginfo statistics for selected object.
 * @since 13521
 */
public class TaginfoAction extends JosmAction {

    final transient StringProperty TAGINFO_URL_PROP = new StringProperty("taginfo.url", "https://taginfo.openstreetmap.org/");

    private final JTable tagTable;
    private final Function<Integer, String> tagKeySupplier;
    private final Function<Integer, Map<String, Integer>> tagValuesSupplier;

    private final JTable membershipTable;
    private final Function<Integer, Relation> memberValueSupplier;

    /**
     * Constructs a new {@code TaginfoAction}.
     * @param tagTable The tag table. Cannot be null
     * @param tagKeySupplier Finds the key from given row of tag table. Cannot be null
     * @param tagValuesSupplier Finds the values from given row of tag table (map of values and number of occurrences). Cannot be null
     * @param membershipTable The membership table. Can be null
     * @param memberValueSupplier Finds the parent relation from given row of membership table. Can be null
     */
    public TaginfoAction(JTable tagTable, Function<Integer, String> tagKeySupplier, Function<Integer, Map<String, Integer>> tagValuesSupplier,
            JTable membershipTable, Function<Integer, Relation> memberValueSupplier) {
        super(tr("Go to Taginfo"), "dialogs/taginfo", tr("Launch browser with Taginfo statistics for selected object"), null, false);
        this.tagTable = Objects.requireNonNull(tagTable);
        this.tagKeySupplier = Objects.requireNonNull(tagKeySupplier);
        this.tagValuesSupplier = Objects.requireNonNull(tagValuesSupplier);
        this.membershipTable = membershipTable;
        this.memberValueSupplier = memberValueSupplier;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final String url;
        if (tagTable.getSelectedRowCount() == 1) {
            final int row = tagTable.getSelectedRow();
            final String key = Utils.encodeUrl(tagKeySupplier.apply(row)).replaceAll("\\+", "%20");
            Map<String, Integer> values = tagValuesSupplier.apply(row);
            if (values.size() == 1) {
                url = TAGINFO_URL_PROP.get() + "tags/" + key
                        + '=' + Utils.encodeUrl(values.keySet().iterator().next()).replaceAll("\\+", "%20");
            } else {
                url = TAGINFO_URL_PROP.get() + "keys/" + key;
            }
        } else if (membershipTable != null && membershipTable.getSelectedRowCount() == 1) {
            final String type = (memberValueSupplier.apply(membershipTable.getSelectedRow())).get("type");
            url = TAGINFO_URL_PROP.get() + "relations/" + type;
        } else {
            return;
        }
        OpenBrowser.displayUrl(url);
    }
}
