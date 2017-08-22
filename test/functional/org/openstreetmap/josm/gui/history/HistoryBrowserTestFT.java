// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import org.junit.BeforeClass;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.OsmServerHistoryReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Logging;

public class HistoryBrowserTestFT extends JFrame {

    @BeforeClass
    public static void init() {
        JOSMFixture.createFunctionalTestFixture().init();
    }

    private HistoryBrowser browser;

    protected void build() {
        setSize(500, 500);
        getContentPane().setLayout(new BorderLayout());
        browser = new HistoryBrowser();
        getContentPane().add(browser, BorderLayout.CENTER);
    }

    protected void populate(OsmPrimitiveType type, long id) {
        OsmServerHistoryReader reader = new OsmServerHistoryReader(type, id);
        HistoryDataSet ds = null;
        try {
            ds = reader.parseHistory(NullProgressMonitor.INSTANCE);
        } catch (OsmTransferException e) {
            Logging.error(e);
            return;
        }
        History h = ds.getHistory(new SimplePrimitiveId(id, type));
        browser.populate(h);
    }

    /**
     * Constructs a new {@code HistoryBrowserTest}.
     */
    public HistoryBrowserTestFT() {
        build();
        //populate(OsmPrimitiveType.NODE,354117);
        //populate(OsmPrimitiveType.WAY,37951);
        populate(OsmPrimitiveType.RELATION, 5055);

    }

    public static void main(String[] args) {
        HistoryBrowserTestFT.init();
        new HistoryBrowserTestFT().setVisible(true);
    }
}
