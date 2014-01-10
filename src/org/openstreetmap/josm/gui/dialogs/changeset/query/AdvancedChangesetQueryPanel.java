// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.JosmUserIdentityManager;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.BoundingBoxSelectionPanel;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.tools.CheckParameterUtil;


/**
 * This panel allows to specify a changeset query
 *
 */
public class AdvancedChangesetQueryPanel extends JPanel {

    private JCheckBox cbUserRestriction;
    private JCheckBox cbOpenAndCloseRestrictions;
    private JCheckBox cbTimeRestrictions;
    private JCheckBox cbBoundingBoxRestriction;
    private UserRestrictionPanel pnlUserRestriction;
    private OpenAndCloseStateRestrictionPanel pnlOpenAndCloseRestriction;
    private TimeRestrictionPanel pnlTimeRestriction;
    private BBoxRestrictionPanel pnlBoundingBoxRestriction;

    protected JPanel buildQueryPanel() {
        ItemListener stateChangeHandler = new RestrictionGroupStateChangeHandler();
        JPanel pnl  = new VerticallyScrollablePanel();
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        pnl.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        // -- select changesets by a specific user
        //
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 0.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        pnl.add(cbUserRestriction = new JCheckBox(), gc);
        cbUserRestriction.addItemListener(stateChangeHandler);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(new JMultilineLabel(tr("Select changesets owned by specific users")),gc);

        gc.gridy = 1;
        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(pnlUserRestriction = new UserRestrictionPanel(), gc);

        // -- restricting the query to open and closed changesets
        //
        gc.gridy = 2;
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 0.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        pnl.add(cbOpenAndCloseRestrictions = new JCheckBox(), gc);
        cbOpenAndCloseRestrictions.addItemListener(stateChangeHandler);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(new JMultilineLabel(tr("Select changesets depending on whether they are open or closed")),gc);

        gc.gridy = 3;
        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(pnlOpenAndCloseRestriction = new OpenAndCloseStateRestrictionPanel(), gc);

        // -- restricting the query to a specific time
        //
        gc.gridy = 4;
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 0.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        pnl.add(cbTimeRestrictions = new JCheckBox(), gc);
        cbTimeRestrictions.addItemListener(stateChangeHandler);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(new JMultilineLabel(tr("Select changesets based on the date/time they have been created or closed")),gc);

        gc.gridy = 5;
        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(pnlTimeRestriction = new TimeRestrictionPanel(), gc);


        // -- restricting the query to a specific bounding box
        //
        gc.gridy = 6;
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.weightx = 0.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        pnl.add(cbBoundingBoxRestriction = new JCheckBox(), gc);
        cbBoundingBoxRestriction.addItemListener(stateChangeHandler);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(new JMultilineLabel(tr("Select only changesets related to a specific bounding box")),gc);

        gc.gridy = 7;
        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(pnlBoundingBoxRestriction = new BBoxRestrictionPanel(), gc);


        gc.gridy = 8;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.fill  =GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        pnl.add(new JPanel(), gc);

        return pnl;
    }

    protected void build() {
        setLayout(new BorderLayout());
        JScrollPane spQueryPanel = GuiHelper.embedInVerticalScrollPane(buildQueryPanel());
        add(spQueryPanel, BorderLayout.CENTER);
    }

    /**
     * Constructs a new {@code AdvancedChangesetQueryPanel}.
     */
    public AdvancedChangesetQueryPanel() {
        build();
    }

    public void startUserInput() {
        restoreFromSettings();
        pnlBoundingBoxRestriction.setVisible(cbBoundingBoxRestriction.isSelected());
        pnlOpenAndCloseRestriction.setVisible(cbOpenAndCloseRestrictions.isSelected());
        pnlTimeRestriction.setVisible(cbTimeRestrictions.isSelected());
        pnlUserRestriction.setVisible(cbUserRestriction.isSelected());
        pnlOpenAndCloseRestriction.startUserInput();
        pnlUserRestriction.startUserInput();
        pnlTimeRestriction.startUserInput();
    }

    public void displayMessageIfInvalid() {
        if (cbUserRestriction.isSelected()) {
            if (! pnlUserRestriction.isValidChangesetQuery()) {
                pnlUserRestriction.displayMessageIfInvalid();
            }
        } else if (cbTimeRestrictions.isSelected()) {
            if (!pnlTimeRestriction.isValidChangesetQuery()) {
                pnlTimeRestriction.displayMessageIfInvalid();
            }
        } else if (cbBoundingBoxRestriction.isSelected()) {
            if (!pnlBoundingBoxRestriction.isValidChangesetQuery()) {
                pnlBoundingBoxRestriction.displayMessageIfInvalid();
            }
        }
    }

