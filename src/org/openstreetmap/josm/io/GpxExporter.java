// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.GBC;

public class GpxExporter extends FileExporter {
    private final static String warningGpl = "<html><font color='red' size='-2'>"
        + tr("Note: GPL is not compatible with the OSM license. Do not upload GPL licensed tracks.") + "</html>";

    public GpxExporter() {
        super(new ExtensionFileFilter("gpx,gpx.gz", "gpx", tr("GPX Files") + " (*.gpx *.gpx.gz)"));
    }

    @Override
    public boolean acceptFile(File pathname, Layer layer) {
        if (!(layer instanceof OsmDataLayer) && !(layer instanceof GpxLayer))
            return false;
        return super.acceptFile(pathname, layer);
    }

    @Override
    public void exportData(File file, Layer layer) throws IOException {
        if (layer == null)
            throw new IllegalArgumentException(tr("paramenter ''{0}'' must not be null", "layer"));
        if (!(layer instanceof OsmDataLayer) && !(layer instanceof GpxLayer))
            throw new IllegalArgumentException(tr("expected instance of OsmDataLayer or GpxLayer. Got ''{0}''.", layer
                    .getClass().getName()));
        if (file == null)
            throw new IllegalArgumentException(tr("paramenter ''{0}'' must not be null", "file"));

        String fn = file.getPath();
        if (fn.indexOf('.') == -1) {
            fn += ".gpx";
            file = new File(fn);
        }

        // open the dialog asking for options
        JPanel p = new JPanel(new GridBagLayout());

        p.add(new JLabel(tr("gps track description")), GBC.eol());
        JTextArea desc = new JTextArea(3, 40);
        desc.setWrapStyleWord(true);
        desc.setLineWrap(true);
        p.add(new JScrollPane(desc), GBC.eop().fill(GBC.BOTH));

        JCheckBox author = new JCheckBox(tr("Add author information"), Main.pref.getBoolean("lastAddAuthor", true));
        author.setSelected(true);
        p.add(author, GBC.eol());
        JLabel nameLabel = new JLabel(tr("Real name"));
        p.add(nameLabel, GBC.std().insets(10, 0, 5, 0));
        JTextField authorName = new JTextField(Main.pref.get("lastAuthorName"));
        p.add(authorName, GBC.eol().fill(GBC.HORIZONTAL));
        JLabel emailLabel = new JLabel(tr("E-Mail"));
        p.add(emailLabel, GBC.std().insets(10, 0, 5, 0));
        JTextField email = new JTextField(Main.pref.get("osm-server.username"));
        p.add(email, GBC.eol().fill(GBC.HORIZONTAL));
        JLabel copyrightLabel = new JLabel(tr("Copyright (URL)"));
        p.add(copyrightLabel, GBC.std().insets(10, 0, 5, 0));
        JTextField copyright = new JTextField();
        p.add(copyright, GBC.std().fill(GBC.HORIZONTAL));
        JButton predefined = new JButton(tr("Predefined"));
        p.add(predefined, GBC.eol().insets(5, 0, 0, 0));
        JLabel copyrightYearLabel = new JLabel(tr("Copyright year"));
        p.add(copyrightYearLabel, GBC.std().insets(10, 0, 5, 5));
        JTextField copyrightYear = new JTextField("");
        p.add(copyrightYear, GBC.eol().fill(GBC.HORIZONTAL));
        JLabel warning = new JLabel("<html><font size='-2'>&nbsp;</html");
        p.add(warning, GBC.eol().fill(GBC.HORIZONTAL).insets(15, 0, 0, 0));
        addDependencies(author, authorName, email, copyright, predefined, copyrightYear, nameLabel, emailLabel,
                copyrightLabel, copyrightYearLabel, warning);

        // if the user name is not the email address, but the osm user name
        // move it from the email textfield to the author textfield
        if (!email.getText().contains("@")) {
            authorName.setText(email.getText());
            email.setText("");
        }

        p.add(new JLabel(tr("Keywords")), GBC.eol());
        JTextField keywords = new JTextField();
        p.add(keywords, GBC.eop().fill(GBC.HORIZONTAL));

        ExtendedDialog ed = new ExtendedDialog(Main.parent,
                tr("Export options"),
                new String[] { tr("Export and Save"), tr("Cancel") });
        ed.setButtonIcons(new String[] { "exportgpx.png", "cancel.png" });
        ed.setContent(p);
        ed.showDialog();

        if (ed.getValue() != 1)
            return;

        Main.pref.put("lastAddAuthor", author.isSelected());
        if (authorName.getText().length() != 0) {
            Main.pref.put("lastAuthorName", authorName.getText());
        }
        if (copyright.getText().length() != 0) {
            Main.pref.put("lastCopyright", copyright.getText());
        }

        GpxData gpxData;
        if (layer instanceof OsmDataLayer) {
            gpxData = ((OsmDataLayer) layer).toGpxData();
        } else if (layer instanceof GpxLayer) {
            gpxData = ((GpxLayer) layer).data;
        } else {
            gpxData = OsmDataLayer.toGpxData(getCurrentDataSet(), file);
        }

        // add author and copyright details to the gpx data
        if (author.isSelected()) {
            if (authorName.getText().length() > 0) {
                gpxData.attr.put(GpxData.META_AUTHOR_NAME, authorName.getText());
                gpxData.attr.put(GpxData.META_COPYRIGHT_AUTHOR, authorName.getText());
            }
            if (email.getText().length() > 0) {
                gpxData.attr.put(GpxData.META_AUTHOR_EMAIL, email.getText());
            }
            if (copyright.getText().length() > 0) {
                gpxData.attr.put(GpxData.META_COPYRIGHT_LICENSE, copyright.getText());
            }
            if (copyrightYear.getText().length() > 0) {
                gpxData.attr.put(GpxData.META_COPYRIGHT_YEAR, copyrightYear.getText());
            }
        }

        // add the description to the gpx data
        if (desc.getText().length() > 0) {
            gpxData.attr.put(GpxData.META_DESC, desc.getText());
        }

        // add keywords to the gpx data
        if (keywords.getText().length() > 0) {
            gpxData.attr.put(GpxData.META_KEYWORDS, keywords.getText());
        }

        try {
            FileOutputStream fo = new FileOutputStream(file);
            new GpxWriter(fo).write(gpxData);
            fo.flush();
            fo.close();
        } catch (IOException x) {
            x.printStackTrace();
            JOptionPane.showMessageDialog(Main.parent, tr("Error while exporting {0}:\n{1}", fn, x.getMessage()),
                    tr("Error"), JOptionPane.ERROR_MESSAGE);
        }

    }

