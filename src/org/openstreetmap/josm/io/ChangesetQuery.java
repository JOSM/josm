// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.util.Date;

import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.DateUtils;

import static org.openstreetmap.josm.tools.I18n.tr;

public class ChangesetQuery {
    private Long user = null;
    private LatLon min = null;
    private LatLon max = null;
    private Date closedAfter = null;
    private Date createdBefore = null;
    private Boolean open = null;
    private Boolean closed = null;

    public ChangesetQuery() {}

    public ChangesetQuery forUser(long uid) {
        if (uid <= 0)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' > 0 expected. Got ''{1}''.", "uid", uid));
        this.user = uid;
        return this;
    }

    public ChangesetQuery inBbox(double minLon, double minLat, double maxLon, double maxLat) {
        return inBbox(new LatLon(minLon, minLat), new LatLon(maxLon, maxLat));
    }

    public ChangesetQuery inBbox(LatLon min, LatLon max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public ChangesetQuery closedAfter(Date d) {
        this.closedAfter = d;
        return this;
    }

    public ChangesetQuery between(Date closedAfter, Date createdBefore ) {
        this.closedAfter = closedAfter;
        this.createdBefore = createdBefore;
        return this;
    }

    public ChangesetQuery beingOpen() {
        this.open =  true;
        this.closed = null;
        return this;
    }

    public ChangesetQuery beingClosed() {
        this.open =  null;
        this.closed = true;
        return this;
    }

    public String getQueryString() {
        StringBuffer sb = new StringBuffer();
        if (user != null) {
            sb.append("user").append("=").append(user);
        }
        if (min!=null && max != null) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append("min_lon").append("=").append(min.lonToString(CoordinateFormat.DECIMAL_DEGREES));
            sb.append("&");
            sb.append("min_lat").append("=").append(min.latToString(CoordinateFormat.DECIMAL_DEGREES));
            sb.append("&");
            sb.append("max_lon").append("=").append(max.lonToString(CoordinateFormat.DECIMAL_DEGREES));
            sb.append("&");
            sb.append("max_lat").append("=").append(max.latToString(CoordinateFormat.DECIMAL_DEGREES));
        }
        if (closedAfter != null && createdBefore != null) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append("time").append("=").append(DateUtils.fromDate(closedAfter))
            .append(",").append(DateUtils.fromDate(createdBefore));
        } else if (closedAfter != null) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append("time").append("=").append(DateUtils.fromDate(closedAfter));
        }

        if (open != null) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append("open=true");
        } else if (closed != null) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append("closed=true");
        }
        return sb.toString();
    }
}
