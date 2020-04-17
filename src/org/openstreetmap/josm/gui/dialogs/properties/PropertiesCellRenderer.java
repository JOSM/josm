// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.CachingProperty;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.gui.mappaint.mapcss.CSSColors;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Pair;

/**
 * Cell renderer of tags table.
 * @since 6314
 */
public class PropertiesCellRenderer extends DefaultTableCellRenderer {

    private static final CachingProperty<Color> SELECTED_FG
            = new NamedColorProperty(marktr("Discardable key: selection Foreground"), Color.GRAY).cached();
    private static final CachingProperty<Color> SELECTED_BG;
    private static final CachingProperty<Color> NORMAL_FG
            = new NamedColorProperty(marktr("Discardable key: foreground"), Color.GRAY).cached();
    private static final CachingProperty<Color> NORMAL_BG;
    private static final CachingProperty<Boolean> DISCARDABLE
            = new BooleanProperty("display.discardable-keys", false).cached();

    static {
        SELECTED_BG = new NamedColorProperty(marktr("Discardable key: selection Background"),
                Optional.ofNullable(UIManager.getColor("Table.selectionBackground")).orElse(Color.BLUE)).cached();
        NORMAL_BG = new NamedColorProperty(marktr("Discardable key: background"),
                Optional.ofNullable(UIManager.getColor("Table.background")).orElse(Color.WHITE)).cached();
    }

    private final Collection<TableCellRenderer> customRenderer = new CopyOnWriteArrayList<>();

    private static void setColors(Component c, String key, boolean isSelected) {

        if (AbstractPrimitive.getDiscardableKeys().contains(key)) {
            c.setForeground((isSelected ? SELECTED_FG : NORMAL_FG).get());
            c.setBackground((isSelected ? SELECTED_BG : NORMAL_BG).get());
        } else {
            c.setForeground(UIManager.getColor("Table."+(isSelected ? "selectionF" : "f")+"oreground"));
            c.setBackground(UIManager.getColor("Table."+(isSelected ? "selectionB" : "b")+"ackground"));
        }
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        for (TableCellRenderer renderer : customRenderer) {
            final Component component = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (component != null) {
                return component;
            }
        }
        if (value == null)
            return this;
        Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
        if (c instanceof JLabel) {
            String str = null;
            if (value instanceof String) {
                str = (String) value;
            } else if (value instanceof Map<?, ?>) {
                Map<?, ?> v = (Map<?, ?>) value;
                if (v.size() != 1) {    // Multiple values: give user a short summary of the values
                    Integer blankCount;
                    Integer otherCount;
                    if (v.get("") == null) {
                        blankCount = 0;
                        otherCount = v.size();
                    } else {
                        blankCount = (Integer) v.get("");
                        otherCount = v.size()-1;
                    }
                    StringBuilder sb = new StringBuilder("<");
                    if (otherCount == 1) {
                        // Find the non-blank value in the map
                        v.entrySet().stream().filter(entry -> !Objects.equals(entry.getKey(), ""))
                            /* I18n: properties display partial string joined with comma, first is count, second is value */
                            .findAny().ifPresent(entry -> sb.append(tr("{0} ''{1}''", entry.getValue().toString(), entry.getKey())));
                    } else {
                        /* I18n: properties display partial string joined with comma */
                        sb.append(trn("{0} different", "{0} different", otherCount, otherCount));
                    }
                    if (blankCount > 0) {
                        /* I18n: properties display partial string joined with comma */
                        sb.append(trn(", {0} unset", ", {0} unset", blankCount, blankCount));
                    }
                    sb.append('>');
                    str = sb.toString();
                    c.setFont(c.getFont().deriveFont(Font.ITALIC));

                } else { // One value: display the value
                    str = (String) v.entrySet().iterator().next().getKey();
                }
            }
            boolean enableHTML = false;
            if (column == 0 && str != null) {
                Pair<String, Boolean> label = I18n.getLocalizedLanguageName(str);
                if (label != null && label.b) {
                    enableHTML = true;
                    str = "<html><body>" + str + " <i>&lt;" + label.a + "&gt;</i></body></html>";
                }
            } else if (column == 1 && str != null && String.valueOf(getKeyInRow(table, row)).contains("colour")) {
                enableHTML = true;
                // U+25A0 BLACK SQUARE
                final String color = str.matches("#[0-9A-Fa-f]{3,8}")
                        ? str
                        : ColorHelper.color2html(CSSColors.get(str));
                if (color != null) {
                    str = "<html><body><span color='" + color + "'>\u25A0</span> " + str + "</body></html>";
                }
            }
            ((JLabel) c).putClientProperty("html.disable", enableHTML ? null : Boolean.TRUE); // Fix #8730
            ((JLabel) c).setText(str);
            if (DISCARDABLE.get()) {
                String key = null;
                if (column == 0) {
                    key = str;
                } else if (column == 1) {
                    Object value0 = getKeyInRow(table, row);
                    if (value0 instanceof String) {
                        key = (String) value0;
                    }
                }
                setColors(c, key, isSelected);
            }
        }
        return c;
    }

    private Object getKeyInRow(JTable table, int row) {
        return table.getModel().getValueAt(table.convertRowIndexToModel(row), 0);
    }

    /**
     * Adds a custom table cell renderer to render cells of the tags table.
     *
     * If the renderer is not capable performing a {@link TableCellRenderer#getTableCellRendererComponent},
     * it should return {@code null} to fall back to the
     * {@link PropertiesCellRenderer#getTableCellRendererComponent default implementation}.
     * @param renderer the renderer to add
     * @since 9149
     */
    public void addCustomRenderer(TableCellRenderer renderer) {
        customRenderer.add(renderer);
    }

    /**
     * Removes a custom table cell renderer.
     * @param renderer the renderer to remove
     * @since 9149
     */
    public void removeCustomRenderer(TableCellRenderer renderer) {
        customRenderer.remove(renderer);
    }
}
