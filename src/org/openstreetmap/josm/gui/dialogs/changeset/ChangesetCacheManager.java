// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.JosmUserIdentityManager;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.changeset.query.ChangesetQueryDialog;
import org.openstreetmap.josm.gui.dialogs.changeset.query.ChangesetQueryTask;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.io.CloseChangesetTask;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * ChangesetCacheManager manages the local cache of changesets
 * retrieved from the OSM API. It displays both a table of the locally cached changesets
 * and detail information about an individual changeset. It also provides actions for
 * downloading, querying, closing changesets, in addition to removing changesets from
 * the local cache.
 *
 */
public class ChangesetCacheManager extends JFrame {

    /** The changeset download icon **/
    public static final ImageIcon DOWNLOAD_CONTENT_ICON = ImageProvider.get("dialogs/changeset", "downloadchangesetcontent");
    /** The changeset update icon **/
    public static final ImageIcon UPDATE_CONTENT_ICON   = ImageProvider.get("dialogs/changeset", "updatechangesetcontent");

    /** the unique instance of the cache manager  */
    private static ChangesetCacheManager instance;

    /**
     * Replies the unique instance of the changeset cache manager
     *
     * @return the unique instance of the changeset cache manager
     */
    public static ChangesetCacheManager getInstance() {
        if (instance == null) {
            instance = new ChangesetCacheManager();
        }
        return instance;
    }

    /**
     * Hides and destroys the unique instance of the changeset cache
     * manager.
     *
     */
    public static void destroyInstance() {
        if (instance != null) {
            instance.setVisible(true);
            instance.dispose();
            instance = null;
        }
    }

    private ChangesetCacheManagerModel model;
    private JSplitPane spContent;
    private boolean needsSplitPaneAdjustment;

    private RemoveFromCacheAction actRemoveFromCacheAction;
    private CloseSelectedChangesetsAction actCloseSelectedChangesetsAction;
    private DownloadSelectedChangesetsAction actDownloadSelectedChangesets;
    private DownloadSelectedChangesetContentAction actDownloadSelectedContent;
    private JTable tblChangesets;

    /**
     * Creates the various models required
     */
    protected void buildModel() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        model = new ChangesetCacheManagerModel(selectionModel);

