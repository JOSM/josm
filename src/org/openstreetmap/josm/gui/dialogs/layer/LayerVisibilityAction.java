// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.border.Border;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainFrame;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.IEnabledStateUpdating;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog.LayerListModel;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.Layer.LayerAction;
import org.openstreetmap.josm.gui.layer.imagery.ImageryFilterSettings;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

/**
 * This is a menu that includes all settings for the layer visibility. It combines gamma/opacity sliders and the visible-checkbox.
 *
 * @author Michael Zangl
 */
public final class LayerVisibilityAction extends AbstractAction implements IEnabledStateUpdating, LayerAction {
    private static final String DIALOGS_LAYERLIST = "dialogs/layerlist";
    private static final int SLIDER_STEPS = 100;
    /**
     * Steps the value is changed by a mouse wheel change (one full click)
     */
    private static final int SLIDER_WHEEL_INCREMENT = 5;
    private static final double MAX_SHARPNESS_FACTOR = 2;
    private static final double MAX_COLORFUL_FACTOR = 2;
    private final LayerListModel model;
    private final JPopupMenu popup;
    private SideButton sideButton;
    /**
     * The real content, just to add a border
     */
    private final JPanel content = new JPanel();
    final OpacitySlider opacitySlider = new OpacitySlider();
    private final ArrayList<LayerVisibilityMenuEntry> sliders = new ArrayList<>();

    /**
     * Creates a new {@link LayerVisibilityAction}
     * @param model The list to get the selection from.
     */
    public LayerVisibilityAction(LayerListModel model) {
        this.model = model;
        popup = new JPopupMenu();
        // prevent popup close on mouse wheel move
        popup.addMouseWheelListener(MouseWheelEvent::consume);

        popup.add(content);
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.setLayout(new GridBagLayout());

        new ImageProvider(DIALOGS_LAYERLIST, "visibility").getResource().attachImageIcon(this, true);
        putValue(SHORT_DESCRIPTION, tr("Change visibility of the selected layer."));

        addContentEntry(new VisibilityCheckbox());

        addContentEntry(opacitySlider);
        addContentEntry(new ColorfulnessSlider());
        addContentEntry(new GammaFilterSlider());
        addContentEntry(new SharpnessSlider());
        addContentEntry(new ColorSelector(model::getSelectedLayers));
    }

    private void addContentEntry(LayerVisibilityMenuEntry slider) {
        content.add(slider.getPanel(), GBC.eop().fill(GBC.HORIZONTAL));
        sliders.add(slider);
    }

    void setVisibleFlag(boolean visible) {
        for (Layer l : model.getSelectedLayers()) {
            l.setVisible(visible);
        }
        updateValues();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateValues();
        if (e.getSource() == sideButton) {
            if (sideButton.isShowing()) {
                popup.show(sideButton, 0, sideButton.getHeight());
            }
        } else {
            // Action can be trigger either by opacity button or by popup menu (in case toggle buttons are hidden).
            // In that case, show it in the middle of screen (because opacityButton is not visible)
            MainFrame mainFrame = MainApplication.getMainFrame();
            if (mainFrame.isShowing()) {
                popup.show(mainFrame, mainFrame.getWidth() / 2, (mainFrame.getHeight() - popup.getHeight()) / 2);
            }
        }
    }

    void updateValues() {
        List<Layer> layers = model.getSelectedLayers();

        boolean allVisible = true;
        boolean allHidden = true;
        for (Layer l : layers) {
            allVisible &= l.isVisible();
            allHidden &= !l.isVisible();
        }

        for (LayerVisibilityMenuEntry slider : sliders) {
            slider.updateLayers(layers, allVisible, allHidden);
        }
    }

    @Override
    public boolean supportLayers(List<Layer> layers) {
        return !layers.isEmpty();
    }

    @Override
    public Component createMenuComponent() {
        return new JMenuItem(this);
    }

