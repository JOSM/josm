package org.openstreetmap.josm.gui.preferences.imagery;

import java.awt.LayoutManager;
import javax.swing.JPanel;
import org.openstreetmap.josm.data.imagery.ImageryInfo;

public abstract class AddImageryPanel extends JPanel {
    protected AddImageryPanel() {
    }

    protected AddImageryPanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
    }

    protected AddImageryPanel(LayoutManager layout) {
        super(layout);
    }

    protected AddImageryPanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
    }

    abstract ImageryInfo getImageryInfo();
}
