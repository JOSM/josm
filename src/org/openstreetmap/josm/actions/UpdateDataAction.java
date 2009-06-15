// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTaskList;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.tools.Shortcut;

public class UpdateDataAction extends JosmAction {
    public UpdateDataAction() {
        super(tr("Update Data"),
                "updatedata",
                tr("Updates the current data layer from the server (re-downloads data)"),
                Shortcut.registerShortcut("file:updatedata",
                        tr("Update Data"),
                        KeyEvent.VK_U,
                        Shortcut.GROUP_HOTKEY),
                        true);
    }

    public void actionPerformed(ActionEvent e) {
        int bboxCount = 0;
        List<Area> areas = new ArrayList<Area>();
        for(DataSource ds : Main.main.editLayer().data.dataSources) {
            areas.add(new Area(ds.bounds.asRect()));
        }

        // The next two blocks removes every intersection from every DataSource Area
        // This prevents downloading the same data numerous times at intersections
        // and also skips smaller bounding boxes that are contained within larger ones
        // entirely.
        for(int i = 0; i < areas.size(); i++) {
            for(int j = i+1; j < areas.size(); j++) {
                areas.get(i).subtract(areas.get(j));
            }
        }

        for(int i = areas.size()-1; i > 0 ; i--) {
            for(int j = i-1; j > 0; j--) {
                areas.get(i).subtract(areas.get(j));
            }
        }

        for(Area a : areas) {
            if(a.isEmpty()) {
                continue;
            }
            bboxCount++;
        }

        if(bboxCount == 0) {
            JOptionPane.showMessageDialog(Main.parent,
                    tr("No data to update found. Have you already opened or downloaded a data layer?"));
            return;
        }

        new DownloadOsmTaskList().download(false, areas);
    }
}
