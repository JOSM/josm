// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.junit.Assert.fail;

import java.awt.BorderLayout;
import java.io.File;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

import org.junit.BeforeClass;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.OsmServerHistoryReader;
import org.openstreetmap.josm.io.OsmTransferException;

public class HistoryBrowserTest extends JFrame {

    static private Logger logger = Logger.getLogger(HistoryBrowserTest.class.getName());

    static Properties testProperties;

    @BeforeClass
    static public void init() {
        testProperties = new Properties();

        // load properties
        //
        try {
            InputStream is = HistoryBrowserTest.class.getResourceAsStream("/test-functional-env.properties");
            try {
                testProperties.load(is);
            } finally {
                is.close();
            }
        } catch(Exception e){
            logger.log(Level.SEVERE, MessageFormat.format("failed to load property file ''{0}''", "test-functional-env.properties"));
            fail(MessageFormat.format("failed to load property file ''{0}''", "test-functional-env.properties"));
        }

        // check josm.home
        //
        String josmHome = testProperties.getProperty("josm.home");
        if (josmHome == null) {
            fail(MessageFormat.format("property ''{0}'' not set in test environment", "josm.home"));
        } else {
            File f = new File(josmHome);
            if (! f.exists() || ! f.canRead()) {
                fail(MessageFormat.format("property ''{0}'' points to ''{1}'' which is either not existing or not readable", "josm.home", josmHome));
            }
        }
        System.setProperty("josm.home", josmHome);
        Main.pref.init(false);

        // init projection
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator
    }

    private HistoryBrowser browser;

    protected void build() {
        setSize(500,500);
        getContentPane().setLayout(new BorderLayout());
        browser = new HistoryBrowser();
        getContentPane().add(browser, BorderLayout.CENTER);
    }

    protected void populate(OsmPrimitiveType type, long id) {
        OsmServerHistoryReader reader = new OsmServerHistoryReader(type, id);
        HistoryDataSet ds = null;
        try {
            ds = reader.parseHistory(NullProgressMonitor.INSTANCE);
        } catch(OsmTransferException e) {
            Main.error(e);
            return;
        }
        History h = ds.getHistory(new SimplePrimitiveId(id, type));
        browser.populate(h);
    }

    /**
     * Constructs a new {@code HistoryBrowserTest}.
     */
    public HistoryBrowserTest(){
        build();
        //populate(OsmPrimitiveType.NODE,354117);
        //populate(OsmPrimitiveType.WAY,37951);
        populate(OsmPrimitiveType.RELATION,5055);

    }

    static public void main(String args[]) {
        HistoryBrowserTest.init();
        new HistoryBrowserTest().setVisible(true);
    }
}
