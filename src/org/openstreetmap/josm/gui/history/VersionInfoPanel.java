// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.ChangesetDialog;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager;
import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetDiscussionPanel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * VersionInfoPanel is an UI component which displays the basic properties of a version
 * of a {@link OsmPrimitive}.
 * @since 1709
 */
public class VersionInfoPanel extends JPanel implements ChangeListener {
    private final PointInTimeType pointInTimeType;
    private final transient HistoryBrowserModel model;
    private JMultilineLabel lblInfo;
    private UrlLabel lblUser;
    private UrlLabel lblChangeset;
    private final JButton lblChangesetComments = new JButton(ImageProvider.get("dialogs/notes/note_comment"));
    private final OpenChangesetDialogAction changesetCommentsDialogAction = new OpenChangesetDialogAction(ChangesetDiscussionPanel.class);
    private final OpenChangesetDialogAction changesetDialogAction = new OpenChangesetDialogAction(null);
    private final JButton changesetButton = new JButton(changesetDialogAction);
    private final BasicArrowButton arrowButton = new BasicArrowButton(BasicArrowButton.SOUTH);
    private JPanel pnlChangesetSource;
    private JPanel pnlChangesetImageryUsed;
    private JLabel lblSource;
    private JLabel lblImageryUsed;
    private JTextArea texChangesetComment;
    private JTextArea texChangesetSource;
    private JTextArea texChangesetImageryUsed;

    protected static JTextArea buildTextArea(String tooltip) {
        JTextArea lbl = new JTextArea();
        lbl.setLineWrap(true);
        lbl.setWrapStyleWord(true);
        lbl.setEditable(false);
        lbl.setOpaque(false);
        lbl.setToolTipText(tooltip);
        return lbl;
    }

    protected static JLabel buildLabel(String text, String tooltip, JTextArea textArea) {
        // We need text field to be a JTextArea for line wrapping but cannot put HTML code in here,
        // so create a separate JLabel with same characteristics (margin, font)
        JLabel lbl = new JLabel("<html><p style='margin-top:"+textArea.getMargin().top+"'>"+text+"</html>");
        lbl.setFont(textArea.getFont());
        lbl.setToolTipText(tooltip);
        lbl.setLabelFor(textArea);
        return lbl;
    }

    protected static JPanel buildTextPanel(JLabel label, JTextArea textArea) {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.add(label, GBC.std().anchor(GBC.NORTHWEST));
        pnl.add(textArea, GBC.eol().insets(2, 0, 0, 0).fill());
        return pnl;
    }

    protected void build() {
        JPanel pnl1 = new JPanel(new BorderLayout());
        lblInfo = new JMultilineLabel("");
        pnl1.add(lblInfo, BorderLayout.CENTER);

        // +-----------------------+-------------------------------------+
        // | User:                 | lblUser                             |
        // +-----------------------+-------------------------------------+
        // | changesetButton       | lblChangeset | lblChangesetComments |
        // +-----------------------+-------------------------------------+
        JPanel pnlUserAndChangeset = new JPanel(new GridBagLayout());
        pnlUserAndChangeset.add(new JLabel(tr("User:")), GBC.std());

        lblUser = new UrlLabel("", 2);
        pnlUserAndChangeset.add(lblUser, GBC.eol().insets(5, 0, 0, 0).weight(1, 0));

        final JPanel changesetPanel = new JPanel(new BorderLayout());
        changesetButton.setMargin(new Insets(0, 0, 0, 2));
        changesetPanel.add(changesetButton, BorderLayout.CENTER);
        arrowButton.addActionListener(action -> {
            if (changesetDialogAction.id != null) { // fix #15444, #16097
                final OpenChangesetPopupMenu popupMenu = new OpenChangesetPopupMenu(changesetDialogAction.id);
                popupMenu.insert(changesetDialogAction, 0);
                ((AbstractButton) popupMenu.getComponent(0)).setText(tr("Open Changeset Manager"));
                popupMenu.show(arrowButton);
            }
        });
        changesetPanel.add(arrowButton, BorderLayout.EAST);
        pnlUserAndChangeset.add(changesetPanel, GBC.std().fill().weight(0, 0));

        lblChangeset = new UrlLabel("", 2);
        pnlUserAndChangeset.add(lblChangeset, GBC.std().insets(5, 0, 0, 0).weight(1, 0));

        lblChangesetComments.setAction(changesetCommentsDialogAction);
        lblChangesetComments.setMargin(new Insets(0, 0, 0, 0));
        lblChangesetComments.setIcon(new ImageProvider("dialogs/notes/note_comment").setMaxSize(12).get());
        pnlUserAndChangeset.add(lblChangesetComments, GBC.eol());

        texChangesetComment = buildTextArea(tr("Changeset comment"));
        texChangesetSource = buildTextArea(tr("Changeset source"));
        texChangesetImageryUsed = buildTextArea(tr("Imagery used"));

        lblSource = buildLabel(tr("<b>Source</b>:"), tr("Changeset source"), texChangesetSource);
        lblImageryUsed = buildLabel(tr("<b>Imagery</b>:"), tr("Imagery used"), texChangesetImageryUsed);
        pnlChangesetSource = buildTextPanel(lblSource, texChangesetSource);
        pnlChangesetImageryUsed = buildTextPanel(lblImageryUsed, texChangesetImageryUsed);

        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        add(pnl1, gc);
        gc.gridy = 1;
        gc.weighty = 0.0;
        add(pnlUserAndChangeset, gc);
        gc.gridy = 2;
        add(texChangesetComment, gc);
        gc.gridy = 3;
        add(pnlChangesetSource, gc);
        gc.gridy = 4;
        add(pnlChangesetImageryUsed, gc);
    }