    @Override
    public void updateEnabledState() {
        setEnabled(!model.getSelectedLayers().isEmpty());
    }

    /**
     * Sets the corresponding side button.
     * @param sideButton the corresponding side button
     */
    public void setCorrespondingSideButton(SideButton sideButton) {
        this.sideButton = sideButton;
    }

    /**
     * An entry in the visibility settings dropdown.
     * @author Michael Zangl
     */
    private interface LayerVisibilityMenuEntry {

        /**
         * Update the displayed value depending on the current layers
         * @param layers The layers
         * @param allVisible <code>true</code> if all layers are visible
         * @param allHidden <code>true</code> if all layers are hidden
         */
        void updateLayers(List<Layer> layers, boolean allVisible, boolean allHidden);

        /**
         * Get the panel that should be added to the menu
         * @return The panel
         */
        JComponent getPanel();
    }

    private class VisibilityCheckbox extends JCheckBox implements LayerVisibilityMenuEntry {

        VisibilityCheckbox() {
            super(tr("Show layer"));

            // Align all texts
            Icon icon = UIManager.getIcon("CheckBox.icon");
            int iconWidth = icon == null ? 20 : icon.getIconWidth();
            setBorder(BorderFactory.createEmptyBorder(0, Math.max(24 + 5 - iconWidth, 0), 0, 0));
            addChangeListener(e -> setVisibleFlag(isSelected()));
        }

        @Override
        public void updateLayers(List<Layer> layers, boolean allVisible, boolean allHidden) {
            setEnabled(!layers.isEmpty());
            // TODO: Indicate tristate.
            setSelected(allVisible && !allHidden);
        }

        @Override
        public JComponent getPanel() {
            return this;
        }
    }

    /**
     * This is a slider for a filter value.
     * @author Michael Zangl
     *
     * @param <T> The layer type.
     */
    private abstract class AbstractFilterSlider<T extends Layer> extends JPanel implements LayerVisibilityMenuEntry {
        private final double minValue;
        private final double maxValue;
        private final Class<T> layerClassFilter;

        protected final JSlider slider = new JSlider(JSlider.HORIZONTAL);

        /**
         * Create a new filter slider.
         * @param minValue The minimum value to map to the left side.
         * @param maxValue The maximum value to map to the right side.
         * @param layerClassFilter The type of layer influenced by this filter.
         */
        AbstractFilterSlider(double minValue, double maxValue, Class<T> layerClassFilter) {
            super(new GridBagLayout());
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.layerClassFilter = layerClassFilter;

            add(new JLabel(getIcon()), GBC.std().span(1, 2).insets(0, 0, 5, 0));
            add(new JLabel(getLabel()), GBC.eol().insets(5, 0, 5, 0));
            add(slider, GBC.eol());
            addMouseWheelListener(this::mouseWheelMoved);

            slider.setMaximum(SLIDER_STEPS);
            int tick = convertFromRealValue(1);
            slider.setMinorTickSpacing(tick);
            slider.setMajorTickSpacing(tick);
            slider.setPaintTicks(true);

            slider.addChangeListener(e -> onStateChanged());

            //final NumberFormat format = DecimalFormat.getInstance();
            //setLabels(format.format(minValue), format.format((minValue + maxValue) / 2), format.format(maxValue));
        }

        protected void setLabels(String labelMinimum, String labelMiddle, String labelMaximum) {
            final Dictionary<Integer, JLabel> labels = new Hashtable<>();
            labels.put(slider.getMinimum(), new JLabel(labelMinimum));
            labels.put((slider.getMaximum() + slider.getMinimum()) / 2, new JLabel(labelMiddle));
            labels.put(slider.getMaximum(), new JLabel(labelMaximum));
            slider.setLabelTable(labels);
            slider.setPaintLabels(true);
        }