        actRemoveFromCacheAction = new RemoveFromCacheAction();
        actCloseSelectedChangesetsAction = new CloseSelectedChangesetsAction();
        actDownloadSelectedChangesets = new DownloadSelectedChangesetsAction();
        actDownloadSelectedContent = new DownloadSelectedChangesetContentAction();
    }

    /**
     * builds the toolbar panel in the heading of the dialog
     *
     * @return the toolbar panel
     */
    protected JPanel buildToolbarPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));

        SideButton btn = new SideButton(new QueryAction());
        pnl.add(btn);
        pnl.add(new SingleChangesetDownloadPanel());
        pnl.add(new SideButton(new DownloadMyChangesets()));

        return pnl;
    }

    /**
     * builds the button panel in the footer of the dialog
     *
     * @return the button row pane
     */
    protected JPanel buildButtonPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));

        //-- cancel and close action
        pnl.add(new SideButton(new CancelAction()));

        //-- help action
        pnl.add(new SideButton(
                new ContextSensitiveHelpAction(
                        HelpUtil.ht("/Dialog/ChangesetCacheManager"))
        )
        );

        return pnl;
    }

    /**
     * Builds the panel with the changeset details
     *
     * @return the panel with the changeset details
     */
    protected JPanel buildChangesetDetailPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        JTabbedPane tp = new JTabbedPane();

        // -- add the details panel
        ChangesetDetailPanel pnlChangesetDetail = new ChangesetDetailPanel();
        tp.add(pnlChangesetDetail);
        model.addPropertyChangeListener(pnlChangesetDetail);

        // -- add the tags panel
        ChangesetTagsPanel pnlChangesetTags = new ChangesetTagsPanel();
        tp.add(pnlChangesetTags);
        model.addPropertyChangeListener(pnlChangesetTags);

        // -- add the panel for the changeset content
        ChangesetContentPanel pnlChangesetContent = new ChangesetContentPanel();
        tp.add(pnlChangesetContent);
        model.addPropertyChangeListener(pnlChangesetContent);

        tp.setTitleAt(0, tr("Properties"));
        tp.setToolTipTextAt(0, tr("Display the basic properties of the changeset"));
        tp.setTitleAt(1, tr("Tags"));
        tp.setToolTipTextAt(1, tr("Display the tags of the changeset"));
        tp.setTitleAt(2, tr("Content"));
        tp.setToolTipTextAt(2, tr("Display the objects created, updated, and deleted by the changeset"));

        pnl.add(tp, BorderLayout.CENTER);
        return pnl;
    }

    /**
     * builds the content panel of the dialog
     *
     * @return the content panel
     */
    protected JPanel buildContentPanel() {
        JPanel pnl = new JPanel(new BorderLayout());

        spContent = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        spContent.setLeftComponent(buildChangesetTablePanel());
        spContent.setRightComponent(buildChangesetDetailPanel());
        spContent.setOneTouchExpandable(true);
        spContent.setDividerLocation(0.5);

        pnl.add(spContent, BorderLayout.CENTER);
        return pnl;
    }

    /**
     * Builds the table with actions which can be applied to the currently visible changesets
     * in the changeset table.
     *
     * @return changset actions panel
     */
    protected JPanel buildChangesetTableActionPanel() {
        JPanel pnl = new JPanel(new BorderLayout());

        JToolBar tb = new JToolBar(JToolBar.VERTICAL);
        tb.setFloatable(false);

        // -- remove from cache action
        model.getSelectionModel().addListSelectionListener(actRemoveFromCacheAction);
        tb.add(actRemoveFromCacheAction);

        // -- close selected changesets action
        model.getSelectionModel().addListSelectionListener(actCloseSelectedChangesetsAction);
        tb.add(actCloseSelectedChangesetsAction);

        // -- download selected changesets
        model.getSelectionModel().addListSelectionListener(actDownloadSelectedChangesets);
        tb.add(actDownloadSelectedChangesets);

        // -- download the content of the selected changesets
        model.getSelectionModel().addListSelectionListener(actDownloadSelectedContent);
        tb.add(actDownloadSelectedContent);

        pnl.add(tb, BorderLayout.CENTER);
        return pnl;
    }

    /**
     * Builds the panel with the table of changesets
     *
     * @return the panel with the table of changesets
     */
    protected JPanel buildChangesetTablePanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        tblChangesets = new JTable(
                model,
                new ChangesetCacheTableColumnModel(),
                model.getSelectionModel()
        );
        tblChangesets.addMouseListener(new MouseEventHandler());
        tblChangesets.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "showDetails");
        tblChangesets.getActionMap().put("showDetails", new ShowDetailAction());
        model.getSelectionModel().addListSelectionListener(new ChangesetDetailViewSynchronizer());

        // activate DEL on the table
        tblChangesets.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0), "removeFromCache");
        tblChangesets.getActionMap().put("removeFromCache", actRemoveFromCacheAction);

        pnl.add(new JScrollPane(tblChangesets), BorderLayout.CENTER);
        pnl.add(buildChangesetTableActionPanel(), BorderLayout.WEST);
        return pnl;
    }

    protected void build() {
        setTitle(tr("Changeset Management Dialog"));
        setIconImage(ImageProvider.get("dialogs/changeset", "changesetmanager").getImage());
        Container cp = getContentPane();

        cp.setLayout(new BorderLayout());

        buildModel();
        cp.add(buildToolbarPanel(), BorderLayout.NORTH);
        cp.add(buildContentPanel(), BorderLayout.CENTER);
        cp.add(buildButtonPanel(), BorderLayout.SOUTH);

        // the help context
        HelpUtil.setHelpContext(getRootPane(), HelpUtil.ht("/Dialog/ChangesetCacheManager"));

        // make the dialog respond to ESC
        getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0), "cancelAndClose");
        getRootPane().getActionMap().put("cancelAndClose", new CancelAction());

        // install a window event handler
        addWindowListener(new WindowEventHandler());
    }

    /**
     * Constructs a new {@code ChangesetCacheManager}.
     */
    public ChangesetCacheManager() {
        build();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            getParent(),
                            new Dimension(1000,600)
                    )
            ).applySafe(this);
            needsSplitPaneAdjustment = true;
            model.init();

        } else if (isShowing()) { // Avoid IllegalComponentStateException like in #8775
            model.tearDown();
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    /**
     * Handler for window events
     *
     */
    class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            new CancelAction().cancelAndClose();
        }

        @Override
        public void windowActivated(WindowEvent arg0) {
            if (needsSplitPaneAdjustment) {
                spContent.setDividerLocation(0.5);
                needsSplitPaneAdjustment = false;
            }
        }
    }

    /**
     * the cancel / close action
     */
    static class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(NAME, tr("Close"));
            putValue(SMALL_ICON, ImageProvider.get("cancel"));
            putValue(SHORT_DESCRIPTION, tr("Close the dialog"));
        }

        public void cancelAndClose() {
            destroyInstance();
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            cancelAndClose();
        }
    }

    /**
     * The action to query and download changesets
     */
    class QueryAction extends AbstractAction {
        public QueryAction() {
            putValue(NAME, tr("Query"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs","search"));
            putValue(SHORT_DESCRIPTION, tr("Launch the dialog for querying changesets"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            ChangesetQueryDialog dialog = new ChangesetQueryDialog(ChangesetCacheManager.this);
            dialog.initForUserInput();
            dialog.setVisible(true);
            if (dialog.isCanceled())
                return;

            try {
                ChangesetQuery query = dialog.getChangesetQuery();
                if (query == null) return;
                ChangesetQueryTask task = new ChangesetQueryTask(ChangesetCacheManager.this, query);
                ChangesetCacheManager.getInstance().runDownloadTask(task);
            } catch (IllegalStateException e) {
                JOptionPane.showMessageDialog(ChangesetCacheManager.this, e.getMessage(), tr("Error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Removes the selected changesets from the local changeset cache
     *
     */
    class RemoveFromCacheAction extends AbstractAction implements ListSelectionListener{
        public RemoveFromCacheAction() {
            putValue(NAME, tr("Remove from cache"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
            putValue(SHORT_DESCRIPTION, tr("Remove the selected changesets from the local cache"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<Changeset> selected = model.getSelectedChangesets();
            ChangesetCache.getInstance().remove(selected);
        }

        protected void updateEnabledState() {
            setEnabled(model.hasSelectedChangesets());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();

        }
    }

    /**
     * Closes the selected changesets
     *
     */
    class CloseSelectedChangesetsAction extends AbstractAction implements ListSelectionListener{
        public CloseSelectedChangesetsAction() {
            putValue(NAME, tr("Close"));
            putValue(SMALL_ICON, ImageProvider.get("closechangeset"));
            putValue(SHORT_DESCRIPTION, tr("Close the selected changesets"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<Changeset> selected = model.getSelectedChangesets();
            Main.worker.submit(new CloseChangesetTask(selected));
        }

        protected void updateEnabledState() {
            List<Changeset> selected = model.getSelectedChangesets();
            JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();
            for (Changeset cs: selected) {
                if (cs.isOpen()) {
                    if (im.isPartiallyIdentified() && cs.getUser() != null && cs.getUser().getName().equals(im.getUserName())) {
                        setEnabled(true);
                        return;
                    }
                    if (im.isFullyIdentified() && cs.getUser() != null && cs.getUser().getId() == im.getUserId()) {
                        setEnabled(true);
                        return;
                    }
                }
            }
            setEnabled(false);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * Downloads the selected changesets
     *
     */
    class DownloadSelectedChangesetsAction extends AbstractAction implements ListSelectionListener{
        public DownloadSelectedChangesetsAction() {
            putValue(NAME, tr("Update changeset"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/changeset", "updatechangeset"));
            putValue(SHORT_DESCRIPTION, tr("Updates the selected changesets with current data from the OSM server"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<Changeset> selected = model.getSelectedChangesets();
            ChangesetHeaderDownloadTask task =ChangesetHeaderDownloadTask.buildTaskForChangesets(ChangesetCacheManager.this,selected);
            ChangesetCacheManager.getInstance().runDownloadTask(task);
        }

        protected void updateEnabledState() {
            setEnabled(model.hasSelectedChangesets());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * Downloads the content of selected changesets from the OSM server
     *
     */
    class DownloadSelectedChangesetContentAction extends AbstractAction implements ListSelectionListener{
        public DownloadSelectedChangesetContentAction() {
            putValue(NAME, tr("Download changeset content"));
            putValue(SMALL_ICON, DOWNLOAD_CONTENT_ICON);
            putValue(SHORT_DESCRIPTION, tr("Download the content of the selected changesets from the server"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            ChangesetContentDownloadTask task = new ChangesetContentDownloadTask(ChangesetCacheManager.this,model.getSelectedChangesetIds());
            ChangesetCacheManager.getInstance().runDownloadTask(task);
        }

        protected void updateEnabledState() {
            setEnabled(model.hasSelectedChangesets());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    class ShowDetailAction extends AbstractAction {

        public void showDetails() {
            List<Changeset> selected = model.getSelectedChangesets();
            if (selected.size() != 1) return;
            model.setChangesetInDetailView(selected.get(0));
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            showDetails();
        }
    }

    class DownloadMyChangesets extends AbstractAction {
        public DownloadMyChangesets() {
            putValue(NAME, tr("My changesets"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/changeset", "downloadchangeset"));
            putValue(SHORT_DESCRIPTION, tr("Download my changesets from the OSM server (max. 100 changesets)"));
        }

        protected void alertAnonymousUser() {
            HelpAwareOptionPane.showOptionDialog(
                    ChangesetCacheManager.this,
                    tr("<html>JOSM is currently running with an anonymous user. It cannot download<br>"
                            + "your changesets from the OSM server unless you enter your OSM user name<br>"
                            + "in the JOSM preferences.</html>"
                    ),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE,
                    HelpUtil.ht("/Dialog/ChangesetCacheManager#CanDownloadMyChangesets")
            );
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();
            if (im.isAnonymous()) {
                alertAnonymousUser();
                return;
            }
            ChangesetQuery query = new ChangesetQuery();
            if (im.isFullyIdentified()) {
                query = query.forUser(im.getUserId());
            } else {
                query = query.forUser(im.getUserName());
            }
            ChangesetQueryTask task = new ChangesetQueryTask(ChangesetCacheManager.this, query);
            ChangesetCacheManager.getInstance().runDownloadTask(task);
        }
    }

    class MouseEventHandler extends PopupMenuLauncher {

        public MouseEventHandler() {
            super(new ChangesetTablePopupMenu());
        }

        @Override
        public void mouseClicked(MouseEvent evt) {
            if (isDoubleClick(evt)) {
                new ShowDetailAction().showDetails();
            }
        }
    }

    class ChangesetTablePopupMenu extends JPopupMenu {
        public ChangesetTablePopupMenu() {
            add(actRemoveFromCacheAction);
            add(actCloseSelectedChangesetsAction);
            add(actDownloadSelectedChangesets);
            add(actDownloadSelectedContent);
        }
    }

    class ChangesetDetailViewSynchronizer implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            List<Changeset> selected = model.getSelectedChangesets();
            if (selected.size() == 1) {
                model.setChangesetInDetailView(selected.get(0));
            } else {
                model.setChangesetInDetailView(null);
            }
        }
    }

    /**
     * Selects the changesets  in <code>changests</code>, provided the
     * respective changesets are already present in the local changeset cache.
     *
     * @param changesets the collection of changesets. If {@code null}, the
     * selection is cleared.
     */
    public void setSelectedChangesets(Collection<Changeset> changesets) {
        model.setSelectedChangesets(changesets);
        int idx = model.getSelectionModel().getMinSelectionIndex();
        if (idx < 0) return;
        tblChangesets.scrollRectToVisible(tblChangesets.getCellRect(idx, 0, true));
        repaint();
    }

    /**
     * Selects the changesets with the ids in <code>ids</code>, provided the
     * respective changesets are already present in the local changeset cache.
     *
     * @param ids the collection of ids. If null, the selection is cleared.
     */
    public void setSelectedChangesetsById(Collection<Integer> ids) {
        if (ids == null) {
            setSelectedChangesets(null);
            return;
        }
        Set<Changeset> toSelect = new HashSet<Changeset>();
        ChangesetCache cc = ChangesetCache.getInstance();
        for (int id: ids) {
            if (cc.contains(id)) {
                toSelect.add(cc.get(id));
            }
        }
        setSelectedChangesets(toSelect);
    }

    /**
     * Runs the given changeset download task.
     * @param task The changeset download task to run
     */
    public void runDownloadTask(final ChangesetDownloadTask task) {
        Main.worker.submit(task);
        Main.worker.submit(new Runnable() {
            @Override
            public void run() {
                if (task.isCanceled() || task.isFailed()) return;
                setSelectedChangesets(task.getDownloadedChangesets());
            }
        });
    }
}
