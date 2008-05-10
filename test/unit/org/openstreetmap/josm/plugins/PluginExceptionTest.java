// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.plugins;

import junit.framework.TestCase;

public class PluginExceptionTest extends TestCase {

	public void testConstructorPassesExceptionParameterAndSetPluginName() {
		RuntimeException barEx = new RuntimeException("bar");
		PluginException e = new PluginException(new PluginProxy(new String(), null), "42", barEx);
		assertEquals(barEx, e.getCause());
		assertEquals("42", e.name);
	}

	public void testMessageContainsThePluginName() {
		PluginException e = new PluginException(new PluginProxy(new String(), null), "42", new RuntimeException());
		assertTrue(e.getMessage().contains("42"));
	}
}
