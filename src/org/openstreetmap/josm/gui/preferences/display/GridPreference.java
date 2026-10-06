// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;
import java.util.function.Function;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.preferences.DoubleProperty;
import org.openstreetmap.josm.gui.draw.BlendComposite;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.layer.MapGridPaintable;
import org.openstreetmap.josm.gui.layer.MapGridPaintable.GridType;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

/**
 * Settings of the grid drawn over the map, see {@link MapGridPaintable}.
 * @since 19640
 */
public class GridPreference extends DefaultTabPreferenceSetting {

    /**
     * Factory used to create a new {@code GridPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new GridPreference();
        }
    }

    private static final DecimalFormat FORMAT = new DecimalFormat("0.#########", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private final JCheckBox enabled = new JCheckBox(tr("Show grid"));
    private final JosmComboBox<GridType> type = new JosmComboBox<>(GridType.values());
    private final JosmTextField spacingX = new JosmTextField(10);
    private final JosmTextField spacingY = new JosmTextField(10);
    private final JosmTextField rotation = new JosmTextField(10);
    private final JosmTextField originX = new JosmTextField(10);
    private final JosmTextField originY = new JosmTextField(10);
    private final JosmTextField minPixelSpacing = new JosmTextField(10);
    private final JosmTextField lineWidth = new JosmTextField(10);
    private final JosmComboBox<BlendComposite.Mode> blendMode = new JosmComboBox<>(BlendComposite.Mode.values());
    private final JLabel spacingXLabel = new JLabel();
    private final JLabel spacingYLabel = new JLabel();
    private final JLabel originXLabel = new JLabel();
    private final JLabel originYLabel = new JLabel();

    GridPreference() {
        super("preferences/grid", tr("Grid"), tr("Settings of the grid drawn over the map."));
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        JPanel panel = new JPanel(new GridBagLayout());

        enabled.setSelected(Boolean.TRUE.equals(MapGridPaintable.ENABLED.get()));
        enabled.setToolTipText(tr("Draw a grid over the map (View menu: Show grid). "
                + "The grid is a visual aid only, there is no snapping to it."));
        type.setSelectedItem(MapGridPaintable.TYPE.get());
        type.setRenderer(new TranslatedRenderer<>(t -> t == GridType.PROJECTED ? tr("Projected coordinates") : tr("Latitude/longitude")));
        type.setToolTipText("<html>" + tr("Latitude/longitude: lines of constant latitude and longitude, spacing in degrees.<br>"
                + "Projected: lines of constant east/north coordinate of the map projection, "
                + "spacing in metres, optionally rotated.") + "</html>");
        type.addActionListener(e -> updateLabels());
        set(spacingX, MapGridPaintable.SPACING_X);
        set(spacingY, MapGridPaintable.SPACING_Y);
        set(rotation, MapGridPaintable.ROTATION);
        rotation.setToolTipText(tr("Rotation of a projected grid in degrees, counter clockwise. Ignored for a latitude/longitude grid."));
        set(originX, MapGridPaintable.ORIGIN_X);
        set(originY, MapGridPaintable.ORIGIN_Y);
        set(minPixelSpacing, MapGridPaintable.MIN_PIXEL_SPACING);
        set(lineWidth, MapGridPaintable.LINE_WIDTH);
        lineWidth.setToolTipText(tr("Width of the grid lines in pixels. The colors of the east and north lines are set in the Colors tab."));
        minPixelSpacing.setToolTipText(tr("When the grid lines get closer than this on screen, the spacing is multiplied by 10 "
                + "so that the grid stays readable when zooming out."));
        blendMode.setSelectedItem(MapGridPaintable.BLEND_MODE.get());
        blendMode.setRenderer(new TranslatedRenderer<>(GridPreference::blendModeName));
        blendMode.setToolTipText(tr("How the lines are combined with the map: normal transparency, multiply (darkens), "
                + "burn, hard light, difference (visible on any background) or divide. The colors are set in the Colors tab."));
        updateLabels();

        panel.add(enabled, GBC.eol().insets(0, 0, 0, 10));
        panel.add(new JLabel(tr("Grid type")), GBC.std().insets(5, 0, 5, 5));
        panel.add(type, GBC.eol().fill(GBC.HORIZONTAL).insets(0, 0, 0, 5));
        panel.add(spacingXLabel, GBC.std().insets(5, 0, 5, 5));
        panel.add(spacingX, GBC.eol().insets(0, 0, 0, 5));
        panel.add(spacingYLabel, GBC.std().insets(5, 0, 5, 5));
        panel.add(spacingY, GBC.eol().insets(0, 0, 0, 5));
        panel.add(new JLabel(tr("Rotation (degrees)")), GBC.std().insets(5, 0, 5, 5));
        panel.add(rotation, GBC.eol().insets(0, 0, 0, 5));
        panel.add(originXLabel, GBC.std().insets(5, 0, 5, 5));
        panel.add(originX, GBC.eol().insets(0, 0, 0, 5));
        panel.add(originYLabel, GBC.std().insets(5, 0, 5, 5));
        panel.add(originY, GBC.eol().insets(0, 0, 0, 5));
        panel.add(new JLabel(tr("Minimum line distance on screen (pixels)")), GBC.std().insets(5, 0, 5, 5));
        panel.add(minPixelSpacing, GBC.eol().insets(0, 0, 0, 5));
        panel.add(new JLabel(tr("Line width (pixels)")), GBC.std().insets(5, 0, 5, 5));
        panel.add(lineWidth, GBC.eol().insets(0, 0, 0, 5));
        panel.add(new JLabel(tr("Blend mode")), GBC.std().insets(5, 0, 5, 5));
        panel.add(blendMode, GBC.eol().fill(GBC.HORIZONTAL).insets(0, 0, 0, 5));
        panel.add(GBC.glue(0, 0), GBC.eol().fill(GBC.BOTH));

        createPreferenceTabWithScrollPane(gui, panel);
    }

    private static String blendModeName(BlendComposite.Mode mode) {
        switch (mode) {
        case MULTIPLY:
            return tr("Multiply");
        case BURN:
            return tr("Burn");
        case HARD_LIGHT:
            return tr("Hard light");
        case DIFFERENCE:
            return tr("Difference");
        case DIVIDE:
            return tr("Divide");
        case NORMAL:
        default:
            return tr("Normal");
        }
    }

    /** Renders enum values with a translated name */
    private static final class TranslatedRenderer<T> extends DefaultListCellRenderer {
        private final Function<T, String> name;

