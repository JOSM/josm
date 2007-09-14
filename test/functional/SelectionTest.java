// License: GPL. Copyright 2007 by Immanuel Scholz and others
import org.openstreetmap.josm.Main;

import framework.FunctionalTestCase;

public class SelectionTest extends FunctionalTestCase {

	public void test() throws Exception {
	    key("ctrl-n", "n");
	    click(100,400);
	    key("shift-n", "shift-n");
	    click(150,400);
	    click(200,400);
	    click(250,500);
	    key("shift-s");
	    drag(250,500,100,400);
	    assertEquals(7, Main.ds.getSelected().size());
    }
}