    /**
     * Builds the changeset query based on the data entered in the form.
     *
     * @return the changeset query. null, if the data entered doesn't represent
     * a valid changeset query.
     */
    public ChangesetQuery buildChangesetQuery() {
        ChangesetQuery query = new ChangesetQuery();
        if (cbUserRestriction.isSelected()) {
            if (! pnlUserRestriction.isValidChangesetQuery())
                return null;
            pnlUserRestriction.fillInQuery(query);
        }
        if (cbOpenAndCloseRestrictions.isSelected()) {
            // don't have to check whether it's valid. It always is.
            pnlOpenAndCloseRestriction.fillInQuery(query);
        }
        if (cbBoundingBoxRestriction.isSelected()) {
            if (!pnlBoundingBoxRestriction.isValidChangesetQuery())
                return null;
            pnlBoundingBoxRestriction.fillInQuery(query);
        }
        if (cbTimeRestrictions.isSelected()) {
            if (!pnlTimeRestriction.isValidChangesetQuery())
                return null;
            pnlTimeRestriction.fillInQuery(query);
        }
        return query;
    }

    public void rememberSettings() {
        Main.pref.put("changeset-query.advanced.user-restrictions", cbUserRestriction.isSelected());
        Main.pref.put("changeset-query.advanced.open-restrictions", cbOpenAndCloseRestrictions.isSelected());
        Main.pref.put("changeset-query.advanced.time-restrictions", cbTimeRestrictions.isSelected());
        Main.pref.put("changeset-query.advanced.bbox-restrictions", cbBoundingBoxRestriction.isSelected());

        pnlUserRestriction.rememberSettings();
        pnlOpenAndCloseRestriction.rememberSettings();
        pnlTimeRestriction.rememberSettings();
    }

    public void restoreFromSettings() {
        cbUserRestriction.setSelected(Main.pref.getBoolean("changeset-query.advanced.user-restrictions", false));
        cbOpenAndCloseRestrictions.setSelected(Main.pref.getBoolean("changeset-query.advanced.open-restrictions", false));
        cbTimeRestrictions.setSelected(Main.pref.getBoolean("changeset-query.advanced.time-restrictions", false));
        cbBoundingBoxRestriction.setSelected(Main.pref.getBoolean("changeset-query.advanced.bbox-restrictions", false));
    }

    class RestrictionGroupStateChangeHandler implements ItemListener {
        protected void userRestrictionStateChanged() {
            if (pnlUserRestriction == null) return;
            pnlUserRestriction.setVisible(cbUserRestriction.isSelected());
        }

        protected void openCloseRestrictionStateChanged() {
            if (pnlOpenAndCloseRestriction == null) return;
            pnlOpenAndCloseRestriction.setVisible(cbOpenAndCloseRestrictions.isSelected());
        }

        protected void timeRestrictionsStateChanged() {
            if (pnlTimeRestriction == null) return;
            pnlTimeRestriction.setVisible(cbTimeRestrictions.isSelected());
        }

