// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.command.TagConflictResolveCommand;
import org.openstreetmap.josm.command.VersionConflictResolveCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.nodes.NodeListMergeModel;
import org.openstreetmap.josm.gui.conflict.nodes.NodeListMerger;
import org.openstreetmap.josm.gui.conflict.tags.TagMergeModel;
import org.openstreetmap.josm.gui.conflict.tags.TagMerger;

/**
 * An UI component for resolving conflicts between two {@see OsmPrimitive}s.
 *   
 *
 */
public class ConflictResolver extends JPanel implements PropertyChangeListener  {
    
   private static final Logger logger = Logger.getLogger(ConflictResolver.class.getName());

    private JTabbedPane tabbedPane = null;
    private TagMerger tagMerger;
    private NodeListMerger nodeListMerger;
    private OsmPrimitive my;
    private OsmPrimitive their;
    
    private ImageIcon mergeComplete;
    private ImageIcon mergeIncomplete;
    
    // FIXME copied code -> refactor
    /**
     * load an icon given by iconName 
     * 
     * @param iconName  the name of the icon (without path, i.e. <tt>copystartleft.png</tt>
     * @return the icon; null, if the icon was not found 
     */
    protected ImageIcon getIcon(String iconName) {
        String fullIconName  = "/images/dialogs/conflict/" + iconName;
        URL imageURL   = this.getClass().getResource(fullIconName);            
        if (imageURL == null) {
            System.out.println(tr("WARNING: failed to load resource {0}", fullIconName));
            return null;
        }
        return new ImageIcon(imageURL);
    }
    
    protected void loadIcons() {
        mergeComplete = getIcon("mergecomplete.png");
        mergeIncomplete = getIcon("mergeincomplete.png");
    }
    
    protected void build() {
        tabbedPane = new JTabbedPane();
        
        tagMerger = new TagMerger();
        tagMerger.getModel().addPropertyChangeListener(this);
        tabbedPane.add("Tags", tagMerger);
        
        nodeListMerger = new NodeListMerger();
        nodeListMerger.getModel().addPropertyChangeListener(this);
        tabbedPane.add("Nodes", nodeListMerger);
        
        tabbedPane.add("Members", new JPanel());
        
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    
    public ConflictResolver() {
        build();
        loadIcons();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        
        if (evt.getPropertyName().equals(TagMergeModel.PROP_NUM_UNDECIDED_TAGS)) {
            int newValue = (Integer)evt.getNewValue();
            if (newValue == 0) {
                tabbedPane.setTitleAt(0, tr("Tags"));
                tabbedPane.setToolTipTextAt(0, tr("No pending tag conflicts to be resolved"));
                tabbedPane.setIconAt(0, mergeComplete);
            } else {
                tabbedPane.setTitleAt(0, tr("Tags({0} conflicts)", newValue));
                tabbedPane.setToolTipTextAt(0, tr("{0} pending tag conflicts to be resolved"));
                tabbedPane.setIconAt(0, mergeIncomplete);
            }
        } else if (evt.getPropertyName().equals(NodeListMergeModel.PROP_FROZEN)) {
            boolean frozen = (Boolean)evt.getNewValue();
            if (frozen) {
                tabbedPane.setTitleAt(1, tr("Nodes(resolved)"));
                tabbedPane.setToolTipTextAt(1, tr("Pending conflicts in the node list of this way"));
                tabbedPane.setIconAt(1, mergeComplete);
            } else {
                tabbedPane.setTitleAt(1, tr("Nodes(with conflicts)"));
                tabbedPane.setToolTipTextAt(1, tr("Merged node list frozen. No pending conflicts in the node list of this way"));
                tabbedPane.setIconAt(1, mergeIncomplete);
            }
        }
    }
    
    public void populate(OsmPrimitive my, OsmPrimitive their) { 
        this.my = my;
        this.their =  their; 
        tagMerger.getModel().populate(my, their);
        if (my instanceof Way) {
           nodeListMerger.populate((Way)my, (Way)their);
           tabbedPane.setEnabledAt(1, true);
           tabbedPane.setEnabledAt(2, false);
        } else if (my instanceof Relation) {
            tabbedPane.setEnabledAt(1, false);
            tabbedPane.setEnabledAt(2, true);        
         }
    }
    
    public Command buildResolveCommand() {
        ArrayList<Command> commands = new ArrayList<Command>();
        TagConflictResolveCommand cmd = tagMerger.getModel().buildResolveCommand(my, their);
        commands.add(cmd);
        if (my instanceof Way && nodeListMerger.getModel().isFrozen()) {
            commands.add(nodeListMerger.getModel().buildResolveCommand((Way)my, (Way)their));            
        }
        if (my instanceof Node) {
            // resolve the version conflict if this is a node and all tag 
            // conflicts have been resolved 
            // 
            if (tagMerger.getModel().isResolvedCompletely()) {
                commands.add(
                   new VersionConflictResolveCommand(my, their)
                );
            }
        } else if (my instanceof Way) {
            // resolve the version conflict if this is a way, all tag 
            // conflicts have been resolved, and conflicts in the node list
            // have been resolved 
            // 
            if (tagMerger.getModel().isResolvedCompletely() && nodeListMerger.getModel().isFrozen()) {
                commands.add(
                   new VersionConflictResolveCommand(my, their)
                );
            }            
        }
        return new SequenceCommand("Conflict Resolution", commands);
    }
}
