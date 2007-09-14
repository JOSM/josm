// License: GPL. Copyright 2007 by Immanuel Scholz and others
import org.openstreetmap.josm.Main;

import framework.FunctionalTestCase;


public class DeleteTest extends FunctionalTestCase {

	public void test() throws Exception {
	    key("ctrl-n", "n");
	    click(100,400);
	    key("d");
	    assertEquals(1, Main.ds.allPrimitives().size());
	    assertEquals(0, Main.ds.allNonDeletedPrimitives().size());
	    
	    key("ctrl-z", "ctrl-a", "shift-n", "shift-n");
	    click(150,400);
	    key("s");
	    click(150,400);
	    key("d");
	    assertEquals(3, Main.ds.allNonDeletedPrimitives().size());
	    
	    click(150,400);
	    assertPopup();
	    assertEquals(3, Main.ds.allNonDeletedPrimitives().size());
	    
	    click(125, 400);
	    click(100, 400);
	    click(150, 400);
	    assertEquals(0, Main.ds.allNonDeletedPrimitives().size());
    }
}
