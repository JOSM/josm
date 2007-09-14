// License: GPL. Copyright 2007 by Immanuel Scholz and others
import org.openstreetmap.josm.Main;

import framework.FunctionalTestCase;


public class WayTest extends FunctionalTestCase {

	public void test() throws Exception {
		key("ctrl-n", "n");
		click(100,400);
		click(150,400);
		click(200,400);
		key("g");
		drag(200,400,150,400);
		drag(150,400,100,400);
		key("s", "ctrl-a", "w");
		assertPopup();
		assertEquals(1, Main.ds.ways.size());

		key("ctrl-a", "s");
		click(125,400,"ctrl");
		key("w");
		assertPopup();
		assertEquals(2, Main.ds.ways.size());

		key("ctrl-z", "ctrl-z", "ctrl-shift-a");
		click(125,400);
		assertEquals(1, Main.ds.ways.size());
		click(175,400);
		assertEquals(1, Main.ds.ways.size());
	}
}