    private static void enableCopyright(final JTextField copyright, final JButton predefined,
            final JTextField copyrightYear, final JLabel copyrightLabel, final JLabel copyrightYearLabel,
            final JLabel warning, boolean enable) {
        copyright.setEnabled(enable);
        predefined.setEnabled(enable);
        copyrightYear.setEnabled(enable);
        copyrightLabel.setEnabled(enable);
        copyrightYearLabel.setEnabled(enable);
        warning.setText(enable ? warningGpl : "<html><font size='-2'>&nbsp;</html");

        if (enable && copyrightYear.getText().length()==0) {
            copyrightYear.setText(enable ? Integer.toString(Calendar.getInstance().get(Calendar.YEAR)) : "");
        } else if (!enable) {
            copyrightYear.setText("");
        }

        if (enable && copyright.getText().length()==0) {
            copyright.setText(enable ? Main.pref.get("lastCopyright", "http://creativecommons.org/licenses/by-sa/2.5") : "");
            copyright.setCaretPosition(0);
        } else if (!enable) {
            copyright.setText("");
        }
    }

    /**
     * Add all those listeners to handle the enable state of the fields.
     * @param copyrightYearLabel
     * @param copyrightLabel
     * @param emailLabel
     * @param nameLabel
     * @param warning
     */
    private static void addDependencies(
            final JCheckBox author,
            final JTextField authorName,
            final JTextField email,
            final JTextField copyright,
            final JButton predefined,
            final JTextField copyrightYear,
            final JLabel nameLabel,
            final JLabel emailLabel,
            final JLabel copyrightLabel,
            final JLabel copyrightYearLabel,
            final JLabel warning) {

        ActionListener authorActionListener = new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                boolean b = author.isSelected();
                authorName.setEnabled(b);
                email.setEnabled(b);
                nameLabel.setEnabled(b);
                emailLabel.setEnabled(b);
                authorName.setText(b ? Main.pref.get("lastAuthorName") : "");
                email.setText(b ? Main.pref.get("osm-server.username") : "");

                boolean authorSet = authorName.getText().length() != 0;
                GpxExporter.enableCopyright(copyright, predefined, copyrightYear, copyrightLabel, copyrightYearLabel, warning, b && authorSet);
            }
        };
        author.addActionListener(authorActionListener);

        KeyAdapter authorNameListener = new KeyAdapter(){
            @Override public void keyReleased(KeyEvent e) {
                boolean b = authorName.getText().length()!=0 && author.isSelected();
                GpxExporter.enableCopyright(copyright, predefined, copyrightYear, copyrightLabel, copyrightYearLabel, warning, b);
            }
        };
        authorName.addKeyListener(authorNameListener);

        predefined.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                JList l = new JList(new String[]{"Creative Commons By-SA", "public domain", "GNU Lesser Public License (LGPL)", "BSD License (MIT/X11)"});
                l.setVisibleRowCount(4);
                l.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                int answer = JOptionPane.showConfirmDialog(
                        Main.parent,
                        new JScrollPane(l),
                        tr("Choose a predefined license"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (answer != JOptionPane.OK_OPTION || l.getSelectedIndex() == -1)
                    return;
                final String[] urls = {
                        "http://creativecommons.org/licenses/by-sa/2.5",
                        "public domain",
                        "http://www.gnu.org/copyleft/lesser.html",
                "http://www.opensource.org/licenses/bsd-license.php"};
                String license = "";
                for (int i : l.getSelectedIndices()) {
                    if (i == 1) {
                        license = "public domain";
                        break;
                    }
                    license += license.length()==0 ? urls[i] : ", "+urls[i];
                }
                copyright.setText(license);
                copyright.setCaretPosition(0);
            }
        });

        authorActionListener.actionPerformed(null);
        authorNameListener.keyReleased(null);
    }

    /**
     * Replies the current dataset
     *
     * @return the current dataset. null, if no current dataset exists
     */
    private DataSet getCurrentDataSet() {
        return Main.main.getCurrentDataSet();
    }

}
