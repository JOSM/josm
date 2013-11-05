// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.io.MessageNotifier;
import org.openstreetmap.josm.tools.GBC;

/**
 * Preferences panel for OSM messages notifier.
 * @since 6349
 */
public class MessagesNotifierPanel extends JPanel {

    private JCheckBox notifier;
    private JLabel intervalLabel;
    private final JosmTextField notifierInterval = new JosmTextField(4);

    /**
     * Constructs a new {@code MessagesNotifierPanel}.
     */
    public MessagesNotifierPanel() {
        build();
        initFromPreferences();
        updateEnabledState();
    }

    private void build() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        notifier = new JCheckBox(tr("Periodically check for new messages"));
        add(notifier, GBC.eol());
        notifier.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateEnabledState();
            }
        });
        
        intervalLabel = new JLabel(tr("Check interval (minutes):"));
        add(intervalLabel, GBC.std().insets(25,0,0,0));

        notifierInterval.setToolTipText(tr("Default value: {0}", MessageNotifier.PROP_INTERVAL.getDefaultValue()));
        notifierInterval.setMinimumSize(notifierInterval.getPreferredSize());
        add(notifierInterval, GBC.eol().insets(5,0,0,0));
    }
    
    private void updateEnabledState() {
        boolean enabled = notifier.isSelected();
        intervalLabel.setEnabled(enabled);
        notifierInterval.setEnabled(enabled);
        notifierInterval.setEditable(enabled);
    }

    /**
     * Initializes the panel from preferences
     */
    public final void initFromPreferences() {
        notifier.setSelected(MessageNotifier.PROP_NOTIFIER_ENABLED.get());
        notifierInterval.setText(Integer.toString(MessageNotifier.PROP_INTERVAL.get()));
    }

    /**
     * Saves the current values to preferences
     */
    public void saveToPreferences() {
        final boolean enabled = notifier.isSelected();
        boolean changed = MessageNotifier.PROP_NOTIFIER_ENABLED.put(enabled);
        changed |= MessageNotifier.PROP_INTERVAL.parseAndPut(notifierInterval.getText());
        // If parameters have changed, restart notifier
        if (changed) {
            MessageNotifier.stop();
            if (enabled) {
                MessageNotifier.start();
            }
        // Even if they have not changed,
        } else {
            // notifier should be stopped if user is no more identified enough 
            if (!MessageNotifier.isUserEnoughIdentified()) {
                MessageNotifier.stop();
            // or restarted if user is again identified and notifier was enabled in preferences
            } else if (enabled && !MessageNotifier.isRunning()) {
                MessageNotifier.start();
            }
        }
    }
}
