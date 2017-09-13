// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListCellRenderer;
import javax.swing.LookAndFeel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapMover;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.widgets.FileChooserManager;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Look-and-feel preferences.
 */
public class LafPreference implements SubPreferenceSetting {

    /**
     * Look-and-feel property.
     * @since 11713
     */
    public static final StringProperty LAF = new StringProperty("laf", Main.platform.getDefaultStyle());

    static final class LafListCellRenderer implements ListCellRenderer<LookAndFeelInfo> {
        private final DefaultListCellRenderer def = new DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(JList<? extends LookAndFeelInfo> list, LookAndFeelInfo value,
                int index, boolean isSelected, boolean cellHasFocus) {
            return def.getListCellRendererComponent(list, value.getName(), index, isSelected, cellHasFocus);
        }
    }

    /**
     * Factory used to create a new {@code LafPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new LafPreference();
        }
    }

    /**
     * ComboBox with all look and feels.
     */
    private JosmComboBox<LookAndFeelInfo> lafCombo;
    VerticallyScrollablePanel panel;
    private final JCheckBox showSplashScreen = new JCheckBox(tr("Show splash screen at startup"));
    private final JCheckBox showID = new JCheckBox(tr("Show object ID in selection lists"));
    private final JCheckBox showLocalizedName = new JCheckBox(tr("Show localized name in selection lists"));
    private final JCheckBox modeless = new JCheckBox(tr("Modeless working (Potlatch style)"));
    private final JCheckBox dynamicButtons = new JCheckBox(tr("Dynamic buttons in side menus"));
    private final JCheckBox isoDates = new JCheckBox(tr("Display ISO dates"));
    private final JCheckBox nativeFileChoosers = new JCheckBox(tr("Use native file choosers (nicer, but do not support file filters)"));
    private final JCheckBox zoomReverseWheel = new JCheckBox(tr("Reverse zoom with mouse wheel"));
    private final JCheckBox zoomIntermediateSteps = new JCheckBox(tr("Intermediate steps between native resolutions"));
    private JSpinner spinZoomRatio;

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        lafCombo = new JosmComboBox<>(UIManager.getInstalledLookAndFeels());

        // let's try to load additional LookAndFeels and put them into the list
        if (Main.isPlatformOsx()) {
            try {
                Class<?> cquaqua = Class.forName("ch.randelshofer.quaqua.QuaquaLookAndFeel");
                Object oquaqua = cquaqua.getConstructor((Class[]) null).newInstance((Object[]) null);
                // no exception? Then Go!
                lafCombo.addItem(
                        new UIManager.LookAndFeelInfo(((LookAndFeel) oquaqua).getName(), "ch.randelshofer.quaqua.QuaquaLookAndFeel")
                );
            } catch (ReflectiveOperationException ex) {
                // just debug, Quaqua may not even be installed...
                Logging.debug(ex);
            }
        }

        String laf = LAF.get();
        for (int i = 0; i < lafCombo.getItemCount(); ++i) {
            if (lafCombo.getItemAt(i).getClassName().equals(laf)) {
                lafCombo.setSelectedIndex(i);
                break;
            }
        }

        lafCombo.setRenderer(new LafListCellRenderer());

        panel = new VerticallyScrollablePanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Show splash screen on startup
        showSplashScreen.setToolTipText(tr("Show splash screen at startup"));
        showSplashScreen.setSelected(Main.pref.getBoolean("draw.splashscreen", true));
        panel.add(showSplashScreen, GBC.eop().insets(20, 0, 0, 0));

        // Show ID in selection
        showID.setToolTipText(tr("Show object ID in selection lists"));
        showID.setSelected(Main.pref.getBoolean("osm-primitives.showid", false));

        // Show localized names
        showLocalizedName.setToolTipText(tr("Show localized name in selection lists, if available"));
        showLocalizedName.setSelected(Main.pref.getBoolean("osm-primitives.localize-name", true));
        ExpertToggleAction.addVisibilitySwitcher(showLocalizedName);

        modeless.setToolTipText(tr("Do not require to switch modes (potlatch style workflow)"));
        modeless.setSelected(MapFrame.MODELESS.get());
        ExpertToggleAction.addVisibilitySwitcher(modeless);

        panel.add(showID, GBC.eop().insets(20, 0, 0, 0));
        panel.add(showLocalizedName, GBC.eop().insets(20, 0, 0, 0));
        panel.add(modeless, GBC.eop().insets(20, 0, 0, 0));

        dynamicButtons.setToolTipText(tr("Display buttons in right side menus only when mouse is inside the element"));
        dynamicButtons.setSelected(ToggleDialog.PROP_DYNAMIC_BUTTONS.get());
        panel.add(dynamicButtons, GBC.eop().insets(20, 0, 0, 0));

