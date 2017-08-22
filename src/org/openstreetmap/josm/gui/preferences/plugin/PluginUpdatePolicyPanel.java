// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.plugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.Logging;

/**
 * A panel for configuring whether JOSM shall update plugins at startup.
 *
 */
public class PluginUpdatePolicyPanel extends JPanel {

    private enum Policy {
        ASK("ask"),
        ALWAYS("always"),
        NEVER("never");

        private final String preferenceValue;

        Policy(String preferenceValue) {
            this.preferenceValue = preferenceValue;
        }

        public String getPreferencesValue() {
            return preferenceValue;
        }

        static Policy fromPreferenceValue(String preferenceValue) {
            if (preferenceValue == null)
                return null;
            String prefValue = preferenceValue.trim().toLowerCase(Locale.ENGLISH);
            for (Policy p: Policy.values()) {
                if (p.getPreferencesValue().equals(prefValue))
                    return p;
            }
            return null;
        }
    }

    private transient Map<Policy, JRadioButton> rbVersionBasedUpatePolicy;
    private transient Map<Policy, JRadioButton> rbTimeBasedUpatePolicy;
    private final JosmTextField tfUpdateInterval = new JosmTextField(5);
    private final JLabel lblUpdateInterval = new JLabel(tr("Update interval (in days):"));

    /**
     * Constructs a new {@code PluginUpdatePolicyPanel}.
     */
    public PluginUpdatePolicyPanel() {
        build();
        initFromPreferences();
    }

    protected JPanel buildVersionBasedUpdatePolicyPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        ButtonGroup bgVersionBasedUpdatePolicy = new ButtonGroup();
        rbVersionBasedUpatePolicy = new EnumMap<>(Policy.class);
        JRadioButton btn = new JRadioButton(tr("Ask before updating"));
        rbVersionBasedUpatePolicy.put(Policy.ASK, btn);
        bgVersionBasedUpdatePolicy.add(btn);

        btn = new JRadioButton(tr("Always update without asking"));
        rbVersionBasedUpatePolicy.put(Policy.ALWAYS, btn);
        bgVersionBasedUpdatePolicy.add(btn);

        btn = new JRadioButton(tr("Never update"));
        rbVersionBasedUpatePolicy.put(Policy.NEVER, btn);
        bgVersionBasedUpdatePolicy.add(btn);