    protected HistoryOsmPrimitive getPrimitive() {
        if (model == null || pointInTimeType == null)
            return null;
        return model.getPointInTime(pointInTimeType);
    }

    protected String getInfoText(final Date timestamp, final long version, final boolean isLatest) {
        String text;
        if (isLatest) {
            OsmDataLayer editLayer = MainApplication.getLayerManager().getEditLayer();
            text = tr("<html>Version <strong>{0}</strong> currently edited in layer ''{1}''</html>",
                    Long.toString(version),
                    editLayer == null ? tr("unknown") : Utils.escapeReservedCharactersHTML(editLayer.getName())
                    );
        } else {
            String date = "?";
            if (timestamp != null) {
                date = DateUtils.formatDateTime(timestamp, DateFormat.SHORT, DateFormat.SHORT);
            }
            text = tr(
                    "<html>Version <strong>{0}</strong> created on <strong>{1}</strong></html>",
                    Long.toString(version), date);
        }
        return text;
    }

    /**
     * Constructs a new {@code VersionInfoPanel}.
     */
    public VersionInfoPanel() {
        pointInTimeType = null;
        model = null;
        build();
    }

    /**
     * constructor
     *
     * @param model  the model (must not be null)
     * @param pointInTimeType the point in time this panel visualizes (must not be null)
     * @throws IllegalArgumentException if model is null
     * @throws IllegalArgumentException if pointInTimeType is null
     */
    public VersionInfoPanel(HistoryBrowserModel model, PointInTimeType pointInTimeType) {
        CheckParameterUtil.ensureParameterNotNull(pointInTimeType, "pointInTimeType");
        CheckParameterUtil.ensureParameterNotNull(model, "model");

        this.model = model;
        this.pointInTimeType = pointInTimeType;
        model.addChangeListener(this);
        build();
    }

    protected static String getUserUrl(String username) {
        return Main.getBaseUserUrl() + '/' + Utils.encodeUrl(username).replaceAll("\\+", "%20");
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        HistoryOsmPrimitive primitive = getPrimitive();
        if (primitive != null) {
            Changeset cs = primitive.getChangeset();
            update(cs, model.isLatest(primitive), primitive.getTimestamp(), primitive.getVersion());
        }
    }

    /**
     * Updates the content of this panel based on the changeset information given by {@code primitive}.
     * @param primitive the primitive to extract the changeset information from
     * @param isLatest whether this relates to a not yet commited changeset
     */
    public void update(final OsmPrimitive primitive, final boolean isLatest) {
        update(Changeset.fromPrimitive(primitive), isLatest, primitive.getTimestamp(), primitive.getVersion());
    }

