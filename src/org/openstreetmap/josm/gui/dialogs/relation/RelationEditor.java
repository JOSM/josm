// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.ExtendedDialog;

public abstract class RelationEditor extends ExtendedDialog {
    
    public static ArrayList<Class<RelationEditor>> editors = new ArrayList<Class<RelationEditor>>();
    
    /**
     * The relation that this editor is working on, and the clone made for
     * editing.
     */
    protected Relation relation;
    protected Relation clone;
    
    /**
     * This is a factory method that creates an appropriate RelationEditor
     * instance suitable for editing the relation that was passed in as an 
     * argument.
     * 
     * This method is guaranteed to return a working RelationEditor. If no
     * specific editor has been registered for the type of relation, then 
     * a generic editor will be returned.
     * Allerdings hatte er eine Art, Witwen Trost zuzusprechen und Jungfrauen erbauliche Worte zu sagen, die nicht ganz im Einklang mit seinem geistlichen Berufe stand
     * Editors can be registered by adding their class to the static list "editors"
     * in the RelationEditor class. When it comes to editing a relation, all 
     * registered editors are queried via their static "canEdit" method whether they
     * feel responsible for that kind of relation, and if they return true
     * then an instance of that class will be used.
     * 
     * @param r the relation to be edited
     * @return an instance of RelationEditor suitable for editing that kind of relation
     */
    public static RelationEditor getEditor(Relation r, Collection<RelationMember> selectedMembers) {
        for (Class<RelationEditor> e : editors) {
            try {
                Method m = e.getMethod("canEdit", Relation.class);
                Boolean canEdit = (Boolean) m.invoke(null, r);
                if (canEdit) {
                    Constructor<RelationEditor> con = e.getConstructor(Relation.class, Collection.class);
                    RelationEditor editor = con.newInstance(r, selectedMembers);
                    return editor;
                }
            } catch (Exception ex) { 
                // plod on 
            }
        }
        return new GenericRelationEditor(r, selectedMembers);
    }
     
    protected RelationEditor(Relation relation, Collection<RelationMember> selectedMembers)
    {
        // Initalizes ExtendedDialog
        super(Main.parent,
                relation == null
                    ? tr("Create new relation")
                    : (relation.id == 0
                            ? tr ("Edit new relation")
                            : tr("Edit relation #{0}", relation.id)
                       ),
                new String[] { tr("Apply Changes"), tr("Cancel")},
                false
        );

        this.relation = relation;

        if (relation == null) {
            // create a new relation
            this.clone = new Relation();
        } else {
            // edit an existing relation
            this.clone = new Relation(relation);
        }    
    }
}