        Date today = new Date();
        isoDates.setToolTipText(tr("Format dates according to {0}. Today''s date will be displayed as {1} instead of {2}",
                tr("ISO 8601"),
                DateUtils.newIsoDateFormat().format(today),
                DateFormat.getDateInstance(DateFormat.SHORT).format(today)));
        isoDates.setSelected(DateUtils.PROP_ISO_DATES.get());
        panel.add(isoDates, GBC.eop().insets(20, 0, 0, 0));

        nativeFileChoosers.setToolTipText(
                tr("Use file choosers that behave more like native ones. They look nicer but do not support some features like file filters"));
        nativeFileChoosers.setSelected(FileChooserManager.PROP_USE_NATIVE_FILE_DIALOG.get());
        panel.add(nativeFileChoosers, GBC.eop().insets(20, 0, 0, 0));

        zoomReverseWheel.setToolTipText(
                tr("Check if you feel opposite direction more convenient"));
        zoomReverseWheel.setSelected(MapMover.PROP_ZOOM_REVERSE_WHEEL.get());
        panel.add(zoomReverseWheel, GBC.eop().insets(20, 0, 0, 0));

        zoomIntermediateSteps.setToolTipText(
                tr("Divide intervals between native resolution levels to smaller steps if they are much larger than zoom ratio"));
        zoomIntermediateSteps.setSelected(NavigatableComponent.PROP_ZOOM_INTERMEDIATE_STEPS.get());
        ExpertToggleAction.addVisibilitySwitcher(zoomIntermediateSteps);
        panel.add(zoomIntermediateSteps, GBC.eop().insets(20, 0, 0, 0));

        panel.add(Box.createVerticalGlue(), GBC.eol().insets(0, 10, 0, 0));

        double logZoomLevel = Math.log(2) / Math.log(NavigatableComponent.PROP_ZOOM_RATIO.get());
        logZoomLevel = Math.max(1, logZoomLevel);
        logZoomLevel = Math.min(5, logZoomLevel);
        JLabel labelZoomRatio = new JLabel(tr("Zoom steps to get double scale"));
        spinZoomRatio = new JSpinner(new SpinnerNumberModel(logZoomLevel, 1, 5, 1));
        Component spinZoomRatioEditor = spinZoomRatio.getEditor();
        JFormattedTextField jftf = ((JSpinner.DefaultEditor) spinZoomRatioEditor).getTextField();
        jftf.setColumns(2);
        String zoomRatioToolTipText = tr("Higher value means more steps needed, therefore zoom steps will be smaller");
        spinZoomRatio.setToolTipText(zoomRatioToolTipText);
        labelZoomRatio.setToolTipText(zoomRatioToolTipText);
        labelZoomRatio.setLabelFor(spinZoomRatio);
        panel.add(labelZoomRatio, GBC.std().insets(20, 0, 0, 0));
        panel.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        panel.add(spinZoomRatio, GBC.eol());

        panel.add(new JLabel(tr("Look and Feel")), GBC.std().insets(20, 0, 0, 0));
        panel.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        panel.add(lafCombo, GBC.eol().fill(GBC.HORIZONTAL));

        JScrollPane scrollpane = panel.getVerticalScrollPane();
        scrollpane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gui.getDisplayPreference().addSubTab(this, tr("Look and Feel"), scrollpane);
    }

    @Override
    public boolean ok() {
        boolean mod = false;
        Main.pref.putBoolean("draw.splashscreen", showSplashScreen.isSelected());
        Main.pref.putBoolean("osm-primitives.showid", showID.isSelected());
        Main.pref.putBoolean("osm-primitives.localize-name", showLocalizedName.isSelected());
        MapFrame.MODELESS.put(modeless.isSelected());
        Main.pref.putBoolean(ToggleDialog.PROP_DYNAMIC_BUTTONS.getKey(), dynamicButtons.isSelected());
        Main.pref.putBoolean(DateUtils.PROP_ISO_DATES.getKey(), isoDates.isSelected());
        Main.pref.putBoolean(FileChooserManager.PROP_USE_NATIVE_FILE_DIALOG.getKey(), nativeFileChoosers.isSelected());
        MapMover.PROP_ZOOM_REVERSE_WHEEL.put(zoomReverseWheel.isSelected());
        NavigatableComponent.PROP_ZOOM_INTERMEDIATE_STEPS.put(zoomIntermediateSteps.isSelected());
        NavigatableComponent.PROP_ZOOM_RATIO.put(Math.pow(2, 1/(double) spinZoomRatio.getModel().getValue()));
        mod |= LAF.put(((LookAndFeelInfo) lafCombo.getSelectedItem()).getClassName());
        return mod;
    }

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(final PreferenceTabbedPane gui) {
        return gui.getDisplayPreference();
    }
}
