// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.widgets.ChangesetIdTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;

/**
 * This panel allows to enter the ID of single changeset and to download
 * the respective changeset.
 *
 */
public class SingleChangesetDownloadPanel extends JPanel {

    private ChangesetIdTextField tfChangesetId;
    private DownloadAction actDownload;

    protected void build() {
        setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
        setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.GRAY),
                        BorderFactory.createEmptyBorder(0,3,0,3)
                )
        );

        add(new JLabel(tr("Changeset ID: ")));
        add(tfChangesetId = new ChangesetIdTextField());
        tfChangesetId.setToolTipText(tr("Enter a changeset id"));
        SelectAllOnFocusGainedDecorator.decorate(tfChangesetId);

        actDownload = new DownloadAction();
        SideButton btn = new SideButton(actDownload);
        tfChangesetId.addActionListener(actDownload);
        tfChangesetId.getDocument().addDocumentListener(actDownload);
        add(btn);

        if (Main.pref.getBoolean("downloadchangeset.autopaste", true)) {
            tfChangesetId.tryToPasteFromClipboard();
        }
    }

    /**
     * Constructs a new {@link SingleChangesetDownloadPanel}
     */
    public SingleChangesetDownloadPanel() {
        build();
    }

    /**
     * Replies the changeset id entered in this panel. 0 if no changeset id
     * or an invalid changeset id is currently entered.
     *
     * @return the changeset id entered in this panel
     */
    public int getChangesetId() {
        return tfChangesetId.getChangesetId();
    }

    /**
     * Downloads the single changeset from the OSM API
     */
    class DownloadAction extends AbstractAction implements DocumentListener{

        public DownloadAction() {
            putValue(SMALL_ICON, ChangesetCacheManager.DOWNLOAD_CONTENT_ICON);
            putValue(SHORT_DESCRIPTION, tr("Download the changeset with the specified id, including the changeset content"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (!isEnabled())
                return;
            int id = getChangesetId();
            if (id == 0) return;
            ChangesetContentDownloadTask task =  new ChangesetContentDownloadTask(
                    SingleChangesetDownloadPanel.this,
                    Collections.singleton(id)
            );
            ChangesetCacheManager.getInstance().runDownloadTask(task);
        }

        protected void updateEnabledState() {
            setEnabled(tfChangesetId.readIds());
        }

        @Override
        public void changedUpdate(DocumentEvent arg0) {
            updateEnabledState();
        }

        @Override
        public void insertUpdate(DocumentEvent arg0) {
            updateEnabledState();
        }

        @Override
        public void removeUpdate(DocumentEvent arg0) {
            updateEnabledState();
        }
    }
}
