// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.oauth.OsmPrivileges;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;

public class OsmPrivilegesPanel extends VerticallyScrollablePanel{

    private JCheckBox cbWriteApi;
    private JCheckBox cbWriteGpx;
    private JCheckBox cbReadGpx;
    private JCheckBox cbWritePrefs;
    private JCheckBox cbReadPrefs;
    private JCheckBox cbModifyNotes;

    protected void build() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // checkbox for "allow to upload map data"
        //
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.insets = new Insets(0,0,3,3);
        add(cbWriteApi = new JCheckBox(), gc);
        cbWriteApi.setText(tr("Allow to upload map data"));
        cbWriteApi.setToolTipText(tr("Select to grant JOSM the right to upload map data on your behalf"));
        cbWriteApi.setSelected(true);

        // checkbox for "allow to upload gps traces"
        //
        gc.gridy = 1;
        add(cbWriteGpx = new JCheckBox(), gc);
        cbWriteGpx.setText(tr("Allow to upload GPS traces"));
        cbWriteGpx.setToolTipText(tr("Select to grant JOSM the right to upload GPS traces on your behalf"));
        cbWriteGpx.setSelected(true);

        // checkbox for "allow to download private gps traces"
        //
        gc.gridy = 2;
        add(cbReadGpx = new JCheckBox(), gc);
        cbReadGpx.setText(tr("Allow to download your private GPS traces"));
        cbReadGpx.setToolTipText(tr("Select to grant JOSM the right to download your private GPS traces into JOSM layers"));
        cbReadGpx.setSelected(true);

        // checkbox for "allow to download private gps traces"
        //
        gc.gridy = 3;
        add(cbReadPrefs = new JCheckBox(), gc);
        cbReadPrefs.setText(tr("Allow to read your preferences"));
        cbReadPrefs.setToolTipText(tr("Select to grant JOSM the right to read your server preferences"));
        cbReadPrefs.setSelected(true);

        // checkbox for "allow to download private gps traces"
        //
        gc.gridy = 4;
        add(cbWritePrefs = new JCheckBox(), gc);
        cbWritePrefs.setText(tr("Allow to write your preferences"));
        cbWritePrefs.setToolTipText(tr("Select to grant JOSM the right to write your server preferences"));
        cbWritePrefs.setSelected(true);

        gc.gridy = 5;
        add(cbModifyNotes = new JCheckBox(), gc);
        cbModifyNotes.setText(tr("Allow modifications of notes"));
        cbModifyNotes.setToolTipText(tr("Select to grant JOSM the right to modify notes on your behalf"));
        cbModifyNotes.setSelected(true);

        // filler - grab remaining space
        gc.gridy = 6;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        add(new JPanel(), gc);
    }

    public OsmPrivilegesPanel() {
        build();
    }

    /**
     * Replies the currently entered privileges
     *
     * @return the privileges
     */
    public OsmPrivileges getPrivileges() {
        OsmPrivileges privileges = new OsmPrivileges();
        privileges.setAllowWriteApi(cbWriteApi.isSelected());
        privileges.setAllowWriteGpx(cbWriteGpx.isSelected());
        privileges.setAllowReadGpx(cbReadGpx.isSelected());
        privileges.setAllowWritePrefs(cbWritePrefs.isSelected());
        privileges.setAllowReadPrefs(cbReadPrefs.isSelected());
        privileges.setAllowModifyNotes(cbModifyNotes.isSelected());
        return privileges;
    }
}
