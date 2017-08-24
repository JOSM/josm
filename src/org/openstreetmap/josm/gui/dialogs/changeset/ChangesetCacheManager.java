// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
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
import org.openstreetmap.josm.actions.downloadtasks.AbstractChangesetDownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.ChangesetContentDownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.ChangesetHeaderDownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.ChangesetQueryTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.ChangesetDataSet;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.JosmUserIdentityManager;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.changeset.query.ChangesetQueryDialog;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.io.CloseChangesetTask;
import org.openstreetmap.josm.gui.io.DownloadPrimitivesWithReferrersTask;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.StreamUtils;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * ChangesetCacheManager manages the local cache of changesets
 * retrieved from the OSM API. It displays both a table of the locally cached changesets
 * and detail information about an individual changeset. It also provides actions for
 * downloading, querying, closing changesets, in addition to removing changesets from
 * the local cache.
 * @since 2689
 */
public class ChangesetCacheManager extends JFrame {

    /** the unique instance of the cache manager  */
    private static volatile ChangesetCacheManager instance;
    private JTabbedPane pnlChangesetDetailTabs;

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
     * Hides and destroys the unique instance of the changeset cache manager.
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
    private DownloadSelectedChangesetObjectsAction actDownloadSelectedChangesetObjects;
    private JTable tblChangesets;

