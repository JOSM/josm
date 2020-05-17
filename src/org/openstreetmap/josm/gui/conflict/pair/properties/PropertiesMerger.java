// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.properties;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.conflict.ConflictColors;
import org.openstreetmap.josm.gui.conflict.pair.AbstractMergePanel;
import org.openstreetmap.josm.gui.conflict.pair.IConflictResolver;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.gui.history.VersionInfoPanel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

/**
 * This class represents a UI component for resolving conflicts in some properties of {@link OsmPrimitive}.
 * @since 1654
 */
public class PropertiesMerger extends AbstractMergePanel implements ChangeListener, IConflictResolver {
    private static final DecimalFormat COORD_FORMATTER = new DecimalFormat("###0.0000000");

    private final JLabel lblMyCoordinates = buildValueLabel("label.mycoordinates");
    private final JLabel lblMergedCoordinates = buildValueLabel("label.mergedcoordinates");
    private final JLabel lblTheirCoordinates = buildValueLabel("label.theircoordinates");

    private final JLabel lblMyDeletedState = buildValueLabel("label.mydeletedstate");
    private final JLabel lblMergedDeletedState = buildValueLabel("label.mergeddeletedstate");
    private final JLabel lblTheirDeletedState = buildValueLabel("label.theirdeletedstate");

    private final JLabel lblMyReferrers = buildValueLabel("label.myreferrers");
    private final JLabel lblTheirReferrers = buildValueLabel("label.theirreferrers");

    private final transient PropertiesMergeModel model = new PropertiesMergeModel();
    private final VersionInfoPanel mineVersionInfo = new VersionInfoPanel();
    private final VersionInfoPanel theirVersionInfo = new VersionInfoPanel();

    /**
     * Constructs a new {@code PropertiesMerger}.
     */
    public PropertiesMerger() {
        model.addChangeListener(this);
        buildRows();
    }

    @Override
    protected List<? extends MergeRow> getRows() {
        return Arrays.asList(
                new AbstractMergePanel.TitleRow(),
                new VersionInfoRow(),
                new MergeCoordinatesRow(),
                new UndecideCoordinatesRow(),
                new MergeDeletedStateRow(),
                new UndecideDeletedStateRow(),
                new ReferrersRow(),
                new EmptyFillRow());
    }

