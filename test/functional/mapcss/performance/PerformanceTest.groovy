
// License: GPL. For details, see LICENSE file.
package mapcss.performance;

import static org.junit.Assert.*

import java.awt.Graphics2D
import java.awt.image.BufferedImage

import org.junit.*
import org.openstreetmap.josm.Main
import org.openstreetmap.josm.data.Bounds
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer
import org.openstreetmap.josm.gui.MainApplication
import org.openstreetmap.josm.gui.layer.OsmDataLayer
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource
import org.openstreetmap.josm.gui.preferences.SourceEntry
import org.openstreetmap.josm.io.OsmReader

/**
 * This performance tests measures the time for a full run of MapPaintVisitor.visitAll()
 * against a test data set using a test style.
 * 
 */
class PerformanceTest {

    /* ------------------------ configuration section  ---------------------------- */
    /**
    * The path to the JOSM home environment
    */
   def static JOSM_HOME="/my/josm/home/dir"
   
   /**
    * The path to the style file used for rendering.
    */
   def static STYLE_FILE="/my/test-style.mapcss"

   /**
    * The data file to be rendered
    */
   def static DATA_FILE = "/my/test-data.osm"
    /* ------------------------ / configuration section  ---------------------------- */     
    
    def DataSet ds
    
    def static boolean checkTestEnvironment() {
          File f = new File(JOSM_HOME)
          if  (!f.isDirectory() || !f.exists()) {
              fail("JOSM_HOME refers to '${JOSM_HOME}. This is either not a directory or doesn't exist.\nPlease update configuration settings in the unit test file.")              
          }
          
          f = new File(STYLE_FILE);
          if ( !f.isFile() || ! f.exists()) {
              fail("STYLE_FILE refers to '${STYLE_FILE}. This is either not a file or doesn't exist.\nPlease update configuration settings in the unit test file.")
          }
          
          f = new File(DATA_FILE);
          if ( !f.isFile() || ! f.exists()) {
              fail("DATA_FILE refers to '${DATA_FILE}. This is either not a file or doesn't exist.\nPlease update configuration settings in the unit test file.")
          }
    }
    
    @BeforeClass
    public static void createJOSMFixture(){
        checkTestEnvironment()
        System.setProperty("josm.home", JOSM_HOME)
        MainApplication.main(new String[0])
    }
    
    def timed(Closure c){
        long before = System.currentTimeMillis()
        c()
        long after = System.currentTimeMillis()
        return after - before
    }
    
    def  loadStyle() {
        print "Loading style '$STYLE_FILE' ..."
        MapCSSStyleSource source = new MapCSSStyleSource(
            new SourceEntry(
                new File(STYLE_FILE).toURI().toURL().toString(),
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
        new File(DATA_FILE).withInputStream {
            InputStream is ->
            ds = OsmReader.parseDataSet(is,null)
        }
        Main.main.addLayer(new OsmDataLayer(ds,"test layer",null /* no file */));
        println "DONE"
    }
    
    @Test
    public void measureTimeForStylePreparation() {
        loadStyle()
        loadData()
        
        def mv = Main.map.mapView
        
        BufferedImage img = mv.createImage(mv.getWidth(), mv.getHeight())
        Graphics2D g = img.createGraphics()
        g.setClip(0,0, mv.getWidth(), mv.getHeight())
        def visitor = new StyledMapRenderer()
        visitor.setNavigatableComponent(Main.map.mapView)
        visitor.setGraphics(g)

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
