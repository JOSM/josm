// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.IntFunction;

import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.gui.MainApplication;

/**
 * Launch browser with wiki help for selected membership.
 * @since 15581
 */
public class HelpMembershipAction extends HelpAction {
    private final JTable membershipTable;
    private final IntFunction<IRelation<?>> memberValueSupplier;

    /**
     * Constructs a new {@code HelpAction}.
     * @param membershipTable The membership table. Can be null
     * @param memberValueSupplier Finds the parent relation from given row of membership table. Can be null
     */
    public HelpMembershipAction(JTable membershipTable, IntFunction<IRelation<?>> memberValueSupplier) {
        this.membershipTable = Objects.requireNonNull(membershipTable);
        this.memberValueSupplier = Objects.requireNonNull(memberValueSupplier);
        putValue(NAME, tr("Go to OSM wiki for relation help"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (membershipTable.getSelectedRowCount() == 1) {
            int row = membershipTable.getSelectedRow();
            final IRelation<?> relation = memberValueSupplier.apply(row);
            MainApplication.worker.execute(() -> displayRelationHelp(relation));
        } else {
            super.actionPerformed(e);
        }
    }
}
