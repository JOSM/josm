// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.properties;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.conflict.pair.IConflictResolver;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This class represents a UI component for resolving conflicts in some properties
 * of {@see OsmPrimitive}.
 *
 */
public class PropertiesMerger extends JPanel implements Observer, IConflictResolver {
    private static DecimalFormat COORD_FORMATTER = new DecimalFormat("###0.0000000");

    public final static Color BGCOLOR_NO_CONFLICT = new Color(234,234,234);
    public final static Color BGCOLOR_UNDECIDED = new Color(255,197,197);
    public final static Color BGCOLOR_DECIDED = new Color(217,255,217);

    private  JLabel lblMyVersion;
    private  JLabel lblMergedVersion;
    private  JLabel lblTheirVersion;

    private JLabel lblMyCoordinates;
    private JLabel lblMergedCoordinates;
    private JLabel lblTheirCoordinates;

    private JLabel lblMyDeletedState;
    private JLabel lblMergedDeletedState;
    private JLabel lblTheirDeletedState;

    private JLabel lblMyVisibleState;
    private JLabel lblMergedVisibleState;
    private JLabel lblTheirVisibleState;

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
        lblMyVersion = new JLabel(tr("My version"));
        lblMyVersion.setToolTipText(tr("Properties in my dataset, i.e. the local dataset"));
        add(lblMyVersion, gc);

        gc.gridx = 3;
        gc.gridy = 0;
        lblMergedVersion = new JLabel(tr("Merged version"));
        lblMergedVersion.setToolTipText(tr("Properties in the merged element. They will replace properties in my elements when merge decisions are applied."));
        add(lblMergedVersion, gc);

        gc.gridx = 5;
        gc.gridy = 0;
        lblTheirVersion = new JLabel(tr("Their version"));
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

