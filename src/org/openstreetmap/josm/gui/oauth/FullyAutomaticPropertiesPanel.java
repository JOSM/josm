// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.JosmPasswordField;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;

public class FullyAutomaticPropertiesPanel extends JPanel {

    private JosmTextField tfUserName;
    private JosmPasswordField tfPassword;

    protected JPanel buildUserNamePasswordPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.insets = new Insets(0,0,3,3);
        pnl.add(new JLabel(tr("Username: ")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(tfUserName = new JosmTextField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfUserName);
        UserNameValidator valUserName = new UserNameValidator(tfUserName);
        valUserName.validate();

        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 1;
        gc.gridx = 0;
        gc.weightx = 0.0;
        pnl.add(new JLabel(tr("Password: ")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(tfPassword = new JosmPasswordField(), gc);
        SelectAllOnFocusGainedDecorator.decorate(tfPassword);

        return pnl;
    }


    public FullyAutomaticPropertiesPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        setBorder(BorderFactory.createEmptyBorder(3,3,3,3));

        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        add(buildUserNamePasswordPanel(), gc);

        gc.gridy = 1;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        add(new JPanel(), gc);
    }

    static private class UserNameValidator extends AbstractTextComponentValidator {

        public UserNameValidator(JTextComponent tc) {
            super(tc);
        }

        @Override
        public boolean isValid() {
            return getComponent().getText().trim().length() > 0;
        }

        @Override
        public void validate() {
            if (isValid()) {
                feedbackValid(tr("Please enter your OSM user name"));
            } else {
                feedbackInvalid(tr("The user name cannot be empty. Please enter your OSM user name"));
            }
        }
    }
}
