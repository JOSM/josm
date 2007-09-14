// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.io.StringReader;
import java.util.NoSuchElementException;

import org.openstreetmap.josm.tools.XmlObjectParser.Uniform;

import junit.framework.TestCase;

public class XmlObjectParserTest extends TestCase {

	private XmlObjectParser parser;

	public static class Foo {
		public String bar;
	}
	public static class Bar {
		private String ada;
		public void setAda(String value) {
			ada = value;
		}
	}

	@Override protected void setUp() throws Exception {
		super.setUp();
		parser = new XmlObjectParser();
	}

	private XmlObjectParser createParser(String string) {
		XmlObjectParser parser = new XmlObjectParser();
		parser.map("foo", Foo.class);
		parser.start(new StringReader(string));
		return parser;
	}

	public void testSimpleStructWithAttributes() throws Exception {
		parser = createParser("<xml><foo bar='foobar'/><foo bar='baz'/></xml>");

		assertEquals("foobar", ((Foo)parser.next()).bar);
		assertEquals("baz", ((Foo)parser.next()).bar);
		assertFalse(parser.hasNext());
		try {
			parser.next();
			fail();
		} catch (NoSuchElementException e) {
		}
	}

	public void testSubtagsWithCharacters() throws Exception {
		parser = createParser("<foo><bar>asd</bar></foo>");
		assertEquals("asd", ((Foo)parser.next()).bar);
	}

	public void testIterable() throws Exception {
		parser = createParser("<xml><foo bar='yo'/><foo bar='yo'/><foo bar='yo'/></xml>");
		for (Object o : parser)
			assertEquals("yo", ((Foo)o).bar);
	}

	public void testUniformIterable() throws Exception {
		XmlObjectParser.Uniform<Foo> p = new Uniform<Foo>(new StringReader("<xml><foo bar='sdf'/><foo bar='sdf'/></xml>"), "foo", Foo.class);
		for (Foo foo : p)
			assertEquals("sdf", foo.bar);
	}


	public void testObjectIntersection() throws Exception {
		parser.map("foo", Foo.class);
		parser.map("imi", Bar.class);
		parser.start(new StringReader("<xml><foo bar='yo'><imi ada='123'/></foo></xml>"));

		Object imi = parser.next();
		Object foo = parser.next();
		assertTrue(imi instanceof Bar);
		assertTrue(foo instanceof Foo);
		assertEquals("yo", ((Foo)foo).bar);
		assertEquals("123", ((Bar)imi).ada);
	}

	public void testObjectIntersectionWithMapOnStart() throws Exception {
		parser.mapOnStart("foo", Foo.class);
		parser.map("imi", Bar.class);
		parser.start(new StringReader("<xml><foo><imi/></foo></xml>"));

		Object foo = parser.next();
		Object imi = parser.next();
		assertTrue(imi instanceof Bar);
		assertTrue(foo instanceof Foo);
	}

	public void testMapReportsObjectsAndDoNotFillAttributes() throws Exception {
		parser.map("foo", Foo.class);
		parser.map("bar", Bar.class);
		parser.start(new StringReader("<xml><foo><bar/></foo></xml>"));

		assertTrue(parser.next() instanceof Bar);
		Object foo = parser.next();
		assertTrue(foo instanceof Foo);
		assertNull(((Foo)foo).bar);
	}
}
