// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.xml.sax.SAXException;

public class LanguagePreference implements PreferenceSetting {
    static private final Logger logger = Logger.getLogger(LanguagePreference.class.getName());

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new LanguagePreference();
        }
    }

    /**
     * ComboBox with all available Translations
     */
    private JComboBox langCombo;
    private boolean translationsLoaded = false;

    public void addGui(final PreferenceDialog gui) {
        //langCombo = new JComboBox(I18n.getAvailableTranslations());
        langCombo = new JComboBox(new Locale[0]);

        final ListCellRenderer oldRenderer = langCombo.getRenderer();
        langCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                Locale l = (Locale) value;
                return oldRenderer.getListCellRendererComponent(list,
                        l == null ? tr("Default (Auto determined)") : l.getDisplayName(),
                                index, isSelected, cellHasFocus);
            }
        });

        LafPreference lafPreference = gui.getSetting(LafPreference.class);
        final JPanel panel = lafPreference.panel;
        panel.add(new JLabel(tr("Language")), GBC.std().insets(20, 0, 0, 0));
        panel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        panel.add(langCombo, GBC.eol().fill(GBC.HORIZONTAL));
        panel.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.BOTH));

        // this defers loading of available translations to the first time the tab
        // with the available translations is selected by the user
        //
        gui.displaycontent.addChangeListener(
                new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        int i = gui.displaycontent.getSelectedIndex();
                        String title = gui.displaycontent.getTitleAt(i);
                        if (title.equals(tr("Look and Feel"))) {
                            initiallyLoadAvailableTranslations();
                        }
                    }
                }
        );
    }

    public boolean ok() {
        if(langCombo.getSelectedItem() == null)
            return Main.pref.put("language", null);
        else
            return Main.pref.put("language",
                    ((Locale)langCombo.getSelectedItem()).toString());
    }

    public void initiallyLoadAvailableTranslations() {
        if (!translationsLoaded) {
            reloadAvailableTranslations();
        }
        translationsLoaded = true;
    }

    protected void reloadAvailableTranslations() {
        Main.worker.submit(new AvailableTranslationsLoader());
    }

    /**
     * Asynchronous task to lookup the available translations.
     * 
     */
    private class AvailableTranslationsLoader extends PleaseWaitRunnable {
        public AvailableTranslationsLoader() {
            super(tr("Looking up available translations..."));
        }

        @Override
        protected void cancel() {
            // can't cancel
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            final List<Locale> locales = new ArrayList<Locale>(
                    Arrays.asList(I18n.getAvailableTranslations(getProgressMonitor()))
            );
            locales.add(0,Locale.ENGLISH);
            Runnable r = new Runnable() {
                public void run() {
                    langCombo.removeAll();
                    langCombo.addItem(null); // the default enry
                    for (Locale locale : locales) {
                        langCombo.addItem(locale);
                    }
                    String ln = Main.pref.get("language");
                    langCombo.setSelectedIndex(0);
                    if (ln != null) {
                        for (int i = 1; i < langCombo.getItemCount(); ++i) {
                            if (((Locale) langCombo.getItemAt(i)).toString().equals(ln)) {
                                langCombo.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                }
            };
            try {
                SwingUtilities.invokeAndWait(r);
            } catch(InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            } catch(InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void finish() {}
    }
}
