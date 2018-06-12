// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.tools.LanguageInfo;

/**
 * IPrimitive captures the common functions of {@link OsmPrimitive} and {@link PrimitiveData}.
 * @since 4098
 */
public interface IPrimitive extends Tagged, PrimitiveId, Stylable, Comparable<IPrimitive> {

    /**
     * Replies <code>true</code> if the object has been modified since it was loaded from
     * the server. In this case, on next upload, this object will be updated.
     *
     * Deleted objects are deleted from the server. If the objects are added (id=0),
     * the modified is ignored and the object is added to the server.
     *
     * @return <code>true</code> if the object has been modified since it was loaded from
     * the server
     */
    boolean isModified();

    /**
     * Marks this primitive as being modified.
     *
     * @param modified true, if this primitive is to be modified
     */
    void setModified(boolean modified);

    /**
     * Checks if object is known to the server.
     * Replies true if this primitive is either unknown to the server (i.e. its id
     * is 0) or it is known to the server and it hasn't be deleted on the server.
     * Replies false, if this primitive is known on the server and has been deleted
     * on the server.
     *
     * @return <code>true</code>, if the object is visible on server.
     * @see #setVisible(boolean)
     */
    boolean isVisible();

    /**
     * Sets whether this primitive is visible, i.e. whether it is known on the server
     * and not deleted on the server.
     * @param visible {@code true} if this primitive is visible
     *
     * @throws IllegalStateException if visible is set to false on an primitive with id==0
     * @see #isVisible()
     */
    void setVisible(boolean visible);

    /**
     * Replies <code>true</code>, if the object has been deleted.
     *
     * @return <code>true</code>, if the object has been deleted.
     * @see #setDeleted(boolean)
     */
    boolean isDeleted();

    /**
     * Sets whether this primitive is deleted or not.
     *
     * Also marks this primitive as modified if deleted is true.
     *
     * @param deleted  true, if this primitive is deleted; false, otherwise
     */
    void setDeleted(boolean deleted);

    /**
     * Determines if this primitive is incomplete.
     * @return {@code true} if this primitive is incomplete, {@code false} otherwise
     */
    boolean isIncomplete();

    /**
     * Replies <code>true</code> if the object has been deleted on the server and was undeleted by the user.
     * @return <code>true</code> if the object has been undeleted
     */
    boolean isUndeleted();

    /**
     * Replies <code>true</code>, if the object is usable
     * (i.e. complete and not deleted).
     *
     * @return <code>true</code>, if the object is usable.
     * @see #setDeleted(boolean)
     */
    boolean isUsable();

    /**
     * Determines if this primitive is new or undeleted.
     * @return True if primitive is new or undeleted
     * @see #isNew()
     * @see #isUndeleted()
     */
    boolean isNewOrUndeleted();

    /**
     * Replies true, if this primitive is disabled. (E.g. a filter applies)
     * @return {@code true} if this object has the "disabled" flag enabled
     * @since 13662
     */
    default boolean isDisabled() {
        return false;
    }

    /**
     * Replies true, if this primitive is disabled and marked as completely hidden on the map.
     * @return {@code true} if this object has both the "disabled" and "hide if disabled" flags enabled
     * @since 13662
     */
    default boolean isDisabledAndHidden() {
        return false;
    }

    /**
     * Replies true, if this primitive is preserved from filtering.
     * @return {@code true} if this object has the "preserved" flag enabled
     * @since 13764
     */
    default boolean isPreserved() {
        return false;
    }

    /**
     * Determines if this object is selectable.
     * <p>
     * A primitive can be selected if all conditions are met:
     * <ul>
     * <li>it is drawable
     * <li>it is not disabled (greyed out) by a filter.
     * </ul>
     * @return {@code true} if this object is selectable
     * @since 13664
     */
    default boolean isSelectable() {
        return true;
    }

    /**
     * Determines if this object is drawable.
     * <p>
     * A primitive is complete if all conditions are met:
     * <ul>
     * <li>type and id is known
     * <li>tags are known
     * <li>it is not deleted
     * <li>it is not hidden by a filter
     * <li>for nodes: lat/lon are known
     * <li>for ways: all nodes are known and complete
     * <li>for relations: all members are known and complete
     * </ul>
     * @return {@code true} if this object is drawable
     * @since 13664
     */
    default boolean isDrawable() {
        return true;
    }

    /**
     * Determines whether the primitive is selected
     * @return whether the primitive is selected
     * @since 13664
     */
    default boolean isSelected() {
        return false;
    }

