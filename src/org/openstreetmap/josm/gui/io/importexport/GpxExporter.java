// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.time.Year;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.GpxWriter;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;

/**
 * Exports data to a .gpx file. Data may be native GPX or OSM data which will be converted.
 * @since 1949
 */
public class GpxExporter extends FileExporter implements GpxConstants {

    private static final String GPL_WARNING = "<html><font color='red' size='-2'>"
        + tr("Note: GPL is not compatible with the OSM license. Do not upload GPL licensed tracks.") + "</html>";

    private static final String[] LICENSES = {
            "Creative Commons By-SA",
            "Open Database License (ODbL)",
            "public domain",
            "GNU Lesser Public License (LGPL)",
            "BSD License (MIT/X11)"};

    private static final String[] URLS = {
            "https://creativecommons.org/licenses/by-sa/3.0",
            "http://opendatacommons.org/licenses/odbl/1.0",
            "public domain",
            "https://www.gnu.org/copyleft/lesser.html",
            "http://www.opensource.org/licenses/bsd-license.php"};

    /**
     * Constructs a new {@code GpxExporter}.
     */
    public GpxExporter() {
        super(GpxImporter.getFileFilter());
    }

    @Override
    public boolean acceptFile(File pathname, Layer layer) {
        if (!(layer instanceof OsmDataLayer) && !(layer instanceof GpxLayer))
            return false;
        return super.acceptFile(pathname, layer);
    }

    @Override
    public void exportData(File file, Layer layer) throws IOException {
        exportData(file, layer, false);
    }

    @Override
    public void exportDataQuiet(File file, Layer layer) throws IOException {
        exportData(file, layer, true);
    }

    private void exportData(File file, Layer layer, boolean quiet) throws IOException {
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

        GpxData gpxData;
        if (quiet) {
            gpxData = getGpxData(layer, file);
            try (OutputStream fo = Compression.getCompressedFileOutputStream(file)) {
                GpxWriter w = new GpxWriter(fo);
                w.write(gpxData);
                w.close();
                fo.flush();
            }
            return;
        }

        // open the dialog asking for options
        JPanel p = new JPanel(new GridBagLayout());

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

        JCheckBox author = new JCheckBox(tr("Add author information"), Config.getPref().getBoolean("lastAddAuthor", true));
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
        p.add(keywords, GBC.eol().fill(GBC.HORIZONTAL));

        boolean sel = Config.getPref().getBoolean("gpx.export.colors", true);
        JCheckBox colors = new JCheckBox(tr("Save track colors in GPX file"), sel);
        p.add(colors, GBC.eol().fill(GBC.HORIZONTAL));
        JCheckBox garmin = new JCheckBox(tr("Use Garmin compatible GPX extensions"),
                Config.getPref().getBoolean("gpx.export.colors.garmin", false));
        garmin.setEnabled(sel);
        p.add(garmin, GBC.eol().fill(GBC.HORIZONTAL).insets(20, 0, 0, 0));

        boolean hasPrefs = !gpxData.getLayerPrefs().isEmpty();
        JCheckBox layerPrefs = new JCheckBox(tr("Save layer specific preferences"),
                hasPrefs && Config.getPref().getBoolean("gpx.export.prefs", true));
        layerPrefs.setEnabled(hasPrefs);
        p.add(layerPrefs, GBC.eop().fill(GBC.HORIZONTAL));

        ExtendedDialog ed = new ExtendedDialog(MainApplication.getMainFrame(),
                tr("Export options"),
                tr("Export and Save"), tr("Cancel"))
            .setButtonIcons("exportgpx", "cancel")
            .setContent(p);

        colors.addActionListener(l -> {
            garmin.setEnabled(colors.isSelected());
        });

        garmin.addActionListener(l -> {
            if (garmin.isSelected() &&
                    !ConditionalOptionPaneUtil.showConfirmationDialog(
                            "gpx_color_garmin",
                            ed,
                            new JLabel(tr("<html>Garmin track extensions only support 16 colors.<br>If you continue, the closest supported track color will be used.</html>")),
                            tr("Information"),
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            JOptionPane.OK_OPTION)) {
                garmin.setSelected(false);
            }
        });

        if (ed.showDialog().getValue() != 1) {
            setCanceled(true);
            return;
        }
        setCanceled(false);

        Config.getPref().putBoolean("lastAddAuthor", author.isSelected());
        if (!authorName.getText().isEmpty()) {
            Config.getPref().put("lastAuthorName", authorName.getText());
        }
        if (!copyright.getText().isEmpty()) {
            Config.getPref().put("lastCopyright", copyright.getText());
        }
        Config.getPref().putBoolean("gpx.export.colors", colors.isSelected());
        Config.getPref().putBoolean("gpx.export.colors.garmin", garmin.isSelected());
        if (hasPrefs) {
            Config.getPref().putBoolean("gpx.export.prefs", layerPrefs.isSelected());
        }
        ColorFormat cFormat = null;
        if (colors.isSelected()) {
            cFormat = garmin.isSelected() ? ColorFormat.GPXX : ColorFormat.GPXD;
        }

        gpxData = getGpxData(layer, file);

        // add author and copyright details to the gpx data
        if (author.isSelected()) {
            if (!authorName.getText().isEmpty()) {
                gpxData.put(META_AUTHOR_NAME, authorName.getText());
                gpxData.put(META_COPYRIGHT_AUTHOR, authorName.getText());
            }
            if (!email.getText().isEmpty()) {
                gpxData.put(META_AUTHOR_EMAIL, email.getText());
            }
            if (!copyright.getText().isEmpty()) {
                gpxData.put(META_COPYRIGHT_LICENSE, copyright.getText());
            }
            if (!copyrightYear.getText().isEmpty()) {
                gpxData.put(META_COPYRIGHT_YEAR, copyrightYear.getText());
            }
        }

        // add the description to the gpx data
        if (!desc.getText().isEmpty()) {
            gpxData.put(META_DESC, desc.getText());
        }

        // add keywords to the gpx data
        if (!keywords.getText().isEmpty()) {
            gpxData.put(META_KEYWORDS, keywords.getText());
        }

        try (OutputStream fo = Compression.getCompressedFileOutputStream(file)) {
            GpxWriter w = new GpxWriter(fo);
            w.write(gpxData, cFormat, layerPrefs.isSelected());
            w.close();
            fo.flush();
        }
    }

