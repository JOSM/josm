// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.server.UserNameValidator;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * This is the panel for selecting whether the query should be restricted to a specific user.
 * @since 11326 (extracted from AdvancedChangesetQueryPanel)
 */
public class UserRestrictionPanel extends JPanel implements RestrictionPanel {
    private static final String PREF_ROOT = "changeset-query.advanced.user-restrictions";
    private static final String PREF_QUERY_TYPE = PREF_ROOT + ".query-type";

    private final ButtonGroup bgUserRestrictions = new ButtonGroup();
    private final JRadioButton rbRestrictToMyself = new JRadioButton();
    private final JRadioButton rbRestrictToUid = new JRadioButton();
    private final JRadioButton rbRestrictToUserName = new JRadioButton();
    private final JosmTextField tfUid = new JosmTextField(10);
    private transient UidInputFieldValidator valUid;
    private final JosmTextField tfUserName = new JosmTextField(10);
    private transient UserNameValidator valUserName;
    private final JMultilineLabel lblRestrictedToMyself = new JMultilineLabel(tr("Only changesets owned by myself"));

    /**
     * Constructs a new {@code UserRestrictionPanel}.
     */
    public UserRestrictionPanel() {
        build();
    }

    protected JPanel buildUidInputPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.insets = new Insets(0, 0, 0, 3);
        pnl.add(new JLabel(tr("User ID:")), gc);

        gc.gridx = 1;
        pnl.add(tfUid, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfUid);
        valUid = UidInputFieldValidator.decorate(tfUid);

