// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Nice about screen.
 *
 * The REVISION resource is read and if present, it shows the revision information of the jar-file.
 *
 * @author imi
 */
public final class AboutAction extends JosmAction {

    /**
     * Constructs a new {@code AboutAction}.
     */
    public AboutAction() {
        super(tr("About"), "logo", tr("Display the about screen."),
            Shortcut.registerShortcut("system:about", tr("About"),
            KeyEvent.VK_F1, Shortcut.SHIFT), true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final JTabbedPane about = new JTabbedPane();

        Version version = Version.getInstance();

        JosmTextArea readme = new JosmTextArea();
        readme.setFont(GuiHelper.getMonospacedFont(readme));
        readme.setEditable(false);
        setTextFromResourceFile(readme, "/README");
        readme.setCaretPosition(0);

        JosmTextArea revision = new JosmTextArea();
        revision.setFont(GuiHelper.getMonospacedFont(revision));
        revision.setEditable(false);
        revision.setText(version.getReleaseAttributes());
        revision.setCaretPosition(0);

        JosmTextArea contribution = new JosmTextArea();
        contribution.setEditable(false);
        setTextFromResourceFile(contribution, "/CONTRIBUTION");
        contribution.setCaretPosition(0);

        JosmTextArea license = new JosmTextArea();
        license.setEditable(false);
        setTextFromResourceFile(license, "/LICENSE");
        license.setCaretPosition(0);

        JPanel info = new JPanel(new GridBagLayout());
        final JMultilineLabel label = new JMultilineLabel("<html>" +
                "<h1>" + "JOSM – " + tr("Java OpenStreetMap Editor") + "</h1>" +
                "<p style='font-size:75%'></p>" +
                "<p>" + tr("Version {0}", version.getVersionString()) + "</p>" +
                "<p style='font-size:50%'></p>" +
                "<p>" + tr("Last change at {0}", version.getTime()) + "</p>" +
                "<p style='font-size:50%'></p>" +
                "<p>" + tr("Java Version {0}", System.getProperty("java.version")) + "</p>" +
                "<p style='font-size:50%'></p>" +
                "</html>");
        info.add(label, GBC.eol().fill(GBC.HORIZONTAL).insets(10, 0, 0, 0));
        info.add(new JLabel(tr("Homepage")), GBC.std().insets(10, 0, 10, 0));
        info.add(new UrlLabel(Main.getJOSMWebsite(), 2), GBC.eol().fill(GBC.HORIZONTAL));
        info.add(GBC.glue(0, 5), GBC.eol());

        about.addTab(tr("Info"), info);
        about.addTab(tr("Readme"), createScrollPane(readme));
        about.addTab(tr("Revision"), createScrollPane(revision));
        about.addTab(tr("Contribution"), createScrollPane(contribution));
        about.addTab(tr("License"), createScrollPane(license));
        about.addTab(tr("Plugins"), new JScrollPane(PluginHandler.getInfoPanel()));

        // Intermediate panel to allow proper optionPane resizing
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(890, 300));
        panel.add(new JLabel("", ImageProvider.get("logo.svg", ImageProvider.ImageSizes.ABOUT_LOGO),
                JLabel.CENTER), GBC.std().insets(0, 5, 0, 0));
        panel.add(about, GBC.std().fill());

        GuiHelper.prepareResizeableOptionPane(panel, panel.getPreferredSize());
        int ret = new ExtendedDialog(Main.parent, tr("About JOSM..."), tr("OK"), tr("Report bug"))
            .setButtonIcons("ok", "bug")
            .setContent(panel, false)
            .showDialog().getValue();
        if (2 == ret) {
            Main.main.menu.reportbug.actionPerformed(null);
        }
    }

    /**
     * Reads the contents of the resource file that is described by the {@code filePath}-attribute and puts that text
     * into the {@link JTextArea} given by the {@code ta}-attribute.
     * @param ta the {@link JTextArea} to put the files contents into
     * @param filePath the path where the resource file to read resides
     */
    private void setTextFromResourceFile(JTextArea ta, String filePath) {
        InputStream is = getClass().getResourceAsStream(filePath);
        if (is == null) {
            displayErrorMessage(ta, tr("Failed to locate resource ''{0}''.", filePath));
        } else {
            try (InputStreamReader reader = new InputStreamReader(is, "UTF-8");
                 BufferedReader br = new BufferedReader(reader)) {
                String line;
                while ((line = br.readLine()) != null) {
                    ta.append(line+'\n');
                }
            } catch (IOException e) {
                Logging.warn(e);
                displayErrorMessage(ta, tr("Failed to load resource ''{0}'', error is {1}.", filePath, e.toString()));
            }
        }
    }

    private static void displayErrorMessage(JTextArea ta, String msg) {
        Logging.warn(msg);
        ta.setForeground(new Color(200, 0, 0));
        ta.setText(msg);
    }

    private static JScrollPane createScrollPane(JosmTextArea area) {
        area.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        area.setOpaque(false);
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(null);
        sp.setOpaque(false);
        return sp;
    }
}
