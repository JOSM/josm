// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.IPreferences;
import org.openstreetmap.josm.tools.GBC;

/**
 * Dialog for advanced GPX correlation settings.
 * @since 18044 (extracted from {@link CorrelateGpxWithImages})
 */
public class AdvancedCorrelationSettingsDialog extends ExtendedDialog {

    private JCheckBox cInterpolSeg;
    private JCheckBox cInterpolSegTime;
    private JSpinner sInterpolSegTime;
    private JCheckBox cInterpolSegDist;
    private JSpinner sInterpolSegDist;
    private JCheckBox cTagSeg;
    private JCheckBox cTagSegTime;
    private JSpinner sTagSegTime;
    private JCheckBox cInterpolTrack;
    private JCheckBox cInterpolTrackTime;
    private JSpinner sInterpolTrackTime;
    private JCheckBox cInterpolTrackDist;
    private JSpinner sInterpolTrackDist;
    private JCheckBox cTagTrack;
    private JCheckBox cTagTrackTime;
    private JSpinner sTagTrackTime;
    private JCheckBox cForce;

    /**
     * Constructs a new {@code AdvancedCorrelationSettingsDialog}.
     * @param parent The parent element that will be used for position and maximum size
     * @param forceTags
     */
    public AdvancedCorrelationSettingsDialog(Component parent, boolean forceTags) {
        super(parent, tr("Advanced settings"), tr("OK"), tr("Cancel"));
        setButtonIcons("ok", "cancel");
        setContent(buildContent(forceTags));
    }