    /**
     * Creates the various models required.
     * @return the changeset cache model
     */
    static ChangesetCacheManagerModel buildModel() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        return new ChangesetCacheManagerModel(selectionModel);
    }

    /**
     * builds the toolbar panel in the heading of the dialog
     *
     * @return the toolbar panel
     */
    static JPanel buildToolbarPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton btn = new JButton(new QueryAction());
        pnl.add(btn);
        pnl.add(new SingleChangesetDownloadPanel());
        pnl.add(new JButton(new DownloadMyChangesets()));

        return pnl;
    }

    /**
     * builds the button panel in the footer of the dialog
     *
     * @return the button row pane
     */
    static JPanel buildButtonPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));

        //-- cancel and close action
        pnl.add(new JButton(new CancelAction()));

        //-- help action
        pnl.add(new JButton(new ContextSensitiveHelpAction(HelpUtil.ht("/Dialog/ChangesetManager"))));

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
        pnlChangesetDetailTabs = tp;

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

        // -- add the panel for the changeset discussion
        ChangesetDiscussionPanel pnlChangesetDiscussion = new ChangesetDiscussionPanel();
        tp.add(pnlChangesetDiscussion);
        model.addPropertyChangeListener(pnlChangesetDiscussion);

        tp.setTitleAt(0, tr("Properties"));
        tp.setToolTipTextAt(0, tr("Display the basic properties of the changeset"));
        tp.setTitleAt(1, tr("Tags"));
        tp.setToolTipTextAt(1, tr("Display the tags of the changeset"));
        tp.setTitleAt(2, tr("Content"));
        tp.setToolTipTextAt(2, tr("Display the objects created, updated, and deleted by the changeset"));
        tp.setTitleAt(3, tr("Discussion"));
        tp.setToolTipTextAt(3, tr("Display the public discussion around this changeset"));

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

        // -- download the objects contained in the selected changesets from the OSM server
        model.getSelectionModel().addListSelectionListener(actDownloadSelectedChangesetObjects);
        tb.add(actDownloadSelectedChangesetObjects);

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
        InputMapUtils.addEnterAction(tblChangesets, new ShowDetailAction(model));
        model.getSelectionModel().addListSelectionListener(new ChangesetDetailViewSynchronizer(model));

        // activate DEL on the table
        tblChangesets.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removeFromCache");
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

        model = buildModel();
        actRemoveFromCacheAction = new RemoveFromCacheAction(model);
        actCloseSelectedChangesetsAction = new CloseSelectedChangesetsAction(model);
        actDownloadSelectedChangesets = new DownloadSelectedChangesetsAction(model);
        actDownloadSelectedContent = new DownloadSelectedChangesetContentAction(model);
        actDownloadSelectedChangesetObjects = new DownloadSelectedChangesetObjectsAction();

        cp.add(buildToolbarPanel(), BorderLayout.NORTH);
        cp.add(buildContentPanel(), BorderLayout.CENTER);
        cp.add(buildButtonPanel(), BorderLayout.SOUTH);

        // the help context
        HelpUtil.setHelpContext(getRootPane(), HelpUtil.ht("/Dialog/ChangesetManager"));

        // make the dialog respond to ESC
        InputMapUtils.addEscapeAction(getRootPane(), new CancelAction());

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
                            new Dimension(1000, 600)
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
        public void windowActivated(WindowEvent e) {
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
        CancelAction() {
            putValue(NAME, tr("Close"));
            new ImageProvider("cancel").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Close the dialog"));
        }

        public void cancelAndClose() {
            destroyInstance();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            cancelAndClose();
        }
    }

    /**
     * The action to query and download changesets
     */
    static class QueryAction extends AbstractAction {

        QueryAction() {
            putValue(NAME, tr("Query"));
            new ImageProvider("dialogs", "search").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Launch the dialog for querying changesets"));
            setEnabled(!Main.isOffline(OnlineResource.OSM_API));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            Window parent = GuiHelper.getWindowAncestorFor(evt);
            if (!GraphicsEnvironment.isHeadless()) {
                ChangesetQueryDialog dialog = new ChangesetQueryDialog(parent);
                dialog.initForUserInput();
                dialog.setVisible(true);
                if (dialog.isCanceled())
                    return;

                try {
                    ChangesetQuery query = dialog.getChangesetQuery();
                    if (query != null) {
                        ChangesetCacheManager.getInstance().runDownloadTask(new ChangesetQueryTask(parent, query));
                    }
                } catch (IllegalStateException e) {
                    Logging.error(e);
                    JOptionPane.showMessageDialog(parent, e.getMessage(), tr("Error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Removes the selected changesets from the local changeset cache
     *
     */
    static class RemoveFromCacheAction extends AbstractAction implements ListSelectionListener {
        private final ChangesetCacheManagerModel model;

        RemoveFromCacheAction(ChangesetCacheManagerModel model) {
            putValue(NAME, tr("Remove from cache"));
            new ImageProvider("dialogs", "delete").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Remove the selected changesets from the local cache"));
            this.model = model;
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ChangesetCache.getInstance().remove(model.getSelectedChangesets());
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
    static class CloseSelectedChangesetsAction extends AbstractAction implements ListSelectionListener {
        private final ChangesetCacheManagerModel model;

        CloseSelectedChangesetsAction(ChangesetCacheManagerModel model) {
            putValue(NAME, tr("Close"));
            new ImageProvider("closechangeset").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Close the selected changesets"));
            this.model = model;
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            MainApplication.worker.submit(new CloseChangesetTask(model.getSelectedChangesets()));
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
    static class DownloadSelectedChangesetsAction extends AbstractAction implements ListSelectionListener {
        private final ChangesetCacheManagerModel model;

        DownloadSelectedChangesetsAction(ChangesetCacheManagerModel model) {
            putValue(NAME, tr("Update changeset"));
            new ImageProvider("dialogs/changeset", "updatechangeset").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Updates the selected changesets with current data from the OSM server"));
            this.model = model;
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!GraphicsEnvironment.isHeadless()) {
                ChangesetCacheManager.getInstance().runDownloadTask(
                        ChangesetHeaderDownloadTask.buildTaskForChangesets(GuiHelper.getWindowAncestorFor(e), model.getSelectedChangesets()));
            }
        }

        protected void updateEnabledState() {
            setEnabled(model.hasSelectedChangesets() && !Main.isOffline(OnlineResource.OSM_API));
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
    static class DownloadSelectedChangesetContentAction extends AbstractAction implements ListSelectionListener {
        private final ChangesetCacheManagerModel model;

        DownloadSelectedChangesetContentAction(ChangesetCacheManagerModel model) {
            putValue(NAME, tr("Download changeset content"));
            new ImageProvider("dialogs/changeset", "downloadchangesetcontent").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Download the content of the selected changesets from the server"));
            this.model = model;
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!GraphicsEnvironment.isHeadless()) {
                ChangesetCacheManager.getInstance().runDownloadTask(
                        new ChangesetContentDownloadTask(GuiHelper.getWindowAncestorFor(e), model.getSelectedChangesetIds()));
            }
        }

        protected void updateEnabledState() {
            setEnabled(model.hasSelectedChangesets() && !Main.isOffline(OnlineResource.OSM_API));
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * Downloads the objects contained in the selected changesets from the OSM server
     */
    private class DownloadSelectedChangesetObjectsAction extends AbstractAction implements ListSelectionListener {

        DownloadSelectedChangesetObjectsAction() {
            putValue(NAME, tr("Download changed objects"));
            new ImageProvider("downloadprimitive").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Download the current version of the changed objects in the selected changesets"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!GraphicsEnvironment.isHeadless()) {
                actDownloadSelectedContent.actionPerformed(e);
                MainApplication.worker.submit(() -> {
                    final List<PrimitiveId> primitiveIds = model.getSelectedChangesets().stream()
                            .map(Changeset::getContent)
                            .filter(Objects::nonNull)
                            .flatMap(content -> StreamUtils.toStream(content::iterator))
                            .map(ChangesetDataSet.ChangesetDataSetEntry::getPrimitive)
                            .map(HistoryOsmPrimitive::getPrimitiveId)
                            .distinct()
                            .collect(Collectors.toList());
                    new DownloadPrimitivesWithReferrersTask(false, primitiveIds, true, true, null, null).run();
                });
            }
        }

        protected void updateEnabledState() {
            setEnabled(model.hasSelectedChangesets() && !Main.isOffline(OnlineResource.OSM_API));
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    static class ShowDetailAction extends AbstractAction {
        private final ChangesetCacheManagerModel model;

        ShowDetailAction(ChangesetCacheManagerModel model) {
            this.model = model;
        }

        protected void showDetails() {
            List<Changeset> selected = model.getSelectedChangesets();
            if (selected.size() == 1) {
                model.setChangesetInDetailView(selected.get(0));
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showDetails();
        }
    }

    static class DownloadMyChangesets extends AbstractAction {
        DownloadMyChangesets() {
            putValue(NAME, tr("My changesets"));
            new ImageProvider("dialogs/changeset", "downloadchangeset").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Download my changesets from the OSM server (max. 100 changesets)"));
            setEnabled(!Main.isOffline(OnlineResource.OSM_API));
        }

        protected void alertAnonymousUser(Component parent) {
            HelpAwareOptionPane.showOptionDialog(
                    parent,
                    tr("<html>JOSM is currently running with an anonymous user. It cannot download<br>"
                            + "your changesets from the OSM server unless you enter your OSM user name<br>"
                            + "in the JOSM preferences.</html>"
                    ),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE,
                    HelpUtil.ht("/Dialog/ChangesetManager#CanDownloadMyChangesets")
            );
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Window parent = GuiHelper.getWindowAncestorFor(e);
            try {
                ChangesetQuery query = ChangesetQuery.forCurrentUser();
                if (!GraphicsEnvironment.isHeadless()) {
                    ChangesetCacheManager.getInstance().runDownloadTask(new ChangesetQueryTask(parent, query));
                }
            } catch (IllegalStateException ex) {
                alertAnonymousUser(parent);
                Logging.trace(ex);
            }
        }
    }

    class MouseEventHandler extends PopupMenuLauncher {

        MouseEventHandler() {
            super(new ChangesetTablePopupMenu());
        }

        @Override
        public void mouseClicked(MouseEvent evt) {
            if (isDoubleClick(evt)) {
                new ShowDetailAction(model).showDetails();
            }
        }
    }

    class ChangesetTablePopupMenu extends JPopupMenu {
        ChangesetTablePopupMenu() {
            add(actRemoveFromCacheAction);
            add(actCloseSelectedChangesetsAction);
            add(actDownloadSelectedChangesets);
            add(actDownloadSelectedContent);
            add(actDownloadSelectedChangesetObjects);
        }
    }

    static class ChangesetDetailViewSynchronizer implements ListSelectionListener {
        private final ChangesetCacheManagerModel model;

        ChangesetDetailViewSynchronizer(ChangesetCacheManagerModel model) {
            this.model = model;
        }

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
     * Returns the changeset cache model.
     * @return the changeset cache model
     * @since 12495
     */
    public ChangesetCacheManagerModel getModel() {
        return model;
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
        final int idx = model.getSelectionModel().getMinSelectionIndex();
        if (idx < 0)
            return;
        GuiHelper.runInEDTAndWait(() -> tblChangesets.scrollRectToVisible(tblChangesets.getCellRect(idx, 0, true)));
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
        Set<Changeset> toSelect = new HashSet<>();
        ChangesetCache cc = ChangesetCache.getInstance();
        for (int id: ids) {
            if (cc.contains(id)) {
                toSelect.add(cc.get(id));
            }
        }
        setSelectedChangesets(toSelect);
    }

    /**
     * Selects the given component in the detail tabbed panel
     * @param clazz the class of the component to select
     */
    public void setSelectedComponentInDetailPanel(Class<? extends JComponent> clazz) {
        for (Component component : pnlChangesetDetailTabs.getComponents()) {
            if (component.getClass().equals(clazz)) {
                pnlChangesetDetailTabs.setSelectedComponent(component);
                break;
            }
        }
    }

    /**
     * Runs the given changeset download task.
     * @param task The changeset download task to run
     */
    public void runDownloadTask(final AbstractChangesetDownloadTask task) {
        MainApplication.worker.submit(new PostDownloadHandler(task, task.download()));
        MainApplication.worker.submit(() -> {
            if (task.isCanceled() || task.isFailed())
                return;
            GuiHelper.runInEDT(() -> setSelectedChangesets(task.getDownloadedData()));
        });
    }
}
