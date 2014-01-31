// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.properties;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.conflict.ConflictColors;
import org.openstreetmap.josm.gui.conflict.pair.IConflictResolver;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This class represents a UI component for resolving conflicts in some properties
 * of {@link OsmPrimitive}.
 *
 */
public class PropertiesMerger extends JPanel implements Observer, IConflictResolver {
    private static final DecimalFormat COORD_FORMATTER = new DecimalFormat("###0.0000000");

    private JLabel lblMyCoordinates;
    private JLabel lblMergedCoordinates;
    private JLabel lblTheirCoordinates;

    private JLabel lblMyDeletedState;
    private JLabel lblMergedDeletedState;
    private JLabel lblTheirDeletedState;

    private JLabel lblMyReferrers;
    private JLabel lblTheirReferrers;

    private final PropertiesMergeModel model;

    protected JLabel buildValueLabel(String name) {
        JLabel lbl = new JLabel();
        lbl.setName(name);
        lbl.setHorizontalAlignment(JLabel.CENTER);
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createLoweredBevelBorder());
        return lbl;
    }

    protected void buildHeaderRow() {
        GridBagConstraints gc = new GridBagConstraints();

        gc.gridx = 1;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.insets = new Insets(10,0,10,0);
        JLabel lblMyVersion = new JLabel(tr("My version"));
        lblMyVersion.setToolTipText(tr("Properties in my dataset, i.e. the local dataset"));
        add(lblMyVersion, gc);

        gc.gridx = 3;
        gc.gridy = 0;
        JLabel lblMergedVersion = new JLabel(tr("Merged version"));
        lblMergedVersion.setToolTipText(tr("Properties in the merged element. They will replace properties in my elements when merge decisions are applied."));
        add(lblMergedVersion, gc);

        gc.gridx = 5;
        gc.gridy = 0;
        JLabel lblTheirVersion = new JLabel(tr("Their version"));
        lblTheirVersion.setToolTipText(tr("Properties in their dataset, i.e. the server dataset"));
        add(lblTheirVersion, gc);
    }

    protected void buildCoordinateConflictRows() {
        GridBagConstraints gc = new GridBagConstraints();

        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.insets = new Insets(0,5,0,5);
        add(new JLabel(tr("Coordinates:")), gc);

        gc.gridx = 1;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.33;
        gc.weighty = 0.0;
        add(lblMyCoordinates = buildValueLabel("label.mycoordinates"), gc);

        gc.gridx = 2;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        KeepMyCoordinatesAction actKeepMyCoordinates = new KeepMyCoordinatesAction();
        model.addObserver(actKeepMyCoordinates);
        JButton btnKeepMyCoordinates = new JButton(actKeepMyCoordinates);
        btnKeepMyCoordinates.setName("button.keepmycoordinates");
        add(btnKeepMyCoordinates, gc);

        gc.gridx = 3;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.33;
        gc.weighty = 0.0;
        add(lblMergedCoordinates = buildValueLabel("label.mergedcoordinates"), gc);

        gc.gridx = 4;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        KeepTheirCoordinatesAction actKeepTheirCoordinates = new KeepTheirCoordinatesAction();
        model.addObserver(actKeepTheirCoordinates);
        JButton btnKeepTheirCoordinates = new JButton(actKeepTheirCoordinates);
        add(btnKeepTheirCoordinates, gc);

        gc.gridx = 5;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.33;
        gc.weighty = 0.0;
        add(lblTheirCoordinates = buildValueLabel("label.theircoordinates"), gc);

        // ---------------------------------------------------
        gc.gridx = 3;
        gc.gridy = 2;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        UndecideCoordinateConflictAction actUndecideCoordinates = new UndecideCoordinateConflictAction();
        model.addObserver(actUndecideCoordinates);
        JButton btnUndecideCoordinates = new JButton(actUndecideCoordinates);
        add(btnUndecideCoordinates, gc);
    }

    protected void buildDeletedStateConflictRows() {
        GridBagConstraints gc = new GridBagConstraints();

        gc.gridx = 0;
        gc.gridy = 3;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.insets = new Insets(0,5,0,5);
        add(new JLabel(tr("Deleted State:")), gc);

        gc.gridx = 1;
        gc.gridy = 3;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.33;
        gc.weighty = 0.0;
        add(lblMyDeletedState = buildValueLabel("label.mydeletedstate"), gc);

        gc.gridx = 2;
        gc.gridy = 3;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        KeepMyDeletedStateAction actKeepMyDeletedState = new KeepMyDeletedStateAction();
        model.addObserver(actKeepMyDeletedState);
        JButton btnKeepMyDeletedState = new JButton(actKeepMyDeletedState);
        btnKeepMyDeletedState.setName("button.keepmydeletedstate");
        add(btnKeepMyDeletedState, gc);

        gc.gridx = 3;
        gc.gridy = 3;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.33;
        gc.weighty = 0.0;
        add(lblMergedDeletedState = buildValueLabel("label.mergeddeletedstate"), gc);

        gc.gridx = 4;
        gc.gridy = 3;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        KeepTheirDeletedStateAction actKeepTheirDeletedState = new KeepTheirDeletedStateAction();
        model.addObserver(actKeepTheirDeletedState);
        JButton btnKeepTheirDeletedState = new JButton(actKeepTheirDeletedState);
        btnKeepTheirDeletedState.setName("button.keeptheirdeletedstate");
        add(btnKeepTheirDeletedState, gc);

        gc.gridx = 5;
        gc.gridy = 3;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.33;
        gc.weighty = 0.0;
        add(lblTheirDeletedState = buildValueLabel("label.theirdeletedstate"), gc);

        // ---------------------------------------------------
        gc.gridx = 3;
        gc.gridy = 4;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        UndecideDeletedStateConflictAction actUndecideDeletedState = new UndecideDeletedStateConflictAction();
        model.addObserver(actUndecideDeletedState);
        JButton btnUndecideDeletedState = new JButton(actUndecideDeletedState);
        btnUndecideDeletedState.setName("button.undecidedeletedstate");
        add(btnUndecideDeletedState, gc);
    }

    protected void buildReferrersRow() {
        GridBagConstraints gc = new GridBagConstraints();

        gc.gridx = 0;
        gc.gridy = 7;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.insets = new Insets(0,5,0,5);
        add(new JLabel(tr("Referenced by:")), gc);

        gc.gridx = 1;
        gc.gridy = 7;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.33;
        gc.weighty = 0.0;
        add(lblMyReferrers = buildValueLabel("label.myreferrers"), gc);

        gc.gridx = 5;
        gc.gridy = 7;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.33;
        gc.weighty = 0.0;
        add(lblTheirReferrers = buildValueLabel("label.theirreferrers"), gc);
    }

    protected void build() {
        setLayout(new GridBagLayout());
        buildHeaderRow();
        buildCoordinateConflictRows();
        buildDeletedStateConflictRows();
        buildReferrersRow();
    }

    public PropertiesMerger() {
        model = new PropertiesMergeModel();
        model.addObserver(this);
        build();
    }

    public String coordToString(LatLon coord) {
        if (coord == null)
            return tr("(none)");
        StringBuilder sb = new StringBuilder();
        sb.append("(")
        .append(COORD_FORMATTER.format(coord.lat()))
        .append(",")
        .append(COORD_FORMATTER.format(coord.lon()))
        .append(")");
        return sb.toString();
    }

    public String deletedStateToString(Boolean deleted) {
        if (deleted == null)
            return tr("(none)");
        if (deleted)
            return tr("deleted");
        else
            return tr("not deleted");
    }

    public String referrersToString(List<OsmPrimitive> referrers) {
        if (referrers.isEmpty())
            return tr("(none)");
        StringBuilder str = new StringBuilder("<html>");
        for (OsmPrimitive r: referrers) {
            str.append(r.getDisplayName(DefaultNameFormatter.getInstance())).append("<br>");
        }
        str.append("</html>");
        return str.toString();
    }

    protected void updateCoordinates() {
        lblMyCoordinates.setText(coordToString(model.getMyCoords()));
        lblMergedCoordinates.setText(coordToString(model.getMergedCoords()));
        lblTheirCoordinates.setText(coordToString(model.getTheirCoords()));
        if (! model.hasCoordConflict()) {
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

        if (! model.hasDeletedStateConflict()) {
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
    public void update(Observable o, Object arg) {
        updateCoordinates();
        updateDeletedState();
        updateReferrers();
    }

    public PropertiesMergeModel getModel() {
        return model;
    }

    class KeepMyCoordinatesAction extends AbstractAction implements Observer {
        public KeepMyCoordinatesAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs/conflict", "tagkeepmine"));
            putValue(Action.SHORT_DESCRIPTION, tr("Keep my coordinates"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.decideCoordsConflict(MergeDecisionType.KEEP_MINE);
        }

        @Override
        public void update(Observable o, Object arg) {
            setEnabled(model.hasCoordConflict() && ! model.isDecidedCoord());
        }
    }

    class KeepTheirCoordinatesAction extends AbstractAction implements Observer {
        public KeepTheirCoordinatesAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs/conflict", "tagkeeptheir"));
            putValue(Action.SHORT_DESCRIPTION, tr("Keep their coordinates"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.decideCoordsConflict(MergeDecisionType.KEEP_THEIR);
        }

        @Override
        public void update(Observable o, Object arg) {
            setEnabled(model.hasCoordConflict() && ! model.isDecidedCoord());
        }
    }

    class UndecideCoordinateConflictAction extends AbstractAction implements Observer {
        public UndecideCoordinateConflictAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs/conflict", "tagundecide"));
            putValue(Action.SHORT_DESCRIPTION, tr("Undecide conflict between different coordinates"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.decideCoordsConflict(MergeDecisionType.UNDECIDED);
        }

        @Override
        public void update(Observable o, Object arg) {
            setEnabled(model.hasCoordConflict() && model.isDecidedCoord());
        }
    }

    class KeepMyDeletedStateAction extends AbstractAction implements Observer {
        public KeepMyDeletedStateAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs/conflict", "tagkeepmine"));
            putValue(Action.SHORT_DESCRIPTION, tr("Keep my deleted state"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.decideDeletedStateConflict(MergeDecisionType.KEEP_MINE);
        }

        @Override
        public void update(Observable o, Object arg) {
            setEnabled(model.hasDeletedStateConflict() && ! model.isDecidedDeletedState());
        }
    }

    class KeepTheirDeletedStateAction extends AbstractAction implements Observer {
        public KeepTheirDeletedStateAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs/conflict", "tagkeeptheir"));
            putValue(Action.SHORT_DESCRIPTION, tr("Keep their deleted state"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.decideDeletedStateConflict(MergeDecisionType.KEEP_THEIR);
        }

        @Override
        public void update(Observable o, Object arg) {
            setEnabled(model.hasDeletedStateConflict() && ! model.isDecidedDeletedState());
        }
    }

    class UndecideDeletedStateConflictAction extends AbstractAction implements Observer {
        public UndecideDeletedStateConflictAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs/conflict", "tagundecide"));
            putValue(Action.SHORT_DESCRIPTION, tr("Undecide conflict between deleted state"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.decideDeletedStateConflict(MergeDecisionType.UNDECIDED);
        }

        @Override
        public void update(Observable o, Object arg) {
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
    }
}