    private Component buildContent(boolean forceTags) {
        IPreferences s = Config.getPref();
        JPanel p = new JPanel(new GridBagLayout());

        Border border1 = BorderFactory.createEmptyBorder(0, 20, 0, 0);
        Border border2 = BorderFactory.createEmptyBorder(10, 0, 5, 0);
        Border border = BorderFactory.createEmptyBorder(0, 40, 0, 0);
        FlowLayout layout = new FlowLayout();

        JLabel l = new JLabel(tr("Segment settings"));
        l.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        p.add(l, GBC.eol());
        cInterpolSeg = new JCheckBox(tr("Interpolate between segments"), s.getBoolean("geoimage.seg.int", true));
        cInterpolSeg.setBorder(border1);
        p.add(cInterpolSeg, GBC.eol());

        cInterpolSegTime = new JCheckBox(tr("only when the segments are less than # minutes apart:"),
                s.getBoolean("geoimage.seg.int.time", true));
        sInterpolSegTime = new JSpinner(
                new SpinnerNumberModel(s.getInt("geoimage.seg.int.time.val", 60), 0, Integer.MAX_VALUE, 1));
        ((JSpinner.DefaultEditor) sInterpolSegTime.getEditor()).getTextField().setColumns(3);
        JPanel pInterpolSegTime = new JPanel(layout);
        pInterpolSegTime.add(cInterpolSegTime);
        pInterpolSegTime.add(sInterpolSegTime);
        pInterpolSegTime.setBorder(border);
        p.add(pInterpolSegTime, GBC.eol());

        cInterpolSegDist = new JCheckBox(tr("only when the segments are less than # meters apart:"),
                s.getBoolean("geoimage.seg.int.dist", true));
        sInterpolSegDist = new JSpinner(
                new SpinnerNumberModel(s.getInt("geoimage.seg.int.dist.val", 50), 0, Integer.MAX_VALUE, 1));
        ((JSpinner.DefaultEditor) sInterpolSegDist.getEditor()).getTextField().setColumns(3);
        JPanel pInterpolSegDist = new JPanel(layout);
        pInterpolSegDist.add(cInterpolSegDist);
        pInterpolSegDist.add(sInterpolSegDist);
        pInterpolSegDist.setBorder(border);
        p.add(pInterpolSegDist, GBC.eol());

        cTagSeg = new JCheckBox(tr("Tag images at the closest end of a segment, when not interpolated"),
                s.getBoolean("geoimage.seg.tag", true));
        cTagSeg.setBorder(border1);
        p.add(cTagSeg, GBC.eol());

        cTagSegTime = new JCheckBox(tr("only within # minutes of the closest trackpoint:"),
                s.getBoolean("geoimage.seg.tag.time", true));
        sTagSegTime = new JSpinner(
                new SpinnerNumberModel(s.getInt("geoimage.seg.tag.time.val", 2), 0, Integer.MAX_VALUE, 1));
        ((JSpinner.DefaultEditor) sTagSegTime.getEditor()).getTextField().setColumns(3);
        JPanel pTagSegTime = new JPanel(layout);
        pTagSegTime.add(cTagSegTime);
        pTagSegTime.add(sTagSegTime);
        pTagSegTime.setBorder(border);
        p.add(pTagSegTime, GBC.eol());

        l = new JLabel(tr("Track settings (note that multiple tracks can be in one GPX file)"));
        l.setBorder(border2);
        p.add(l, GBC.eol());
        cInterpolTrack = new JCheckBox(tr("Interpolate between tracks"), s.getBoolean("geoimage.trk.int", false));
        cInterpolTrack.setBorder(border1);
        p.add(cInterpolTrack, GBC.eol());

        cInterpolTrackTime = new JCheckBox(tr("only when the tracks are less than # minutes apart:"),
                s.getBoolean("geoimage.trk.int.time", false));
        sInterpolTrackTime = new JSpinner(
                new SpinnerNumberModel(s.getInt("geoimage.trk.int.time.val", 60), 0, Integer.MAX_VALUE, 1));
        ((JSpinner.DefaultEditor) sInterpolTrackTime.getEditor()).getTextField().setColumns(3);
        JPanel pInterpolTrackTime = new JPanel(layout);
        pInterpolTrackTime.add(cInterpolTrackTime);
        pInterpolTrackTime.add(sInterpolTrackTime);
        pInterpolTrackTime.setBorder(border);
        p.add(pInterpolTrackTime, GBC.eol());

        cInterpolTrackDist = new JCheckBox(tr("only when the tracks are less than # meters apart:"),
                s.getBoolean("geoimage.trk.int.dist", false));
        sInterpolTrackDist = new JSpinner(
                new SpinnerNumberModel(s.getInt("geoimage.trk.int.dist.val", 50), 0, Integer.MAX_VALUE, 1));
        ((JSpinner.DefaultEditor) sInterpolTrackDist.getEditor()).getTextField().setColumns(3);
        JPanel pInterpolTrackDist = new JPanel(layout);
        pInterpolTrackDist.add(cInterpolTrackDist);
        pInterpolTrackDist.add(sInterpolTrackDist);
        pInterpolTrackDist.setBorder(border);
        p.add(pInterpolTrackDist, GBC.eol());

        cTagTrack = new JCheckBox("<html>" +
                tr("Tag images at the closest end of a track, when not interpolated<br>" +
                "(also applies before the first and after the last track)") + "</html>",
                s.getBoolean("geoimage.trk.tag", true));
        cTagTrack.setBorder(border1);
        p.add(cTagTrack, GBC.eol());

        cTagTrackTime = new JCheckBox(tr("only within # minutes of the closest trackpoint:"),
                s.getBoolean("geoimage.trk.tag.time", true));
        sTagTrackTime = new JSpinner(
                new SpinnerNumberModel(s.getInt("geoimage.trk.tag.time.val", 2), 0, Integer.MAX_VALUE, 1));
        ((JSpinner.DefaultEditor) sTagTrackTime.getEditor()).getTextField().setColumns(3);
        JPanel pTagTrackTime = new JPanel(layout);
        pTagTrackTime.add(cTagTrackTime);
        pTagTrackTime.add(sTagTrackTime);
        pTagTrackTime.setBorder(border);
        p.add(pTagTrackTime, GBC.eol());

        l = new JLabel(tr("Advanced"));
        l.setBorder(border2);
        p.add(l, GBC.eol());
        cForce = new JCheckBox("<html>" +
                tr("Force tagging of all pictures (temporarily overrides the settings above).") + "<br>" +
                tr("This option will not be saved permanently.") + "</html>", forceTags);
        cForce.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 0));
        p.add(cForce, GBC.eol());

        addCheckBoxActionListener(cInterpolSegTime, sInterpolSegTime);
        addCheckBoxActionListener(cInterpolSegDist, sInterpolSegDist);
        addCheckBoxActionListener(cInterpolSeg, pInterpolSegTime, pInterpolSegDist);

        addCheckBoxActionListener(cTagSegTime, sTagSegTime);
        addCheckBoxActionListener(cTagSeg, pTagSegTime);

        addCheckBoxActionListener(cInterpolTrackTime, sInterpolTrackTime);
        addCheckBoxActionListener(cInterpolTrackDist, sInterpolTrackDist);
        addCheckBoxActionListener(cInterpolTrack, pInterpolTrackTime, pInterpolTrackDist);

        addCheckBoxActionListener(cTagTrackTime, sTagTrackTime);
        addCheckBoxActionListener(cTagTrack, pTagTrackTime);

        return p;
    }

    @Override
    protected void buttonAction(int buttonIndex, ActionEvent evt) {
        super.buttonAction(buttonIndex, evt);
        if (buttonIndex == 0) {
            IPreferences s = Config.getPref();

            s.putBoolean("geoimage.seg.int", cInterpolSeg.isSelected());
            s.putBoolean("geoimage.seg.int.dist", cInterpolSegDist.isSelected());
            s.putInt("geoimage.seg.int.dist.val", (int) sInterpolSegDist.getValue());
            s.putBoolean("geoimage.seg.int.time", cInterpolSegTime.isSelected());
            s.putInt("geoimage.seg.int.time.val", (int) sInterpolSegTime.getValue());
            s.putBoolean("geoimage.seg.tag", cTagSeg.isSelected());
            s.putBoolean("geoimage.seg.tag.time", cTagSegTime.isSelected());
            s.putInt("geoimage.seg.tag.time.val", (int) sTagSegTime.getValue());

            s.putBoolean("geoimage.trk.int", cInterpolTrack.isSelected());
            s.putBoolean("geoimage.trk.int.dist", cInterpolTrackDist.isSelected());
            s.putInt("geoimage.trk.int.dist.val", (int) sInterpolTrackDist.getValue());
            s.putBoolean("geoimage.trk.int.time", cInterpolTrackTime.isSelected());
            s.putInt("geoimage.trk.int.time.val", (int) sInterpolTrackTime.getValue());
            s.putBoolean("geoimage.trk.tag", cTagTrack.isSelected());
            s.putBoolean("geoimage.trk.tag.time", cTagTrackTime.isSelected());
            s.putInt("geoimage.trk.tag.time.val", (int) sTagTrackTime.getValue());
        }
    }

    /**
     * Determines if the forced tagging of all pictures is selected.
     * @return {@code true} if the forced tagging of all pictures is selected
     */
    public boolean isForceTaggingSelected() {
        return cForce.isSelected();
    }

    protected static class CheckBoxActionListener implements ActionListener {
        private final JComponent[] comps;

        CheckBoxActionListener(JComponent... c) {
            comps = Objects.requireNonNull(c);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setEnabled((JCheckBox) e.getSource());
        }

        public void setEnabled(JCheckBox cb) {
            for (JComponent comp : comps) {
                if (comp instanceof JSpinner) {
                    comp.setEnabled(cb.isSelected());
                } else if (comp instanceof JPanel) {
                    boolean en = cb.isSelected();
                    for (Component c : comp.getComponents()) {
                        if (c instanceof JSpinner) {
                            c.setEnabled(en);
                        } else {
                            c.setEnabled(cb.isSelected());
                            if (en && c instanceof JCheckBox) {
                                en = ((JCheckBox) c).isSelected();
                            }
                        }
                    }
                }
            }
        }
    }

    protected static void addCheckBoxActionListener(JCheckBox cb, JComponent... c) {
        CheckBoxActionListener listener = new CheckBoxActionListener(c);
        cb.addActionListener(listener);
        listener.setEnabled(cb);
    }
}
