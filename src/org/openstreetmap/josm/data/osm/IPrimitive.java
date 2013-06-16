// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Date;

import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;

/**
 * IPrimitive captures the common functions of {@link OsmPrimitive} and {@link PrimitiveData}.
 */
public interface IPrimitive extends Tagged, PrimitiveId {

    boolean isModified();
    void setModified(boolean modified);
    boolean isVisible();
    void setVisible(boolean visible);
    boolean isDeleted();
    void setDeleted(boolean deleted);
    boolean isIncomplete();
    boolean isNewOrUndeleted();
    long getId();
    PrimitiveId getPrimitiveId();
    int getVersion();
    void setOsmId(long id, int version);
    User getUser();
    void setUser(User user);
    Date getTimestamp();
    void setTimestamp(Date timestamp);
    boolean isTimestampEmpty();
    int getChangesetId();
    void setChangesetId(int changesetId);

    void accept(PrimitiveVisitor visitor);
    String getName();
    String getLocalName();

}
