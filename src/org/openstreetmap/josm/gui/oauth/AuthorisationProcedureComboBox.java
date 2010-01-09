// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

public class AuthorisationProcedureComboBox extends JComboBox {

    public AuthorisationProcedureComboBox() {
        setModel(new AuthorisationProcedureComboBoxModel());
        setRenderer(new AuthorisationProcedureCellRenderer());
        setSelectedItem(AuthorisationProcedure.FULLY_AUTOMATIC);
    }

    static private class AuthorisationProcedureComboBoxModel extends DefaultComboBoxModel {
        @Override
        public Object getElementAt(int index) {
            switch(index) {
            case 0: return AuthorisationProcedure.FULLY_AUTOMATIC;
            case 1: return AuthorisationProcedure.SEMI_AUTOMATIC;
            case 2: return AuthorisationProcedure.MANUALLY;
            }
            return null;
        }

        @Override
        public int getSize() {
            return 3;
        }
    }

    static private class AuthorisationProcedureCellRenderer extends JLabel implements ListCellRenderer {
        public AuthorisationProcedureCellRenderer() {
            setOpaque(true);
        }

        protected void renderColors(boolean isSelected) {
            if (isSelected) {
                setForeground(UIManager.getColor("List.selectionForeground"));
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setForeground(UIManager.getColor("List.foreground"));
                setBackground(UIManager.getColor("List.background"));
            }
        }

        protected void renderText(AuthorisationProcedure value) {
            switch(value) {
            case FULLY_AUTOMATIC:
                setText(tr("Fully automatic"));
                break;
            case SEMI_AUTOMATIC:
                setText(tr("Semi-automatic"));
                break;
            case MANUALLY:
                setText(tr("Manual"));
                break;
            }
        }

        protected void renderToolTipText(AuthorisationProcedure value) {
            switch(value) {
            case FULLY_AUTOMATIC:
                setToolTipText(tr(
                        "<html>Run a fully automatic procedure to get an access token from the OSM website.<br>"
                        + "JOSM accesses the OSM website on behalf of the JOSM user and fully<br>"
                        + "automatically authorises the user and retrieves an Access Token.</html>"
                ));
                break;
            case SEMI_AUTOMATIC:
                setToolTipText(tr(
                        "<html>Run a semi-automatic procedure to get an access token from the OSM website.<br>"
                        + "JOSM submits the standards OAuth requests to get a Request Token and an<br>"
                        + "Access Token. It dispatches the user to the OSM website in an external browser<br>"
                        + "to authenticate itself and to accept the request token submitted by JOSM.</html>"
                ));
                break;
            case MANUALLY:
                setToolTipText(tr(
                        "<html>Enter an Access Token manually if it was generated and retrieved outside<br>"
                        + "of JOSM.</html>"
                ));
                break;
            }
        }

        public Component getListCellRendererComponent(JList list, Object value, int idx, boolean isSelected, boolean hasFocus) {
            AuthorisationProcedure procedure = (AuthorisationProcedure)value;
            renderColors(isSelected);
            renderText(procedure);
            renderToolTipText(procedure);
            return this;
        }
    }
}