        /**
         * Called whenever the state of the slider was changed.
         * @see JSlider#getValueIsAdjusting()
         * @see #getRealValue()
         */
        protected void onStateChanged() {
            Collection<T> layers = filterLayers(model.getSelectedLayers());
            for (T layer : layers) {
                applyValueToLayer(layer);
            }
        }

        protected void mouseWheelMoved(MouseWheelEvent e) {
            e.consume();
            if (!isEnabled()) {
                // ignore mouse wheel in disabled state.
                return;
            }
            double rotation = -1 * e.getPreciseWheelRotation();
            double destinationValue = slider.getValue() + rotation * SLIDER_WHEEL_INCREMENT;
            if (rotation < 0) {
                destinationValue = Math.floor(destinationValue);
            } else {
                destinationValue = Math.ceil(destinationValue);
            }
            slider.setValue(Utils.clamp((int) destinationValue, slider.getMinimum(), slider.getMaximum()));
        }

        abstract void applyValueToLayer(T layer);

        protected double getRealValue() {
            return convertToRealValue(slider.getValue());
        }

        protected double convertToRealValue(int value) {
            double s = (double) value / SLIDER_STEPS;
            return s * maxValue + (1-s) * minValue;
        }

        protected void setRealValue(double value) {
            slider.setValue(convertFromRealValue(value));
        }

        protected int convertFromRealValue(double value) {
            int i = (int) ((value - minValue) / (maxValue - minValue) * SLIDER_STEPS + .5);
            return Utils.clamp(i, slider.getMinimum(), slider.getMaximum());
        }

        public abstract ImageIcon getIcon();

        public abstract String getLabel();

        @Override
        public void updateLayers(List<Layer> layers, boolean allVisible, boolean allHidden) {
            Collection<? extends Layer> usedLayers = filterLayers(layers);
            setVisible(!usedLayers.isEmpty());
            if (usedLayers.stream().noneMatch(Layer::isVisible)) {
                slider.setEnabled(false);
            } else {
                slider.setEnabled(true);
                updateSliderWhileEnabled(usedLayers, allHidden);
            }
        }

        protected Collection<T> filterLayers(List<Layer> layers) {
            return Utils.filteredCollection(layers, layerClassFilter);
        }

        protected abstract void updateSliderWhileEnabled(Collection<? extends Layer> usedLayers, boolean allHidden);

        @Override
        public JComponent getPanel() {
            return this;
        }
    }

    /**
     * This slider allows you to change the opacity of a layer.
     *
     * @author Michael Zangl
     * @see Layer#setOpacity(double)
     */
    class OpacitySlider extends AbstractFilterSlider<Layer> {
        /**
         * Create a new {@link OpacitySlider}.
         */
        OpacitySlider() {
            super(0, 1, Layer.class);
            setLabels("0%", "50%", "100%");
            slider.setToolTipText(tr("Adjust opacity of the layer."));
        }

        @Override
        protected void onStateChanged() {
            if (getRealValue() <= 0.001 && !slider.getValueIsAdjusting()) {
                setVisibleFlag(false);
            } else {
                super.onStateChanged();
            }
        }

        @Override
        protected void mouseWheelMoved(MouseWheelEvent e) {
            if (!isEnabled() && !filterLayers(model.getSelectedLayers()).isEmpty() && e.getPreciseWheelRotation() < 0) {
                // make layer visible and set the value.
                // this allows users to use the mouse wheel to make the layer visible if it was hidden previously.
                e.consume();
                setVisibleFlag(true);
            } else {
                super.mouseWheelMoved(e);
            }
        }

        @Override
        protected void applyValueToLayer(Layer layer) {
            layer.setOpacity(getRealValue());
        }

        @Override
        protected void updateSliderWhileEnabled(Collection<? extends Layer> usedLayers, boolean allHidden) {
            double opacity = 0;
            for (Layer l : usedLayers) {
                opacity += l.getOpacity();
            }
            opacity /= usedLayers.size();
            if (opacity == 0) {
                opacity = 1;
                setVisibleFlag(true);
            }
            setRealValue(opacity);
        }

