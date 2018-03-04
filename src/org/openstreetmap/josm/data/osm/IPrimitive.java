// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Date;

import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.tools.LanguageInfo;

/**
 * IPrimitive captures the common functions of {@link OsmPrimitive} and {@link PrimitiveData}.
 * @since 4098
 */
public interface IPrimitive extends Tagged, PrimitiveId {

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
     * Replies the id of this primitive.
     *
     * @return the id of this primitive.
     */
    long getId();

    /**
     * Replies the unique primitive id for this primitive
     *
     * @return the unique primitive id for this primitive
     */
    PrimitiveId getPrimitiveId();

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
     * Replies the name of this primitive. The default implementation replies the value
     * of the tag <code>name</code> or null, if this tag is not present.
     *
     * @return the name of this primitive
     */
    String getName();

    /**
     * Replies a localized name for this primitive given by the value of the name tags
     * accessed from very specific (language variant) to more generic (default name).
     *
     * @return the name of this primitive, <code>null</code> if no name exists
     * @see LanguageInfo#getLanguageCodes
     */
    String getLocalName();
}