        // grab remaining space
        gc.gridx = 2;
        gc.weightx = 1.0;
        pnl.add(new JPanel(), gc);
        return pnl;
    }

    protected JPanel buildUserNameInputPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.insets = new Insets(0, 0, 0, 3);
        pnl.add(new JLabel(tr("User name:")), gc);

        gc.gridx = 1;
        pnl.add(tfUserName, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfUserName);
        valUserName = new UserNameValidator(tfUserName);

        // grab remaining space
        gc.gridx = 2;
        gc.weightx = 1.0;
        pnl.add(new JPanel(), gc);
        return pnl;
    }

    protected void build() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 3, 3, 3),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.GRAY),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                )
        ));

        ItemListener userRestrictionChangeHandler = new UserRestrictionChangedHandler();
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        add(rbRestrictToMyself, gc);
        rbRestrictToMyself.addItemListener(userRestrictionChangeHandler);

        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        add(lblRestrictedToMyself, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        add(rbRestrictToUid, gc);
        rbRestrictToUid.addItemListener(userRestrictionChangeHandler);

        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        add(new JMultilineLabel(tr("Only changesets owned by the user with the following user ID")), gc);

        gc.gridx = 1;
        gc.gridy = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        add(buildUidInputPanel(), gc);

        gc.gridx = 0;
        gc.gridy = 3;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        add(rbRestrictToUserName, gc);
        rbRestrictToUserName.addItemListener(userRestrictionChangeHandler);

        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        add(new JMultilineLabel(tr("Only changesets owned by the user with the following user name")), gc);

        gc.gridx = 1;
        gc.gridy = 4;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        add(buildUserNameInputPanel(), gc);

        bgUserRestrictions.add(rbRestrictToMyself);
        bgUserRestrictions.add(rbRestrictToUid);
        bgUserRestrictions.add(rbRestrictToUserName);
    }

    /**
     * Initializes HMI for user input.
     */
    public void startUserInput() {
        if (UserIdentityManager.getInstance().isAnonymous()) {
            lblRestrictedToMyself.setText(tr("Only changesets owned by myself (disabled. JOSM is currently run by an anonymous user)"));
            rbRestrictToMyself.setEnabled(false);
            if (rbRestrictToMyself.isSelected()) {
                rbRestrictToUid.setSelected(true);
            }
        } else {
            lblRestrictedToMyself.setText(tr("Only changesets owned by myself"));
            rbRestrictToMyself.setEnabled(true);
            rbRestrictToMyself.setSelected(true);
        }
        restoreFromSettings();
    }

    /**
     * Sets the query restrictions on <code>query</code> for changeset owner based restrictions.
     *
     * @param query the query. Must not be null.
     * @throws IllegalArgumentException if query is null
     * @throws IllegalStateException if one of the available values for query parameters in this panel isn't valid
     */
    @Override
    public void fillInQuery(ChangesetQuery query) {
        CheckParameterUtil.ensureParameterNotNull(query, "query");
        if (rbRestrictToMyself.isSelected()) {
            UserIdentityManager im = UserIdentityManager.getInstance();
            if (im.isPartiallyIdentified()) {
                query.forUser(im.getUserName());
            } else if (im.isFullyIdentified()) {
                query.forUser(im.getUserId());
            } else
                throw new IllegalStateException(
                        tr("Cannot restrict changeset query to the current user because the current user is anonymous"));
        } else if (rbRestrictToUid.isSelected()) {
            int uid = valUid.getUid();
            if (uid > 0) {
                query.forUser(uid);
            } else
                throw new IllegalStateException(tr("Current value ''{0}'' for user ID is not valid", tfUid.getText()));
        } else if (rbRestrictToUserName.isSelected()) {
            if (!valUserName.isValid())
                throw new IllegalStateException(
                        tr("Cannot restrict the changeset query to the user name ''{0}''", tfUserName.getText()));
            query.forUser(tfUserName.getText());
        }
    }

    /**
     * Determines if the changeset query time information is valid.
     * @return {@code true} if the changeset query time information is valid.
     */
    @Override
    public boolean isValidChangesetQuery() {
        if (rbRestrictToUid.isSelected())
            return valUid.isValid();
        else if (rbRestrictToUserName.isSelected())
            return valUserName.isValid();
        return true;
    }

    protected void alertInvalidUid() {
        HelpAwareOptionPane.showOptionDialog(
                this,
                tr("Please enter a valid user ID"),
                tr("Invalid user ID"),
                JOptionPane.ERROR_MESSAGE,
                HelpUtil.ht("/Dialog/ChangesetQueryDialog#InvalidUserId")
        );
    }

    protected void alertInvalidUserName() {
        HelpAwareOptionPane.showOptionDialog(
                this,
                tr("Please enter a non-empty user name"),
                tr("Invalid user name"),
                JOptionPane.ERROR_MESSAGE,
                HelpUtil.ht("/Dialog/ChangesetQueryDialog#InvalidUserName")
        );
    }

    @Override
    public void displayMessageIfInvalid() {
        if (rbRestrictToUid.isSelected()) {
            if (!valUid.isValid()) {
                alertInvalidUid();
            }
        } else if (rbRestrictToUserName.isSelected()) {
            if (!valUserName.isValid()) {
                alertInvalidUserName();
            }
        }
    }

    /**
     * Remember settings in preferences.
     */
    public void rememberSettings() {
        if (rbRestrictToMyself.isSelected()) {
            Config.getPref().put(PREF_QUERY_TYPE, "mine");
        } else if (rbRestrictToUid.isSelected()) {
            Config.getPref().put(PREF_QUERY_TYPE, "uid");
        } else if (rbRestrictToUserName.isSelected()) {
            Config.getPref().put(PREF_QUERY_TYPE, "username");
        }
        Config.getPref().put(PREF_ROOT + ".uid", tfUid.getText());
        Config.getPref().put(PREF_ROOT + ".username", tfUserName.getText());
    }

    /**
     * Restore settings from preferences.
     */
    public void restoreFromSettings() {
        String v = Config.getPref().get(PREF_QUERY_TYPE, "mine");
        if ("mine".equals(v)) {
            UserIdentityManager im = UserIdentityManager.getInstance();
            if (im.isAnonymous()) {
                rbRestrictToUid.setSelected(true);
            } else {
                rbRestrictToMyself.setSelected(true);
            }
        } else if ("uid".equals(v)) {
            rbRestrictToUid.setSelected(true);
        } else if ("username".equals(v)) {
            rbRestrictToUserName.setSelected(true);
        }
        tfUid.setText(Config.getPref().get(PREF_ROOT + ".uid", ""));
        if (!valUid.isValid()) {
            tfUid.setText("");
        }
        tfUserName.setText(Config.getPref().get(PREF_ROOT + ".username", ""));
    }

    class UserRestrictionChangedHandler implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            tfUid.setEnabled(rbRestrictToUid.isSelected());
            tfUserName.setEnabled(rbRestrictToUserName.isSelected());
            if (rbRestrictToUid.isSelected()) {
                tfUid.requestFocusInWindow();
            } else if (rbRestrictToUserName.isSelected()) {
                tfUserName.requestFocusInWindow();
            }
        }
    }
}