        @Override
        public String getLabel() {
            return tr("Opacity");
        }

        @Override
        public ImageIcon getIcon() {
            return ImageProvider.get(DIALOGS_LAYERLIST, "transparency");
        }

        @Override
        public String toString() {
            return "OpacitySlider [getRealValue()=" + getRealValue() + ']';
        }
    }

    /**
     * This slider allows you to change the gamma value of a layer.
     *
     * @author Michael Zangl
     * @see ImageryFilterSettings#setGamma(double)
     */
    private class GammaFilterSlider extends AbstractFilterSlider<ImageryLayer> {

        /**
         * Create a new {@link GammaFilterSlider}
         */
        GammaFilterSlider() {
            super(-1, 1, ImageryLayer.class);
            setLabels("0", "1", "∞");
            slider.setToolTipText(tr("Adjust gamma value of the layer."));
        }

        @Override
        protected void updateSliderWhileEnabled(Collection<? extends Layer> usedLayers, boolean allHidden) {
            double gamma = ((ImageryLayer) usedLayers.iterator().next()).getFilterSettings().getGamma();
            setRealValue(mapGammaToInterval(gamma));
        }

        @Override
        protected void applyValueToLayer(ImageryLayer layer) {
            layer.getFilterSettings().setGamma(mapIntervalToGamma(getRealValue()));
        }

        @Override
        public ImageIcon getIcon() {
           return ImageProvider.get(DIALOGS_LAYERLIST, "gamma");
        }

        @Override
        public String getLabel() {
            return tr("Gamma");
        }

        /**
         * Maps a number x from the range (-1,1) to a gamma value.
         * Gamma value is in the range (0, infinity).
         * Gamma values of 3 and 1/3 have opposite effects, so the mapping
         * should be symmetric in that sense.
         * @param x the slider value in the range (-1,1)
         * @return the gamma value
         */
        private double mapIntervalToGamma(double x) {
            // properties of the mapping:
            // g(-1) = 0
            // g(0) = 1
            // g(1) = infinity
            // g(-x) = 1 / g(x)
            return (1 + x) / (1 - x);
        }

        private double mapGammaToInterval(double gamma) {
            return (gamma - 1) / (gamma + 1);
        }
    }

    /**
     * This slider allows you to change the sharpness of a layer.
     *
     * @author Michael Zangl
     * @see ImageryFilterSettings#setSharpenLevel(double)
     */
    private class SharpnessSlider extends AbstractFilterSlider<ImageryLayer> {

        /**
         * Creates a new {@link SharpnessSlider}
         */
        SharpnessSlider() {
            super(0, MAX_SHARPNESS_FACTOR, ImageryLayer.class);
            setLabels(trc("image sharpness", "blurred"), trc("image sharpness", "normal"), trc("image sharpness", "sharp"));
            slider.setToolTipText(tr("Adjust sharpness/blur value of the layer."));
        }

        @Override
        protected void updateSliderWhileEnabled(Collection<? extends Layer> usedLayers, boolean allHidden) {
            setRealValue(((ImageryLayer) usedLayers.iterator().next()).getFilterSettings().getSharpenLevel());
        }

        @Override
        protected void applyValueToLayer(ImageryLayer layer) {
            layer.getFilterSettings().setSharpenLevel(getRealValue());
        }

        @Override
        public ImageIcon getIcon() {
           return ImageProvider.get(DIALOGS_LAYERLIST, "sharpness");
        }

        @Override
        public String getLabel() {
            return tr("Sharpness");
        }
    }

    /**
     * This slider allows you to change the colorfulness of a layer.
     *
     * @author Michael Zangl
     * @see ImageryFilterSettings#setColorfulness(double)
     */
    private class ColorfulnessSlider extends AbstractFilterSlider<ImageryLayer> {