    protected void buildVisibleStateRows() {
        GridBagConstraints gc = new GridBagConstraints();

        gc.gridx = 0;
        gc.gridy = 5;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.insets = new Insets(0,5,0,5);
        add(new JLabel(tr("Visible State:")), gc);

        gc.gridx = 1;
        gc.gridy = 5;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.33;
        gc.weighty = 0.0;
        add(lblMyVisibleState = buildValueLabel("label.myvisiblestate"), gc);

        gc.gridx = 2;
        gc.gridy = 5;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        KeepMyVisibleStateAction actKeepMyVisibleState = new KeepMyVisibleStateAction();
        model.addObserver(actKeepMyVisibleState);
        JButton btnKeepMyVisibleState = new JButton(actKeepMyVisibleState);
        btnKeepMyVisibleState.setName("button.keepmyvisiblestate");
        add(btnKeepMyVisibleState, gc);

        gc.gridx = 3;
        gc.gridy = 5;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.33;
        gc.weighty = 0.0;
        add(lblMergedVisibleState = buildValueLabel("label.mergedvisiblestate"), gc);

        gc.gridx = 4;
        gc.gridy = 5;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        KeepTheirVisibleStateAction actKeepTheirVisibleState = new KeepTheirVisibleStateAction();
        model.addObserver(actKeepTheirVisibleState);
        JButton btnKeepTheirVisibleState = new JButton(actKeepTheirVisibleState);
        btnKeepTheirVisibleState.setName("button.keeptheirvisiblestate");
        add(btnKeepTheirVisibleState, gc);

        gc.gridx = 5;
        gc.gridy = 5;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.33;
        gc.weighty = 0.0;
        add(lblTheirVisibleState = buildValueLabel("label.theirvisiblestate"), gc);

        // ---------------------------------------------------
        gc.gridx = 3;
        gc.gridy = 6;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        UndecideVisibleStateConflictAction actUndecideVisibleState = new UndecideVisibleStateConflictAction();
        model.addObserver(actUndecideVisibleState);
        JButton btnUndecideVisibleState = new JButton(actUndecideVisibleState);
        btnUndecideVisibleState.setName("button.undecidevisiblestate");
        add(btnUndecideVisibleState, gc);
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
        buildVisibleStateRows();
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

    public String visibleStateToString(Boolean visible) {
        if (visible == null)
            return tr("(none)");
        if (visible)
            return tr("visible (on the server)");
        else
            return tr("not visible (on the server)");
    }

    public String visibleStateToStringMerged(Boolean visible) {
        if (visible == null)
            return tr("(none)");
        if (visible)
            return tr("Keep a clone of the local version");
        else
            return tr("Physically delete from local dataset");
    }

    public String referrersToString(List<OsmPrimitive> referrers) {
        if (referrers.isEmpty())
            return tr("(none)");
        String str = "<html>";
        for (OsmPrimitive r: referrers) {
            str = str + r.getDisplayName(DefaultNameFormatter.getInstance()) + "<br>";
        }
        str = str + "</html>";
        return str;
    }

    protected void updateCoordinates() {
        lblMyCoordinates.setText(coordToString(model.getMyCoords()));
        lblMergedCoordinates.setText(coordToString(model.getMergedCoords()));
        lblTheirCoordinates.setText(coordToString(model.getTheirCoords()));
        if (! model.hasCoordConflict()) {
            lblMyCoordinates.setBackground(BGCOLOR_NO_CONFLICT);
            lblMergedCoordinates.setBackground(BGCOLOR_NO_CONFLICT);
            lblTheirCoordinates.setBackground(BGCOLOR_NO_CONFLICT);
        } else {
            if (!model.isDecidedCoord()) {
                lblMyCoordinates.setBackground(BGCOLOR_UNDECIDED);
                lblMergedCoordinates.setBackground(BGCOLOR_NO_CONFLICT);
                lblTheirCoordinates.setBackground(BGCOLOR_UNDECIDED);
            } else {
                lblMyCoordinates.setBackground(
                        model.isCoordMergeDecision(MergeDecisionType.KEEP_MINE)
                        ? BGCOLOR_DECIDED : BGCOLOR_NO_CONFLICT
                );
                lblMergedCoordinates.setBackground(BGCOLOR_DECIDED);
                lblTheirCoordinates.setBackground(
                        model.isCoordMergeDecision(MergeDecisionType.KEEP_THEIR)
                        ? BGCOLOR_DECIDED : BGCOLOR_NO_CONFLICT
                );
            }
        }
    }

    protected void updateDeletedState() {
        lblMyDeletedState.setText(deletedStateToString(model.getMyDeletedState()));
        lblMergedDeletedState.setText(deletedStateToString(model.getMergedDeletedState()));
        lblTheirDeletedState.setText(deletedStateToString(model.getTheirDeletedState()));

        if (! model.hasDeletedStateConflict()) {
            lblMyDeletedState.setBackground(BGCOLOR_NO_CONFLICT);
            lblMergedDeletedState.setBackground(BGCOLOR_NO_CONFLICT);
            lblTheirDeletedState.setBackground(BGCOLOR_NO_CONFLICT);
        } else {
            if (!model.isDecidedDeletedState()) {
                lblMyDeletedState.setBackground(BGCOLOR_UNDECIDED);
                lblMergedDeletedState.setBackground(BGCOLOR_NO_CONFLICT);
                lblTheirDeletedState.setBackground(BGCOLOR_UNDECIDED);
            } else {
                lblMyDeletedState.setBackground(
                        model.isDeletedStateDecision(MergeDecisionType.KEEP_MINE)
                        ? BGCOLOR_DECIDED : BGCOLOR_NO_CONFLICT
                );
                lblMergedDeletedState.setBackground(BGCOLOR_DECIDED);
                lblTheirDeletedState.setBackground(
                        model.isDeletedStateDecision(MergeDecisionType.KEEP_THEIR)
                        ? BGCOLOR_DECIDED : BGCOLOR_NO_CONFLICT
                );
            }
        }
    }

    protected void updateVisibleState() {
        lblMyVisibleState.setText(visibleStateToString(model.getMyVisibleState()));
        lblMergedVisibleState.setText(visibleStateToStringMerged(model.getMergedVisibleState()));
        lblTheirVisibleState.setText(visibleStateToString(model.getTheirVisibleState()));

        if (! model.hasVisibleStateConflict()) {
            lblMyVisibleState.setBackground(BGCOLOR_NO_CONFLICT);
            lblMergedVisibleState.setBackground(BGCOLOR_NO_CONFLICT);
            lblTheirVisibleState.setBackground(BGCOLOR_NO_CONFLICT);
        } else {
            if (!model.isDecidedVisibleState()) {
                lblMyVisibleState.setBackground(BGCOLOR_UNDECIDED);
                lblMergedVisibleState.setBackground(BGCOLOR_NO_CONFLICT);
                lblTheirVisibleState.setBackground(BGCOLOR_UNDECIDED);
            } else {
                lblMyVisibleState.setBackground(
                        model.isVisibleStateDecision(MergeDecisionType.KEEP_MINE)
                        ? BGCOLOR_DECIDED : BGCOLOR_NO_CONFLICT
                );
                lblMergedVisibleState.setBackground(BGCOLOR_DECIDED);
                lblTheirVisibleState.setBackground(
                        model.isVisibleStateDecision(MergeDecisionType.KEEP_THEIR)
                        ? BGCOLOR_DECIDED : BGCOLOR_NO_CONFLICT
                );
            }
        }
    }

    protected void updateReferrers() {
        lblMyReferrers.setText(referrersToString(model.getMyReferrers()));
        lblMyReferrers.setBackground(BGCOLOR_NO_CONFLICT);
        lblTheirReferrers.setText(referrersToString(model.getTheirReferrers()));
        lblTheirReferrers.setBackground(BGCOLOR_NO_CONFLICT);
    }

    public void update(Observable o, Object arg) {
        updateCoordinates();
        updateDeletedState();
        updateVisibleState();
        updateReferrers();
    }

    public PropertiesMergeModel getModel() {
        return model;
    }

    class KeepMyCoordinatesAction extends AbstractAction implements Observer {
        public KeepMyCoordinatesAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs/conflict", "tagkeepmine"));
            putValue(Action.SHORT_DESCRIPTION, tr("Keep my coordiates"));
        }

        public void actionPerformed(ActionEvent e) {
            model.decideCoordsConflict(MergeDecisionType.KEEP_MINE);
        }

        public void update(Observable o, Object arg) {
            setEnabled(model.hasCoordConflict() && ! model.isDecidedCoord());
        }
    }