        protected void boundingBoxRestrictionChanged() {
            if (pnlBoundingBoxRestriction == null) return;
            pnlBoundingBoxRestriction.setVisible(cbBoundingBoxRestriction.isSelected());
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getSource() == cbUserRestriction) {
                userRestrictionStateChanged();
            } else if (e.getSource() == cbOpenAndCloseRestrictions) {
                openCloseRestrictionStateChanged();
            } else if (e.getSource() == cbTimeRestrictions) {
                timeRestrictionsStateChanged();
            } else if (e.getSource() == cbBoundingBoxRestriction) {
                boundingBoxRestrictionChanged();
            }
            validate();
            repaint();
        }
    }

    /**
     * This is the panel for selecting whether the changeset query should be restricted to
     * open or closed changesets
     */
    static private class OpenAndCloseStateRestrictionPanel extends JPanel {

        private JRadioButton rbOpenOnly;
        private JRadioButton rbClosedOnly;
        private JRadioButton rbBoth;

        protected void build() {
            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(3,3,3,3),
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.GRAY),
                            BorderFactory.createEmptyBorder(5,5,5,5)
                    )
            ));
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 0.0;
            add(rbOpenOnly = new JRadioButton(), gc);

            gc.gridx = 1;
            gc.weightx = 1.0;
            add(new JMultilineLabel(tr("Query open changesets only")), gc);

            gc.gridy = 1;
            gc.gridx = 0;
            gc.weightx = 0.0;
            add(rbClosedOnly = new JRadioButton(), gc);

            gc.gridx = 1;
            gc.weightx = 1.0;
            add(new JMultilineLabel(tr("Query closed changesets only")), gc);

            gc.gridy = 2;
            gc.gridx = 0;
            gc.weightx = 0.0;
            add(rbBoth = new JRadioButton(), gc);

            gc.gridx = 1;
            gc.weightx = 1.0;
            add(new JMultilineLabel(tr("Query both open and closed changesets")), gc);

            ButtonGroup bgRestrictions = new ButtonGroup();
            bgRestrictions.add(rbBoth);
            bgRestrictions.add(rbClosedOnly);
            bgRestrictions.add(rbOpenOnly);
        }

        public OpenAndCloseStateRestrictionPanel() {
            build();
        }

        public void startUserInput() {
            restoreFromSettings();
        }

        public void fillInQuery(ChangesetQuery query) {
            if (rbBoth.isSelected()) {
                query.beingClosed(true);
                query.beingOpen(true);
            } else if (rbOpenOnly.isSelected()) {
                query.beingOpen(true);
            } else if (rbClosedOnly.isSelected()) {
                query.beingClosed(true);
            }
        }

        public void rememberSettings() {
            String prefRoot = "changeset-query.advanced.open-restrictions";
            if (rbBoth.isSelected()) {
                Main.pref.put(prefRoot + ".query-type", "both");
            } else if (rbOpenOnly.isSelected()) {
                Main.pref.put(prefRoot + ".query-type", "open");
            } else if (rbClosedOnly.isSelected()) {
                Main.pref.put(prefRoot + ".query-type", "closed");
            }
        }

        public void restoreFromSettings() {
            String prefRoot = "changeset-query.advanced.open-restrictions";
            String v = Main.pref.get(prefRoot + ".query-type", "open");
            rbBoth.setSelected(v.equals("both"));
            rbOpenOnly.setSelected(v.equals("open"));
            rbClosedOnly.setSelected(v.equals("closed"));
        }
    }

    /**
     * This is the panel for selecting whether the query should be restricted to a specific
     * user
     *
     */
    static private class UserRestrictionPanel extends JPanel {
        private ButtonGroup bgUserRestrictions;
        private JRadioButton rbRestrictToMyself;
        private JRadioButton rbRestrictToUid;
        private JRadioButton rbRestrictToUserName;
        private JosmTextField tfUid;
        private UidInputFieldValidator valUid;
        private JosmTextField tfUserName;
        private UserNameInputValidator valUserName;
        private JMultilineLabel lblRestrictedToMyself;

        protected JPanel buildUidInputPanel() {
            JPanel pnl = new JPanel(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 0.0;
            gc.insets = new Insets(0,0,0,3);
            pnl.add(new JLabel(tr("User ID:")), gc);

            gc.gridx = 1;
            pnl.add(tfUid = new JosmTextField(10),gc);
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
            gc.insets = new Insets(0,0,0,3);
            pnl.add(new JLabel(tr("User name:")), gc);

            gc.gridx = 1;
            pnl.add(tfUserName = new JosmTextField(10),gc);
            SelectAllOnFocusGainedDecorator.decorate(tfUserName);
            valUserName = UserNameInputValidator.decorate(tfUserName);

            // grab remaining space
            gc.gridx = 2;
            gc.weightx = 1.0;
            pnl.add(new JPanel(), gc);
            return pnl;
        }

        protected void build() {
            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(3,3,3,3),
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.GRAY),
                            BorderFactory.createEmptyBorder(5,5,5,5)
                    )
            ));

            ItemListener userRestrictionChangeHandler = new UserRestrictionChangedHandler();
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.gridx = 0;
            gc.fill= GridBagConstraints.HORIZONTAL;
            gc.weightx = 0.0;
            add(rbRestrictToMyself = new JRadioButton(), gc);
            rbRestrictToMyself.addItemListener(userRestrictionChangeHandler);

            gc.gridx = 1;
            gc.fill =  GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            add(lblRestrictedToMyself = new JMultilineLabel(tr("Only changesets owned by myself")), gc);

            gc.gridx = 0;
            gc.gridy = 1;
            gc.fill= GridBagConstraints.HORIZONTAL;
            gc.weightx = 0.0;
            add(rbRestrictToUid = new JRadioButton(), gc);
            rbRestrictToUid.addItemListener(userRestrictionChangeHandler);

            gc.gridx = 1;
            gc.fill =  GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            add(new JMultilineLabel(tr("Only changesets owned by the user with the following user ID")),gc);

            gc.gridx = 1;
            gc.gridy = 2;
            gc.fill =  GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            add(buildUidInputPanel(),gc);

            gc.gridx = 0;
            gc.gridy = 3;
            gc.fill= GridBagConstraints.HORIZONTAL;
            gc.weightx = 0.0;
            add(rbRestrictToUserName = new JRadioButton(), gc);
            rbRestrictToUserName.addItemListener(userRestrictionChangeHandler);

            gc.gridx = 1;
            gc.fill =  GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            add(new JMultilineLabel(tr("Only changesets owned by the user with the following user name")),gc);

            gc.gridx = 1;
            gc.gridy = 4;
            gc.fill =  GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            add(buildUserNameInputPanel(),gc);

            bgUserRestrictions = new ButtonGroup();
            bgUserRestrictions.add(rbRestrictToMyself);
            bgUserRestrictions.add(rbRestrictToUid);
            bgUserRestrictions.add(rbRestrictToUserName);
        }

        public UserRestrictionPanel() {
            build();
        }

        public void startUserInput() {
            if (JosmUserIdentityManager.getInstance().isAnonymous()) {
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
         * Sets the query restrictions on <code>query</code> for changeset owner based
         * restrictions.
         *
         * @param query the query. Must not be null.
         * @throws IllegalArgumentException thrown if query is null
         * @throws IllegalStateException thrown if one of the available values for query parameters in
         * this panel isn't valid
         *
         */
        public void fillInQuery(ChangesetQuery query) throws IllegalStateException, IllegalArgumentException  {
            CheckParameterUtil.ensureParameterNotNull(query, "query");
            if (rbRestrictToMyself.isSelected()) {
                JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();
                if (im.isPartiallyIdentified()) {
                    query.forUser(im.getUserName());
                } else if (im.isFullyIdentified()) {
                    query.forUser(im.getUserId());
                } else
                    throw new IllegalStateException(tr("Cannot restrict changeset query to the current user because the current user is anonymous"));
            } else if (rbRestrictToUid.isSelected()) {
                int uid  = valUid.getUid();
                if (uid > 0) {
                    query.forUser(uid);
                } else
                    throw new IllegalStateException(tr("Current value ''{0}'' for user ID is not valid", tfUid.getText()));
            } else if (rbRestrictToUserName.isSelected()) {
                if (! valUserName.isValid())
                    throw new IllegalStateException(tr("Cannot restrict the changeset query to the user name ''{0}''", tfUserName.getText()));
                query.forUser(tfUserName.getText());
            }
        }


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

        public void rememberSettings() {
            String prefRoot = "changeset-query.advanced.user-restrictions";
            if (rbRestrictToMyself.isSelected()) {
                Main.pref.put(prefRoot + ".query-type", "mine");
            } else if (rbRestrictToUid.isSelected()) {
                Main.pref.put(prefRoot + ".query-type", "uid");
            } else if (rbRestrictToUserName.isSelected()) {
                Main.pref.put(prefRoot + ".query-type", "username");
            }
            Main.pref.put(prefRoot + ".uid", tfUid.getText());
            Main.pref.put(prefRoot + ".username", tfUserName.getText());
        }

        public void restoreFromSettings() {
            String prefRoot = "changeset-query.advanced.user-restrictions";
            String v = Main.pref.get(prefRoot + ".query-type", "mine");
            if (v.equals("mine")) {
                JosmUserIdentityManager im = JosmUserIdentityManager.getInstance();
                if (im.isAnonymous()) {
                    rbRestrictToUid.setSelected(true);
                } else {
                    rbRestrictToMyself.setSelected(true);
                }
            } else if (v.equals("uid")) {
                rbRestrictToUid.setSelected(true);
            } else if (v.equals("username")) {
                rbRestrictToUserName.setSelected(true);
            }
            tfUid.setText(Main.pref.get(prefRoot + ".uid", ""));
            if (!valUid.isValid()) {
                tfUid.setText("");
            }
            tfUserName.setText(Main.pref.get(prefRoot + ".username", ""));
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

    /**
     * This is the panel to apply a time restriction to the changeset query
     */
    static private class TimeRestrictionPanel extends JPanel {

        private JRadioButton rbClosedAfter;
        private JRadioButton rbClosedAfterAndCreatedBefore;
        private JosmTextField tfClosedAfterDate1;
        private DateValidator valClosedAfterDate1;
        private JosmTextField tfClosedAfterTime1;
        private TimeValidator valClosedAfterTime1;
        private JosmTextField tfClosedAfterDate2;
        private DateValidator valClosedAfterDate2;
        private JosmTextField tfClosedAfterTime2;
        private TimeValidator valClosedAfterTime2;
        private JosmTextField tfCreatedBeforeDate;
        private DateValidator valCreatedBeforeDate;
        private JosmTextField tfCreatedBeforeTime;
        private TimeValidator valCreatedBeforeTime;

        protected JPanel buildClosedAfterInputPanel() {
            JPanel pnl = new JPanel(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 0.0;
            gc.insets = new Insets(0,0,0,3);
            pnl.add(new JLabel(tr("Date: ")), gc);

            gc.gridx = 1;
            gc.weightx = 0.7;
            pnl.add(tfClosedAfterDate1 = new JosmTextField(),gc);
            SelectAllOnFocusGainedDecorator.decorate(tfClosedAfterDate1);
            valClosedAfterDate1 = DateValidator.decorate(tfClosedAfterDate1);
            tfClosedAfterDate1.setToolTipText(valClosedAfterDate1.getStandardTooltipTextAsHtml());

            gc.gridx = 2;
            gc.weightx = 0.0;
            pnl.add(new JLabel(tr("Time:")),gc);

            gc.gridx = 3;
            gc.weightx = 0.3;
            pnl.add(tfClosedAfterTime1 = new JosmTextField(),gc);
            SelectAllOnFocusGainedDecorator.decorate(tfClosedAfterTime1);
            valClosedAfterTime1 = TimeValidator.decorate(tfClosedAfterTime1);
            tfClosedAfterTime1.setToolTipText(valClosedAfterTime1.getStandardTooltipTextAsHtml());
            return pnl;
        }

        protected JPanel buildClosedAfterAndCreatedBeforeInputPanel() {
            JPanel pnl = new JPanel(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 0.0;
            gc.insets = new Insets(0,0,0,3);
            pnl.add(new JLabel(tr("Closed after - ")), gc);

            gc.gridx = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 0.0;
            gc.insets = new Insets(0,0,0,3);
            pnl.add(new JLabel(tr("Date:")), gc);

            gc.gridx = 2;
            gc.weightx = 0.7;
            pnl.add(tfClosedAfterDate2 = new JosmTextField(),gc);
            SelectAllOnFocusGainedDecorator.decorate(tfClosedAfterDate2);
            valClosedAfterDate2 = DateValidator.decorate(tfClosedAfterDate2);
            tfClosedAfterDate2.setToolTipText(valClosedAfterDate2.getStandardTooltipTextAsHtml());
            gc.gridx = 3;
            gc.weightx = 0.0;
            pnl.add(new JLabel(tr("Time:")),gc);

            gc.gridx = 4;
            gc.weightx = 0.3;
            pnl.add(tfClosedAfterTime2 = new JosmTextField(),gc);
            SelectAllOnFocusGainedDecorator.decorate(tfClosedAfterTime2);
            valClosedAfterTime2 = TimeValidator.decorate(tfClosedAfterTime2);
            tfClosedAfterTime2.setToolTipText(valClosedAfterTime2.getStandardTooltipTextAsHtml());

            gc.gridy = 1;
            gc.gridx = 0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 0.0;
            gc.insets = new Insets(0,0,0,3);
            pnl.add(new JLabel(tr("Created before - ")), gc);

            gc.gridx = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 0.0;
            gc.insets = new Insets(0,0,0,3);
            pnl.add(new JLabel(tr("Date:")), gc);

            gc.gridx = 2;
            gc.weightx = 0.7;
            pnl.add(tfCreatedBeforeDate = new JosmTextField(),gc);
            SelectAllOnFocusGainedDecorator.decorate(tfCreatedBeforeDate);
            valCreatedBeforeDate = DateValidator.decorate(tfCreatedBeforeDate);
            tfCreatedBeforeDate.setToolTipText(valCreatedBeforeDate.getStandardTooltipTextAsHtml());

            gc.gridx = 3;
            gc.weightx = 0.0;
            pnl.add(new JLabel(tr("Time:")),gc);

            gc.gridx = 4;
            gc.weightx = 0.3;
            pnl.add(tfCreatedBeforeTime = new JosmTextField(),gc);
            SelectAllOnFocusGainedDecorator.decorate(tfCreatedBeforeTime);
            valCreatedBeforeTime = TimeValidator.decorate(tfCreatedBeforeTime);
            tfCreatedBeforeTime.setToolTipText(valCreatedBeforeDate.getStandardTooltipTextAsHtml());

            return pnl;
        }

        protected void build() {
            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(3,3,3,3),
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.GRAY),
                            BorderFactory.createEmptyBorder(5,5,5,5)
                    )
            ));

            // -- changesets closed after a specific date/time
            //
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.gridx = 0;
            gc.fill= GridBagConstraints.HORIZONTAL;
            gc.weightx = 0.0;
            add(rbClosedAfter = new JRadioButton(), gc);

            gc.gridx = 1;
            gc.fill =  GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            add(new JMultilineLabel(tr("Only changesets closed after the following date/time")), gc);

            gc.gridx = 1;
            gc.gridy = 1;
            gc.fill =  GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            add(buildClosedAfterInputPanel(),gc);

            // -- changesets closed after a specific date/time and created before a specific date time
            //
            gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.gridy = 2;
            gc.gridx = 0;
            gc.fill= GridBagConstraints.HORIZONTAL;
            gc.weightx = 0.0;
            add(rbClosedAfterAndCreatedBefore = new JRadioButton(), gc);

            gc.gridx = 1;
            gc.fill =  GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            add(new JMultilineLabel(tr("Only changesets closed after and created before a specific date/time")), gc);

            gc.gridx = 1;
            gc.gridy = 3;
            gc.fill =  GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            add(buildClosedAfterAndCreatedBeforeInputPanel(),gc);

            ButtonGroup bg = new ButtonGroup();
            bg.add(rbClosedAfter);
            bg.add(rbClosedAfterAndCreatedBefore);

            ItemListener restrictionChangeHandler = new TimeRestrictionChangedHandler();
            rbClosedAfter.addItemListener(restrictionChangeHandler);
            rbClosedAfterAndCreatedBefore.addItemListener(restrictionChangeHandler);

            rbClosedAfter.setSelected(true);
        }

        public TimeRestrictionPanel() {
            build();
        }

        public boolean isValidChangesetQuery() {
            if (rbClosedAfter.isSelected())
                return valClosedAfterDate1.isValid() && valClosedAfterTime1.isValid();
            else if (rbClosedAfterAndCreatedBefore.isSelected())
                return valClosedAfterDate2.isValid() && valClosedAfterTime2.isValid()
                && valCreatedBeforeDate.isValid() && valCreatedBeforeTime.isValid();
            // should not happen
            return true;
        }

        class TimeRestrictionChangedHandler implements ItemListener {
            @Override
            public void itemStateChanged(ItemEvent e) {
                tfClosedAfterDate1.setEnabled(rbClosedAfter.isSelected());
                tfClosedAfterTime1.setEnabled(rbClosedAfter.isSelected());

                tfClosedAfterDate2.setEnabled(rbClosedAfterAndCreatedBefore.isSelected());
                tfClosedAfterTime2.setEnabled(rbClosedAfterAndCreatedBefore.isSelected());
                tfCreatedBeforeDate.setEnabled(rbClosedAfterAndCreatedBefore.isSelected());
                tfCreatedBeforeTime.setEnabled(rbClosedAfterAndCreatedBefore.isSelected());
            }
        }

        public void startUserInput() {
            restoreFromSettings();
        }

        public void fillInQuery(ChangesetQuery query) throws IllegalStateException{
            if (!isValidChangesetQuery())
                throw new IllegalStateException(tr("Cannot build changeset query with time based restrictions. Input is not valid."));
            if (rbClosedAfter.isSelected()) {
                GregorianCalendar cal = new GregorianCalendar();
                Date d1 = valClosedAfterDate1.getDate();
                Date d2 = valClosedAfterTime1.getDate();
                cal.setTimeInMillis(d1.getTime() + (d2 == null ? 0 : d2.getTime()));
                query.closedAfter(cal.getTime());
            } else if (rbClosedAfterAndCreatedBefore.isSelected()) {
                GregorianCalendar cal = new GregorianCalendar();
                Date d1 = valClosedAfterDate2.getDate();
                Date d2 = valClosedAfterTime2.getDate();
                cal.setTimeInMillis(d1.getTime() + (d2 == null ? 0 : d2.getTime()));
                Date d3 = cal.getTime();

                d1 = valCreatedBeforeDate.getDate();
                d2 = valCreatedBeforeTime.getDate();
                cal.setTimeInMillis(d1.getTime() + (d2 == null ? 0 : d2.getTime()));
                Date d4 = cal.getTime();

                query.closedAfterAndCreatedBefore(d3, d4);
            }
        }

        public void displayMessageIfInvalid() {
            if (isValidChangesetQuery()) return;
            HelpAwareOptionPane.showOptionDialog(
                    this,
                    tr(
                            "<html>Please enter valid date/time values to restrict<br>"
                            + "the query to a specific time range.</html>"
                    ),
                    tr("Invalid date/time values"),
                    JOptionPane.ERROR_MESSAGE,
                    HelpUtil.ht("/Dialog/ChangesetQueryDialog#InvalidDateTimeValues")
            );
        }


        public void rememberSettings() {
            String prefRoot = "changeset-query.advanced.time-restrictions";
            if (rbClosedAfter.isSelected()) {
                Main.pref.put(prefRoot + ".query-type", "closed-after");
            } else if (rbClosedAfterAndCreatedBefore.isSelected()) {
                Main.pref.put(prefRoot + ".query-type", "closed-after-created-before");
            }
            Main.pref.put(prefRoot + ".closed-after.date", tfClosedAfterDate1.getText());
            Main.pref.put(prefRoot + ".closed-after.time", tfClosedAfterTime1.getText());
            Main.pref.put(prefRoot + ".closed-created.closed.date", tfClosedAfterDate2.getText());
            Main.pref.put(prefRoot + ".closed-created.closed.time", tfClosedAfterTime2.getText());
            Main.pref.put(prefRoot + ".closed-created.created.date", tfCreatedBeforeDate.getText());
            Main.pref.put(prefRoot + ".closed-created.created.time", tfCreatedBeforeTime.getText());
        }

        public void restoreFromSettings() {
            String prefRoot = "changeset-query.advanced.open-restrictions";
            String v = Main.pref.get(prefRoot + ".query-type", "closed-after");
            rbClosedAfter.setSelected(v.equals("closed-after"));
            rbClosedAfterAndCreatedBefore.setSelected(v.equals("closed-after-created-before"));
            if (!rbClosedAfter.isSelected() && !rbClosedAfterAndCreatedBefore.isSelected()) {
                rbClosedAfter.setSelected(true);
            }
            tfClosedAfterDate1.setText(Main.pref.get(prefRoot + ".closed-after.date", ""));
            tfClosedAfterTime1.setText(Main.pref.get(prefRoot + ".closed-after.time", ""));
            tfClosedAfterDate2.setText(Main.pref.get(prefRoot + ".closed-created.closed.date", ""));
            tfClosedAfterTime2.setText(Main.pref.get(prefRoot + ".closed-created.closed.time", ""));
            tfCreatedBeforeDate.setText(Main.pref.get(prefRoot + ".closed-created.created.date", ""));
            tfCreatedBeforeTime.setText(Main.pref.get(prefRoot + ".closed-created.created.time", ""));
            if (!valClosedAfterDate1.isValid()) {
                tfClosedAfterDate1.setText("");
            }
            if (!valClosedAfterTime1.isValid()) {
                tfClosedAfterTime1.setText("");
            }
            if (!valClosedAfterDate2.isValid()) {
                tfClosedAfterDate2.setText("");
            }
            if (!valClosedAfterTime2.isValid()) {
                tfClosedAfterTime2.setText("");
            }
            if (!valCreatedBeforeDate.isValid()) {
                tfCreatedBeforeDate.setText("");
            }
            if (!valCreatedBeforeTime.isValid()) {
                tfCreatedBeforeTime.setText("");
            }
        }
    }

    static private class BBoxRestrictionPanel extends BoundingBoxSelectionPanel {
        public BBoxRestrictionPanel() {
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(3,3,3,3),
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.GRAY),
                            BorderFactory.createEmptyBorder(5,5,5,5)
                    )
            ));
        }

        public boolean isValidChangesetQuery() {
            return getBoundingBox() != null;
        }

        public void fillInQuery(ChangesetQuery query) {
            if (!isValidChangesetQuery())
                throw new IllegalStateException(tr("Cannot restrict the changeset query to a specific bounding box. The input is invalid."));
            query.inBbox(getBoundingBox());
        }

        public void displayMessageIfInvalid() {
            if (isValidChangesetQuery()) return;
            HelpAwareOptionPane.showOptionDialog(
                    this,
                    tr(
                            "<html>Please enter valid longitude/latitude values to restrict<br>" +
                            "the changeset query to a specific bounding box.</html>"
                    ),
                    tr("Invalid bounding box"),
                    JOptionPane.ERROR_MESSAGE,
                    HelpUtil.ht("/Dialog/ChangesetQueryDialog#InvalidBoundingBox")
            );
        }
    }

    /**
     * Validator for user ids entered in in a {@link JTextComponent}.
     *
     */
    static private class UidInputFieldValidator extends AbstractTextComponentValidator {
        static public UidInputFieldValidator decorate(JTextComponent tc) {
            return new UidInputFieldValidator(tc);
        }

        public UidInputFieldValidator(JTextComponent tc) {
            super(tc);
        }

        @Override
        public boolean isValid() {
            return getUid() > 0;
        }

        @Override
        public void validate() {
            String value  = getComponent().getText();
            if (value == null || value.trim().length() == 0) {
                feedbackInvalid("");
                return;
            }
            try {
                int uid = Integer.parseInt(value);
                if (uid <= 0) {
                    feedbackInvalid(tr("The current value is not a valid user ID. Please enter an integer value > 0"));
                    return;
                }
            } catch(NumberFormatException e) {
                feedbackInvalid(tr("The current value is not a valid user ID. Please enter an integer value > 0"));
                return;
            }
            feedbackValid(tr("Please enter an integer value > 0"));
        }

        public int getUid() {
            String value  = getComponent().getText();
            if (value == null || value.trim().length() == 0) return 0;
            try {
                int uid = Integer.parseInt(value.trim());
                if (uid > 0) return uid;
                return 0;
            } catch(NumberFormatException e) {
                return 0;
            }
        }
    }

    static private class UserNameInputValidator extends AbstractTextComponentValidator {
        static public UserNameInputValidator decorate(JTextComponent tc) {
            return new UserNameInputValidator(tc);
        }

        public UserNameInputValidator(JTextComponent tc) {
            super(tc);
        }

        @Override
        public boolean isValid() {
            return getComponent().getText().trim().length() > 0;
        }

        @Override
        public void validate() {
            String value  = getComponent().getText();
            if (value.trim().length() == 0) {
                feedbackInvalid(tr("<html>The  current value is not a valid user name.<br>Please enter an non-empty user name.</html>"));
                return;
            }
            feedbackValid(tr("Please enter an non-empty user name"));
        }
    }

    /**
     * Validates dates entered as text in in a {@link JTextComponent}. Validates the input
     * on the fly and gives feedback about whether the date is valid or not.
     *
     * Dates can be entered in one of four standard formats defined for the current locale.
     */
    static private class DateValidator extends AbstractTextComponentValidator {
        static public DateValidator decorate(JTextComponent tc) {
            return new DateValidator(tc);
        }

        public DateValidator(JTextComponent tc) {
            super(tc);
        }

        @Override
        public boolean isValid() {
            return getDate() != null;
        }

        public String getStandardTooltipTextAsHtml() {
            return "<html>" + getStandardTooltipText() + "</html>";
        }

        public String getStandardTooltipText() {
            return  tr(
                    "Please enter a date in the usual format for your locale.<br>"
                    + "Example: {0}<br>"
                    + "Example: {1}<br>"
                    + "Example: {2}<br>"
                    + "Example: {3}<br>",
                    DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(new Date()),
                    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(new Date()),
                    DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(new Date()),
                    DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(new Date())
            );
        }

        @Override
        public void validate() {
            if (!isValid()) {
                String msg = "<html>The current value isn't a valid date.<br>" + getStandardTooltipText()+ "</html>";
                feedbackInvalid(msg);
                return;
            } else {
                String msg = "<html>" + getStandardTooltipText() + "</html>";
                feedbackValid(msg);
            }
        }

        public Date getDate() {
            for (int format: new int[] {DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL}) {
                DateFormat df = DateFormat.getDateInstance(format);
                try {
                    return df.parse(getComponent().getText());
                } catch (ParseException e) {
                    // Try next format
                }
            }
            return null;
        }
    }

    /**
     * Validates time values entered as text in in a {@link JTextComponent}. Validates the input
     * on the fly and gives feedback about whether the time value is valid or not.
     *
     * Time values can be entered in one of four standard formats defined for the current locale.
     */
    static private class TimeValidator extends AbstractTextComponentValidator {
        static public TimeValidator decorate(JTextComponent tc) {
            return new TimeValidator(tc);
        }

        public TimeValidator(JTextComponent tc) {
            super(tc);
        }

        @Override
        public boolean isValid() {
            if (getComponent().getText().trim().length() == 0) return true;
            return getDate() != null;
        }

        public String getStandardTooltipTextAsHtml() {
            return "<html>" + getStandardTooltipText() + "</html>";
        }

        public String getStandardTooltipText() {
            return tr(
                    "Please enter a valid time in the usual format for your locale.<br>"
                    + "Example: {0}<br>"
                    + "Example: {1}<br>"
                    + "Example: {2}<br>"
                    + "Example: {3}<br>",
                    DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(new Date()),
                    DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.getDefault()).format(new Date()),
                    DateFormat.getTimeInstance(DateFormat.LONG, Locale.getDefault()).format(new Date()),
                    DateFormat.getTimeInstance(DateFormat.FULL, Locale.getDefault()).format(new Date())
            );
        }

        @Override
        public void validate() {

            if (!isValid()) {
                String msg = "<html>The current value isn't a valid time.<br>" + getStandardTooltipText() + "</html>";
                feedbackInvalid(msg);
                return;
            } else {
                String msg = "<html>" + getStandardTooltipText() + "</html>";
                feedbackValid(msg);
            }
        }

        public Date getDate() {
            if (getComponent().getText().trim().length() == 0)
                return null;

            for (int style : new int[]{DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL}) {
                try {
                    return DateFormat.getTimeInstance(style, Locale.getDefault()).parse(getComponent().getText());
                } catch(ParseException e) {
                    continue;
                }
            }
            return null;
        }
    }
}
