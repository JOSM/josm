//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.PluginDownloader;
import org.openstreetmap.josm.plugins.PluginSelection;
import org.openstreetmap.josm.tools.GBC;

public class PluginPreference implements PreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new PluginPreference();
        }
    }

    private JPanel plugin;
    private JPanel pluginPanel = new NoHorizontalScrollPanel(new GridBagLayout());
    private PreferenceDialog gui;
    private JScrollPane pluginPane;
    private PluginSelection selection = new PluginSelection();
    private JTextField txtFilter;

    public void addGui(final PreferenceDialog gui) {
        this.gui = gui;
        plugin = gui.createPreferenceTab("plugin", tr("Plugins"), tr("Configure available plugins."), false);

        txtFilter = new JTextField();
        JLabel lbFilter = new JLabel(tr("Search: "));
        lbFilter.setLabelFor(txtFilter);
        plugin.add(lbFilter);
        plugin.add(txtFilter, GBC.eol().fill(GBC.HORIZONTAL));
        txtFilter.getDocument().addDocumentListener(new DocumentListener(){
            public void changedUpdate(DocumentEvent e) {
                action();
            }

            public void insertUpdate(DocumentEvent e) {
                action();
            }

            public void removeUpdate(DocumentEvent e) {
                action();
            }

            private void action() {
                selection.drawPanel(pluginPanel);
            }
        });
        plugin.add(GBC.glue(0,10), GBC.eol());

        /* main plugin area */
        pluginPane = new JScrollPane(pluginPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pluginPane.setBorder(null);
        plugin.add(pluginPane, GBC.eol().fill(GBC.BOTH));
        plugin.add(GBC.glue(0,10), GBC.eol());

        /* buttons at the bottom */
        JButton morePlugins = new JButton(tr("Download List"));
        morePlugins.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                selection.updateDescription(pluginPanel);
            }
        });
        plugin.add(morePlugins, GBC.std().insets(0,0,10,0));

        JButton update = new JButton(tr("Update"));
        update.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                selection.update(pluginPanel);
            }
        });
        plugin.add(update, GBC.std().insets(0,0,10,0));

        JButton configureSites = new JButton(tr("Configure Sites..."));
        configureSites.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                configureSites();
            }
        });
        plugin.add(configureSites, GBC.std());

        selection.passTxtFilter(txtFilter);
        selection.loadPlugins();
        selection.drawPanel(pluginPanel);
    }

    private void configureSites() {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(tr("Add JOSM Plugin description URL.")), GBC.eol());
        final DefaultListModel model = new DefaultListModel();
        for (String s : PluginDownloader.getSites()) {
            model.addElement(s);
        }
        final JList list = new JList(model);
        p.add(new JScrollPane(list), GBC.std().fill());
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.add(new JButton(new AbstractAction(tr("Add")){
            public void actionPerformed(ActionEvent e) {
                String s = JOptionPane.showInputDialog(
                        gui,
                        tr("Add JOSM Plugin description URL."),
                        tr("Enter URL"),
                        JOptionPane.QUESTION_MESSAGE
                );
                if (s != null) {
                    model.addElement(s);
                }
            }
        }), GBC.eol().fill(GBC.HORIZONTAL));
        buttons.add(new JButton(new AbstractAction(tr("Edit")){
            public void actionPerformed(ActionEvent e) {
                if (list.getSelectedValue() == null) {
                    JOptionPane.showMessageDialog(
                            gui,
                            tr("Please select an entry."),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
                String s = (String)JOptionPane.showInputDialog(
                        Main.parent,
                        tr("Edit JOSM Plugin description URL."),
                        tr("JOSM Plugin description URL"),
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        null,
                        list.getSelectedValue()
                );
                model.setElementAt(s, list.getSelectedIndex());
            }
        }), GBC.eol().fill(GBC.HORIZONTAL));
        buttons.add(new JButton(new AbstractAction(tr("Delete")){
            public void actionPerformed(ActionEvent event) {
                if (list.getSelectedValue() == null) {
                    JOptionPane.showMessageDialog(
                            gui,
                            tr("Please select an entry."),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
                model.removeElement(list.getSelectedValue());
            }
        }), GBC.eol().fill(GBC.HORIZONTAL));
        p.add(buttons, GBC.eol());
        int answer = JOptionPane.showConfirmDialog(
                gui,
                p,
                tr("Configure Plugin Sites"), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION)
            return;
        Collection<String> sites = new LinkedList<String>();
        for (int i = 0; i < model.getSize(); ++i) {
            sites.add((String)model.getElementAt(i));
        }
        PluginDownloader.setSites(sites);
    }

    public boolean ok() {
        return selection.finish();
    }

    private static class NoHorizontalScrollPanel extends JPanel implements Scrollable {
        public NoHorizontalScrollPanel(GridBagLayout gridBagLayout) {
            super(gridBagLayout);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return super.getPreferredSize();
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 30;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }
    }
}