    class KeepTheirCoordinatesAction extends AbstractAction implements Observer {
        public KeepTheirCoordinatesAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs/conflict", "tagkeeptheir"));
            putValue(Action.SHORT_DESCRIPTION, tr("Keep their coordiates"));
        }

        public void actionPerformed(ActionEvent e) {
            model.decideCoordsConflict(MergeDecisionType.KEEP_THEIR);
        }

        public void update(Observable o, Object arg) {
            setEnabled(model.hasCoordConflict() && ! model.isDecidedCoord());
        }
    }

    class UndecideCoordinateConflictAction extends AbstractAction implements Observer {
        public UndecideCoordinateConflictAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs/conflict", "tagundecide"));
            putValue(Action.SHORT_DESCRIPTION, tr("Undecide conflict between different coordinates"));
        }

        public void actionPerformed(ActionEvent e) {
            model.decideCoordsConflict(MergeDecisionType.UNDECIDED);
        }

        public void update(Observable o, Object arg) {
            setEnabled(model.hasCoordConflict() && model.isDecidedCoord());
        }
    }

    class KeepMyDeletedStateAction extends AbstractAction implements Observer {
        public KeepMyDeletedStateAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs/conflict", "tagkeepmine"));
            putValue(Action.SHORT_DESCRIPTION, tr("Keep my deleted state"));
        }

        public void actionPerformed(ActionEvent e) {
            model.decideDeletedStateConflict(MergeDecisionType.KEEP_MINE);
        }

        public void update(Observable o, Object arg) {
            setEnabled(model.hasDeletedStateConflict() && ! model.isDecidedDeletedState());
        }
    }

    class KeepTheirDeletedStateAction extends AbstractAction implements Observer {
        public KeepTheirDeletedStateAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs/conflict", "tagkeeptheir"));
            putValue(Action.SHORT_DESCRIPTION, tr("Keep their deleted state"));
        }

        public void actionPerformed(ActionEvent e) {
            model.decideDeletedStateConflict(MergeDecisionType.KEEP_THEIR);
        }

        public void update(Observable o, Object arg) {
            setEnabled(model.hasDeletedStateConflict() && ! model.isDecidedDeletedState());
        }
    }

    class UndecideDeletedStateConflictAction extends AbstractAction implements Observer {
        public UndecideDeletedStateConflictAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs/conflict", "tagundecide"));
            putValue(Action.SHORT_DESCRIPTION, tr("Undecide conflict between deleted state"));
        }

        public void actionPerformed(ActionEvent e) {
            model.decideDeletedStateConflict(MergeDecisionType.UNDECIDED);
        }

        public void update(Observable o, Object arg) {
            setEnabled(model.hasDeletedStateConflict() && model.isDecidedDeletedState());
        }
    }

    class KeepMyVisibleStateAction extends AbstractAction implements Observer {
        public KeepMyVisibleStateAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs/conflict", "tagkeepmine"));
            putValue(Action.SHORT_DESCRIPTION, tr("Keep my visible state"));
        }

        public void actionPerformed(ActionEvent e) {
            if (confirmKeepMine()) {
                model.decideVisibleStateConflict(MergeDecisionType.KEEP_MINE);
            }
        }

        public void update(Observable o, Object arg) {
            setEnabled(model.hasVisibleStateConflict() && ! model.isDecidedVisibleState());
        }

        protected boolean confirmKeepMine() {
            String [] options = {
                    tr("Yes, reset the id"),
                    tr("No, abort")
            };
            int ret = JOptionPane.showOptionDialog(
                    null,
                    tr("<html>To keep your local version, JOSM<br>"
                            + "has to reset the id of primitive {0} to 0.<br>"
                            + "On the next upload the server will assign<br>"
                            + "it a new id.<br>"
                            + "Do yo agree?</html>",
                            model.getMyPrimitive().getId()
                    ),
                    tr("Reset id to 0"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[1]
            );
            return ret == JOptionPane.YES_OPTION;
        }
    }

    class KeepTheirVisibleStateAction extends AbstractAction implements Observer {
        public KeepTheirVisibleStateAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs/conflict", "tagkeeptheir"));
            putValue(Action.SHORT_DESCRIPTION, tr("Keep their visible state"));
        }

        public void actionPerformed(ActionEvent e) {
            if (confirmKeepTheir()){
                model.decideVisibleStateConflict(MergeDecisionType.KEEP_THEIR);
            }
        }

        public void update(Observable o, Object arg) {
            setEnabled(model.hasVisibleStateConflict() && ! model.isDecidedVisibleState());
        }

        protected boolean confirmKeepTheir() {
            String [] options = {
                    tr("Yes, purge it"),
                    tr("No, abort")
            };
            int ret = JOptionPane.showOptionDialog(
                    null,
                    tr("<html>JOSM will have to remove your local primitive with id {0}<br>"
                            + "from the dataset.<br>"
                            + "Do you agree?</html>",
                            model.getMyPrimitive().getId()
                    ),
                    tr("Remove from dataset"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[1]
            );
            return ret == JOptionPane.YES_OPTION;
        }
    }

    class UndecideVisibleStateConflictAction extends AbstractAction implements Observer {
        public UndecideVisibleStateConflictAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs/conflict", "tagundecide"));
            putValue(Action.SHORT_DESCRIPTION, tr("Undecide conflict between visible state"));
        }

        public void actionPerformed(ActionEvent e) {
            model.decideVisibleStateConflict(MergeDecisionType.UNDECIDED);
        }

        public void update(Observable o, Object arg) {
            setEnabled(model.hasVisibleStateConflict() && model.isDecidedVisibleState());
        }
    }

    public void deletePrimitive(boolean deleted) {
        if (deleted) {
            if (model.getMergedCoords() == null) {
                model.decideCoordsConflict(MergeDecisionType.KEEP_MINE);
            }
        } else {
            model.decideCoordsConflict(MergeDecisionType.UNDECIDED);
        }
    }

    public void populate(Conflict<? extends OsmPrimitive> conflict) {
        model.populate(conflict);
    }
}