    /**
     * Determines if this primitive is a member of a selected relation.
     * @return {@code true} if this primitive is a member of a selected relation, {@code false} otherwise
     * @since 13664
     */
    default boolean isMemberOfSelected() {
        return false;
    }

    /**
     * Determines if this primitive is an outer member of a selected multipolygon relation.
     * @return {@code true} if this primitive is an outer member of a selected multipolygon relation, {@code false} otherwise
     * @since 13664
     */
    default boolean isOuterMemberOfSelected() {
        return false;
    }

    /**
     * Replies the id of this primitive.
     *
     * @return the id of this primitive.
     */
    long getId();

    /**
     * Replies the OSM id of this primitive.
     * By default, returns the same value as {@link #getId}.
     * Can be overidden by primitive implementations handling an internal id different from the OSM one.
     *
     * @return the OSM id of this primitive.
     * @since 13924
     */
    default long getOsmId() {
        return getId();
    }

    /**
     * Replies the OSM primitive id for this primitive.
     *
     * @return the OSM primitive id for this primitive
     * @see #getOsmId
     * @since 13924
     */
    default PrimitiveId getOsmPrimitiveId() {
        return new SimplePrimitiveId(getOsmId(), getType());
    }

    /**
     * Replies the unique primitive id for this primitive.
     *
     * @return the unique primitive id for this primitive
     * @see #getUniqueId
     */
    default PrimitiveId getPrimitiveId() {
        return new SimplePrimitiveId(getUniqueId(), getType());
    }

    /**
     * Replies the version number as returned by the API. The version is 0 if the id is 0 or
     * if this primitive is incomplete.
     * @return the version number as returned by the API
     *
     * @see PrimitiveData#setVersion(int)
     */
    int getVersion();

    /**
     * Sets the id and the version of this primitive if it is known to the OSM API.
     *
     * Since we know the id and its version it can't be incomplete anymore. incomplete
     * is set to false.
     *
     * @param id the id. &gt; 0 required
     * @param version the version &gt; 0 required
     * @throws IllegalArgumentException if id &lt;= 0
     * @throws IllegalArgumentException if version &lt;= 0
     * @throws DataIntegrityProblemException if id is changed and primitive was already added to the dataset
     */
    void setOsmId(long id, int version);

    /**
     * Replies the user who has last touched this object. May be null.
     *
     * @return the user who has last touched this object. May be null.
     */
    User getUser();

    /**
     * Sets the user who has last touched this object.
     *
     * @param user the user
     */
    void setUser(User user);

    /**
     * Time of last modification to this object. This is not set by JOSM but
     * read from the server and delivered back to the server unmodified. It is
     * used to check against edit conflicts.
     *
     * @return date of last modification
     * @see #setTimestamp
     */
    Date getTimestamp();

    /**
     * Time of last modification to this object. This is not set by JOSM but
     * read from the server and delivered back to the server unmodified. It is
     * used to check against edit conflicts.
     *
     * @return last modification as timestamp
     * @see #setRawTimestamp
     */
    int getRawTimestamp();

    /**
     * Sets time of last modification to this object
     * @param timestamp date of last modification
     * @see #getTimestamp
     */
    void setTimestamp(Date timestamp);

    /**
     * Sets time of last modification to this object
     * @param timestamp date of last modification
     * @see #getRawTimestamp
     */
    void setRawTimestamp(int timestamp);

    /**
     * Determines if this primitive has no timestamp information.
     * @return {@code true} if this primitive has no timestamp information
     * @see #getTimestamp
     * @see #getRawTimestamp
     */
    boolean isTimestampEmpty();

    /**
     * Replies the id of the changeset this primitive was last uploaded to.
     * 0 if this primitive wasn't uploaded to a changeset yet or if the
     * changeset isn't known.
     *
     * @return the id of the changeset this primitive was last uploaded to.
     */
    int getChangesetId();

    /**
     * Sets the changeset id of this primitive. Can't be set on a new primitive.
     *
     * @param changesetId the id. &gt;= 0 required.
     * @throws IllegalStateException if this primitive is new.
     * @throws IllegalArgumentException if id &lt; 0
     */
    void setChangesetId(int changesetId);

    /**
     * Makes the given visitor visit this primitive.
     * @param visitor visitor
     */
    void accept(PrimitiveVisitor visitor);

    /**
     * <p>Visits {@code visitor} for all referrers.</p>
     *
     * @param visitor the visitor. Ignored, if null.
     * @since 13806
     */
    void visitReferrers(PrimitiveVisitor visitor);

