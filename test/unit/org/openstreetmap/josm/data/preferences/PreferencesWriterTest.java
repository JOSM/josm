// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openstreetmap.josm.data.Version;

/**
 * Unit tests for class {@link PreferencesWriter}.
 */
public class PreferencesWriterTest {

    private static <T extends AbstractSetting<?>> T setting(T s, long time) {
        s.setTime(time);
        return s;
    }

    /**
     * Unit test of {@link PreferencesWriter#visit(ListListSetting)}.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testListList() throws IOException {
        try (StringWriter out = new StringWriter(); PreferencesWriter w = new PreferencesWriter(new PrintWriter(out), true, true)) {
            long time = System.currentTimeMillis() / 1000;
            w.visit(setting(new ListListSetting(Arrays.asList(Arrays.asList("bar"))), time));
            assertEquals(String.format(
                    "  <lists key='null' time='%d'>%n    <list>%n      <entry value='bar'/>%n    </list>%n  </lists>%n", time),
                    out.toString());
        }
    }

    /**
     * Unit test of {@link PreferencesWriter#visit(ListSetting)}.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testList() throws IOException {
        try (StringWriter out = new StringWriter(); PreferencesWriter w = new PreferencesWriter(new PrintWriter(out), true, true)) {
            long time = System.currentTimeMillis() / 1000;
            w.visit(setting(new ListSetting(Arrays.asList("bar")), time));
            assertEquals(String.format(
                    "  <list key='null' time='%d'>%n    <entry value='bar'/>%n  </list>%n", time),
                    out.toString());
        }
    }

    /**
     * Unit test of {@link PreferencesWriter#visit(MapListSetting)}.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testMapList() throws IOException {
        try (StringWriter out = new StringWriter(); PreferencesWriter w = new PreferencesWriter(new PrintWriter(out), true, true)) {
            long time = System.currentTimeMillis() / 1000;
            Map<String, String> map = new HashMap<>();
            map.put("foo", "bar");
            w.visit(setting(new MapListSetting(Arrays.asList(map)), time));
            assertEquals(String.format(
                    "  <maps key='null' time='%d'>%n    <map>%n      <tag key='foo' value='bar'/>%n    </map>%n  </maps>%n", time),
                    out.toString());
        }
    }

    /**
     * Unit test of {@link PreferencesWriter#visit(StringSetting)}.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testString() throws IOException {
        try (StringWriter out = new StringWriter(); PreferencesWriter w = new PreferencesWriter(new PrintWriter(out), true, true)) {
            long time = System.currentTimeMillis() / 1000;
            w.visit(setting(new StringSetting("bar"), time));
            assertEquals(String.format(
                    "  <tag key='null' time='%d' value='bar'/>%n", time),
                    out.toString());
        }
    }

    /**
     * Unit test of {@link PreferencesWriter#write(java.util.Collection)}.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testWrite() throws IOException {
        try (StringWriter out = new StringWriter(); PreferencesWriter w = new PreferencesWriter(new PrintWriter(out), true, true)) {
            long time = System.currentTimeMillis() / 1000;
            Map<String, Setting<?>> map = new HashMap<>();
            map.put("foo", setting(new StringSetting("bar"), time));
            w.write(map.entrySet());
            assertEquals(String.format(
                    // CHECKSTYLE.OFF: LineLength
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n<preferences-defaults xmlns='http://josm.openstreetmap.de/preferences-1.0' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' version='%d'>%n  <tag key='foo' time='%d' value='bar'/>%n</preferences-defaults>%n",
                    // CHECKSTYLE.ON: LineLength
                    Version.getInstance().getVersion(), time),
                    out.toString());
        }
    }
}
