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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.gui.tagging.TagTableColumnModelBuilder;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;

/**
 * This conflict resolution dialog is used when tags are pasted from the clipboard that conflict with the existing ones.
 */
public class PasteTagsConflictResolverDialog extends JDialog implements PropertyChangeListener {
    static final Map<OsmPrimitiveType, String> PANE_TITLES;
    static {
        PANE_TITLES = new EnumMap<>(OsmPrimitiveType.class);
        PANE_TITLES.put(OsmPrimitiveType.NODE, tr("Tags from nodes"));
        PANE_TITLES.put(OsmPrimitiveType.WAY, tr("Tags from ways"));
        PANE_TITLES.put(OsmPrimitiveType.RELATION, tr("Tags from relations"));
    }

    enum Mode {
        RESOLVING_ONE_TAGCOLLECTION_ONLY,
        RESOLVING_TYPED_TAGCOLLECTIONS
    }

    private final TagConflictResolverModel model = new TagConflictResolverModel();
    private final transient Map<OsmPrimitiveType, TagConflictResolver> resolvers = new EnumMap<>(OsmPrimitiveType.class);
    private final JTabbedPane tpResolvers = new JTabbedPane();
    private Mode mode;
    private boolean canceled;

    private final ImageIcon iconResolved = ImageProvider.get("dialogs/conflict", "tagconflictresolved");
    private final ImageIcon iconUnresolved = ImageProvider.get("dialogs/conflict", "tagconflictunresolved");
    private final StatisticsTableModel statisticsModel = new StatisticsTableModel();
    private final JPanel pnlTagResolver = new JPanel(new BorderLayout());

    /**
     * Constructs a new {@code PasteTagsConflictResolverDialog}.
     * @param owner parent component
     */
    public PasteTagsConflictResolverDialog(Component owner) {
        super(GuiHelper.getFrameForComponent(owner), ModalityType.DOCUMENT_MODAL);
        build();
    }

    protected final void build() {
        setTitle(tr("Conflicts in pasted tags"));
        for (OsmPrimitiveType type: OsmPrimitiveType.dataValues()) {
            TagConflictResolverModel tagModel = new TagConflictResolverModel();
            resolvers.put(type, new TagConflictResolver(tagModel));
            tagModel.addPropertyChangeListener(this);
        }
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
        getContentPane().add(pnlTagResolver, gc);
        gc.gridx = 0;
        gc.gridy = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        getContentPane().add(buildButtonPanel(), gc);
        InputMapUtils.addEscapeAction(getRootPane(), new CancelAction());
    }

