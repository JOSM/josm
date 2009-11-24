// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.actions.AbstractInfoAction;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * VersionTable shows a list of version in a {@see History} of an {@see OsmPrimitive}.
 *
 *
 */
public class VersionTable extends JTable implements Observer{

    private static Logger logger = Logger.getLogger(VersionTable.class.getName());
    private VersionTablePopupMenu popupMenu;

    protected void build() {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        addMouseListener(new MouseHandler());
        getSelectionModel().addListSelectionListener(new SelectionHandler());
        popupMenu = new VersionTablePopupMenu();
        addMouseListener(new PopupMenuTrigger());
    }

    public VersionTable(HistoryBrowserModel model) {
        super(model.getVersionTableModel(), new VersionTableColumnModel());
        model.addObserver(this);
        build();
    }

    protected void handleSelectReferencePointInTime(int row) {
        getVersionTableModel().setReferencePointInTime(row);
    }

    protected void handleSelectCurrentPointInTime(int row) {
        getVersionTableModel().setCurrentPointInTime(row);
    }

    protected HistoryBrowserModel.VersionTableModel getVersionTableModel() {
        return (HistoryBrowserModel.VersionTableModel) getModel();
    }

    class MouseHandler extends MouseAdapter {
        protected void handleDoubleClick(MouseEvent e) {
            int row = rowAtPoint(e.getPoint());
            handleSelectReferencePointInTime(row);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            switch(e.getClickCount()) {
                case 2: handleDoubleClick(e); break;
            }
        }
    }

    class SelectionHandler implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            DefaultListSelectionModel model = (DefaultListSelectionModel)e.getSource();
            if (model.getMinSelectionIndex() >= 0) {
                handleSelectCurrentPointInTime(model.getMinSelectionIndex());
            }
        }
    }

    public void update(Observable o, Object arg) {
        repaint();
    }

    protected void showPopupMenu(MouseEvent evt) {
        HistoryBrowserModel.VersionTableModel model = getVersionTableModel();
        int row = getSelectedRow();
        if (row == -1) {
            row = rowAtPoint(evt.getPoint());
        }
        HistoryOsmPrimitive primitive = model.getPrimitive(row);
        popupMenu.prepare(primitive);
        popupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }

    class PopupMenuTrigger extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            showPopup(e);
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            showPopup(e);
        }
        private void showPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showPopupMenu(e);
            }
        }
    }

    class ChangesetInfoAction extends AbstractInfoAction {
        private HistoryOsmPrimitive primitive;

        public ChangesetInfoAction() {
            putValue(NAME, tr("Changeset info"));
            putValue(SHORT_DESCRIPTION, tr("Launch browser with information about the changeset"));
            putValue(SMALL_ICON, ImageProvider.get("about"));
        }

        @Override
        protected String createInfoUrl(Object infoObject) {
            HistoryOsmPrimitive primitive = (HistoryOsmPrimitive) infoObject;
            return getBaseBrowseUrl() + "/changeset/" + primitive.getChangesetId();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            String url = createInfoUrl(primitive);
            launchBrowser(url);
        }

        public void prepare(HistoryOsmPrimitive primitive) {
            putValue(NAME, tr("Show changeset {0}", primitive.getChangesetId()));
            this.primitive = primitive;
        }
    }

    class VersionTablePopupMenu extends JPopupMenu {

        private ChangesetInfoAction changesetInfoAction;

        protected void build() {
            changesetInfoAction = new ChangesetInfoAction();
            add(changesetInfoAction);
        }
        public VersionTablePopupMenu() {
            super();
            build();
        }

        public void prepare(HistoryOsmPrimitive primitive) {
            changesetInfoAction.prepare(primitive);
            invalidate();
        }
    }
}
