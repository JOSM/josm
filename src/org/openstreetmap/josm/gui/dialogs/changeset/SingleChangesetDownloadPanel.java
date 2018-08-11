// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.actions.downloadtasks.ChangesetContentDownloadTask;
import org.openstreetmap.josm.gui.widgets.ChangesetIdTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This panel allows to enter the ID of single changeset and to download
 * the respective changeset.
 * @since 2689
 */
public class SingleChangesetDownloadPanel extends JPanel {

    private final ChangesetIdTextField tfChangesetId = new ChangesetIdTextField();

    /**
     * Constructs a new {@link SingleChangesetDownloadPanel}
     */
    public SingleChangesetDownloadPanel() {
        build();
    }

    protected void build() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.GRAY),
                        BorderFactory.createEmptyBorder(0, 3, 0, 3)
                )
        );

        add(new JLabel(tr("Changeset ID: ")));
        add(tfChangesetId);
        tfChangesetId.setToolTipText(tr("Enter a changeset id"));
        SelectAllOnFocusGainedDecorator.decorate(tfChangesetId);

        DownloadAction actDownload = new DownloadAction();
        JButton btn = new JButton(actDownload);
        tfChangesetId.addActionListener(actDownload);
        tfChangesetId.getDocument().addDocumentListener(actDownload);
        add(btn);

        if (Config.getPref().getBoolean("downloadchangeset.autopaste", true)) {
            tfChangesetId.tryToPasteFromClipboard();
        }
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
    class DownloadAction extends AbstractAction implements DocumentListener {

        DownloadAction() {
            new ImageProvider("dialogs/changeset", "downloadchangesetcontent").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Download the changeset with the specified id, including the changeset content"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            int id = getChangesetId();
            if (id == 0)
                return;
            ChangesetContentDownloadTask task = new ChangesetContentDownloadTask(
                    SingleChangesetDownloadPanel.this,
                    Collections.singleton(id)
            );
            ChangesetCacheManager.getInstance().runDownloadTask(task);
        }

        protected void updateEnabledState() {
            setEnabled(tfChangesetId.readIds() && !NetworkManager.isOffline(OnlineResource.OSM_API));
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            updateEnabledState();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            updateEnabledState();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updateEnabledState();
        }
    }
}