        TranslatedRenderer(Function<T, String> name) {
            this.name = name;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String text = value == null ? "" : name.apply((T) value);
            return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
        }
    }

    private void updateLabels() {
        boolean latlon = type.getSelectedItem() != GridType.PROJECTED;
        spacingXLabel.setText(latlon ? tr("Longitude spacing (degrees)") : tr("East spacing (metres)"));
        spacingYLabel.setText(latlon ? tr("Latitude spacing (degrees)") : tr("North spacing (metres)"));
        String spacingTip = latlon
                ? tr("Distance between the grid lines in degrees.")
                : tr("Distance between the grid lines as a true distance in metres (as measured by the parallel way tool "
                        + "and the status bar), measured at the grid origin. Place the origin in the area of interest.");
        spacingX.setToolTipText(spacingTip);
        spacingY.setToolTipText(spacingTip);
        originXLabel.setText(latlon ? tr("Origin longitude") : tr("Origin east"));
        originYLabel.setText(latlon ? tr("Origin latitude") : tr("Origin north"));
        rotation.setEnabled(!latlon);
    }

    private static void set(JosmTextField field, DoubleProperty property) {
        field.setText(FORMAT.format(property.get()));
    }

    private static void save(JosmTextField field, DoubleProperty property, boolean positive) {
        try {
            double value = FORMAT.parse(field.getText().trim()).doubleValue();
            if (positive && !(value > 0)) {
                Logging.warn("Ignoring invalid grid setting {0}: {1}", property.getKey(), field.getText());
                return;
            }
            property.put(value);
        } catch (ParseException e) {
            Logging.warn("Ignoring invalid grid setting {0}: {1}", property.getKey(), field.getText());
            Logging.trace(e);
        }
    }

    @Override
    public boolean ok() {
        MapGridPaintable.ENABLED.put(enabled.isSelected());
        MapGridPaintable.TYPE.put((GridType) type.getSelectedItem());
        save(spacingX, MapGridPaintable.SPACING_X, true);
        save(spacingY, MapGridPaintable.SPACING_Y, true);
        save(rotation, MapGridPaintable.ROTATION, false);
        save(originX, MapGridPaintable.ORIGIN_X, false);
        save(originY, MapGridPaintable.ORIGIN_Y, false);
        save(minPixelSpacing, MapGridPaintable.MIN_PIXEL_SPACING, true);
        save(lineWidth, MapGridPaintable.LINE_WIDTH, true);
        MapGridPaintable.BLEND_MODE.put((BlendComposite.Mode) blendMode.getSelectedItem());
        return false;
    }

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public String getHelpContext() {
        return HelpUtil.ht("/Preferences/Grid");
    }
}
