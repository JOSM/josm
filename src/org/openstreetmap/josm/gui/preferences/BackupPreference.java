// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.openstreetmap.josm.data.AutosaveTask;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.tools.GBC;

public class BackupPreference implements PreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        @Override
        public BackupPreference createPreferenceSetting() {
            return new BackupPreference();
        }
    }
    private static final BooleanProperty PROP_KEEP_BACKUP = new BooleanProperty("save.keepbackup", false);
    private JCheckBox keepBackup;
    private JCheckBox autosave;
    private final JTextField autosaveInterval = new JTextField(8);
    private final JTextField backupPerLayer = new JTextField(8);

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        JPanel panel = new VerticallyScrollablePanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        autosave = new JCheckBox("Auto save enabled");
        autosave.setSelected(AutosaveTask.PROP_AUTOSAVE_ENABLED.get());
        panel.add(autosave, GBC.eol());

        final JLabel autosaveIntervalLabel = new JLabel(tr("Auto save interval (seconds)"));
        panel.add(autosaveIntervalLabel, GBC.std().insets(60,0,0,0));
        autosaveInterval.setText(Integer.toString(AutosaveTask.PROP_INTERVAL.get()));
        autosaveInterval.setToolTipText(tr("Default value: {0}", AutosaveTask.PROP_INTERVAL.getDefaultValue()));
        autosaveInterval.setMinimumSize(autosaveInterval.getPreferredSize());
        panel.add(autosaveInterval, GBC.eol().insets(5,0,0,5));

        final JLabel backupPerLayerLabel = new JLabel(tr("Auto saved files per layer"));
        panel.add(backupPerLayerLabel, GBC.std().insets(60,0,0,0));
        backupPerLayer.setText(Integer.toString(AutosaveTask.PROP_FILES_PER_LAYER.get()));
        backupPerLayer.setToolTipText(tr("Default value: {0}", AutosaveTask.PROP_FILES_PER_LAYER.getDefaultValue()));
        backupPerLayer.setMinimumSize(backupPerLayer.getPreferredSize());
        panel.add(backupPerLayer, GBC.eol().insets(5,0,0,10));

        panel.add(new HtmlPanel(
            tr("<i>(Autosave stores the changed data layers in periodic intervals. " +
                "The backups are saved in JOSM''s preference folder. " +
                "In case of a crash, JOSM tries to recover the unsaved changes " +
                "on next start.)</i>")),
            GBC.eop().fill(GBC.HORIZONTAL).insets(5,0,0,10));

        panel.add(new JSeparator(), GBC.eop().fill(GBC.HORIZONTAL));

        keepBackup = new JCheckBox(tr("Keep backup files when saving data layers"));
        keepBackup.setSelected(PROP_KEEP_BACKUP.get());
        keepBackup.setToolTipText(tr("When saving, keep backup files ending with a ~"));
        panel.add(keepBackup, GBC.eop());

        panel.add(new HtmlPanel(
            tr("<i>(JOSM can keep a backup file when saving data layers. "+
                "It appends ''~'' to the file name and saves it in the same folder.)</i>")),
            GBC.eop().fill(GBC.HORIZONTAL).insets(5,0,0,0));

        ActionListener autosaveEnabled = new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                boolean enabled = autosave.isSelected();
                autosaveIntervalLabel.setEnabled(enabled);
                autosaveInterval.setEnabled(enabled);
                backupPerLayerLabel.setEnabled(enabled);
                backupPerLayer.setEnabled(enabled);
            }
        };
        autosave.addActionListener(autosaveEnabled);
        autosaveEnabled.actionPerformed(null);

        panel.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.BOTH));
        JScrollPane sp = new JScrollPane(panel);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        gui.mapcontent.addTab(tr("File backup"), null, sp, tr("Configure whether to create backup files"));
    }

    @Override
    public boolean ok() {
        boolean restartRequired = false;
        PROP_KEEP_BACKUP.put(keepBackup.isSelected());

        restartRequired |= AutosaveTask.PROP_AUTOSAVE_ENABLED.put(autosave.isSelected());
        restartRequired |= AutosaveTask.PROP_INTERVAL.parseAndPut(autosaveInterval.getText());
        AutosaveTask.PROP_FILES_PER_LAYER.parseAndPut(backupPerLayer.getText());
        return restartRequired;
    }
}