    private static GpxData getGpxData(Layer layer, File file) {
        if (layer instanceof OsmDataLayer) {
            return ((OsmDataLayer) layer).toGpxData();
        } else if (layer instanceof GpxLayer) {
            return ((GpxLayer) layer).data;
        }
        return OsmDataLayer.toGpxData(MainApplication.getLayerManager().getEditDataSet(), file);
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
                copyrightYear.setText(Optional.ofNullable(data.getString(META_COPYRIGHT_YEAR)).orElseGet(
                        () -> Year.now().toString()));
            }
            if (copyright.getText().isEmpty()) {
                copyright.setText(Optional.ofNullable(data.getString(META_COPYRIGHT_LICENSE)).orElseGet(
                        () -> Config.getPref().get("lastCopyright", "https://creativecommons.org/licenses/by-sa/2.5")));
                copyright.setCaretPosition(0);
            }
        } else {
            copyrightYear.setText("");
            copyright.setText("");
        }
    }

    // CHECKSTYLE.OFF: ParameterNumber

    /**
     * Add all those listeners to handle the enable state of the fields.
     * @param data GPX data
     * @param author Author checkbox
     * @param authorName Author name textfield
     * @param email E-mail textfield
     * @param copyright Copyright textfield
     * @param predefined Predefined button
     * @param copyrightYear Copyright year textfield
     * @param nameLabel Name label
     * @param emailLabel E-mail label
     * @param copyrightLabel Copyright label
     * @param copyrightYearLabel Copyright year label
     * @param warning Warning label
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

        // CHECKSTYLE.ON: ParameterNumber
        ActionListener authorActionListener = e -> {
            boolean b = author.isSelected();
            authorName.setEnabled(b);
            email.setEnabled(b);
            nameLabel.setEnabled(b);
            emailLabel.setEnabled(b);
            if (b) {
                authorName.setText(Optional.ofNullable(data.getString(META_AUTHOR_NAME)).orElseGet(
                        () -> Config.getPref().get("lastAuthorName")));
                email.setText(Optional.ofNullable(data.getString(META_AUTHOR_EMAIL)).orElseGet(
                        () -> Config.getPref().get("lastAuthorEmail")));
            } else {
                authorName.setText("");
                email.setText("");
            }
            boolean isAuthorSet = !authorName.getText().isEmpty();
            GpxExporter.enableCopyright(data, copyright, predefined, copyrightYear, copyrightLabel, copyrightYearLabel, warning,
                    b && isAuthorSet);
        };
        author.addActionListener(authorActionListener);

        KeyAdapter authorNameListener = new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                boolean b = !authorName.getText().isEmpty() && author.isSelected();
                GpxExporter.enableCopyright(data, copyright, predefined, copyrightYear, copyrightLabel, copyrightYearLabel, warning, b);
            }
        };
        authorName.addKeyListener(authorNameListener);

        predefined.addActionListener(e -> {
            JList<String> l = new JList<>(LICENSES);
            l.setVisibleRowCount(LICENSES.length);
            l.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            int answer = JOptionPane.showConfirmDialog(
                    MainApplication.getMainFrame(),
                    new JScrollPane(l),
                    tr("Choose a predefined license"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (answer != JOptionPane.OK_OPTION || l.getSelectedIndex() == -1)
                return;
            StringBuilder license = new StringBuilder();
            for (int i : l.getSelectedIndices()) {
                if (i == 2) {
                    license = new StringBuilder("public domain");
                    break;
                }
                if (license.length() > 0) {
                    license.append(", ");
                }
                license.append(URLS[i]);
            }
            copyright.setText(license.toString());
            copyright.setCaretPosition(0);
        });

        authorActionListener.actionPerformed(null);
        authorNameListener.keyReleased(null);
    }
}
