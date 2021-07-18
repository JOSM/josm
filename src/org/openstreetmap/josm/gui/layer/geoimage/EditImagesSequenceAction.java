// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.gpx.GpxImageCorrelation;
import org.openstreetmap.josm.data.gpx.GpxImageCorrelationSettings;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.geoimage.CorrelateGpxWithImages.RepaintTheMapListener;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Edit a sequence of geo-located images.
 * @since 18065
 */
public class EditImagesSequenceAction extends JosmAction {

    private final transient GeoImageLayer yLayer;
    private final ImageDirectionPositionPanel pDirectionPosition;

    /**
     * Constructs a new {@code EditImagesSequenceAction}.
     * @param layer The image layer
     */
    public EditImagesSequenceAction(GeoImageLayer layer) {
        super(tr("Edit images sequence"), "dialogs/geoimage/gpx2img", "geoimage_editsequence",
            Shortcut.registerShortcut("geoimage:editsequence", tr("Edit images sequence"), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE),
            false);
        this.yLayer = Objects.requireNonNull(layer);
        this.pDirectionPosition = ImageDirectionPositionPanel.forImageSequence();

        pDirectionPosition.addFocusListenerOnComponent(new RepaintTheMapListener(yLayer));
        pDirectionPosition.addChangeListenerOnComponents(new Updater());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ExtendedDialog ed = new ExtendedDialog(
                MainApplication.getMainFrame(), tr("Edit images sequence"), tr("Apply"), tr("Cancel"))
            .setContent(pDirectionPosition).setButtonIcons("ok", "cancel");
        ed.setResizable(false);
        if (ed.showDialog().getValue() == 1) {
            yLayer.applyTmp();
        } else {
            yLayer.discardTmp();
        }
        yLayer.updateBufferAndRepaint();
    }

    class Updater implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            matchAndUpdate();
        }

        void matchAndUpdate() {
            // The selection of images we are about to correlate may have changed.
            // So reset all images.
            yLayer.discardTmp();
            // Construct a list of images that have a date, and sort them on the date.
            List<ImageEntry> dateImgLst = yLayer.getSortedImgList(true, true);
            // Create a temporary copy for each image
            dateImgLst.forEach(ie -> ie.createTmp().unflagNewGpsData());
            GpxImageCorrelation.matchGpxTrack(dateImgLst, yLayer.getFauxGpxData(),
                            new GpxImageCorrelationSettings(0, false, pDirectionPosition.getSettings()));
            yLayer.updateBufferAndRepaint();
        }
    }
}
