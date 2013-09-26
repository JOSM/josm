// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

public class PasteTagsConflictResolverDialog extends JDialog  implements PropertyChangeListener {
    static private final Map<OsmPrimitiveType, String> PANE_TITLES;
    static {
        PANE_TITLES = new HashMap<OsmPrimitiveType, String>();
        PANE_TITLES.put(OsmPrimitiveType.NODE, tr("Tags from nodes"));
        PANE_TITLES.put(OsmPrimitiveType.WAY, tr("Tags from ways"));
        PANE_TITLES.put(OsmPrimitiveType.RELATION, tr("Tags from relations"));
    }

    private enum Mode {
        RESOLVING_ONE_TAGCOLLECTION_ONLY,
        RESOLVING_TYPED_TAGCOLLECTIONS
    }

    private TagConflictResolver allPrimitivesResolver;
    private Map<OsmPrimitiveType, TagConflictResolver> resolvers;
    private JTabbedPane tpResolvers;
    private Mode mode;
    private boolean canceled = false;

    private ImageIcon iconResolved;
    private ImageIcon iconUnresolved;
    private StatisticsTableModel statisticsModel;
    private JPanel pnlTagResolver;

    public PasteTagsConflictResolverDialog(Component owner) {
        super(JOptionPane.getFrameForComponent(owner), ModalityType.DOCUMENT_MODAL);
        build();
        iconResolved = ImageProvider.get("dialogs/conflict", "tagconflictresolved");
        iconUnresolved = ImageProvider.get("dialogs/conflict", "tagconflictunresolved");
    }

    protected void build() {
        setTitle(tr("Conflicts in pasted tags"));
        allPrimitivesResolver = new TagConflictResolver();
        resolvers = new HashMap<OsmPrimitiveType, TagConflictResolver>();
        for (OsmPrimitiveType type: OsmPrimitiveType.dataValues()) {
            resolvers.put(type, new TagConflictResolver());
            resolvers.get(type).getModel().addPropertyChangeListener(this);
        }
        tpResolvers = new JTabbedPane();
        getContentPane().setLayout(new GridBagLayout());
        mode = null;
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        getContentPane().add(buildSourceAndTargetInfoPanel(), gc);
        gc.gridx = 0;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        getContentPane().add(pnlTagResolver = new JPanel(), gc);
        gc.gridx = 0;
        gc.gridy = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        getContentPane().add(buildButtonPanel(), gc);
    }

