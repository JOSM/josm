// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset.query;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * This is the panel to apply a time restriction to the changeset query.
 * @since 11326 (extracted from AdvancedChangesetQueryPanel)
 */
public class TimeRestrictionPanel extends JPanel implements RestrictionPanel {

    private final JRadioButton rbClosedAfter = new JRadioButton();
    private final JRadioButton rbClosedAfterAndCreatedBefore = new JRadioButton();
    private final JosmTextField tfClosedAfterDate1 = new JosmTextField();
    private transient DateValidator valClosedAfterDate1;
    private final JosmTextField tfClosedAfterTime1 = new JosmTextField();
    private transient TimeValidator valClosedAfterTime1;
    private final JosmTextField tfClosedAfterDate2 = new JosmTextField();
    private transient DateValidator valClosedAfterDate2;
    private final JosmTextField tfClosedAfterTime2 = new JosmTextField();
    private transient TimeValidator valClosedAfterTime2;
    private final JosmTextField tfCreatedBeforeDate = new JosmTextField();
    private transient DateValidator valCreatedBeforeDate;
    private final JosmTextField tfCreatedBeforeTime = new JosmTextField();
    private transient TimeValidator valCreatedBeforeTime;

    /**
     * Constructs a new {@code TimeRestrictionPanel}.
     */
    public TimeRestrictionPanel() {
        build();
    }

    protected JPanel buildClosedAfterInputPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.insets = new Insets(0, 0, 0, 3);
        pnl.add(new JLabel(tr("Date: ")), gc);

        gc.gridx = 1;
        gc.weightx = 0.7;
        pnl.add(tfClosedAfterDate1, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfClosedAfterDate1);
        valClosedAfterDate1 = DateValidator.decorate(tfClosedAfterDate1);
        tfClosedAfterDate1.setToolTipText(valClosedAfterDate1.getStandardTooltipTextAsHtml());

        gc.gridx = 2;
        gc.weightx = 0.0;
        pnl.add(new JLabel(tr("Time:")), gc);

        gc.gridx = 3;
        gc.weightx = 0.3;
        pnl.add(tfClosedAfterTime1, gc);
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
        gc.insets = new Insets(0, 0, 0, 3);
        pnl.add(new JLabel(tr("Closed after - ")), gc);

        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.insets = new Insets(0, 0, 0, 3);
        pnl.add(new JLabel(tr("Date:")), gc);

        gc.gridx = 2;
        gc.weightx = 0.7;
        pnl.add(tfClosedAfterDate2, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfClosedAfterDate2);
        valClosedAfterDate2 = DateValidator.decorate(tfClosedAfterDate2);
        tfClosedAfterDate2.setToolTipText(valClosedAfterDate2.getStandardTooltipTextAsHtml());
        gc.gridx = 3;
        gc.weightx = 0.0;
        pnl.add(new JLabel(tr("Time:")), gc);

        gc.gridx = 4;
        gc.weightx = 0.3;
        pnl.add(tfClosedAfterTime2, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfClosedAfterTime2);
        valClosedAfterTime2 = TimeValidator.decorate(tfClosedAfterTime2);
        tfClosedAfterTime2.setToolTipText(valClosedAfterTime2.getStandardTooltipTextAsHtml());

        gc.gridy = 1;
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.insets = new Insets(0, 0, 0, 3);
        pnl.add(new JLabel(tr("Created before - ")), gc);

        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.insets = new Insets(0, 0, 0, 3);
        pnl.add(new JLabel(tr("Date:")), gc);

        gc.gridx = 2;
        gc.weightx = 0.7;
        pnl.add(tfCreatedBeforeDate, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfCreatedBeforeDate);
        valCreatedBeforeDate = DateValidator.decorate(tfCreatedBeforeDate);
        tfCreatedBeforeDate.setToolTipText(valCreatedBeforeDate.getStandardTooltipTextAsHtml());

        gc.gridx = 3;
        gc.weightx = 0.0;
        pnl.add(new JLabel(tr("Time:")), gc);

        gc.gridx = 4;
        gc.weightx = 0.3;
        pnl.add(tfCreatedBeforeTime, gc);
        SelectAllOnFocusGainedDecorator.decorate(tfCreatedBeforeTime);
        valCreatedBeforeTime = TimeValidator.decorate(tfCreatedBeforeTime);
        tfCreatedBeforeTime.setToolTipText(valCreatedBeforeDate.getStandardTooltipTextAsHtml());

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

        // -- changesets closed after a specific date/time
        //
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        add(rbClosedAfter, gc);

        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        add(new JMultilineLabel(tr("Only changesets closed after the following date/time")), gc);

        gc.gridx = 1;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        add(buildClosedAfterInputPanel(), gc);

