// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import java.util.Objects;

import javax.swing.Icon;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.download.DownloadSourceSizingPolicy.FixedDownloadSourceSizePolicy;

/**
 * GUI representation of {@link DownloadSource} that is shown to the user in
 * {@link DownloadDialog}.
 * @param <T> The type of the data that a download source uses.
 * @since 12652
 */
public abstract class AbstractDownloadSourcePanel<T> extends JPanel {

    /**
     * A prefix to be used for tab height preferences
     */
    public static final String TAB_SPLIT_NAMESPACE = "download.tabsplit.";

    /**
     * Called when creating a new {@link AbstractDownloadSourcePanel} for the given download source
     * @param downloadSource The download source this panel is for
     */
    public AbstractDownloadSourcePanel(final DownloadSource<T> downloadSource) {
        Objects.requireNonNull(downloadSource);
        this.downloadSource = downloadSource;
    }

    /**
     * The download source of this panel.
     */
    protected transient DownloadSource<T> downloadSource;

    /**
     * Gets the data.
     * @return Returns the data.
     */
    public abstract T getData();

    /**
     * Gets the download source of this panel.
     * @return Returns the download source of this panel.
     */
    public DownloadSource<T> getDownloadSource() {
        return this.downloadSource;
    }

    /**
     * Saves the current user preferences devoted to the data source.
     */
    public abstract void rememberSettings();

    /**
     * Restores the latest user preferences devoted to the data source.
     */
    public abstract void restoreSettings();

    /**
     * Performs the logic needed in case if the user triggered the download
     * action in {@link DownloadDialog}.
     * @param settings The settings to check.
     * @return Returns {@code true} if the required procedure of handling the
     * download action succeeded and {@link DownloadDialog} can be closed, e.g. validation,
     * otherwise {@code false}.
     */
    public abstract boolean checkDownload(DownloadSettings settings);

    /**
     * Performs the logic needed in case if the user triggered the cancel
     * action in {@link DownloadDialog}.
     */
    public void checkCancel() {
        // nothing, let download dialog to close
        // override if necessary
    }

    /**
     * Gets the icon of the download source panel.
     * @return The icon. Can be {@code null} if there is no icon associated with
     * this download source.
     */
    public Icon getIcon() {
        return null;
    }

    /**
     * Updates GUI components of the panel according to the bbox changes.
     * @param bbox The new value for the bounding box.
     * @deprecated Use {@link #boundingBoxChanged} instead
     */
    @Deprecated
    public void boudingBoxChanged(Bounds bbox) {
        // override this if the panel must react on bbox changes
    }

    /**
     * Updates GUI components of the panel according to the bbox changes.
     * @param bbox The new value for the bounding box.
     * @since 13498
     */
    public void boundingBoxChanged(Bounds bbox) {
        // override this if the panel must react on bbox changes
    }

    /**
     * Tells the {@link DownloadSource} to start downloading
     * @param downloadSettings The download settings
     */
    public void triggerDownload(DownloadSettings downloadSettings) {
        getDownloadSource().doDownload(getData(), downloadSettings);
    }

    /**
     * Returns a simple name describing this panel. This string can be used from other GUI parts
     * of JOSM to save the user preferences related to the GUI settings. For example, the panel for downloading
     * the OSM data can be named 'downloadosmpanel'. Note, choose the name such that it is unique to avoid
     * collisions with other names.
     * @return A simple name describing this panel.
     */
    public abstract String getSimpleName();

    /**
     * Gets the policy that defines how this component should be sized
     * @return The sizing policy. A fixed policy on default.
     * @since 12705
     */
    public DownloadSourceSizingPolicy getSizingPolicy() {
        return new FixedDownloadSourceSizePolicy(this);
    }
}
