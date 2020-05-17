// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.HistoryInfoAction;
import org.openstreetmap.josm.actions.downloadtasks.ChangesetContentDownloadTask;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.history.HistoryBrowserDialogManager;
import org.openstreetmap.josm.gui.history.HistoryLoadTask;
import org.openstreetmap.josm.gui.io.DownloadPrimitivesWithReferrersTask;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReportExceptionHandler;

/**
 * The panel which displays the content of a changeset in a scrollable table.
 *
 * It listens to property change events for {@link ChangesetCacheManagerModel#CHANGESET_IN_DETAIL_VIEW_PROP}
 * and updates its view accordingly.
 * @since 2689
 */
public class ChangesetContentPanel extends JPanel implements PropertyChangeListener, ChangesetAware {
    private JTable tblContent;
    private ChangesetContentTableModel model;
    private transient Changeset currentChangeset;

    private DownloadChangesetContentAction actDownloadContentAction;
    private ShowHistoryAction actShowHistory;
    private SelectInCurrentLayerAction actSelectInCurrentLayerAction;
    private ZoomInCurrentLayerAction actZoomInCurrentLayerAction;

    private final HeaderPanel pnlHeader = new HeaderPanel();
    protected DownloadObjectAction actDownloadObjectAction;

    protected void buildModels() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        model = new ChangesetContentTableModel(selectionModel);
        actDownloadContentAction = new DownloadChangesetContentAction(this);
        actDownloadContentAction.initProperties();

        actDownloadObjectAction = new DownloadObjectAction();
        model.getSelectionModel().addListSelectionListener(actDownloadObjectAction);

        actShowHistory = new ShowHistoryAction();
        model.getSelectionModel().addListSelectionListener(actShowHistory);

        actSelectInCurrentLayerAction = new SelectInCurrentLayerAction();
        model.getSelectionModel().addListSelectionListener(actSelectInCurrentLayerAction);
        MainApplication.getLayerManager().addActiveLayerChangeListener(actSelectInCurrentLayerAction);

        actZoomInCurrentLayerAction = new ZoomInCurrentLayerAction();
        model.getSelectionModel().addListSelectionListener(actZoomInCurrentLayerAction);
        MainApplication.getLayerManager().addActiveLayerChangeListener(actZoomInCurrentLayerAction);

