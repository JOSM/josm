// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;

/**
 * This is the panel for selecting whether the query should be restricted to a specific user.
 * @since 11326 (extracted from AdvancedChangesetQueryPanel)
 */
public class UserRestrictionPanel extends JPanel implements RestrictionPanel {
    private static final String PREF_ROOT = "changeset-query.advanced.user-restrictions";
    private static final String PREF_QUERY_TYPE = PREF_ROOT + ".query-type";

    private final ButtonGroup bgUserRestrictions = new ButtonGroup();
    private final JRadioButton rbRestrictToMyself = new JRadioButton(); // text is set in #startUserInput
    private final JRadioButton rbRestrictToUid = new JRadioButton(tr("Only changesets owned by the user with the following user ID"));
    private final JRadioButton rbRestrictToUserName = new JRadioButton(tr("Only changesets owned by the user with the following user name"));
    private final JosmTextField tfUid = new JosmTextField(10);
    private transient UidInputFieldValidator valUid;
    private final JosmTextField tfUserName = new JosmTextField(10);
    private transient UserNameValidator valUserName;

    /**
     * Constructs a new {@code UserRestrictionPanel}.
     */
    public UserRestrictionPanel() {
        build();
    }

    protected JPanel buildUidInputPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.add(new JLabel(tr("User ID:")), GBC.std());

        pnl.add(tfUid, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        SelectAllOnFocusGainedDecorator.decorate(tfUid);
        valUid = UidInputFieldValidator.decorate(tfUid);

        return pnl;
    }

    protected JPanel buildUserNameInputPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.add(new JLabel(tr("User name:")), GBC.std());

        pnl.add(tfUserName, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        SelectAllOnFocusGainedDecorator.decorate(tfUserName);
        valUserName = new UserNameValidator(tfUserName);

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
        GridBagConstraints gc = GBC.eol().fill(GridBagConstraints.HORIZONTAL);

        add(rbRestrictToMyself, gc);
        rbRestrictToMyself.addItemListener(userRestrictionChangeHandler);

        add(rbRestrictToUid, gc);
        rbRestrictToUid.addItemListener(userRestrictionChangeHandler);
        add(buildUidInputPanel(), gc);

        add(rbRestrictToUserName, gc);
        rbRestrictToUserName.addItemListener(userRestrictionChangeHandler);

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
            rbRestrictToMyself.setText(tr("Only changesets owned by myself (disabled. JOSM is currently run by an anonymous user)"));
            rbRestrictToMyself.setEnabled(false);
            if (rbRestrictToMyself.isSelected()) {
                rbRestrictToUid.setSelected(true);
            }
        } else {
            rbRestrictToMyself.setText(tr("Only changesets owned by myself"));
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
