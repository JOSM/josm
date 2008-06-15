//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.command;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;


/**
 * Classes implementing Command modify a dataset in a specific way. A command is
 * one atomic action on a specific dataset, such as move or delete.
 *
 * Remember that the command must be executable and undoable, even if the 
 * Main.ds has changed, so the command must save the dataset it operates on
 * if necessary.
 *
 * @author imi
 */
abstract public class Command {

   private static final class CloneVisitor implements Visitor {
      public Map<OsmPrimitive, OsmPrimitive> orig = new HashMap<OsmPrimitive, OsmPrimitive>();

      public void visit(Node n) {
         orig.put(n, new Node(n));
      }
      public void visit(Way w) {
         orig.put(w, new Way(w));
      }
      public void visit(Relation e) {
         orig.put(e, new Relation(e));
      }
   }

   private CloneVisitor orig; 

   protected DataSet ds;

   public Command() {
      this.ds = Main.main.editLayer().data;
   }
   /**
    * Executes the command on the dataset. This implementation will remember all
    * primitives returned by fillModifiedData for restoring them on undo.
    */
   public boolean did_execute = false;
   public boolean executeCommand() {
      did_execute = true;
      orig = new CloneVisitor();
      Collection<OsmPrimitive> all = new HashSet<OsmPrimitive>();
      fillModifiedData(all, all, all);
      for (OsmPrimitive osm : all)
         osm.visit(orig);
      return true;
   }

   /**
    * Undoes the command. 
    * It can be assumed that all objects are in the same state they were before.
    * It can also be assumed that executeCommand was called exactly once before.
    * 
    * This implementation undoes all objects stored by a former call to executeCommand.
    */
   public void undoCommand() {
      for (Entry<OsmPrimitive, OsmPrimitive> e : orig.orig.entrySet())
         e.getKey().cloneFrom(e.getValue());
   }

   /**
    * Called when a layer has been removed to have the command remove itself from
    * any buffer if it is not longer applicable to the dataset (e.g. it was part of
    * the removed layer)
    */
   public boolean invalidBecauselayerRemoved(Layer oldLayer) {
      if (!(oldLayer instanceof OsmDataLayer))
         return false;
      HashSet<OsmPrimitive> modified = new HashSet<OsmPrimitive>();
      fillModifiedData(modified, modified, modified);
      if (modified.isEmpty())
         return false;

      HashSet<OsmPrimitive> all = new HashSet<OsmPrimitive>(((OsmDataLayer)oldLayer).data.allPrimitives());
      for (OsmPrimitive osm : all)
         if (all.contains(osm))
                 return true;

      return false;
   }

    /**
     * Lets other commands access the original version
     * of the object. Usually for undoing.
     */
    public OsmPrimitive getOrig(OsmPrimitive osm) {
        OsmPrimitive o = orig.orig.get(osm);
        if (o != null)
             return o;
        Main.debug("unable to find osm with id: " + osm.id + " hashCode: " + osm.hashCode());
        for (OsmPrimitive t : orig.orig.keySet()) {
             OsmPrimitive to = orig.orig.get(t);
             Main.debug("now: " + t.id + " hashCode: " + t.hashCode());
             Main.debug("orig: " + to.id + " hashCode: " + to.hashCode());
        }
        return o;
    }

   /**
    * Fill in the changed data this command operates on.
    * Add to the lists, don't clear them.
    * 
    * @param modified The modified primitives
    * @param deleted The deleted primitives
    * @param added The added primitives
    */
   abstract public void fillModifiedData(Collection<OsmPrimitive> modified,
         Collection<OsmPrimitive> deleted,
         Collection<OsmPrimitive> added);

   abstract public MutableTreeNode description();
}
