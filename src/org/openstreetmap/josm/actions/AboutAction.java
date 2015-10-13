// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.BugReportExceptionHandler;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Nice about screen.
 *
 * The REVISION resource is read and if present, it shows the revision information of the jar-file.
 *
 * @author imi
 */
public class AboutAction extends JosmAction {

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
        readme.setEditable(false);
        readme.setText(Version.loadResourceFile(Main.class.getResource("/README")));
        readme.setCaretPosition(0);

        JosmTextArea revision = new JosmTextArea();
        revision.setEditable(false);
        revision.setText(version.getReleaseAttributes());
        revision.setCaretPosition(0);

        JosmTextArea contribution = new JosmTextArea();
        contribution.setEditable(false);
        contribution.setText(Version.loadResourceFile(Main.class.getResource("/CONTRIBUTION")));
        contribution.setCaretPosition(0);

        JosmTextArea license = new JosmTextArea();
        license.setEditable(false);
        license.setText(Version.loadResourceFile(Main.class.getResource("/LICENSE")));
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
        info.add(new JLabel(tr("Bug Reports")), GBC.std().insets(10, 0, 10, 0));
        info.add(BugReportExceptionHandler.getBugReportUrlLabel(Utils.strip(ShowStatusReportAction.getReportHeader())),
                GBC.eol().fill(GBC.HORIZONTAL));

        about.addTab(tr("Info"), info);
        about.addTab(tr("Readme"), createScrollPane(readme));
        about.addTab(tr("Revision"), createScrollPane(revision));
        about.addTab(tr("Contribution"), createScrollPane(contribution));
        about.addTab(tr("License"), createScrollPane(license));
        about.addTab(tr("Plugins"), new JScrollPane(PluginHandler.getInfoPanel()));

        // Intermediate panel to allow proper optionPane resizing
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(600, 300));
        panel.add(about, GBC.std().fill());

        GuiHelper.prepareResizeableOptionPane(panel, panel.getPreferredSize());
        JOptionPane.showMessageDialog(Main.parent, panel, tr("About JOSM..."), JOptionPane.INFORMATION_MESSAGE,
                new ImageIcon(ImageProvider.get("logo.svg").getImage().getScaledInstance(256, 258, Image.SCALE_SMOOTH)));
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
