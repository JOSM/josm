// License: GPL. Copyright 2007 by Immanuel Scholz and others
import java.util.Iterator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;

import framework.FunctionalTestCase;

public class NodeTest extends FunctionalTestCase {

	public void test() throws Exception {
		key("ctrl-n");
		assertNotNull(Main.map);

		key("n");
		click(100,500);
		assertEquals(1, Main.ds.nodes.size());
		assertEquals(1, Main.ds.getSelected().size());
		assertEquals(Main.ds.nodes.iterator().next(), Main.ds.getSelected().iterator().next());
		
		key("d");
		assertEquals(0, Main.ds.getSelected().size());
		assertEquals(0, Main.ds.allNonDeletedPrimitives().size());
		
		key("ctrl-z", "n");
		click(200,500);
		key("g");
		drag(200,500,100,500);
		key("n","shift-n");
		click(150,500);
		assertEquals(3, Main.ds.nodes.size());
		assertEquals(2, Main.ds.segments.size());
		assertEquals(1, Main.ds.getSelected().size());
		
		Node n = (Node)Main.ds.getSelected().iterator().next();
		Iterator<Segment> segIt = Main.ds.segments.iterator();
		Segment s1 = segIt.next();
		Segment s2 = segIt.next();
		if (s1.from == n)
			assertSame(n, s2.to);
		else
			assertSame(n, s2.from);
		
		key("shift-n");
		click(150,550);
		checkSegments(n);
		
		key("ctrl-z", "w");
		click(125,500);
		click(175,500);
		assertEquals(1, Main.ds.ways.size());
		key("s");
		click(150,500);
		key("n");
		click(150,550);
		assertEquals(1, Main.ds.ways.size());
		Way way = Main.ds.ways.iterator().next();
		assertEquals("segment not added to middle of way", 2, way.segments.size());
		checkSegments(n);
		
		key("ctrl-z", "s");
		assertEquals(2, Main.ds.segments.size());
		segIt = Main.ds.segments.iterator();
		s1 = segIt.next();
		s2 = segIt.next();
		click(100,500);
		key("n");
		click(100,550);
		assertEquals(1, Main.ds.ways.size());
		assertSame(way, Main.ds.ways.iterator().next());
		assertEquals(3, way.segments.size());
		segIt = way.segments.iterator();
		assertSame(s1, segIt.next());
		assertSame(s2, segIt.next());
		assertSame(s2.to, segIt.next().from);
		
		key("ctrl-z", "s");
		click(200,500);
		key("n");
		click(200,550);
		assertEquals(1, Main.ds.ways.size());
		assertSame(way, Main.ds.ways.iterator().next());
		segIt = way.segments.iterator();
		assertSame(s1.from, segIt.next().to);
		assertSame(s1, segIt.next());
		assertSame(s2, segIt.next());
    }

	private void checkSegments(Node n) {
		assertEquals(3, Main.ds.segments.size());
		assertEquals(4, Main.ds.nodes.size());

		Iterator<Segment> segIt = Main.ds.segments.iterator();
		Segment s1 = segIt.next();
		Segment s2 = segIt.next();
		Segment s3 = segIt.next();
		if (s1.to == n) {
			assertSame(n, s2.from);
			assertSame(n, s3.from);
		} else if (s2.to == n) {
			assertSame(n, s1.from);
			assertSame(n, s3.from);
		} else {
			assertSame(n, s1.from);
			assertSame(n, s2.from);
		}
    }
}
