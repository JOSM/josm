// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Arrays;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.tools.Destroyable;

/**
 * HistoryBrowser is an UI component which displays history information about an {@link OsmPrimitive}.
 *
 * @since 1709
 */
public class HistoryBrowser extends JPanel implements Destroyable {

    /** the model */
    private transient HistoryBrowserModel model;
    private TagInfoViewer tagInfoViewer;
    private NodeListViewer nodeListViewer;
    private RelationMemberListViewer relationMemberListViewer;
    private CoordinateInfoViewer coordinateInfoViewer;
    private JTabbedPane tpViewers;

    /**
     * Constructs a new {@code HistoryBrowser}.
     */
    public HistoryBrowser() {
        model = new HistoryBrowserModel();
        build();
    }

    /**
     * Constructs a new {@code HistoryBrowser}.
     * @param history the history of an {@link OsmPrimitive}
     */
    public HistoryBrowser(History history) {
        this();
        populate(history);
    }

    /**
     * creates the table which shows the list of versions
     *
     * @return  the panel with the version table
     */
    protected JPanel createVersionTablePanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(new JScrollPane(new VersionTable(model)), BorderLayout.CENTER);
        return pnl;
    }

    /**
     * creates the panel which shows information about two different versions
     * of the same {@link OsmPrimitive}.
     *
     * @return the panel
     */
    protected JPanel createVersionComparePanel() {
        tpViewers = new JTabbedPane();

        // create the viewers, but don't add them yet.
        // see populate()
        //
        tagInfoViewer = new TagInfoViewer(model);
        nodeListViewer = new NodeListViewer(model);
        relationMemberListViewer = new RelationMemberListViewer(model);
        coordinateInfoViewer = new CoordinateInfoViewer(model);
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(tpViewers, BorderLayout.CENTER);

        tpViewers.addChangeListener(e -> {
            if (tpViewers.getSelectedComponent() == coordinateInfoViewer) {
                // while building the component size is not yet known, thus panning does not give reasonable results
                coordinateInfoViewer.setDisplayToFitMapMarkers();
            }
        });

        return pnl;
    }

    /**
     * builds the GUI
     */
    protected void build() {
        JPanel left = createVersionTablePanel();
        JPanel right = createVersionComparePanel();
        setLayout(new BorderLayout());
        JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        add(pane, BorderLayout.CENTER);

        pane.setOneTouchExpandable(true);
        pane.setDividerLocation(300);

        Dimension minimumSize = new Dimension(100, 50);
        left.setMinimumSize(minimumSize);
        right.setMinimumSize(minimumSize);
    }

    /**
     * populates the browser with the history of a specific {@link OsmPrimitive}
     *
     * @param history the history
     */
    public void populate(History history) {
        model.setHistory(history);

        tpViewers.removeAll();

        tpViewers.add(tagInfoViewer);
        tpViewers.setTitleAt(0, tr("Tags"));

        if (history.getEarliest().getType() == OsmPrimitiveType.NODE) {
            tpViewers.add(coordinateInfoViewer);
            tpViewers.setTitleAt(1, tr("Coordinates"));
        } else if (history.getEarliest().getType() == OsmPrimitiveType.WAY) {
            tpViewers.add(nodeListViewer);
            tpViewers.setTitleAt(1, tr("Nodes"));
        } else if (history.getEarliest().getType() == OsmPrimitiveType.RELATION) {
            tpViewers.add(relationMemberListViewer);
            tpViewers.setTitleAt(1, tr("Members"));
        }
        revalidate();
    }

    /**
     * replies the {@link History} currently displayed by this browser
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

    @Override
    public void destroy() {
        if (model != null) {
            model.unlinkAsListener();
            model = null;
        }
        Arrays.asList(tagInfoViewer, nodeListViewer, relationMemberListViewer, coordinateInfoViewer).stream()
                .filter(Destroyable.class::isInstance).forEach(Destroyable::destroy);
        tagInfoViewer = null;
        nodeListViewer = null;
        relationMemberListViewer = null;
        coordinateInfoViewer = null;
    }
}
