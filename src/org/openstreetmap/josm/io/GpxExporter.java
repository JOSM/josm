// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Calendar;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;

/**
 * Exports data to a .gpx file. Data may be native GPX or OSM data which will be converted.
 * @since 1949
 */
public class GpxExporter extends FileExporter implements GpxConstants {

    private static final String GPL_WARNING = "<html><font color='red' size='-2'>"
        + tr("Note: GPL is not compatible with the OSM license. Do not upload GPL licensed tracks.") + "</html>";

    /**
     * Constructs a new {@code GpxExporter}.
     */
    public GpxExporter() {
        super(GpxImporter.FILE_FILTER);
    }

    @Override
    public boolean acceptFile(File pathname, Layer layer) {
        if (!(layer instanceof OsmDataLayer) && !(layer instanceof GpxLayer))
            return false;
        return super.acceptFile(pathname, layer);
    }

    @Override
    public void exportData(File file, Layer layer) throws IOException {
        CheckParameterUtil.ensureParameterNotNull(layer, "layer");
        if (!(layer instanceof OsmDataLayer) && !(layer instanceof GpxLayer))
            throw new IllegalArgumentException(MessageFormat.format("Expected instance of OsmDataLayer or GpxLayer. Got ''{0}''.", layer
                    .getClass().getName()));
        CheckParameterUtil.ensureParameterNotNull(file, "file");

        String fn = file.getPath();
        if (fn.indexOf('.') == -1) {
            fn += ".gpx";
            file = new File(fn);
        }

        // open the dialog asking for options
        JPanel p = new JPanel(new GridBagLayout());

        GpxData gpxData;
        // At this moment, we only need to know the attributes of the GpxData,
        // conversion of OsmDataLayer (if needed) will be done after the dialog is closed.
        if (layer instanceof GpxLayer) {
            gpxData = ((GpxLayer) layer).data;
        } else {
            gpxData = new GpxData();
        }

        p.add(new JLabel(tr("GPS track description")), GBC.eol());
        JosmTextArea desc = new JosmTextArea(3, 40);
        desc.setWrapStyleWord(true);
        desc.setLineWrap(true);
        desc.setText(gpxData.getString(META_DESC));
        p.add(new JScrollPane(desc), GBC.eop().fill(GBC.BOTH));

        JCheckBox author = new JCheckBox(tr("Add author information"), Main.pref.getBoolean("lastAddAuthor", true));
        p.add(author, GBC.eol());

        JLabel nameLabel = new JLabel(tr("Real name"));
        p.add(nameLabel, GBC.std().insets(10, 0, 5, 0));
        JosmTextField authorName = new JosmTextField();
        p.add(authorName, GBC.eol().fill(GBC.HORIZONTAL));
        nameLabel.setLabelFor(authorName);

        JLabel emailLabel = new JLabel(tr("E-Mail"));
        p.add(emailLabel, GBC.std().insets(10, 0, 5, 0));
        JosmTextField email = new JosmTextField();
        p.add(email, GBC.eol().fill(GBC.HORIZONTAL));
        emailLabel.setLabelFor(email);

        JLabel copyrightLabel = new JLabel(tr("Copyright (URL)"));
        p.add(copyrightLabel, GBC.std().insets(10, 0, 5, 0));
        JosmTextField copyright = new JosmTextField();
        p.add(copyright, GBC.std().fill(GBC.HORIZONTAL));
        copyrightLabel.setLabelFor(copyright);

        JButton predefined = new JButton(tr("Predefined"));
        p.add(predefined, GBC.eol().insets(5, 0, 0, 0));

        JLabel copyrightYearLabel = new JLabel(tr("Copyright year"));
        p.add(copyrightYearLabel, GBC.std().insets(10, 0, 5, 5));
        JosmTextField copyrightYear = new JosmTextField("");
        p.add(copyrightYear, GBC.eol().fill(GBC.HORIZONTAL));
        copyrightYearLabel.setLabelFor(copyrightYear);

        JLabel warning = new JLabel("<html><font size='-2'>&nbsp;</html");
        p.add(warning, GBC.eol().fill(GBC.HORIZONTAL).insets(15, 0, 0, 0));
        addDependencies(gpxData, author, authorName, email, copyright, predefined, copyrightYear, nameLabel, emailLabel,
                copyrightLabel, copyrightYearLabel, warning);

        p.add(new JLabel(tr("Keywords")), GBC.eol());
        JosmTextField keywords = new JosmTextField();
        keywords.setText(gpxData.getString(META_KEYWORDS));
        p.add(keywords, GBC.eop().fill(GBC.HORIZONTAL));

        ExtendedDialog ed = new ExtendedDialog(Main.parent,
                tr("Export options"),
                new String[] { tr("Export and Save"), tr("Cancel") });
        ed.setButtonIcons(new String[] { "exportgpx", "cancel" });
        ed.setContent(p);
        ed.showDialog();

        if (ed.getValue() != 1) {
            setCanceled(true);
            return;
        }
        setCanceled(false);

        Main.pref.put("lastAddAuthor", author.isSelected());
        if (authorName.getText().length() != 0) {
            Main.pref.put("lastAuthorName", authorName.getText());
        }
        if (copyright.getText().length() != 0) {
            Main.pref.put("lastCopyright", copyright.getText());
        }

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
                gpxData.put(META_AUTHOR_NAME, authorName.getText());
                gpxData.put(META_COPYRIGHT_AUTHOR, authorName.getText());
            }
            if (email.getText().length() > 0) {
                gpxData.put(META_AUTHOR_EMAIL, email.getText());
            }
            if (copyright.getText().length() > 0) {
                gpxData.put(META_COPYRIGHT_LICENSE, copyright.getText());
            }
            if (copyrightYear.getText().length() > 0) {
                gpxData.put(META_COPYRIGHT_YEAR, copyrightYear.getText());
            }
        }

        // add the description to the gpx data
        if (desc.getText().length() > 0) {
            gpxData.put(META_DESC, desc.getText());
        }

        // add keywords to the gpx data
        if (keywords.getText().length() > 0) {
            gpxData.put(META_KEYWORDS, keywords.getText());
        }

        try (OutputStream fo = Compression.getCompressedFileOutputStream(file)) {
            new GpxWriter(fo).write(gpxData);
            fo.flush();
        } catch (IOException x) {
            Main.error(x);
            JOptionPane.showMessageDialog(Main.parent, tr("Error while exporting {0}:\n{1}", fn, x.getMessage()),
                    tr("Error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void enableCopyright(final GpxData data, final JosmTextField copyright, final JButton predefined,
            final JosmTextField copyrightYear, final JLabel copyrightLabel, final JLabel copyrightYearLabel,
            final JLabel warning, boolean enable) {
        copyright.setEnabled(enable);
        predefined.setEnabled(enable);
        copyrightYear.setEnabled(enable);
        copyrightLabel.setEnabled(enable);
        copyrightYearLabel.setEnabled(enable);
        warning.setText(enable ? GPL_WARNING : "<html><font size='-2'>&nbsp;</html");

        if (enable) {
            if (copyrightYear.getText().isEmpty()) {
                String sCopyrightYear = data.getString(META_COPYRIGHT_YEAR);
                if (sCopyrightYear == null) {
                    sCopyrightYear = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
                }
                copyrightYear.setText(sCopyrightYear);
            }
            if (copyright.getText().isEmpty()) {
                String sCopyright = data.getString(META_COPYRIGHT_LICENSE);
                if (sCopyright == null) {
                    sCopyright = Main.pref.get("lastCopyright", "https://creativecommons.org/licenses/by-sa/2.5");
                }
                copyright.setText(sCopyright);
                copyright.setCaretPosition(0);
            }
        } else {
            copyrightYear.setText("");
            copyright.setText("");
        }
    }

    /**
     * Add all those listeners to handle the enable state of the fields.
     */
    private static void addDependencies(
            final GpxData data,
            final JCheckBox author,
            final JosmTextField authorName,
            final JosmTextField email,
            final JosmTextField copyright,
            final JButton predefined,
            final JosmTextField copyrightYear,
            final JLabel nameLabel,
            final JLabel emailLabel,
            final JLabel copyrightLabel,
            final JLabel copyrightYearLabel,
            final JLabel warning) {

        ActionListener authorActionListener = new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean b = author.isSelected();
                authorName.setEnabled(b);
                email.setEnabled(b);
                nameLabel.setEnabled(b);
                emailLabel.setEnabled(b);
                if (b) {
                    String sAuthorName = data.getString(META_AUTHOR_NAME);
                    if (sAuthorName == null) {
                        sAuthorName = Main.pref.get("lastAuthorName");
                    }
                    authorName.setText(sAuthorName);
                    String sEmail = data.getString(META_AUTHOR_EMAIL);
                    if (sEmail == null) {
                        sEmail = Main.pref.get("lastAuthorEmail");
                    }
                    email.setText(sEmail);
                } else {
                    authorName.setText("");
                    email.setText("");
                }
                boolean isAuthorSet = authorName.getText().length() != 0;
                GpxExporter.enableCopyright(data, copyright, predefined, copyrightYear, copyrightLabel, copyrightYearLabel, warning, b && isAuthorSet);
            }
        };
        author.addActionListener(authorActionListener);

        KeyAdapter authorNameListener = new KeyAdapter(){
            @Override public void keyReleased(KeyEvent e) {
                boolean b = authorName.getText().length()!=0 && author.isSelected();
                GpxExporter.enableCopyright(data, copyright, predefined, copyrightYear, copyrightLabel, copyrightYearLabel, warning, b);
            }
        };
        authorName.addKeyListener(authorNameListener);

        predefined.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                final String[] licenses = {
                        "Creative Commons By-SA",
                        "Open Database License (ODbL)",
                        "public domain",
                        "GNU Lesser Public License (LGPL)",
                        "BSD License (MIT/X11)"};
                JList<String> l = new JList<>(licenses);
                l.setVisibleRowCount(licenses.length);
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
                        "https://creativecommons.org/licenses/by-sa/3.0",
                        "http://opendatacommons.org/licenses/odbl/1.0",
                        "public domain",
                        "https://www.gnu.org/copyleft/lesser.html",
                        "http://www.opensource.org/licenses/bsd-license.php"};
                String license = "";
                for (int i : l.getSelectedIndices()) {
                    if (i == 2) {
                        license = "public domain";
                        break;
                    }
                    license += license.isEmpty() ? urls[i] : ", "+urls[i];
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