    protected static JLabel buildValueLabel(String name) {
        JLabel lbl = new JLabel();
        lbl.setName(name);
        lbl.setHorizontalAlignment(JLabel.CENTER);
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createLoweredBevelBorder());
        return lbl;
    }

    protected static String coordToString(LatLon coord) {
        if (coord == null)
            return tr("(none)");
        StringBuilder sb = new StringBuilder();
        sb.append('(')
        .append(COORD_FORMATTER.format(coord.lat()))
        .append(',')
        .append(COORD_FORMATTER.format(coord.lon()))
        .append(')');
        return sb.toString();
    }

    protected static String deletedStateToString(Boolean deleted) {
        if (deleted == null)
            return tr("(none)");
        if (deleted)
            return tr("deleted");
        else
            return tr("not deleted");
    }

    protected static String referrersToString(List<OsmPrimitive> referrers) {
        if (referrers.isEmpty())
            return tr("(none)");
        return referrers.stream()
                .map(r -> Utils.escapeReservedCharactersHTML(r.getDisplayName(DefaultNameFormatter.getInstance())) + "<br>")
                .collect(Collectors.joining("", "<html>", "</html>"));
    }

    protected void updateCoordinates() {
        lblMyCoordinates.setText(coordToString(model.getMyCoords()));
        lblMergedCoordinates.setText(coordToString(model.getMergedCoords()));
        lblTheirCoordinates.setText(coordToString(model.getTheirCoords()));
        if (!model.hasCoordConflict()) {
            lblMyCoordinates.setBackground(ConflictColors.BGCOLOR_NO_CONFLICT.get());
            lblMergedCoordinates.setBackground(ConflictColors.BGCOLOR_NO_CONFLICT.get());
            lblTheirCoordinates.setBackground(ConflictColors.BGCOLOR_NO_CONFLICT.get());
        } else {
            if (!model.isDecidedCoord()) {
                lblMyCoordinates.setBackground(ConflictColors.BGCOLOR_UNDECIDED.get());
                lblMergedCoordinates.setBackground(ConflictColors.BGCOLOR_NO_CONFLICT.get());
                lblTheirCoordinates.setBackground(ConflictColors.BGCOLOR_UNDECIDED.get());
            } else {
                lblMyCoordinates.setBackground(
                        model.isCoordMergeDecision(MergeDecisionType.KEEP_MINE)
                        ? ConflictColors.BGCOLOR_DECIDED.get() : ConflictColors.BGCOLOR_NO_CONFLICT.get()
                );
                lblMergedCoordinates.setBackground(ConflictColors.BGCOLOR_DECIDED.get());
                lblTheirCoordinates.setBackground(
                        model.isCoordMergeDecision(MergeDecisionType.KEEP_THEIR)
                        ? ConflictColors.BGCOLOR_DECIDED.get() : ConflictColors.BGCOLOR_NO_CONFLICT.get()
                );
            }
        }
    }

    protected void updateDeletedState() {
        lblMyDeletedState.setText(deletedStateToString(model.getMyDeletedState()));
        lblMergedDeletedState.setText(deletedStateToString(model.getMergedDeletedState()));
        lblTheirDeletedState.setText(deletedStateToString(model.getTheirDeletedState()));

        if (!model.hasDeletedStateConflict()) {
            lblMyDeletedState.setBackground(ConflictColors.BGCOLOR_NO_CONFLICT.get());
            lblMergedDeletedState.setBackground(ConflictColors.BGCOLOR_NO_CONFLICT.get());
            lblTheirDeletedState.setBackground(ConflictColors.BGCOLOR_NO_CONFLICT.get());
        } else {
            if (!model.isDecidedDeletedState()) {
                lblMyDeletedState.setBackground(ConflictColors.BGCOLOR_UNDECIDED.get());
                lblMergedDeletedState.setBackground(ConflictColors.BGCOLOR_NO_CONFLICT.get());
                lblTheirDeletedState.setBackground(ConflictColors.BGCOLOR_UNDECIDED.get());
            } else {
                lblMyDeletedState.setBackground(
                        model.isDeletedStateDecision(MergeDecisionType.KEEP_MINE)
                        ? ConflictColors.BGCOLOR_DECIDED.get() : ConflictColors.BGCOLOR_NO_CONFLICT.get()
                );
                lblMergedDeletedState.setBackground(ConflictColors.BGCOLOR_DECIDED.get());
                lblTheirDeletedState.setBackground(
                        model.isDeletedStateDecision(MergeDecisionType.KEEP_THEIR)
                        ? ConflictColors.BGCOLOR_DECIDED.get() : ConflictColors.BGCOLOR_NO_CONFLICT.get()
                );
            }
        }
    }

    protected void updateReferrers() {
        lblMyReferrers.setText(referrersToString(model.getMyReferrers()));
        lblMyReferrers.setBackground(ConflictColors.BGCOLOR_NO_CONFLICT.get());
        lblTheirReferrers.setText(referrersToString(model.getTheirReferrers()));
        lblTheirReferrers.setBackground(ConflictColors.BGCOLOR_NO_CONFLICT.get());
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        updateCoordinates();
        updateDeletedState();
        updateReferrers();
    }

    /**
     * Returns properties merge model.
     * @return properties merge model
     */
    public PropertiesMergeModel getModel() {
        return model;
    }

    private final class MergeDeletedStateRow extends AbstractMergePanel.MergeRow {
        @Override
        protected JComponent rowTitle() {
            return new JLabel(tr("Deleted State:"));
        }

        @Override
        protected JComponent mineField() {
            return lblMyDeletedState;
        }

        @Override
        protected JComponent mineButton() {
            KeepMyDeletedStateAction actKeepMyDeletedState = new KeepMyDeletedStateAction();
            model.addChangeListener(actKeepMyDeletedState);
            JButton btnKeepMyDeletedState = new JButton(actKeepMyDeletedState);
            btnKeepMyDeletedState.setName("button.keepmydeletedstate");
            return btnKeepMyDeletedState;
        }

        @Override
        protected JComponent merged() {
            return lblMergedDeletedState;
        }

        @Override
        protected JComponent theirsButton() {
            KeepTheirDeletedStateAction actKeepTheirDeletedState = new KeepTheirDeletedStateAction();
            model.addChangeListener(actKeepTheirDeletedState);
            JButton btnKeepTheirDeletedState = new JButton(actKeepTheirDeletedState);
            btnKeepTheirDeletedState.setName("button.keeptheirdeletedstate");
            return btnKeepTheirDeletedState;
        }

        @Override
        protected JComponent theirsField() {
            return lblTheirDeletedState;
        }
    }

    private final class MergeCoordinatesRow extends AbstractMergePanel.MergeRow {
        @Override
        protected JComponent rowTitle() {
            return new JLabel(tr("Coordinates:"));
        }

        @Override
        protected JComponent mineField() {
            return lblMyCoordinates;
        }

        @Override
        protected JComponent mineButton() {
            KeepMyCoordinatesAction actKeepMyCoordinates = new KeepMyCoordinatesAction();
            model.addChangeListener(actKeepMyCoordinates);
            JButton btnKeepMyCoordinates = new JButton(actKeepMyCoordinates);
            btnKeepMyCoordinates.setName("button.keepmycoordinates");
            return btnKeepMyCoordinates;
        }

        @Override
        protected JComponent merged() {
            return lblMergedCoordinates;
        }

        @Override
        protected JComponent theirsButton() {
            KeepTheirCoordinatesAction actKeepTheirCoordinates = new KeepTheirCoordinatesAction();
            model.addChangeListener(actKeepTheirCoordinates);
            JButton btnKeepTheirCoordinates = new JButton(actKeepTheirCoordinates);
            btnKeepTheirCoordinates.setName("button.keeptheircoordinates");
            return btnKeepTheirCoordinates;
        }

        @Override
        protected JComponent theirsField() {
            return lblTheirCoordinates;
        }
    }

    private final class UndecideCoordinatesRow extends AbstractUndecideRow {
        @Override
        protected UndecideCoordinateConflictAction createAction() {
            UndecideCoordinateConflictAction action = new UndecideCoordinateConflictAction();
            model.addChangeListener(action);
            return action;
        }

        @Override
        protected String getButtonName() {
            return "button.undecidecoordinates";
        }
    }

    private final class UndecideDeletedStateRow extends AbstractUndecideRow {
        @Override
        protected UndecideDeletedStateConflictAction createAction() {
            UndecideDeletedStateConflictAction action = new UndecideDeletedStateConflictAction();
            model.addChangeListener(action);
            return action;
        }

        @Override
        protected String getButtonName() {
            return "button.undecidedeletedstate";
        }
    }

    private final class VersionInfoRow extends AbstractMergePanel.MergeRowWithoutButton {
        @Override
        protected JComponent mineField() {
            return mineVersionInfo;
        }

        @Override
        protected JComponent theirsField() {
            return theirVersionInfo;
        }
    }

    private final class ReferrersRow extends AbstractMergePanel.MergeRow {
        @Override
        protected JComponent rowTitle() {
            return new JLabel(tr("Referenced by:"));
        }

        @Override
        protected JComponent mineField() {
            return lblMyReferrers;
        }

        @Override
        protected JComponent theirsField() {
            return lblTheirReferrers;
        }
    }

    private static final class EmptyFillRow extends AbstractMergePanel.MergeRow {
        @Override
        protected JComponent merged() {
            return new JPanel();
        }

        @Override
        protected void addConstraints(GBC constraints, int columnIndex) {
            super.addConstraints(constraints, columnIndex);
            // fill to bottom
            constraints.weighty = 1;
        }
    }

    class KeepMyCoordinatesAction extends AbstractAction implements ChangeListener {
        KeepMyCoordinatesAction() {
            new ImageProvider("dialogs/conflict", "tagkeepmine").getResource().attachImageIcon(this, true);
            putValue(Action.SHORT_DESCRIPTION, tr("Keep my coordinates"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.decideCoordsConflict(MergeDecisionType.KEEP_MINE);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            setEnabled(model.hasCoordConflict() && !model.isDecidedCoord() && model.getMyCoords() != null);
        }
    }

    class KeepTheirCoordinatesAction extends AbstractAction implements ChangeListener {
        KeepTheirCoordinatesAction() {
            new ImageProvider("dialogs/conflict", "tagkeeptheir").getResource().attachImageIcon(this, true);
            putValue(Action.SHORT_DESCRIPTION, tr("Keep their coordinates"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.decideCoordsConflict(MergeDecisionType.KEEP_THEIR);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            setEnabled(model.hasCoordConflict() && !model.isDecidedCoord() && model.getTheirCoords() != null);
        }
    }

    class UndecideCoordinateConflictAction extends AbstractAction implements ChangeListener {
        UndecideCoordinateConflictAction() {
            new ImageProvider("dialogs/conflict", "tagundecide").getResource().attachImageIcon(this, true);
            putValue(Action.SHORT_DESCRIPTION, tr("Undecide conflict between different coordinates"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.decideCoordsConflict(MergeDecisionType.UNDECIDED);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            setEnabled(model.hasCoordConflict() && model.isDecidedCoord());
        }
    }

    class KeepMyDeletedStateAction extends AbstractAction implements ChangeListener {
        KeepMyDeletedStateAction() {
            new ImageProvider("dialogs/conflict", "tagkeepmine").getResource().attachImageIcon(this, true);
            putValue(Action.SHORT_DESCRIPTION, tr("Keep my deleted state"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.decideDeletedStateConflict(MergeDecisionType.KEEP_MINE);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            setEnabled(model.hasDeletedStateConflict() && !model.isDecidedDeletedState());
        }
    }

    class KeepTheirDeletedStateAction extends AbstractAction implements ChangeListener {
        KeepTheirDeletedStateAction() {
            new ImageProvider("dialogs/conflict", "tagkeeptheir").getResource().attachImageIcon(this, true);
            putValue(Action.SHORT_DESCRIPTION, tr("Keep their deleted state"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.decideDeletedStateConflict(MergeDecisionType.KEEP_THEIR);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            setEnabled(model.hasDeletedStateConflict() && !model.isDecidedDeletedState());
        }
    }

    class UndecideDeletedStateConflictAction extends AbstractAction implements ChangeListener {
        UndecideDeletedStateConflictAction() {
            new ImageProvider("dialogs/conflict", "tagundecide").getResource().attachImageIcon(this, true);
            putValue(Action.SHORT_DESCRIPTION, tr("Undecide conflict between deleted state"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.decideDeletedStateConflict(MergeDecisionType.UNDECIDED);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            setEnabled(model.hasDeletedStateConflict() && model.isDecidedDeletedState());
        }
    }

    @Override
    public void deletePrimitive(boolean deleted) {
        if (deleted) {
            if (model.getMergedCoords() == null) {
                model.decideCoordsConflict(MergeDecisionType.KEEP_MINE);
            }
        } else {
            model.decideCoordsConflict(MergeDecisionType.UNDECIDED);
        }
    }

    @Override
    public void populate(Conflict<? extends OsmPrimitive> conflict) {
        model.populate(conflict);
        mineVersionInfo.update(conflict.getMy(), true);
        theirVersionInfo.update(conflict.getTheir(), false);
    }

    @Override
    public void decideRemaining(MergeDecisionType decision) {
        if (!model.isDecidedCoord()) {
            model.decideDeletedStateConflict(decision);
        }
        if (!model.isDecidedCoord()) {
            model.decideCoordsConflict(decision);
        }
    }
}
