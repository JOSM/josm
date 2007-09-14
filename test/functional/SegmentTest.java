// License: GPL. Copyright 2007 by Immanuel Scholz and others
import org.openstreetmap.josm.Main;

import framework.FunctionalTestCase;


public class SegmentTest extends FunctionalTestCase {

	public void test() throws Exception {
	    key("ctrl-n", "n");
		click(100,500);
		click(200,500);
		key("g");
		drag(200,500,100,500);
		assertEquals(1, Main.ds.segments.size());
		key("s");
		click(150,500);
		assertEquals(1, Main.ds.getSelected().size());
		assertEquals(Main.ds.segments.iterator().next(), Main.ds.getSelected().iterator().next());
		key("d");
		assertEquals(0, Main.ds.getSelected().size());
		assertTrue(Main.ds.segments.iterator().next().deleted);
    }
}
