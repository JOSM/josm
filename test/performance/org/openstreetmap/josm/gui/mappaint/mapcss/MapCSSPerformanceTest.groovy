// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.Assert.*

import java.awt.Graphics2D
import java.awt.image.BufferedImage

import org.junit.*
import org.openstreetmap.josm.JOSMFixture
import org.openstreetmap.josm.data.Bounds
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer
import org.openstreetmap.josm.gui.NavigatableComponent
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles
import org.openstreetmap.josm.gui.preferences.SourceEntry
import org.openstreetmap.josm.io.Compression
import org.openstreetmap.josm.io.OsmReader

/**
 * This performance test measures the time for a full run of MapPaintVisitor.visitAll()
 * against a test data set using a test style.
 *
 */
class MapCSSPerformanceTest {

    /* ------------------------ configuration section  ---------------------------- */
    /**
     * The path to the style file used for rendering.
     */
    def static STYLE_FILE="styles/standard/elemstyles.mapcss"

    /**
     * The data file to be rendered
     */
    def static DATA_FILE = "data_nodist/neubrandenburg.osm.bz2"
    /* ------------------------ / configuration section  ---------------------------- */

    def DataSet ds

    def static boolean checkTestEnvironment() {
          File f = new File(STYLE_FILE);
          if ( !f.isFile() || ! f.exists()) {
              fail("STYLE_FILE refers to '${STYLE_FILE}. This is either not a file or doesn't exist.\nPlease update configuration settings in the unit test file.")
          }
    }

    @BeforeClass
    public static void createJOSMFixture() {
        JOSMFixture.createPerformanceTestFixture().init(true);
    }

    def timed(Closure c){
        long before = System.currentTimeMillis()
        c()
        long after = System.currentTimeMillis()
        return after - before
    }

    def loadStyle() {
        print "Loading style '$STYLE_FILE' ..."
        MapCSSStyleSource source = new MapCSSStyleSource(
            new SourceEntry(
                STYLE_FILE,
                "test style",
                "a test style",
                true // active
            )
        )
        source.loadStyleSource()
        if (!source.errors.isEmpty()) {
            fail("Failed to load style file ''${STYLE_FILE}''. Errors: ${source.errors}")
        }
        MapPaintStyles.getStyles().clear()
        MapPaintStyles.getStyles().add(source)
        println "DONE"
    }

    def loadData() {
        print "Loading data file '$DATA_FILE' ..."
        ds = OsmReader.parseDataSet(Compression.getUncompressedFileInputStream(new File(DATA_FILE)), null);
        println "DONE"
    }

    @Test
    public void measureTimeForStylePreparation() {
        loadStyle()
        loadData()

        NavigatableComponent mv = new NavigatableComponent();
        mv.setBounds(0, 0, 1024, 768)
        BufferedImage img = new BufferedImage(mv.getWidth(), mv.getHeight(), BufferedImage.TYPE_3BYTE_BGR)
        Graphics2D g = img.createGraphics()
        g.setClip(0,0, mv.getWidth(), mv.getHeight())
        StyledMapRenderer visitor = new StyledMapRenderer(g, mv, false)

        print "Rendering ..."
        long time = timed {
            visitor.render(ds, false, new Bounds(-90,-180,90,180))
        }
        println "DONE"
        println "data file : ${DATA_FILE}"
        println "style file: ${STYLE_FILE}"
        println ""
        println "Rendering took $time ms."
    }
}
