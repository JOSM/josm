// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.IEnabledStateUpdating;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog.LayerListModel;
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
    private final JCheckBox visibilityCheckbox;
    final OpacitySlider opacitySlider = new OpacitySlider();
    private final ArrayList<FilterSlider<?>> sliders = new ArrayList<>();

    /**
     * Creates a new {@link LayerVisibilityAction}
     * @param model The list to get the selection from.
     */
    public LayerVisibilityAction(LayerListModel model) {
        this.model = model;
        popup = new JPopupMenu();

        // just to add a border
        JPanel content = new JPanel();
        popup.add(content);
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.setLayout(new GridBagLayout());

        new ImageProvider("dialogs/layerlist", "visibility").getResource().attachImageIcon(this, true);
        putValue(SHORT_DESCRIPTION, tr("Change visibility of the selected layer."));

        visibilityCheckbox = new JCheckBox(tr("Show layer"));
        visibilityCheckbox.addChangeListener(e -> setVisibleFlag(visibilityCheckbox.isSelected()));
        content.add(visibilityCheckbox, GBC.eop());

        addSlider(content, opacitySlider);
        addSlider(content, new ColorfulnessSlider());
        addSlider(content, new GammaFilterSlider());
        addSlider(content, new SharpnessSlider());
    }

    private void addSlider(JPanel content, FilterSlider<?> slider) {
        content.add(new JLabel(slider.getIcon()), GBC.std().span(1, 2).insets(0, 0, 5, 0));
        content.add(new JLabel(slider.getLabel()), GBC.eol());
        content.add(slider, GBC.eop());
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
            popup.show(sideButton, 0, sideButton.getHeight());
        } else {
            // Action can be trigger either by opacity button or by popup menu (in case toggle buttons are hidden).
            // In that case, show it in the middle of screen (because opacityButton is not visible)
            popup.show(Main.parent, Main.parent.getWidth() / 2, (Main.parent.getHeight() - popup.getHeight()) / 2);
        }
    }

    void updateValues() {
        List<Layer> layers = model.getSelectedLayers();

        visibilityCheckbox.setEnabled(!layers.isEmpty());
        boolean allVisible = true;
        boolean allHidden = true;
        for (Layer l : layers) {
            allVisible &= l.isVisible();
            allHidden &= !l.isVisible();
        }
        // TODO: Indicate tristate.
        visibilityCheckbox.setSelected(allVisible && !allHidden);

        for (FilterSlider<?> slider : sliders) {
            slider.updateSlider(layers, allHidden);
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
     * This is a slider for a filter value.
     * @author Michael Zangl
     *
     * @param <T> The layer type.
     */
    private abstract class FilterSlider<T extends Layer> extends JSlider {
        private final double minValue;
        private final double maxValue;
        private final Class<T> layerClassFilter;

        /**
         * Create a new filter slider.
         * @param minValue The minimum value to map to the left side.
         * @param maxValue The maximum value to map to the right side.
         * @param layerClassFilter The type of layer influenced by this filter.
         */
        FilterSlider(double minValue, double maxValue, Class<T> layerClassFilter) {
            super(JSlider.HORIZONTAL);
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.layerClassFilter = layerClassFilter;
            setMaximum(SLIDER_STEPS);
            int tick = convertFromRealValue(1);
            setMinorTickSpacing(tick);
            setMajorTickSpacing(tick);
            setPaintTicks(true);

            addChangeListener(e -> onStateChanged());
            addMouseWheelListener(this::mouseWheelMoved);
        }

        /**
         * Called whenever the state of the slider was changed.
         * @see #getValueIsAdjusting()
         * @see #getRealValue()
         */
        protected void onStateChanged() {
            Collection<T> layers = filterLayers(model.getSelectedLayers());
            for (T layer : layers) {
                applyValueToLayer(layer);
            }
        }

        protected void mouseWheelMoved(MouseWheelEvent e) {
            double rotation = e.getPreciseWheelRotation();
            double destinationValue = getValue() + rotation * SLIDER_WHEEL_INCREMENT;
            if (rotation < 0) {
                destinationValue = Math.floor(destinationValue);
            } else {
                destinationValue = Math.ceil(destinationValue);
            }
            setValue(Utils.clamp((int) destinationValue, getMinimum(), getMaximum()));
            e.consume();
        }

        protected void applyValueToLayer(T layer) {
        }

        protected double getRealValue() {
            return convertToRealValue(getValue());
        }

        protected double convertToRealValue(int value) {
            double s = (double) value / SLIDER_STEPS;
            return s * maxValue + (1-s) * minValue;
        }

        protected void setRealValue(double value) {
            setValue(convertFromRealValue(value));
        }

        protected int convertFromRealValue(double value) {
            int i = (int) ((value - minValue) / (maxValue - minValue) * SLIDER_STEPS + .5);
            return Utils.clamp(i, getMinimum(), getMaximum());
        }

        public abstract ImageIcon getIcon();

        public abstract String getLabel();

        public void updateSlider(List<Layer> layers, boolean allHidden) {
            Collection<? extends Layer> usedLayers = filterLayers(layers);
            if (usedLayers.isEmpty() || allHidden) {
                setEnabled(false);
            } else {
                setEnabled(true);
                updateSliderWhileEnabled(usedLayers, allHidden);
            }
        }

        protected Collection<T> filterLayers(List<Layer> layers) {
            return Utils.filteredCollection(layers, layerClassFilter);
        }

        protected abstract void updateSliderWhileEnabled(Collection<? extends Layer> usedLayers, boolean allHidden);
    }

    /**
     * This slider allows you to change the opacity of a layer.
     *
     * @author Michael Zangl
     * @see Layer#setOpacity(double)
     */
    class OpacitySlider extends FilterSlider<Layer> {
        /**
         * Creaate a new {@link OpacitySlider}.
         */
        OpacitySlider() {
            super(0, 1, Layer.class);
            setToolTipText(tr("Adjust opacity of the layer."));
        }

        @Override
        protected void onStateChanged() {
            if (getRealValue() <= 0.001 && !getValueIsAdjusting()) {
                setVisibleFlag(false);
            } else {
                super.onStateChanged();
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
            return ImageProvider.get("dialogs/layerlist", "transparency");
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
    private class GammaFilterSlider extends FilterSlider<ImageryLayer> {

        /**
         * Create a new {@link GammaFilterSlider}
         */
        GammaFilterSlider() {
            super(-1, 1, ImageryLayer.class);
            setToolTipText(tr("Adjust gamma value of the layer."));
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
           return ImageProvider.get("dialogs/layerlist", "gamma");
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
    private class SharpnessSlider extends FilterSlider<ImageryLayer> {

        /**
         * Creates a new {@link SharpnessSlider}
         */
        SharpnessSlider() {
            super(0, MAX_SHARPNESS_FACTOR, ImageryLayer.class);
            setToolTipText(tr("Adjust sharpness/blur value of the layer."));
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
           return ImageProvider.get("dialogs/layerlist", "sharpness");
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
    private class ColorfulnessSlider extends FilterSlider<ImageryLayer> {

        /**
         * Create a new {@link ColorfulnessSlider}
         */
        ColorfulnessSlider() {
            super(0, MAX_COLORFUL_FACTOR, ImageryLayer.class);
            setToolTipText(tr("Adjust colorfulness of the layer."));
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
           return ImageProvider.get("dialogs/layerlist", "colorfulness");
        }

        @Override
        public String getLabel() {
            return tr("Colorfulness");
        }
    }
}