        JMultilineLabel lbl = new JMultilineLabel(
                tr("Please decide whether JOSM shall automatically update active plugins at startup after an update of JOSM itself."));
        gc.gridy = 0;
        pnl.add(lbl, gc);
        for (Policy p: Policy.values()) {
            gc.gridy++;
            pnl.add(rbVersionBasedUpatePolicy.get(p), gc);
        }
        return pnl;
    }

    protected JPanel buildUpdateIntervalPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnl.add(lblUpdateInterval);
        pnl.add(tfUpdateInterval);
        lblUpdateInterval.setLabelFor(tfUpdateInterval);
        SelectAllOnFocusGainedDecorator.decorate(tfUpdateInterval);
        return pnl;
    }

    protected JPanel buildTimeBasedUpdatePolicyPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        TimeBasedPolicyChangeListener changeListener = new TimeBasedPolicyChangeListener();

        ButtonGroup bgTimeBasedUpdatePolicy = new ButtonGroup();
        rbTimeBasedUpatePolicy = new EnumMap<>(Policy.class);
        JRadioButton btn = new JRadioButton(tr("Ask before updating"));
        btn.addChangeListener(changeListener);
        rbTimeBasedUpatePolicy.put(Policy.ASK, btn);
        bgTimeBasedUpdatePolicy.add(btn);

        btn = new JRadioButton(tr("Always update without asking"));
        btn.addChangeListener(changeListener);
        rbTimeBasedUpatePolicy.put(Policy.ALWAYS, btn);
        bgTimeBasedUpdatePolicy.add(btn);

        btn = new JRadioButton(tr("Never update"));
        btn.addChangeListener(changeListener);
        rbTimeBasedUpatePolicy.put(Policy.NEVER, btn);
        bgTimeBasedUpdatePolicy.add(btn);

        JMultilineLabel lbl = new JMultilineLabel(
                tr("Please decide whether JOSM shall automatically update active plugins after a certain period of time."));
        gc.gridy = 0;
        pnl.add(lbl, gc);
        for (Policy p: Policy.values()) {
            gc.gridy++;
            pnl.add(rbTimeBasedUpatePolicy.get(p), gc);
        }
        gc.gridy++;
        pnl.add(buildUpdateIntervalPanel(), gc);
        return pnl;
    }

    protected final void build() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.insets = new Insets(5, 5, 10, 5);

        add(buildVersionBasedUpdatePolicyPanel(), gc);
        gc.gridy = 1;
        add(buildTimeBasedUpdatePolicyPanel(), gc);

        gc.gridy = 2;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        add(new JPanel(), gc);
    }

    /**
     * Loads the relevant preference values from the JOSM preferences
     */
    public final void initFromPreferences() {
        rbVersionBasedUpatePolicy.get(Optional.ofNullable(Policy.fromPreferenceValue(
                Main.pref.get("pluginmanager.version-based-update.policy", "ask"))).orElse(Policy.ASK)).setSelected(true);
        rbTimeBasedUpatePolicy.get(Optional.ofNullable(Policy.fromPreferenceValue(
                Main.pref.get("pluginmanager.time-based-update.policy", "ask"))).orElse(Policy.ASK)).setSelected(true);

        String pref = Main.pref.get("pluginmanager.warntime", null);
        int days = 0;
        if (pref != null) {
            // remove legacy preference
            Main.pref.put("pluginmanager.warntime", null);
            try {
                days = Integer.parseInt(pref.trim());
            } catch (NumberFormatException e) {
                // ignore - load from preference pluginmanager.time-based-update.interval
                Logging.trace(e);
            }
            if (days <= 0) {
                days = PluginHandler.DEFAULT_TIME_BASED_UPDATE_INTERVAL;
            }
        }
        if (days == 0) {
            days = Main.pref.getInteger("pluginmanager.time-based-update.interval", PluginHandler.DEFAULT_TIME_BASED_UPDATE_INTERVAL);
        }
        tfUpdateInterval.setText(Integer.toString(days));
    }

    /**
     * Remebers the update policy preference settings on the JOSM preferences
     */
    public void rememberInPreferences() {

        // remember policy for version based update
        //
        for (Policy p: Policy.values()) {
            if (rbVersionBasedUpatePolicy.get(p).isSelected()) {
                Main.pref.put("pluginmanager.version-based-update.policy", p.getPreferencesValue());
                break;
            }
        }

        // remember policy for time based update
        //
        for (Policy p: Policy.values()) {
            if (rbTimeBasedUpatePolicy.get(p).isSelected()) {
                Main.pref.put("pluginmanager.time-based-update.policy", p.getPreferencesValue());
                break;
            }
        }

        // remember update interval
        //
        int days = 0;
        try {
            days = Integer.parseInt(tfUpdateInterval.getText().trim());
            if (days <= 0) {
                days = PluginHandler.DEFAULT_TIME_BASED_UPDATE_INTERVAL;
            }
        } catch (NumberFormatException e) {
            days = PluginHandler.DEFAULT_TIME_BASED_UPDATE_INTERVAL;
        }
        Main.pref.putInteger("pluginmanager.time-based-update.interval", days);
    }

    class TimeBasedPolicyChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            lblUpdateInterval.setEnabled(!rbTimeBasedUpatePolicy.get(Policy.NEVER).isSelected());
            tfUpdateInterval.setEnabled(!rbTimeBasedUpatePolicy.get(Policy.NEVER).isSelected());
        }
    }

}
