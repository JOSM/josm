// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.gui.PleaseWaitDialog;

public class OsmServerHistoryReaderTest {

    @BeforeClass
    public static void init() {
        System.setProperty("josm.home", "C:\\data\\projekte\\osm\\tag-editor-plugin");
        Main.pleaseWaitDlg = new PleaseWaitDialog(null);
        Main.pref.init(false);
    }

    @Test
    public void test1()  throws OsmTransferException {
        OsmServerHistoryReader reader = new OsmServerHistoryReader(OsmPrimitiveType.NODE,266187);
        HistoryDataSet ds = reader.parseHistory();
        History h = ds.getHistory(266187);
        System.out.println("num versions: " + h.getNumVersions());
    }

    @Test
    public void test2()  throws OsmTransferException {
        OsmServerHistoryReader reader = new OsmServerHistoryReader(OsmPrimitiveType.WAY,32916);
        HistoryDataSet ds = reader.parseHistory();
        History h = ds.getHistory(32916);
        System.out.println("num versions: " + h.getNumVersions());
    }

    @Test
    public void test3()  throws OsmTransferException {
        OsmServerHistoryReader reader = new OsmServerHistoryReader(OsmPrimitiveType.RELATION,49);
        HistoryDataSet ds = reader.parseHistory();
        History h = ds.getHistory(49);
        System.out.println("num versions: " + h.getNumVersions());
    }
}