    /**
     * Updates the content of this panel based on the changeset information given by {@code cs}.
     * @param cs the changeset information
     * @param isLatest whether this relates to a not yet commited changeset
     * @param timestamp the timestamp
     * @param version the version of the primitive
     */
    public void update(final Changeset cs, final boolean isLatest, final Date timestamp, final long version) {
        lblInfo.setText(getInfoText(timestamp, version, isLatest));

        if (!isLatest && cs != null) {
            User user = cs.getUser();
            String url = Main.getBaseBrowseUrl() + "/changeset/" + cs.getId();
            lblChangeset.setUrl(url);
            lblChangeset.setDescription(Long.toString(cs.getId()));
            changesetCommentsDialogAction.setId(cs.getId());
            lblChangesetComments.setVisible(cs.getCommentsCount() > 0);
            lblChangesetComments.setText(String.valueOf(cs.getCommentsCount()));
            lblChangesetComments.setToolTipText(trn("This changeset has {0} comment", "This changeset has {0} comments",
                    cs.getCommentsCount(), cs.getCommentsCount()));
            changesetDialogAction.setId(cs.getId());
            changesetButton.setEnabled(true);
            arrowButton.setEnabled(true);

            String username = "";
            if (user != null) {
                username = user.getName();
            }
            lblUser.setDescription(insertWbr(username), false);
            if (user != null && user != User.getAnonymous()) {
                lblUser.setUrl(getUserUrl(username));
            } else {
                lblUser.setUrl(null);
            }
        } else {
            String username = UserIdentityManager.getInstance().getUserName();
            if (username == null) {
                lblUser.setDescription(tr("anonymous"));
                lblUser.setUrl(null);
            } else {
                lblUser.setDescription(insertWbr(username), false);
                lblUser.setUrl(getUserUrl(username));
            }
            lblChangeset.setDescription(tr("none"));
            lblChangeset.setUrl(null);
            lblChangesetComments.setVisible(false);
            changesetDialogAction.setId(null);
            changesetButton.setEnabled(false);
            arrowButton.setEnabled(false);
        }

        final Changeset oppCs = model != null ? model.getPointInTime(pointInTimeType.opposite()).getChangeset() : null;
        updateText(cs, "comment", texChangesetComment, null, oppCs, texChangesetComment);
        updateText(cs, "source", texChangesetSource, lblSource, oppCs, pnlChangesetSource);
        updateText(cs, "imagery_used", texChangesetImageryUsed, lblImageryUsed, oppCs, pnlChangesetImageryUsed);
    }

    private static String insertWbr(String s) {
        return Utils.escapeReservedCharactersHTML(s).replace("_", "_<wbr>");
    }

    protected static void updateText(Changeset cs, String attr, JTextArea textArea, JLabel label, Changeset oppCs, JComponent container) {
        final String text = cs != null ? cs.get(attr) : null;
        // Update text, hide prefixing label if empty
        if (label != null) {
            label.setVisible(text != null && !Utils.isStripEmpty(text));
        }
        textArea.setText(text);
        // Hide container if values of both versions are empty
        container.setVisible(text != null || (oppCs != null && oppCs.get(attr) != null));
    }

    static class OpenChangesetDialogAction extends AbstractAction {
        private final Class<? extends JComponent> componentToSelect;
        private Integer id;

        OpenChangesetDialogAction(Class<? extends JComponent> componentToSelect) {
            super(tr("Changeset"));
            new ImageProvider("dialogs/changeset", "changesetmanager").resetMaxSize(new Dimension(16, 16))
                .getResource().attachImageIcon(this, true);
            putValue(SHORT_DESCRIPTION, tr("Opens the Changeset Manager window for the selected changesets"));
            this.componentToSelect = componentToSelect;
        }

        void setId(Integer id) {
            this.id = id;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (id != null) {
                ChangesetDialog.LaunchChangesetManager.displayChangesets(Collections.singleton(id));
            }
            if (componentToSelect != null) {
                ChangesetCacheManager.getInstance().setSelectedComponentInDetailPanel(componentToSelect);
            }
        }
    }
}