        // -- changesets closed after a specific date/time and created before a specific date time
        //
        gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.gridy = 2;
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        add(rbClosedAfterAndCreatedBefore, gc);

        gc.gridx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        add(new JMultilineLabel(tr("Only changesets closed after and created before a specific date/time")), gc);

        gc.gridx = 1;
        gc.gridy = 3;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        add(buildClosedAfterAndCreatedBeforeInputPanel(), gc);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbClosedAfter);
        bg.add(rbClosedAfterAndCreatedBefore);

        ItemListener restrictionChangeHandler = new TimeRestrictionChangedHandler();
        rbClosedAfter.addItemListener(restrictionChangeHandler);
        rbClosedAfterAndCreatedBefore.addItemListener(restrictionChangeHandler);

        rbClosedAfter.setSelected(true);
    }

    /**
     * Determines if the changeset query time information is valid.
     * @return {@code true} if the changeset query time information is valid.
     */
    @Override
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

    /**
     * Initializes HMI for user input.
     */
    public void startUserInput() {
        restoreFromSettings();
    }

    /**
     * Sets the query restrictions on <code>query</code> for time based restrictions.
     * @param query the query to fill
     */
    @Override
    public void fillInQuery(ChangesetQuery query) {
        if (!isValidChangesetQuery())
            throw new IllegalStateException(tr("Cannot build changeset query with time based restrictions. Input is not valid."));
        if (rbClosedAfter.isSelected()) {
            LocalDate d1 = valClosedAfterDate1.getDate();
            LocalTime d2 = valClosedAfterTime1.getTime();
            final Date d3 = new Date(d1.atTime(d2).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            query.closedAfter(d3);
        } else if (rbClosedAfterAndCreatedBefore.isSelected()) {
            LocalDate d1 = valClosedAfterDate2.getDate();
            LocalTime d2 = valClosedAfterTime2.getTime();
            Date d3 = new Date(d1.atTime(d2).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

            d1 = valCreatedBeforeDate.getDate();
            d2 = valCreatedBeforeTime.getTime();
            Date d4 = new Date(d1.atTime(d2).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

            query.closedAfterAndCreatedBefore(d3, d4);
        }
    }

    @Override
    public void displayMessageIfInvalid() {
        if (isValidChangesetQuery())
            return;
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

    /**
     * Remember settings in preferences.
     */
    public void rememberSettings() {
        String prefRoot = "changeset-query.advanced.time-restrictions";
        if (rbClosedAfter.isSelected()) {
            Config.getPref().put(prefRoot + ".query-type", "closed-after");
        } else if (rbClosedAfterAndCreatedBefore.isSelected()) {
            Config.getPref().put(prefRoot + ".query-type", "closed-after-created-before");
        }
        Config.getPref().put(prefRoot + ".closed-after.date", tfClosedAfterDate1.getText());
        Config.getPref().put(prefRoot + ".closed-after.time", tfClosedAfterTime1.getText());
        Config.getPref().put(prefRoot + ".closed-created.closed.date", tfClosedAfterDate2.getText());
        Config.getPref().put(prefRoot + ".closed-created.closed.time", tfClosedAfterTime2.getText());
        Config.getPref().put(prefRoot + ".closed-created.created.date", tfCreatedBeforeDate.getText());
        Config.getPref().put(prefRoot + ".closed-created.created.time", tfCreatedBeforeTime.getText());
    }

    /**
     * Restore settings from preferences.
     */
    public void restoreFromSettings() {
        String prefRoot = "changeset-query.advanced.open-restrictions";
        String v = Config.getPref().get(prefRoot + ".query-type", "closed-after");
        rbClosedAfter.setSelected("closed-after".equals(v));
        rbClosedAfterAndCreatedBefore.setSelected("closed-after-created-before".equals(v));
        if (!rbClosedAfter.isSelected() && !rbClosedAfterAndCreatedBefore.isSelected()) {
            rbClosedAfter.setSelected(true);
        }
        tfClosedAfterDate1.setText(Config.getPref().get(prefRoot + ".closed-after.date", ""));
        tfClosedAfterTime1.setText(Config.getPref().get(prefRoot + ".closed-after.time", ""));
        tfClosedAfterDate2.setText(Config.getPref().get(prefRoot + ".closed-created.closed.date", ""));
        tfClosedAfterTime2.setText(Config.getPref().get(prefRoot + ".closed-created.closed.time", ""));
        tfCreatedBeforeDate.setText(Config.getPref().get(prefRoot + ".closed-created.created.date", ""));
        tfCreatedBeforeTime.setText(Config.getPref().get(prefRoot + ".closed-created.created.time", ""));
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
