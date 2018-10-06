// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bbox;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;

import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.josm.gui.widgets.PopupMenuButton;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Button that allows to choose the imagery source used for slippy map background.
 * @since 1390
 */
public class SourceButton extends PopupMenuButton {
    protected class TileSourceButtonModel extends JToggleButton.ToggleButtonModel implements ActionListener {
        protected final TileSource tileSource;

        public TileSourceButtonModel(TileSource tileSource) {
            this.tileSource = tileSource;
            this.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (SourceButton.this.slippyMapBBoxChooser.getTileController().getTileSource() != this.tileSource) { // prevent infinite recursion
                SourceButton.this.slippyMapBBoxChooser.toggleMapSource(this.tileSource);
            }
        }
    }

    protected final SlippyMapBBoxChooser slippyMapBBoxChooser;
    protected final ButtonModel showDownloadAreaButtonModel;
    private List<TileSource> sources;
    private ButtonGroup sourceButtonGroup;

    /**
     * Constructs a new {@code SourceButton}.
     * @param slippyMapBBoxChooser parent slippy map
     * @param sources list of imagery sources to display
     * @param showDownloadAreaButtonModel model for the "Show downloaded area" button
     * @since 12955
     */
    public SourceButton(
        SlippyMapBBoxChooser slippyMapBBoxChooser,
        Collection<TileSource> sources,
        ButtonModel showDownloadAreaButtonModel
    ) {
        super(new ImageProvider("dialogs/layerlist").getResource().getImageIcon(new Dimension(16, 16)));
        this.showDownloadAreaButtonModel = showDownloadAreaButtonModel;
        this.slippyMapBBoxChooser = slippyMapBBoxChooser;
        this.setPreferredSize(new Dimension(24, 24));
        this.setSources(sources);
    }

    protected void generatePopupMenu() {
        JPopupMenu pm = new JPopupMenu();
        this.sourceButtonGroup = new ButtonGroup();
        for (TileSource ts : this.sources) {
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(ts.getName());
            TileSourceButtonModel buttonModel = new TileSourceButtonModel(ts);
            menuItem.setModel(buttonModel);
            pm.add(menuItem);
            this.sourceButtonGroup.add(menuItem);

            // attempt to initialize button group matching current state of slippyMapBBoxChooser
            buttonModel.setSelected(this.slippyMapBBoxChooser.getTileController().getTileSource().getId().equals(ts.getId()));
        }

        pm.addSeparator();

        JCheckBoxMenuItem showDownloadAreaItem = new JCheckBoxMenuItem(tr("Show downloaded area"));
        showDownloadAreaItem.setModel(this.showDownloadAreaButtonModel);
        pm.add(showDownloadAreaItem);

        this.setPopupMenu(pm);
    }

    private void setSourceDefault() {
        Enumeration<AbstractButton> elems = this.sourceButtonGroup.getElements();
        if (elems.hasMoreElements()) {
            elems.nextElement().setSelected(true);
        }
    }

    /**
     * Set the tile sources.
     * @param sources The tile sources to display
     * @since 6364
     */
    public final void setSources(Collection<TileSource> sources) {
        this.sources = new ArrayList<>(Objects.requireNonNull(sources, "sources"));
        this.generatePopupMenu();
        if (this.sourceButtonGroup.getSelection() == null) {
            this.setSourceDefault();
        }
    }

    /**
     * Get the tile sources.
     * @return unmodifiable collection of tile sources
     */
    public final Collection<TileSource> getSources() {
        return Collections.unmodifiableCollection(this.sources);
    }

    /**
     * Get the currently-selected tile source.
     * @return currently-selected tile source
     */
    public final TileSource getCurrentSource() {
        TileSourceButtonModel buttonModel = (TileSourceButtonModel) this.sourceButtonGroup.getSelection();
        if (buttonModel != null) {
            return buttonModel.tileSource;
        }
        return null;
    }

    /**
     * Changes the current imagery source used for slippy map background.
     * @param tileSource the new imagery source to use
     */
    public void setCurrentMap(TileSource tileSource) {
        Enumeration<AbstractButton> elems = this.sourceButtonGroup.getElements();
        while (elems.hasMoreElements()) {
            AbstractButton b = elems.nextElement();
            if (((TileSourceButtonModel) b.getModel()).tileSource == tileSource) {
                b.setSelected(true);
                return;
            }
        }
        // failed to find the correct one
        this.setSourceDefault();
    }
}
