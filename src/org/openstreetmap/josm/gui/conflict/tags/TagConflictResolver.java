// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class TagConflictResolver extends JPanel {

    private TagConflictResolverModel model;

    protected void build() {
        setLayout(new BorderLayout());
        add(new JScrollPane(new TagConflictResolverTable(model)), BorderLayout.CENTER);
    }
    public TagConflictResolver() {
        this.model = new TagConflictResolverModel();
        build();
    }

    public TagConflictResolverModel getModel() {
        return model;
    }
}
