// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.upload;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.osm.CyclicUploadDependencyException;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.util.WindowGeometry;

/**
 * This upload hook reorders the list of new relations to upload such that child
 * relations are uploaded before parent relations. It also checks for cyclic
 * dependencies in the list of new relations.
 *
 *
 */
public class RelationUploadOrderHook implements UploadHook {

    /**
     * builds the panel which warns users about a cyclic dependency
     *
     * @param dep  the list of relations with a cyclic dependency
     * @return the panel
     */
    protected JPanel buildWarningPanel(List<Relation> dep) {
        JPanel pnl = new JPanel(new BorderLayout());
        String msg = tr("<html>{0} relations build a cycle because they refer to each other.<br>"
                + "JOSM cannot upload them. Please edit the relations and remove the "
                + "cyclic dependency.</html>", dep.size()-1);
        pnl.add(new JLabel(msg), BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel();
        model.addColumn(tr("Relation ..."));
        model.addColumn(tr("... refers to relation"));
        for (int i = 0; i < dep.size()-1; i++) {
            Relation r1 = dep.get(i);
            Relation r2 = dep.get(i+1);
            model.addRow(new Relation[] {r1, r2});
        }
        JTable tbl = new JTable(model);
        OsmPrimitivRenderer renderer = new OsmPrimitivRenderer();
        tbl.getColumnModel().getColumn(0).setCellRenderer(renderer);
        tbl.getColumnModel().getColumn(1).setCellRenderer(renderer);
        pnl.add(new JScrollPane(tbl), BorderLayout.CENTER);
        return pnl;
    }

    /**
     * Warns the user if a cyclic dependency is detected
     *
     * @param e the cyclic dependency exception
     */
    protected void warnCyclicUploadDependency(CyclicUploadDependencyException e) {
        List<Relation> dep = new ArrayList<>(e.getCyclicUploadDependency());
        Relation last = dep.get(dep.size() -1);
        Iterator<Relation> it = dep.iterator();
        while (it.hasNext()) {
            if (it.next() != last) {
                it.remove();
            } else {
                break;
            }
        }
        JPanel pnl = buildWarningPanel(dep);
        ExtendedDialog dialog = new ExtendedDialog(
                Main.parent,
                tr("Cycling dependencies"),
                tr("OK")
        );
        dialog.setContent(pnl, false /* don't embed in scroll pane */);
        dialog.setButtonIcons("ok");
        dialog.setRememberWindowGeometry(
                getClass().getName() + ".geometry",
                WindowGeometry.centerInWindow(Main.parent, new Dimension(300, 300))
        );
        dialog.showDialog();
    }

    @Override
    public boolean checkUpload(APIDataSet apiDataSet) {
        try {
            apiDataSet.adjustRelationUploadOrder();
            return true;
        } catch (CyclicUploadDependencyException e) {
            warnCyclicUploadDependency(e);
            return false;
        }
    }
}