    protected JPanel buildButtonPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));

        // -- apply button
        ApplyAction applyAction = new ApplyAction();
        model.addPropertyChangeListener(applyAction);
        for (TagConflictResolver r : resolvers.values()) {
            r.getModel().addPropertyChangeListener(applyAction);
        }
        pnl.add(new JButton(applyAction));

        // -- cancel button
        CancelAction cancelAction = new CancelAction();
        pnl.add(new JButton(cancelAction));

        return pnl;
    }

    protected JPanel buildSourceAndTargetInfoPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
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
    protected void initResolver(OsmPrimitiveType type, TagCollection tc, Map<OsmPrimitiveType, Integer> targetStatistics) {
        TagConflictResolver resolver = resolvers.get(type);
        resolver.getModel().populate(tc, tc.getKeysWithMultipleValues());
        resolver.getModel().prepareDefaultTagDecisions();
        if (!tc.isEmpty() && targetStatistics.get(type) != null && targetStatistics.get(type) > 0) {
            tpResolvers.add(PANE_TITLES.get(type), resolver);
        }
    }

    /**
     * Populates the conflict resolver with one tag collection
     *
     * @param tagsForAllPrimitives  the tag collection
     * @param sourceStatistics histogram of tag source, number of primitives of each type in the source
     * @param targetStatistics histogram of paste targets, number of primitives of each type in the paste target
     */
    public void populate(TagCollection tagsForAllPrimitives, Map<OsmPrimitiveType, Integer> sourceStatistics,
            Map<OsmPrimitiveType, Integer> targetStatistics) {
        mode = Mode.RESOLVING_ONE_TAGCOLLECTION_ONLY;
        tagsForAllPrimitives = tagsForAllPrimitives == null ? new TagCollection() : tagsForAllPrimitives;
        sourceStatistics = sourceStatistics == null ? new HashMap<>() : sourceStatistics;
        targetStatistics = targetStatistics == null ? new HashMap<>() : targetStatistics;

        // init the resolver
        //
        model.populate(tagsForAllPrimitives, tagsForAllPrimitives.getKeysWithMultipleValues());
        model.prepareDefaultTagDecisions();

        // prepare the dialog with one tag resolver
        pnlTagResolver.removeAll();
        pnlTagResolver.add(new TagConflictResolver(model), BorderLayout.CENTER);

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
        return (TagConflictResolver) tpResolvers.getComponentAt(idx);
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
    public void populate(TagCollection tagsForNodes, TagCollection tagsForWays, TagCollection tagsForRelations,
            Map<OsmPrimitiveType, Integer> sourceStatistics, Map<OsmPrimitiveType, Integer> targetStatistics) {
        tagsForNodes = (tagsForNodes == null) ? new TagCollection() : tagsForNodes;
        tagsForWays = (tagsForWays == null) ? new TagCollection() : tagsForWays;
        tagsForRelations = (tagsForRelations == null) ? new TagCollection() : tagsForRelations;
        if (tagsForNodes.isEmpty() && tagsForWays.isEmpty() && tagsForRelations.isEmpty()) {
            populate(null, null, null);
            return;
        }
        tpResolvers.removeAll();
        initResolver(OsmPrimitiveType.NODE, tagsForNodes, targetStatistics);
        initResolver(OsmPrimitiveType.WAY, tagsForWays, targetStatistics);
        initResolver(OsmPrimitiveType.RELATION, tagsForRelations, targetStatistics);

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

        for (int i = 0; i < getNumResolverTabs(); i++) {
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

    final class CancelAction extends AbstractAction {

        private CancelAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Cancel conflict resolution"));
            putValue(Action.NAME, tr("Cancel"));
            new ImageProvider("cancel").getResource().attachImageIcon(this);
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            setVisible(false);
            setCanceled(true);
        }
    }

    final class ApplyAction extends AbstractAction implements PropertyChangeListener {

        private ApplyAction() {
            putValue(Action.SHORT_DESCRIPTION, tr("Apply resolved conflicts"));
            putValue(Action.NAME, tr("Apply"));
            new ImageProvider("ok").getResource().attachImageIcon(this);
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            setVisible(false);
        }

        void updateEnabledState() {
            if (mode == null) {
                setEnabled(false);
            } else if (mode == Mode.RESOLVING_ONE_TAGCOLLECTION_ONLY) {
                setEnabled(model.isResolvedCompletely());
            } else {
                setEnabled(resolvers.values().stream().allMatch(val -> val.getModel().isResolvedCompletely()));
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
                    WindowGeometry.centerOnScreen(new Dimension(600, 400))
            ).applySafe(this);
        } else if (isShowing()) { // Avoid IllegalComponentStateException like in #8775
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    /**
     * Returns conflict resolution.
     * @return conflict resolution
     */
    public TagCollection getResolution() {
        return model.getResolution();
    }

    public TagCollection getResolution(OsmPrimitiveType type) {
        if (type == null) return null;
        return resolvers.get(type).getModel().getResolution();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(TagConflictResolverModel.NUM_CONFLICTS_PROP)) {
            TagConflictResolverModel tagModel = (TagConflictResolverModel) evt.getSource();
            for (int i = 0; i < tpResolvers.getTabCount(); i++) {
                TagConflictResolver resolver = (TagConflictResolver) tpResolvers.getComponentAt(i);
                if (tagModel == resolver.getModel()) {
                    tpResolvers.setIconAt(i,
                            (Integer) evt.getNewValue() == 0 ? iconResolved : iconUnresolved
                    );
                }
            }
        }
    }

    static final class StatisticsInfo {
        int numTags;
        final Map<OsmPrimitiveType, Integer> sourceInfo;
        final Map<OsmPrimitiveType, Integer> targetInfo;

        StatisticsInfo() {
            sourceInfo = new EnumMap<>(OsmPrimitiveType.class);
            targetInfo = new EnumMap<>(OsmPrimitiveType.class);
        }
    }

    static final class StatisticsTableModel extends DefaultTableModel {
        private static final String[] HEADERS = {tr("Paste ..."), tr("From ..."), tr("To ...") };
        private final transient List<StatisticsInfo> data = new ArrayList<>();

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
            return data == null ? 1 : data.size() + 1;
        }

        void reset() {
            data.clear();
        }

        void append(StatisticsInfo info) {
            data.add(info);
            fireTableDataChanged();
        }
    }

    static final class StatisticsInfoRenderer extends JLabel implements TableCellRenderer {
        private void reset() {
            setIcon(null);
            setText("");
            setFont(UIManager.getFont("Table.font"));
        }

        private void renderNumTags(StatisticsInfo info) {
            if (info == null) return;
            setText(trn("{0} tag", "{0} tags", info.numTags, info.numTags));
        }

        private void renderStatistics(Map<OsmPrimitiveType, Integer> stat) {
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
                String msg;
                switch(type) {
                case NODE: msg = trn("{0} node", "{0} nodes", numPrimitives, numPrimitives); break;
                case WAY: msg = trn("{0} way", "{0} ways", numPrimitives, numPrimitives); break;
                case RELATION: msg = trn("{0} relation", "{0} relations", numPrimitives, numPrimitives); break;
                default: throw new AssertionError();
                }
                if (text.length() > 0) {
                    text.append(", ");
                }
                text.append(msg);
            }
            setText(text.toString());
        }

        private void renderFrom(StatisticsInfo info) {
            renderStatistics(info.sourceInfo);
        }

        private void renderTo(StatisticsInfo info) {
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
                setText((String) value);
            } else {
                StatisticsInfo info = (StatisticsInfo) value;

                switch(column) {
                case 0: renderNumTags(info); break;
                case 1: renderFrom(info); break;
                case 2: renderTo(info); break;
                default: // Do nothing
                }
            }
            return this;
        }
    }

    static final class StatisticsInfoTable extends JPanel {

        StatisticsInfoTable(StatisticsTableModel model) {
            JTable infoTable = new JTable(model,
                    new TagTableColumnModelBuilder(new StatisticsInfoRenderer(), tr("Paste ..."), tr("From ..."), tr("To ...")).build());
            infoTable.setShowHorizontalLines(true);
            infoTable.setShowVerticalLines(false);
            infoTable.setEnabled(false);
            setLayout(new BorderLayout());
            add(infoTable, BorderLayout.CENTER);
        }

        @Override
        public Insets getInsets() {
            Insets insets = super.getInsets();
            insets.bottom = 20;
            return insets;
        }
    }
}
