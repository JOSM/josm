/*
 *  JOSMng - a Java Open Street Map editor, the next generation.

 *
 *  Copyright (C) 2008 Petr Nejedly <P.Nejedly@sh.cvut.cz>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.

 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.openstreetmap.josm.data.osm.event;


/**
 * A listener listening for all DataSet changes.
 * INCOMPLETE (missing relation-related events)!
 *
 * @author nenik
 */
public interface DataSetListener {
    /**
     * A bunch of primitives were added into the DataSet, or existing
     * deleted/invisible primitives were resurrected.
     *
     * @param added A collection of newly-visible primitives
     */
    void primtivesAdded(PrimitivesAddedEvent event);

    /**
     * A bunch of primitives were removed from the DataSet, or preexisting
     * primitives were marked as deleted.
     *
     * @param removed A collection of newly-invisible primitives
     */
    void primtivesRemoved(PrimitivesRemovedEvent event);

    /**
     * There was some change in the tag set of a primitive. It can have been
     * a tag addition, tag removal or change in tag value.
     *
     * @param prim the primitive, whose tags were affected.
     */
    void tagsChanged(TagsChangedEvent event);

    /**
     * A node's coordinates were modified.
     * @param node The node that was moved.
     */
    void nodeMoved(NodeMovedEvent event);

    /**
     * A way's node list was changed.
     * @param way The way that was modified.
     */
    void wayNodesChanged(WayNodesChangedEvent event);

    /**
     * A relation's members have changed.
     * @param relation The relation that was modified.
     */
    void relationMembersChanged(RelationMembersChangedEvent event);

    /**
     * Minor dataset change, currently only changeset id changed is supported, but can
     * be extended in future.
     * @param event
     */
    void otherDatasetChange(AbstractDatasetChangedEvent event);

    /**
     * Called after big changes in dataset. Usually other events are stopped using Dataset.beginUpdate() and
     * after operation is completed (Dataset.endUpdate()), {@link #dataChanged()} is called.
     */
    void dataChanged(DataChangedEvent event);
}
