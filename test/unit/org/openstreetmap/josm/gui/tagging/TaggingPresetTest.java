// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.tagging;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;

import junit.framework.TestCase;

import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Check;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Combo;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Key;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Label;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Text;

public class TaggingPresetTest extends TestCase {

	public void testTaggingPresetLoads() throws Exception {
		InputStream in = getClass().getResourceAsStream("taggingpreset-test.xml");
		List<TaggingPreset> all = TaggingPreset.readAll(in);

		assertEquals(1, all.size());
		TaggingPreset a = all.get(0);
		assertEquals("Highway", a.getValue(Action.NAME));
		Field dataField = a.getClass().getDeclaredField("data");
		dataField.setAccessible(true);
		List<?> data = (List<?>)dataField.get(a);
		assertEquals(5, data.size());

		Label label = (Label)data.get(0);
		assertEquals("Inserting a highway in UK", label.text);

		Text text = (Text)data.get(1);
		assertEquals("name", text.key);
		assertEquals("Highway (e.g. M3)", text.text);
		assertFalse(text.delete_if_empty);
		assertNull(text.default_);

		Combo combo = (Combo)data.get(2);
		assertEquals("highway", combo.key);
		assertEquals("Type", combo.text);
		assertEquals("major,minor", combo.values);
		assertTrue(combo.delete_if_empty);
		assertTrue(combo.editable);
		assertNull(combo.default_);

		Check check = (Check)data.get(3);
		assertEquals("oneway", check.key);
		assertEquals("Oneway", check.text);
		assertTrue(check.default_);

		Key key = (Key)data.get(4);
		assertEquals("class", key.key);
		assertEquals("highway", key.value);
	}
	
	public void testIconLoadsFromClasspath() throws Exception {
		String xml = "<annotations><item icon='logo'></item></annotations>";
		List<TaggingPreset> all = TaggingPreset.readAll(new ByteArrayInputStream(xml.getBytes()));

		assertEquals(1, all.size());

		Icon icon = (Icon)all.get(0).getValue(Action.SMALL_ICON);
		assertNotNull(icon);
		assertEquals("Icon loaded and of correct size", 
				24, Math.max(icon.getIconHeight(), icon.getIconWidth()));
    }
}
