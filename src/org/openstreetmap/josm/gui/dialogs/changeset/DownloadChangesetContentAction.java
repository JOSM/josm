// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.actions.downloadtasks.ChangesetContentDownloadTask;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Downloads/Updates the content of the changeset.
 * @since 9493
 */
public class DownloadChangesetContentAction extends AbstractAction {
    private final transient ChangesetAware component;

    /**
     * Constructs a new {@code DownloadChangesetContentAction}.
     * @param component changeset-aware component
     */
    public DownloadChangesetContentAction(ChangesetAware component) {
        CheckParameterUtil.ensureParameterNotNull(component, "component");
        putValue(NAME, tr("Download content"));
        putValue(SMALL_ICON, ChangesetCacheManager.DOWNLOAD_CONTENT_ICON);
        putValue(SHORT_DESCRIPTION, tr("Download the changeset content from the OSM server"));
        this.component = component;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        if (component.getCurrentChangeset() != null) {
            ChangesetCacheManager.getInstance().runDownloadTask(new ChangesetContentDownloadTask(
                    (Component) component, component.getCurrentChangeset().getId()));
        }
    }

    /**
     * Init properties.
     */
    public void initProperties() {
        if (component.getCurrentChangeset() == null) {
            setEnabled(false);
            return;
        } else {
            setEnabled(true);
        }
        if (component.getCurrentChangeset().getContent() == null) {
            putValue(NAME, tr("Download content"));
            putValue(SMALL_ICON, ChangesetCacheManager.DOWNLOAD_CONTENT_ICON);
            putValue(SHORT_DESCRIPTION, tr("Download the changeset content from the OSM server"));
        } else {
            putValue(NAME, tr("Update content"));
            putValue(SMALL_ICON, ChangesetCacheManager.UPDATE_CONTENT_ICON);
            putValue(SHORT_DESCRIPTION, tr("Update the changeset content from the OSM server"));
        }
    }
}
