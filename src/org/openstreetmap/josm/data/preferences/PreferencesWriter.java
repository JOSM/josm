// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.spi.preferences.Setting;
import org.openstreetmap.josm.spi.preferences.ListListSetting;
import org.openstreetmap.josm.spi.preferences.SettingVisitor;
import org.openstreetmap.josm.spi.preferences.ListSetting;
import org.openstreetmap.josm.spi.preferences.MapListSetting;
import org.openstreetmap.josm.spi.preferences.StringSetting;

/**
 * Write preferences to XML.
 * @since 9823
 */
public class PreferencesWriter extends XmlWriter implements SettingVisitor {
    private final boolean noPassword;
    private final boolean defaults;
    private String key;

    /**
     * Construct new {@code PreferencesWriter}.
     * @param out the {@link PrintWriter}
     * @param noPassword if password must be excluded
     * @param defaults true, if default values are converted to XML, false for regular preferences
     */
    public PreferencesWriter(PrintWriter out, boolean noPassword, boolean defaults) {
        super(out);
        this.noPassword = noPassword;
        this.defaults = defaults;
    }

    /**
     * Write preferences.
     *
     * @param settings preferences settings to write
     */
    public void write(Collection<Map.Entry<String, Setting<?>>> settings) {
        write(settings.stream());
    }

    /**
     * Write preferences.
     *
     * @param settings preferences settings to write as stream.
     */
    public void write(Stream<Map.Entry<String, Setting<?>>> settings) {
        out.write(String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n"));
        String rootElement = defaults ? "preferences-defaults" : "preferences";
        out.write(String.format("<%s xmlns='%s/preferences-1.0'", rootElement, Main.getXMLBase()));
        if (defaults) {
            out.write(" xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'");
        }
        out.write(String.format(" version='%d'>%n", Version.getInstance().getVersion()));
        settings.forEachOrdered(e -> {
            setKey(e.getKey());
            e.getValue().visit(this);
        });
        out.write(String.format("</%s>%n", rootElement));
    }

    private void setKey(String key) {
        this.key = key;
    }

    private void addTime(Setting<?> setting) {
        if (defaults) {
            out.write("' time='" + Optional.ofNullable(setting.getTime()).orElseThrow(IllegalStateException::new));
        }
    }

    private void addDefaults() {
        out.write(String.format("' xsi:nil='true'/>%n"));
    }

    @Override
    public void visit(StringSetting setting) {
        if (noPassword && "osm-server.password".equals(key))
            return; // do not store plain password.
        out.write("  <tag key='" + XmlWriter.encode(key));
        addTime(setting);
        if (setting.getValue() != null) {
            out.write(String.format("' value='%s'/>%n", XmlWriter.encode(setting.getValue())));
        } else if (defaults) {
            addDefaults();
        } else {
            throw new IllegalArgumentException(setting.toString());
        }
    }

    @Override
    public void visit(ListSetting setting) {
        out.write("  <list key='" + XmlWriter.encode(key));
        addTime(setting);
        if (setting.getValue() != null) {
            out.write(String.format("'>%n"));
            for (String s : setting.getValue()) {
                out.write(String.format("    <entry value='%s'/>%n", XmlWriter.encode(s)));
            }
            out.write(String.format("  </list>%n"));
        } else if (defaults) {
            addDefaults();
        } else {
            throw new IllegalArgumentException(setting.toString());
        }
    }

    @Override
    public void visit(ListListSetting setting) {
        out.write("  <lists key='" + XmlWriter.encode(key));
        addTime(setting);
        if (setting.getValue() != null) {
            out.write(String.format("'>%n"));
            for (List<String> list : setting.getValue()) {
                out.write(String.format("    <list>%n"));
                for (String s : list) {
                    out.write(String.format("      <entry value='%s'/>%n", encode(s)));
                }
                out.write(String.format("    </list>%n"));
            }
            out.write(String.format("  </lists>%n"));
        } else if (defaults) {
            addDefaults();
        } else {
            throw new IllegalArgumentException(setting.toString());
        }
    }

    @Override
    public void visit(MapListSetting setting) {
        out.write("  <maps key='" + encode(key));
        addTime(setting);
        if (setting.getValue() != null) {
            out.write(String.format("'>%n"));
            for (Map<String, String> struct : setting.getValue()) {
                out.write(String.format("    <map>%n"));
                for (Map.Entry<String, String> e : struct.entrySet()) {
                    out.write(String.format("      <tag key='%s' value='%s'/>%n", encode(e.getKey()), encode(e.getValue())));
                }
                out.write(String.format("    </map>%n"));
            }
            out.write(String.format("  </maps>%n"));
        } else if (defaults) {
            addDefaults();
        } else {
            throw new IllegalArgumentException(setting.toString());
        }
    }
}
