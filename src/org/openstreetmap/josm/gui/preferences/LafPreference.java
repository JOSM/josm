// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

public class LafPreference implements PreferenceSetting {

    /**
     * ComboBox with all look and feels.
     */
    private JComboBox lafCombo;
    public JPanel panel = new JPanel(new GridBagLayout());
    private JCheckBox showSplashScreen = new JCheckBox(tr("Show splash screen at startup"));
    private JCheckBox showID = new JCheckBox(tr("Show object ID in selection lists"));

    public void addGui(PreferenceDialog gui) {
        lafCombo = new JComboBox(UIManager.getInstalledLookAndFeels());

        // let's try to load additional LookAndFeels and put them into the list
        try {
            Class<?> Cquaqua = Class.forName("ch.randelshofer.quaqua.QuaquaLookAndFeel");
            Object Oquaqua = Cquaqua.getConstructor((Class[])null).newInstance((Object[])null);
            // no exception? Then Go!
            lafCombo.addItem(
                new UIManager.LookAndFeelInfo(((javax.swing.LookAndFeel)Oquaqua).getName(), "ch.randelshofer.quaqua.QuaquaLookAndFeel")
            );
        } catch (Exception ex) {
            // just ignore, Quaqua may not even be installed...
            //System.out.println("Failed to load Quaqua: " + ex);
        }

        String laf = Main.pref.get("laf");
        for (int i = 0; i < lafCombo.getItemCount(); ++i) {
            if (((LookAndFeelInfo)lafCombo.getItemAt(i)).getClassName().equals(laf)) {
                lafCombo.setSelectedIndex(i);
                break;
            }
        }

        final ListCellRenderer oldRenderer = lafCombo.getRenderer();
        lafCombo.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return oldRenderer.getListCellRendererComponent(list, ((LookAndFeelInfo)value).getName(), index, isSelected, cellHasFocus);
            }
        });
        lafCombo.addActionListener(gui.requireRestartAction);

        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // Show splash screen on startup
        showSplashScreen.setToolTipText(tr("Show splash screen at startup"));
        showSplashScreen.setSelected(Main.pref.getBoolean("draw.splashscreen", true));
        panel.add(showSplashScreen, GBC.eop().insets(20, 0, 0, 0));

        // Show ID in selection
        showID.setToolTipText(tr("Show object ID in selection lists"));
        showID.setSelected(Main.pref.getBoolean("osm-primitives.showid", false));
        panel.add(showID, GBC.eop().insets(20, 0, 0, 0));

        panel.add(Box.createVerticalGlue(), GBC.eol().insets(0, 20, 0, 0));

        panel.add(new JLabel(tr("Look and Feel")), GBC.std().insets(20, 0, 0, 0));
        panel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        panel.add(lafCombo, GBC.eol().fill(GBC.HORIZONTAL));

        JScrollPane scrollpane = new JScrollPane(panel);
        scrollpane.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        gui.displaycontent.addTab(tr("Look and Feel"), scrollpane);
    }

    public void ok() {
        Main.pref.put("laf", ((LookAndFeelInfo)lafCombo.getSelectedItem()).getClassName());
        Main.pref.put("draw.splashscreen", showSplashScreen.isSelected());
        Main.pref.put("osm-primitives.showid", showID.isSelected());
    }

}
