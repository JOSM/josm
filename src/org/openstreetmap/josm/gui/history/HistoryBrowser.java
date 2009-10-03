// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;

/**
 * HistoryBrowser is an UI component which displays history information about an {@see OsmPrimitive}.
 *
 *
 */
public class HistoryBrowser extends JPanel {

    /** the model */
    private HistoryBrowserModel model;
    private JTabbedPane dataPane;

    /**
     * embedds table in a {@see JScrollPane}
     *
     * @param table the table
     * @return the {@see JScrollPane} with the embedded table
     */
    protected JScrollPane embeddInScrollPane(JTable table) {
        JScrollPane pane = new JScrollPane(table);
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        return pane;
    }

    /**
     * creates the table which shows the list of versions
     *
     * @return  the panel with the version table
     */
    protected JPanel createVersionTablePanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());

        VersionTable tbl = new VersionTable(model);
        pnl.add(embeddInScrollPane(tbl), BorderLayout.CENTER);
        return pnl;
    }

    /**
     * creates the panel which shows information about two different versions
     * of the same {@see OsmPrimitive}.
     *
     * @return the panel
     */

    protected JPanel createVersionComparePanel() {
        dataPane = new JTabbedPane();
        dataPane.add(new TagInfoViewer(model));
        dataPane.setTitleAt(0, tr("Tags"));

        dataPane.add(new NodeListViewer(model));
        dataPane.setTitleAt(1, tr("Nodes"));

        dataPane.add(new RelationMemberListViewer(model));
        dataPane.setTitleAt(2, tr("Members"));

        dataPane.add(new CoordinateViewer(model));
        dataPane.setTitleAt(3, tr("Coordinate"));

        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());
        pnl.add(dataPane, BorderLayout.CENTER);
        return pnl;
    }

    /**
     * builds the GUI
     */
    protected void build() {
        JPanel left;
        JPanel right;
        setLayout(new BorderLayout());
        JSplitPane pane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                left = createVersionTablePanel(),
                right = createVersionComparePanel()
        );
        add(pane, BorderLayout.CENTER);

        pane.setOneTouchExpandable(true);
        pane.setDividerLocation(150);

        Dimension minimumSize = new Dimension(100, 50);
        left.setMinimumSize(minimumSize);
        right.setMinimumSize(minimumSize);
    }

    /**
     * constructor
     */
    public HistoryBrowser() {
        model = new HistoryBrowserModel();
        build();
    }

    /**
     * constructor
     * @param history  the history of an {@see OsmPrimitive}
     */
    public HistoryBrowser(History history) {
        this();
        populate(history);
    }

    /**
     * populates the browser with the history of a specific {@see OsmPrimitive}
     *
     * @param history the history
     */
    public void populate(History history) {
        model.setHistory(history);
        OsmPrimitiveType type = history.getType();
        if(type != null)
        {
            if(type == OsmPrimitiveType.NODE)
            {
                dataPane.setEnabledAt(1, false);
                dataPane.setEnabledAt(2, false);
            }
            else if(type == OsmPrimitiveType.WAY)
            {
                dataPane.setEnabledAt(2, false);
                dataPane.setEnabledAt(3, false);
            }
            else
            {
                dataPane.setEnabledAt(3, false);
            }
        }
    }

    /**
     * replies the {@see History} currently displayed by this browser
     *
     * @return the current history
     */
    public History getHistory() {
        return model.getHistory();
    }

    /**
     * replies the model used by this browser
     * @return the model
     */
    public HistoryBrowserModel getModel() {
        return model;
    }
}
