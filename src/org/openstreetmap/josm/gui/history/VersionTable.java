// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * VersionTable shows a list of version in a {@see History} of an {@see OsmPrimitive}.
 * 
 *
 */
public class VersionTable extends JTable implements Observer{

    private static Logger logger = Logger.getLogger(VersionTable.class.getName());

    protected void build() {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        addMouseListener(new MouseHandler());
        getSelectionModel().addListSelectionListener(new SelectionHandler());
    }

    public VersionTable(HistoryBrowserModel model) {
        super(model.getVersionTableModel(), new VersionTableColumnModel());
        model.addObserver(this);
        build();
    }

    protected void handleSelectReferencePointInTime(int row) {
        getVesionTableModel().setReferencePointInTime(row);
    }

    protected void handleSelectCurrentPointInTime(int row) {
        getVesionTableModel().setCurrentPointInTime(row);
    }

    protected HistoryBrowserModel.VersionTableModel getVesionTableModel() {
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
}