    protected JPanel buildButtonPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout(FlowLayout.CENTER));

        // -- apply button
        ApplyAction applyAction = new ApplyAction();
        allPrimitivesResolver.getModel().addPropertyChangeListener(applyAction);
        for (OsmPrimitiveType type: resolvers.keySet()) {
            resolvers.get(type).getModel().addPropertyChangeListener(applyAction);
        }
        pnl.add(new SideButton(applyAction));

        // -- cancel button
        CancelAction cancelAction = new CancelAction();
        pnl.add(new SideButton(cancelAction));

        return pnl;
    }

    protected JPanel buildSourceAndTargetInfoPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BorderLayout());
        statisticsModel = new StatisticsTableModel();
        pnl.add(new StatisticsInfoTable(statisticsModel), BorderLayout.CENTER);
        return pnl;
    }

    /**
     * Initializes the conflict resolver for a specific type of primitives
     *
     * @param type the type of primitives
     * @param tc the tags belonging to this type of primitives
     * @param targetStatistics histogram of paste targets, number of primitives of each type in the paste target
     */
    protected void initResolver(OsmPrimitiveType type, TagCollection tc, Map<OsmPrimitiveType,Integer> targetStatistics) {
        resolvers.get(type).getModel().populate(tc,tc.getKeysWithMultipleValues());
        resolvers.get(type).getModel().prepareDefaultTagDecisions();
        if (!tc.isEmpty() && targetStatistics.get(type) != null && targetStatistics.get(type) > 0) {
            tpResolvers.add(PANE_TITLES.get(type), resolvers.get(type));
        }
    }

    /**
     * Populates the conflict resolver with one tag collection
     *
     * @param tagsForAllPrimitives  the tag collection
     * @param sourceStatistics histogram of tag source, number of primitives of each type in the source
     * @param targetStatistics histogram of paste targets, number of primitives of each type in the paste target
     */
    public void populate(TagCollection tagsForAllPrimitives, Map<OsmPrimitiveType, Integer> sourceStatistics, Map<OsmPrimitiveType,Integer> targetStatistics) {
        mode = Mode.RESOLVING_ONE_TAGCOLLECTION_ONLY;
        tagsForAllPrimitives = tagsForAllPrimitives == null? new TagCollection() : tagsForAllPrimitives;
        sourceStatistics = sourceStatistics == null ? new HashMap<OsmPrimitiveType, Integer>() :sourceStatistics;
        targetStatistics = targetStatistics == null ? new HashMap<OsmPrimitiveType, Integer>() : targetStatistics;

        // init the resolver
        //
        allPrimitivesResolver.getModel().populate(tagsForAllPrimitives,tagsForAllPrimitives.getKeysWithMultipleValues());
        allPrimitivesResolver.getModel().prepareDefaultTagDecisions();

        // prepare the dialog with one tag resolver
        pnlTagResolver.setLayout(new BorderLayout());
        pnlTagResolver.removeAll();
        pnlTagResolver.add(allPrimitivesResolver, BorderLayout.CENTER);

        statisticsModel.reset();
        StatisticsInfo info = new StatisticsInfo();
        info.numTags = tagsForAllPrimitives.getKeys().size();
        info.sourceInfo.putAll(sourceStatistics);
        info.targetInfo.putAll(targetStatistics);
        statisticsModel.append(info);
        validate();
    }

    protected int getNumResolverTabs() {
        return tpResolvers.getTabCount();
    }

    protected TagConflictResolver getResolver(int idx) {
        return (TagConflictResolver)tpResolvers.getComponentAt(idx);
    }

    /**
     * Populate the tag conflict resolver with tags for each type of primitives
     *
     * @param tagsForNodes the tags belonging to nodes in the paste source
     * @param tagsForWays the tags belonging to way in the paste source
     * @param tagsForRelations the tags belonging to relations in the paste source
     * @param sourceStatistics histogram of tag source, number of primitives of each type in the source
     * @param targetStatistics histogram of paste targets, number of primitives of each type in the paste target
     */
    public void populate(TagCollection tagsForNodes, TagCollection tagsForWays, TagCollection tagsForRelations, Map<OsmPrimitiveType,Integer> sourceStatistics, Map<OsmPrimitiveType, Integer> targetStatistics) {
        tagsForNodes = (tagsForNodes == null) ? new TagCollection() : tagsForNodes;
        tagsForWays = (tagsForWays == null) ? new TagCollection() : tagsForWays;
        tagsForRelations = (tagsForRelations == null) ? new TagCollection() : tagsForRelations;
        if (tagsForNodes.isEmpty() && tagsForWays.isEmpty() && tagsForRelations.isEmpty()) {
            populate(null,null,null);
            return;
        }
        tpResolvers.removeAll();
        initResolver(OsmPrimitiveType.NODE,tagsForNodes, targetStatistics);
        initResolver(OsmPrimitiveType.WAY,tagsForWays, targetStatistics);
        initResolver(OsmPrimitiveType.RELATION,tagsForRelations, targetStatistics);

        pnlTagResolver.setLayout(new BorderLayout());
        pnlTagResolver.removeAll();
        pnlTagResolver.add(tpResolvers, BorderLayout.CENTER);
        mode = Mode.RESOLVING_TYPED_TAGCOLLECTIONS;
        validate();
        statisticsModel.reset();
        if (!tagsForNodes.isEmpty()) {
            StatisticsInfo info = new StatisticsInfo();
            info.numTags = tagsForNodes.getKeys().size();
            int numTargets = targetStatistics.get(OsmPrimitiveType.NODE) == null ? 0 : targetStatistics.get(OsmPrimitiveType.NODE);
            if (numTargets > 0) {
                info.sourceInfo.put(OsmPrimitiveType.NODE, sourceStatistics.get(OsmPrimitiveType.NODE));
                info.targetInfo.put(OsmPrimitiveType.NODE, numTargets);
                statisticsModel.append(info);
            }
        }
        if (!tagsForWays.isEmpty()) {
            StatisticsInfo info = new StatisticsInfo();
            info.numTags = tagsForWays.getKeys().size();
            int numTargets = targetStatistics.get(OsmPrimitiveType.WAY) == null ? 0 : targetStatistics.get(OsmPrimitiveType.WAY);
            if (numTargets > 0) {
                info.sourceInfo.put(OsmPrimitiveType.WAY, sourceStatistics.get(OsmPrimitiveType.WAY));
                info.targetInfo.put(OsmPrimitiveType.WAY, numTargets);
                statisticsModel.append(info);
            }
        }
        if (!tagsForRelations.isEmpty()) {
            StatisticsInfo info = new StatisticsInfo();
            info.numTags = tagsForRelations.getKeys().size();
            int numTargets = targetStatistics.get(OsmPrimitiveType.RELATION) == null ? 0 : targetStatistics.get(OsmPrimitiveType.RELATION);
            if (numTargets > 0) {
                info.sourceInfo.put(OsmPrimitiveType.RELATION, sourceStatistics.get(OsmPrimitiveType.RELATION));
                info.targetInfo.put(OsmPrimitiveType.RELATION, numTargets);
                statisticsModel.append(info);
            }
        }

        for (int i =0; i < getNumResolverTabs(); i++) {
            if (!getResolver(i).getModel().isResolvedCompletely()) {
                tpResolvers.setSelectedIndex(i);
                break;
            }
        }
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    class CancelAction extends AbstractAction {

        public CancelAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Cancel conflict resolution"));
            putValue(Action.NAME, tr("Cancel"));
            putValue(Action.SMALL_ICON, ImageProvider.get("", "cancel"));
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            setVisible(false);
            setCanceled(true);
        }
    }

    class ApplyAction extends AbstractAction implements PropertyChangeListener {

        public ApplyAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Apply resolved conflicts"));
            putValue(Action.NAME, tr("Apply"));
            putValue(Action.SMALL_ICON, ImageProvider.get("ok"));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            setVisible(false);
        }

        protected void updateEnabledState() {
            if (mode == null) {
                setEnabled(false);
            } else if (mode.equals(Mode.RESOLVING_ONE_TAGCOLLECTION_ONLY)) {
                setEnabled(allPrimitivesResolver.getModel().isResolvedCompletely());
            } else {
                boolean enabled = true;
                for (OsmPrimitiveType type: resolvers.keySet()) {
                    enabled &= resolvers.get(type).getModel().isResolvedCompletely();
                }
                setEnabled(enabled);
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(TagConflictResolverModel.NUM_CONFLICTS_PROP)) {
                updateEnabledState();
            }
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerOnScreen(new Dimension(400,300))
            ).applySafe(this);
        } else if (isShowing()) { // Avoid IllegalComponentStateException like in #8775
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    public TagCollection getResolution() {
        return allPrimitivesResolver.getModel().getResolution();
    }

    public TagCollection getResolution(OsmPrimitiveType type) {
        if (type == null) return null;
        return resolvers.get(type).getModel().getResolution();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(TagConflictResolverModel.NUM_CONFLICTS_PROP)) {
            TagConflictResolverModel model = (TagConflictResolverModel)evt.getSource();
            for (int i=0; i < tpResolvers.getTabCount();i++) {
                TagConflictResolver resolver = (TagConflictResolver)tpResolvers.getComponentAt(i);
                if (model == resolver.getModel()) {
                    tpResolvers.setIconAt(i,
                            (Boolean)evt.getNewValue() ? iconResolved : iconUnresolved

                    );
                }
            }
        }
    }

    static public class StatisticsInfo {
        public int numTags;
        public Map<OsmPrimitiveType, Integer> sourceInfo;
        public Map<OsmPrimitiveType, Integer> targetInfo;

        public StatisticsInfo() {
            sourceInfo = new HashMap<OsmPrimitiveType, Integer>();
            targetInfo = new HashMap<OsmPrimitiveType, Integer>();
        }
    }

    static private class StatisticsTableColumnModel extends DefaultTableColumnModel {
        public StatisticsTableColumnModel() {
            TableCellRenderer renderer = new StatisticsInfoRenderer();
            TableColumn col = null;

            // column 0 - Paste
            col = new TableColumn(0);
            col.setHeaderValue(tr("Paste ..."));
            col.setResizable(true);
            col.setCellRenderer(renderer);
            addColumn(col);

            // column 1 - From
            col = new TableColumn(1);
            col.setHeaderValue(tr("From ..."));
            col.setResizable(true);
            col.setCellRenderer(renderer);
            addColumn(col);

            // column 2 - To
            col = new TableColumn(2);
            col.setHeaderValue(tr("To ..."));
            col.setResizable(true);
            col.setCellRenderer(renderer);
            addColumn(col);
        }
    }

    static private class StatisticsTableModel extends DefaultTableModel {
        private static final String[] HEADERS = new String[] {tr("Paste ..."), tr("From ..."), tr("To ...") };
        private List<StatisticsInfo> data;

        public StatisticsTableModel() {
            data = new ArrayList<StatisticsInfo>();
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (row == 0)
                return HEADERS[column];
            else if (row -1 < data.size())
                return data.get(row -1);
            else
                return null;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public int getRowCount() {
            if (data == null) return 1;
            return data.size() + 1;
        }

        public void reset() {
            data.clear();
        }

        public void append(StatisticsInfo info) {
            data.add(info);
            fireTableDataChanged();
        }
    }

    static private class StatisticsInfoRenderer extends JLabel implements TableCellRenderer {
        protected void reset() {
            setIcon(null);
            setText("");
            setFont(UIManager.getFont("Table.font"));
        }
        protected void renderNumTags(StatisticsInfo info) {
            if (info == null) return;
            setText(trn("{0} tag", "{0} tags", info.numTags, info.numTags));
        }

        protected void renderStatistics(Map<OsmPrimitiveType, Integer> stat) {
            if (stat == null) return;
            if (stat.isEmpty()) return;
            if (stat.size() == 1) {
                setIcon(ImageProvider.get(stat.keySet().iterator().next()));
            } else {
                setIcon(ImageProvider.get("data", "object"));
            }
            StringBuilder text = new StringBuilder();
            for (Entry<OsmPrimitiveType, Integer> entry: stat.entrySet()) {
                OsmPrimitiveType type = entry.getKey();
                int numPrimitives = entry.getValue() == null ? 0 : entry.getValue();
                if (numPrimitives == 0) {
                    continue;
                }
                String msg = "";
                switch(type) {
                case NODE: msg = trn("{0} node", "{0} nodes", numPrimitives,numPrimitives); break;
                case WAY: msg = trn("{0} way", "{0} ways", numPrimitives, numPrimitives); break;
                case RELATION: msg = trn("{0} relation", "{0} relations", numPrimitives, numPrimitives); break;
                }
                if (text.length() > 0) {
                    text.append(", ");
                }
                text.append(msg);
            }
            setText(text.toString());
        }

        protected void renderFrom(StatisticsInfo info) {
            renderStatistics(info.sourceInfo);
        }

        protected void renderTo(StatisticsInfo info) {
            renderStatistics(info.targetInfo);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            reset();
            if (value == null)
                return this;

            if (row == 0) {
                setFont(getFont().deriveFont(Font.BOLD));
                setText((String)value);
            } else {
                StatisticsInfo info = (StatisticsInfo) value;

                switch(column) {
                case 0: renderNumTags(info); break;
                case 1: renderFrom(info); break;
                case 2: renderTo(info); break;
                }
            }
            return this;
        }
    }

    static private class StatisticsInfoTable extends JPanel {

        private JTable infoTable;

        protected void build(StatisticsTableModel model) {
            infoTable = new JTable(model, new StatisticsTableColumnModel());
            infoTable.setShowHorizontalLines(true);
            infoTable.setShowVerticalLines(false);
            infoTable.setEnabled(false);
            setLayout(new BorderLayout());
            add(infoTable, BorderLayout.CENTER);
        }

        public StatisticsInfoTable(StatisticsTableModel model) {
            build(model);
        }

        @Override
        public Insets getInsets() {
            Insets insets = super.getInsets();
            insets.bottom = 20;
            return insets;
        }
    }
}