    /**
     * Replies the name of this primitive. The default implementation replies the value
     * of the tag <code>name</code> or null, if this tag is not present.
     *
     * @return the name of this primitive
     */
    default String getName() {
        return get("name");
    }

    /**
     * Replies a localized name for this primitive given by the value of the name tags
     * accessed from very specific (language variant) to more generic (default name).
     *
     * @return the name of this primitive, <code>null</code> if no name exists
     * @see LanguageInfo#getLanguageCodes
     */
    default String getLocalName() {
        for (String s : LanguageInfo.getLanguageCodes(null)) {
            String val = get("name:" + s);
            if (val != null)
                return val;
        }

        return getName();
    }

    /**
     * Replies the display name of a primitive formatted by <code>formatter</code>
     * @param formatter formatter to use
     *
     * @return the display name
     * @since 13564
     */
    String getDisplayName(NameFormatter formatter);

    /**
     * Gets the type this primitive is displayed at
     * @return A {@link OsmPrimitiveType}
     * @since 13564
     */
    default OsmPrimitiveType getDisplayType() {
        return getType();
    }

    /**
     * Updates the highlight flag for this primitive.
     * @param highlighted The new highlight flag.
     * @since 13664
     */
    void setHighlighted(boolean highlighted);

    /**
     * Checks if the highlight flag for this primitive was set
     * @return The highlight flag.
     * @since 13664
     */
    boolean isHighlighted();

    /**
     * Determines if this object is considered "tagged". To be "tagged", an object
     * must have one or more "interesting" tags. "created_by" and "source"
     * are typically considered "uninteresting" and do not make an object "tagged".
     * @return true if this object is considered "tagged"
     * @since 13662
     */
    boolean isTagged();

    /**
     * Determines if this object is considered "annotated". To be "annotated", an object
     * must have one or more "work in progress" tags, such as "note" or "fixme".
     * @return true if this object is considered "annotated"
     * @since 13662
     */
    boolean isAnnotated();

    /**
     * Determines if this object is a relation and behaves as a multipolygon.
     * @return {@code true} if it is a real multipolygon or a boundary relation
     * @since 13667
     */
    default boolean isMultipolygon() {
        return false;
    }

    /**
     * true if this object has direction dependent tags (e.g. oneway)
     * @return {@code true} if this object has direction dependent tags
     * @since 13662
     */
    boolean hasDirectionKeys();

    /**
     * true if this object has the "reversed direction" flag enabled
     * @return {@code true} if this object has the "reversed direction" flag enabled
     * @since 13662
     */
    boolean reversedDirection();

    /**
     * Fetches the bounding box of the primitive.
     * @return Bounding box of the object
     * @since 13764
     */
    BBox getBBox();

    /**
     * Gets a list of all primitives in the current dataset that reference this primitive.
     * @return The referrers
     * @since 13764
     */
    default List<? extends IPrimitive> getReferrers() {
        return getReferrers(false);
    }

    /**
     * Find primitives that reference this primitive. Returns only primitives that are included in the same
     * dataset as this primitive. <br>
     *
     * For example following code will add wnew as referer to all nodes of existingWay, but this method will
     * not return wnew because it's not part of the dataset <br>
     *
     * <code>Way wnew = new Way(existingWay)</code>
     *
     * @param allowWithoutDataset If true, method will return empty list if primitive is not part of the dataset. If false,
     * exception will be thrown in this case
     *
     * @return a collection of all primitives that reference this primitive.
     * @since 13808
     */
    List<? extends IPrimitive> getReferrers(boolean allowWithoutDataset);

    /**
     * Returns the parent data set of this primitive.
     * @return OsmData this primitive is part of.
     * @since 13807
     */
    OsmData<?, ?, ?, ?> getDataSet();

    /**
     * Returns {@link #getKeys()} for which {@code key} does not fulfill uninteresting criteria.
     * @return A map of interesting tags
     * @since 13809
     */
    Map<String, String> getInterestingTags();

    /**
     * Replies true if other isn't null and has the same interesting tags (key/value-pairs) as this.
     *
     * @param other the other object primitive
     * @return true if other isn't null and has the same interesting tags (key/value-pairs) as this.
     * @since 13809
     */
    default boolean hasSameInterestingTags(IPrimitive other) {
        return (!hasKeys() && !other.hasKeys())
                || getInterestingTags().equals(other.getInterestingTags());
    }
}