        addComponentListener(
                new ComponentAdapter() {
                    @Override
                    public void componentShown(ComponentEvent e) {
                        MainApplication.getLayerManager().addAndFireActiveLayerChangeListener(actSelectInCurrentLayerAction);
                        MainApplication.getLayerManager().addAndFireActiveLayerChangeListener(actZoomInCurrentLayerAction);
                    }

                    @Override
                    public void componentHidden(ComponentEvent e) {
                        // make sure the listener is unregistered when the panel becomes invisible
                        MainApplication.getLayerManager().removeActiveLayerChangeListener(actSelectInCurrentLayerAction);
                        MainApplication.getLayerManager().removeActiveLayerChangeListener(actZoomInCurrentLayerAction);
                    }
                }
        );
    }

    protected JPanel buildContentPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        tblContent = new JTable(
                model,
                new ChangesetContentTableColumnModel(),
                model.getSelectionModel()
        );
        tblContent.setAutoCreateRowSorter(true);
        tblContent.addMouseListener(new PopupMenuLauncher(new ChangesetContentTablePopupMenu()));
        HistoryInfoAction historyAction = MainApplication.getMenu().historyinfo;
        tblContent.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(historyAction.getShortcut().getKeyStroke(), "historyAction");
        tblContent.getActionMap().put("historyAction", historyAction);
        pnl.add(new JScrollPane(tblContent), BorderLayout.CENTER);
        return pnl;
    }

    protected JPanel buildActionButtonPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JToolBar tb = new JToolBar(SwingConstants.VERTICAL);
        tb.setFloatable(false);

        tb.add(actDownloadContentAction);
        tb.addSeparator();
        tb.add(actDownloadObjectAction);
        tb.add(actShowHistory);
        tb.addSeparator();
        tb.add(actSelectInCurrentLayerAction);
        tb.add(actZoomInCurrentLayerAction);

        pnl.add(tb);
        return pnl;
    }

    protected final void build() {
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout());
        buildModels();

        add(pnlHeader, BorderLayout.NORTH);
        add(buildActionButtonPanel(), BorderLayout.WEST);
        add(buildContentPanel(), BorderLayout.CENTER);
    }

    /**
     * Constructs a new {@code ChangesetContentPanel}.
     */
    public ChangesetContentPanel() {
        build();
    }

    /**
     * Replies the changeset content model
     * @return The model
     */
    public ChangesetContentTableModel getModel() {
        return model;
    }

    protected void setCurrentChangeset(Changeset cs) {
        currentChangeset = cs;
        if (cs == null) {
            model.populate(null);
        } else {
            model.populate(cs.getContent());
        }
        actDownloadContentAction.initProperties();
        pnlHeader.setChangeset(cs);
    }

    /* ---------------------------------------------------------------------------- */
    /* interface PropertyChangeListener                                             */
    /* ---------------------------------------------------------------------------- */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!evt.getPropertyName().equals(ChangesetCacheManagerModel.CHANGESET_IN_DETAIL_VIEW_PROP))
            return;
        Changeset cs = (Changeset) evt.getNewValue();
        setCurrentChangeset(cs);
    }

    private void alertNoPrimitivesTo(Collection<HistoryOsmPrimitive> primitives, String title, String helpTopic) {
        HelpAwareOptionPane.showOptionDialog(
                this,
                trn("<html>The selected object is not available in the current<br>"
                        + "edit layer ''{0}''.</html>",
                        "<html>None of the selected objects is available in the current<br>"
                        + "edit layer ''{0}''.</html>",
                        primitives.size(),
                        Utils.escapeReservedCharactersHTML(MainApplication.getLayerManager().getEditLayer().getName())
                ),
                title, JOptionPane.WARNING_MESSAGE, helpTopic
        );
    }

    private Set<HistoryOsmPrimitive> getSelectedPrimitives() {
      return model.getSelectedPrimitives(tblContent);
    }

    class ChangesetContentTablePopupMenu extends JPopupMenu {
        ChangesetContentTablePopupMenu() {
            add(actDownloadContentAction);
            add(new JSeparator());
            add(actDownloadObjectAction);
            add(actShowHistory);
            add(new JSeparator());
            add(actSelectInCurrentLayerAction);
            add(actZoomInCurrentLayerAction);
        }
    }

    class ShowHistoryAction extends AbstractAction implements ListSelectionListener {

        private final class ShowHistoryTask implements Runnable {
            private final Collection<HistoryOsmPrimitive> primitives;

            private ShowHistoryTask(Collection<HistoryOsmPrimitive> primitives) {
                this.primitives = primitives;
            }

            @Override
            public void run() {
                try {
                    for (HistoryOsmPrimitive p : primitives) {
                        final History h = HistoryDataSet.getInstance().getHistory(p.getPrimitiveId());
                        if (h == null) {
                            continue;
                        }
                        GuiHelper.runInEDT(() -> HistoryBrowserDialogManager.getInstance().show(h));
                    }
                } catch (final JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
                    GuiHelper.runInEDT(() -> BugReportExceptionHandler.handleException(e));
                }
            }
        }

        ShowHistoryAction() {
            putValue(NAME, tr("Show history"));
            new ImageProvider("dialogs", "history").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Download and show the history of the selected objects"));
            updateEnabledState();
        }

        protected List<HistoryOsmPrimitive> filterPrimitivesWithUnloadedHistory(Collection<HistoryOsmPrimitive> primitives) {
            return primitives.stream()
                    .filter(p -> HistoryDataSet.getInstance().getHistory(p.getPrimitiveId()) == null)
                    .collect(Collectors.toList());
        }

        public void showHistory(final Collection<HistoryOsmPrimitive> primitives) {

            List<HistoryOsmPrimitive> toLoad = filterPrimitivesWithUnloadedHistory(primitives);
            if (!toLoad.isEmpty()) {
                HistoryLoadTask task = new HistoryLoadTask(ChangesetContentPanel.this);
                for (HistoryOsmPrimitive p: toLoad) {
                    task.add(p);
                }
                MainApplication.worker.submit(task);
            }

            MainApplication.worker.submit(new ShowHistoryTask(primitives));
        }

        protected final void updateEnabledState() {
            setEnabled(model.hasSelectedPrimitives());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Set<HistoryOsmPrimitive> selected = getSelectedPrimitives();
            if (selected.isEmpty()) return;
            showHistory(selected);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    class DownloadObjectAction extends AbstractAction implements ListSelectionListener {

        DownloadObjectAction() {
            putValue(NAME, tr("Download objects"));
            new ImageProvider("downloadprimitive").getResource().attachImageIcon(this, true);
            putValue(SHORT_DESCRIPTION, tr("Download the current version of the selected objects"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final List<PrimitiveId> primitiveIds = getSelectedPrimitives().stream().map(HistoryOsmPrimitive::getPrimitiveId)
                    .collect(Collectors.toList());
            MainApplication.worker.submit(new DownloadPrimitivesWithReferrersTask(false, primitiveIds, true, true, null, null));
        }

        protected final void updateEnabledState() {
            setEnabled(model.hasSelectedPrimitives());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    abstract class SelectionBasedAction extends AbstractAction implements ListSelectionListener, ActiveLayerChangeListener {

        protected Set<OsmPrimitive> getTarget() {
            DataSet ds = MainApplication.getLayerManager().getActiveDataSet();
            if (isEnabled() && ds != null) {
                return getSelectedPrimitives().stream()
                        .map(p -> ds.getPrimitiveById(p.getPrimitiveId())).filter(Objects::nonNull).collect(Collectors.toSet());
            }
            return Collections.emptySet();
        }

        public final void updateEnabledState() {
            setEnabled(MainApplication.getLayerManager().getActiveDataSet() != null && model.hasSelectedPrimitives());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        @Override
        public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
            updateEnabledState();
        }
    }

    class SelectInCurrentLayerAction extends SelectionBasedAction {

        SelectInCurrentLayerAction() {
            putValue(NAME, tr("Select in layer"));
            new ImageProvider("dialogs", "select").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Select the corresponding primitives in the current data layer"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final Set<OsmPrimitive> target = getTarget();
            if (target.isEmpty()) {
                alertNoPrimitivesTo(getSelectedPrimitives(), tr("Nothing to select"),
                        HelpUtil.ht("/Dialog/ChangesetCacheManager#NothingToSelectInLayer"));
                return;
            }
            MainApplication.getLayerManager().getActiveDataSet().setSelected(target);
        }
    }

    class ZoomInCurrentLayerAction extends SelectionBasedAction {

        ZoomInCurrentLayerAction() {
            putValue(NAME, tr("Zoom to in layer"));
            new ImageProvider("dialogs/autoscale", "selection").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Zoom to the corresponding objects in the current data layer"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final Set<OsmPrimitive> target = getTarget();
            if (target.isEmpty()) {
                alertNoPrimitivesTo(getSelectedPrimitives(), tr("Nothing to zoom to"),
                        HelpUtil.ht("/Dialog/ChangesetCacheManager#NothingToZoomTo"));
                return;
            }
            MainApplication.getLayerManager().getActiveDataSet().setSelected(target);
            AutoScaleAction.zoomToSelection();
        }
    }

    private static class HeaderPanel extends JPanel {

        private transient Changeset current;

        HeaderPanel() {
            build();
        }

        protected final void build() {
            setLayout(new FlowLayout(FlowLayout.LEFT));
            add(new JMultilineLabel(tr("The content of this changeset is not downloaded yet.")));
            add(new JButton(new DownloadAction()));

        }

        public void setChangeset(Changeset cs) {
            setVisible(cs != null && cs.getContent() == null);
            this.current = cs;
        }

        private class DownloadAction extends AbstractAction {
            DownloadAction() {
                putValue(NAME, tr("Download now"));
                putValue(SHORT_DESCRIPTION, tr("Download the changeset content"));
                new ImageProvider("dialogs/changeset", "downloadchangesetcontent").getResource().attachImageIcon(this);
            }

            @Override
            public void actionPerformed(ActionEvent evt) {
                if (current == null) return;
                ChangesetContentDownloadTask task = new ChangesetContentDownloadTask(HeaderPanel.this, current.getId());
                ChangesetCacheManager.getInstance().runDownloadTask(task);
            }
        }
    }

    @Override
    public Changeset getCurrentChangeset() {
        return currentChangeset;
    }
}