        /**
         * Create a new {@link ColorfulnessSlider}
         */
        ColorfulnessSlider() {
            super(0, MAX_COLORFUL_FACTOR, ImageryLayer.class);
            setLabels(trc("image colorfulness", "less"), trc("image colorfulness", "normal"), trc("image colorfulness", "more"));
            slider.setToolTipText(tr("Adjust colorfulness of the layer."));
        }

        @Override
        protected void updateSliderWhileEnabled(Collection<? extends Layer> usedLayers, boolean allHidden) {
            setRealValue(((ImageryLayer) usedLayers.iterator().next()).getFilterSettings().getColorfulness());
        }

        @Override
        protected void applyValueToLayer(ImageryLayer layer) {
            layer.getFilterSettings().setColorfulness(getRealValue());
        }

        @Override
        public ImageIcon getIcon() {
           return ImageProvider.get(DIALOGS_LAYERLIST, "colorfulness");
        }

        @Override
        public String getLabel() {
            return tr("Colorfulness");
        }
    }

    /**
     * Allows to select the color for the GPX layer
     * @author Michael Zangl
     */
    private static class ColorSelector extends JPanel implements LayerVisibilityMenuEntry {

        private static final Border NORMAL_BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        private static final Border SELECTED_BORDER = BorderFactory.createLineBorder(Color.BLACK, 2);

        // TODO: Nicer color palette
        private static final Color[] COLORS = new Color[] {
                Color.RED,
                Color.ORANGE,
                Color.YELLOW,
                Color.GREEN,
                Color.BLUE,
                Color.CYAN,
                Color.GRAY,
        };
        private final Supplier<List<Layer>> layerSupplier;
        private final HashMap<Color, JPanel> panels = new HashMap<>();

        ColorSelector(Supplier<List<Layer>> layerSupplier) {
            super(new GridBagLayout());
            this.layerSupplier = layerSupplier;
            add(new JLabel(tr("Color")), GBC.eol().insets(24 + 10, 0, 0, 0));
            for (Color color : COLORS) {
                addPanelForColor(color);
            }
        }

        private void addPanelForColor(Color color) {
            JPanel innerPanel = new JPanel();
            innerPanel.setBackground(color);

            JPanel colorPanel = new JPanel(new BorderLayout());
            colorPanel.setBorder(NORMAL_BORDER);
            colorPanel.add(innerPanel);
            colorPanel.setMinimumSize(new Dimension(20, 20));
            colorPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    List<Layer> layers = layerSupplier.get();
                    for (Layer l : layers) {
                        if (l instanceof GpxLayer) {
                            l.setColor(color);
                        }
                    }
                    highlightColor(color);
                }
            });
            add(colorPanel, GBC.std().weight(1, 1).fill().insets(5));
            panels.put(color, colorPanel);

            List<Color> colors = layerSupplier.get().stream().map(l -> l.getColor()).distinct().collect(Collectors.toList());
            if (colors.size() == 1) {
                highlightColor(colors.get(0));
            }
        }

        @Override
        public void updateLayers(List<Layer> layers, boolean allVisible, boolean allHidden) {
            List<Color> colors = layers.stream().filter(l -> l instanceof GpxLayer)
                    .map(l -> ((GpxLayer) l).getColor())
                    .distinct()
                    .collect(Collectors.toList());
            if (colors.size() == 1) {
                setVisible(true);
                highlightColor(colors.get(0));
            } else if (colors.size() > 1) {
                setVisible(true);
                highlightColor(null);
            } else {
                // no GPX layer
                setVisible(false);
            }
        }

        private void highlightColor(Color color) {
            panels.values().forEach(panel -> panel.setBorder(NORMAL_BORDER));
            if (color != null) {
                JPanel selected = panels.get(color);
                if (selected != null) {
                    selected.setBorder(SELECTED_BORDER);
                }
            }
            repaint();
        }

        @Override
        public JComponent getPanel() {
            return this;
        }
    }
}
