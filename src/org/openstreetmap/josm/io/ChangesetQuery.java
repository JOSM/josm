// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.date.DateUtils;

public class ChangesetQuery {

    /**
     * Replies a changeset query object from the query part of a OSM API URL for querying changesets.
     *
     * @param query the query part
     * @return the query object
     * @throws ChangesetQueryUrlException if query doesn't consist of valid query parameters
     */
    public static ChangesetQuery buildFromUrlQuery(String query) throws ChangesetQueryUrlException{
        return new ChangesetQueryUrlParser().parse(query);
    }

    /** the user id this query is restricted to. null, if no restriction to a user id applies */
    private Integer uid = null;
    /** the user name this query is restricted to. null, if no restriction to a user name applies */
    private String userName = null;
    /** the bounding box this query is restricted to. null, if no restriction to a bounding box applies */
    private Bounds bounds = null;

    private Date closedAfter = null;
    private Date createdBefore = null;
    /** indicates whether only open changesets are queried. null, if no restrictions regarding open changesets apply */
    private Boolean open = null;
    /** indicates whether only closed changesets are queried. null, if no restrictions regarding open changesets apply */
    private Boolean closed = null;
    /** a collection of changeset ids to query for */
    private Collection<Long> changesetIds = null;

    /**
     * Restricts the query to changesets owned by the user with id <code>uid</code>.
     *
     * @param uid the uid of the user. &gt; 0 expected.
     * @return the query object with the applied restriction
     * @throws IllegalArgumentException if uid &lt;= 0
     * @see #forUser(String)
     */
    public ChangesetQuery forUser(int uid) {
        if (uid <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' > 0 expected. Got ''{1}''.", "uid", uid));
        this.uid = uid;
        this.userName = null;
        return this;
    }

    /**
     * Restricts the query to changesets owned by the user with user name <code>username</code>.
     *
     * Caveat: for historical reasons the username might not be unique! It is recommended to use
     * {@link #forUser(int)} to restrict the query to a specific user.
     *
     * @param username the username. Must not be null.
     * @return the query object with the applied restriction
     * @throws IllegalArgumentException if username is null.
     * @see #forUser(int)
     */
    public ChangesetQuery forUser(String username) {
        CheckParameterUtil.ensureParameterNotNull(username, "username");
        this.userName = username;
        this.uid = null;
        return this;
    }

    /**
     * Replies true if this query is restricted to user whom we only know the user name for.
     *
     * @return true if this query is restricted to user whom we only know the user name for
     */
    public boolean isRestrictedToPartiallyIdentifiedUser() {
        return userName != null;
    }

    /**
     * Replies the user name which this query is restricted to. null, if this query isn't
     * restricted to a user name, i.e. if {@link #isRestrictedToPartiallyIdentifiedUser()} is false.
     *
     * @return the user name which this query is restricted to
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Replies true if this query is restricted to user whom know the user id for.
     *
     * @return true if this query is restricted to user whom know the user id for
     */
    public boolean isRestrictedToFullyIdentifiedUser() {
        return uid > 0;
    }

    /**
     * Replies a query which is restricted to a bounding box.
     *
     * @param minLon  min longitude of the bounding box. Valid longitude value expected.
     * @param minLat  min latitude of the bounding box. Valid latitude value expected.
     * @param maxLon  max longitude of the bounding box. Valid longitude value expected.
     * @param maxLat  max latitude of the bounding box.  Valid latitude value expected.
     *
     * @return the restricted changeset query
     * @throws IllegalArgumentException if either of the parameters isn't a valid longitude or
     * latitude value
     */
    public ChangesetQuery inBbox(double minLon, double minLat, double maxLon, double maxLat) {
        if (!LatLon.isValidLon(minLon))
            throw new IllegalArgumentException(tr("Illegal longitude value for parameter ''{0}'', got {1}", "minLon", minLon));
        if (!LatLon.isValidLon(maxLon))
            throw new IllegalArgumentException(tr("Illegal longitude value for parameter ''{0}'', got {1}", "maxLon", maxLon));
        if (!LatLon.isValidLat(minLat))
            throw new IllegalArgumentException(tr("Illegal latitude value for parameter ''{0}'', got {1}", "minLat", minLat));
        if (!LatLon.isValidLat(maxLat))
            throw new IllegalArgumentException(tr("Illegal longitude value for parameter ''{0}'', got {1}", "maxLat", maxLat));

        return inBbox(new LatLon(minLon, minLat), new LatLon(maxLon, maxLat));
    }

    /**
     * Replies a query which is restricted to a bounding box.
     *
     * @param min the min lat/lon coordinates of the bounding box. Must not be null.
     * @param max the max lat/lon coordiantes of the bounding box. Must not be null.
     *
     * @return the restricted changeset query
     * @throws IllegalArgumentException if min is null
     * @throws IllegalArgumentException if max is null
     */
    public ChangesetQuery inBbox(LatLon min, LatLon max) {
        CheckParameterUtil.ensureParameterNotNull(min, "min");
        CheckParameterUtil.ensureParameterNotNull(max, "max");
        this.bounds  = new Bounds(min,max);
        return this;
    }

    /**
     *  Replies a query which is restricted to a bounding box given by <code>bbox</code>.
     *
     * @param bbox the bounding box. Must not be null.
     * @return the changeset query
     * @throws IllegalArgumentException if bbox is null.
     */
    public ChangesetQuery inBbox(Bounds bbox) {
        CheckParameterUtil.ensureParameterNotNull(bbox, "bbox");
        this.bounds = bbox;
        return this;
    }

    /**
     * Restricts the result to changesets which have been closed after the date given by <code>d</code>.
     * <code>d</code> d is a date relative to the current time zone.
     *
     * @param d the date . Must not be null.
     * @return the restricted changeset query
     * @throws IllegalArgumentException if d is null
     */
    public ChangesetQuery closedAfter(Date d) {
        CheckParameterUtil.ensureParameterNotNull(d, "d");
        this.closedAfter = d;
        return this;
    }

    /**
     * Restricts the result to changesets which have been closed after <code>closedAfter</code> and which
     * habe been created before <code>createdBefore</code>. Both dates are expressed relative to the current
     * time zone.
     *
     * @param closedAfter only reply changesets closed after this date. Must not be null.
     * @param createdBefore only reply changesets created before this date. Must not be null.
     * @return the restricted changeset query
     * @throws IllegalArgumentException if closedAfter is null
     * @throws IllegalArgumentException if createdBefore is null
     */
    public ChangesetQuery closedAfterAndCreatedBefore(Date closedAfter, Date createdBefore ) {
        CheckParameterUtil.ensureParameterNotNull(closedAfter, "closedAfter");
        CheckParameterUtil.ensureParameterNotNull(createdBefore, "createdBefore");
        this.closedAfter = closedAfter;
        this.createdBefore = createdBefore;
        return this;
    }

    /**
     * Restricts the result to changesets which are or aren't open, depending on the value of
     * <code>isOpen</code>
     *
     * @param isOpen whether changesets should or should not be open
     * @return the restricted changeset query
     */
    public ChangesetQuery beingOpen(boolean isOpen) {
        this.open =  isOpen;
        return this;
    }

    /**
     * Restricts the result to changesets which are or aren't closed, depending on the value of
     * <code>isClosed</code>
     *
     * @param isClosed whether changesets should or should not be open
     * @return the restricted changeset query
     */
    public ChangesetQuery beingClosed(boolean isClosed) {
        this.closed = isClosed;
        return this;
    }

    /**
     * Restricts the query to the given changeset ids (which are added to previously added ones).
     *
     * @param changesetIds the changeset ids
     * @return the query object with the applied restriction
     * @throws IllegalArgumentException if changesetIds is null.
     */
    public ChangesetQuery forChangesetIds(Collection<Long> changesetIds) {
        CheckParameterUtil.ensureParameterNotNull(changesetIds, "changesetIds");
        this.changesetIds = changesetIds;
        return this;
    }

    /**
     * Replies the query string to be used in a query URL for the OSM API.
     *
     * @return the query string
     */
    public String getQueryString() {
        StringBuilder sb = new StringBuilder();
        if (uid != null) {
            sb.append("user=").append(uid);
        } else if (userName != null) {
            sb.append("display_name=").append(Utils.encodeUrl(userName));
        }
        if (bounds != null) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append("bbox=").append(bounds.encodeAsString(","));
        }
        if (closedAfter != null && createdBefore != null) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            DateFormat df = DateUtils.newIsoDateTimeFormat();
            sb.append("time=").append(df.format(closedAfter));
            sb.append(',').append(df.format(createdBefore));
        } else if (closedAfter != null) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            DateFormat df = DateUtils.newIsoDateTimeFormat();
            sb.append("time=").append(df.format(closedAfter));
        }

        if (open != null) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append("open=").append(Boolean.toString(open));
        } else if (closed != null) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append("closed=").append(Boolean.toString(closed));
        } else if (changesetIds != null) {
            // since 2013-12-05, see https://github.com/openstreetmap/openstreetmap-website/commit/1d1f194d598e54a5d6fb4f38fb569d4138af0dc8
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append("changesets=").append(Utils.join(",", changesetIds));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getQueryString();
    }

    public static class ChangesetQueryUrlException extends Exception {

        /**
         * Constructs a new {@code ChangesetQueryUrlException} with the specified detail message.
         *
         * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
         */
        public ChangesetQueryUrlException(String message) {
            super(message);
        }

        /**
         * Constructs a new {@code ChangesetQueryUrlException} with the specified cause and detail message.
         *
         * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
         * @param  cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
         *         (A <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or unknown.)
         */
        public ChangesetQueryUrlException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs a new {@code ChangesetQueryUrlException} with the specified cause and a detail message of
         * <tt>(cause==null ? null : cause.toString())</tt> (which typically contains the class and detail message of <tt>cause</tt>).
         *
         * @param  cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
         *         (A <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or unknown.)
         */
        public ChangesetQueryUrlException(Throwable cause) {
            super(cause);
        }
    }

    public static class ChangesetQueryUrlParser {
        protected int parseUid(String value) throws ChangesetQueryUrlException {
            if (value == null || value.trim().isEmpty())
                throw new ChangesetQueryUrlException(
                        tr("Unexpected value for ''{0}'' in changeset query url, got {1}", "uid", value));
            int id;
            try {
                id = Integer.parseInt(value);
                if (id <= 0)
                    throw new ChangesetQueryUrlException(
                            tr("Unexpected value for ''{0}'' in changeset query url, got {1}", "uid", value));
            } catch(NumberFormatException e) {
                throw new ChangesetQueryUrlException(
                        tr("Unexpected value for ''{0}'' in changeset query url, got {1}", "uid", value), e);
            }
            return id;
        }

        protected boolean parseBoolean(String value, String parameter) throws ChangesetQueryUrlException {
            if (value == null || value.trim().isEmpty())
                throw new ChangesetQueryUrlException(
                        tr("Unexpected value for ''{0}'' in changeset query url, got {1}", parameter, value));
            switch (value) {
            case "true":
                return true;
            case "false":
                return false;
            default:
                throw new ChangesetQueryUrlException(
                        tr("Unexpected value for ''{0}'' in changeset query url, got {1}", parameter, value));
            }
        }

        protected Date parseDate(String value, String parameter) throws ChangesetQueryUrlException {
            if (value == null || value.trim().isEmpty())
                throw new ChangesetQueryUrlException(
                        tr("Unexpected value for ''{0}'' in changeset query url, got {1}", parameter, value));
            DateFormat formatter = DateUtils.newIsoDateTimeFormat();
            try {
                return formatter.parse(value);
            } catch(ParseException e) {
                throw new ChangesetQueryUrlException(
                        tr("Unexpected value for ''{0}'' in changeset query url, got {1}", parameter, value), e);
            }
        }

        protected Date[] parseTime(String value) throws ChangesetQueryUrlException {
            String[] dates = value.split(",");
            if (dates == null || dates.length == 0 || dates.length > 2)
                throw new ChangesetQueryUrlException(
                        tr("Unexpected value for ''{0}'' in changeset query url, got {1}", "time", value));
            if (dates.length == 1)
                return new Date[]{parseDate(dates[0], "time")};
            else if (dates.length == 2)
                return new Date[]{parseDate(dates[0], "time"), parseDate(dates[1], "time")};
            return null;
        }

        protected Collection<Long> parseLongs(String value) {
            return value == null || value.isEmpty()
                    ? Collections.<Long>emptySet() :
                    new HashSet<>(Utils.transform(Arrays.asList(value.split(",")), new Utils.Function<String, Long>() {
                        @Override
                        public Long apply(String x) {
                            return Long.valueOf(x);
                        }
                    }));
        }

        protected ChangesetQuery createFromMap(Map<String, String> queryParams) throws ChangesetQueryUrlException {
            ChangesetQuery csQuery = new ChangesetQuery();

            for (Entry<String, String> entry: queryParams.entrySet()) {
                String k = entry.getKey();
                switch(k) {
                case "uid":
                    if (queryParams.containsKey("display_name"))
                        throw new ChangesetQueryUrlException(
                                tr("Cannot create a changeset query including both the query parameters ''uid'' and ''display_name''"));
                    csQuery.forUser(parseUid(queryParams.get("uid")));
                    break;
                case "display_name":
                    if (queryParams.containsKey("uid"))
                        throw new ChangesetQueryUrlException(
                                tr("Cannot create a changeset query including both the query parameters ''uid'' and ''display_name''"));
                    csQuery.forUser(queryParams.get("display_name"));
                    break;
                case "open":
                    csQuery.beingOpen(parseBoolean(entry.getValue(), "open"));
                    break;
                case "closed":
                    csQuery.beingClosed(parseBoolean(entry.getValue(), "closed"));
                    break;
                case "time":
                    Date[] dates = parseTime(entry.getValue());
                    switch(dates.length) {
                    case 1:
                        csQuery.closedAfter(dates[0]);
                        break;
                    case 2:
                        csQuery.closedAfterAndCreatedBefore(dates[0], dates[1]);
                        break;
                    }
                    break;
                case "bbox":
                    try {
                        csQuery.inBbox(new Bounds(entry.getValue(), ","));
                    } catch (IllegalArgumentException e) {
                        throw new ChangesetQueryUrlException(e);
                    }
                    break;
                case "changesets":
                    try {
                        csQuery.forChangesetIds(parseLongs(entry.getValue()));
                    } catch (NumberFormatException e) {
                        throw new ChangesetQueryUrlException(e);
                    }
                    break;
                default:
                    throw new ChangesetQueryUrlException(
                            tr("Unsupported parameter ''{0}'' in changeset query string", k));
                }
            }
            return csQuery;
        }

        protected Map<String,String> createMapFromQueryString(String query) {
            Map<String,String> queryParams  = new HashMap<>();
            String[] keyValuePairs = query.split("&");
            for (String keyValuePair: keyValuePairs) {
                String[] kv = keyValuePair.split("=");
                queryParams.put(kv[0], kv.length > 1 ? kv[1] : "");
            }
            return queryParams;
        }

        /**
         * Parses the changeset query given as URL query parameters and replies a {@link ChangesetQuery}.
         *
         * <code>query</code> is the query part of a API url for querying changesets,
         * see <a href="http://wiki.openstreetmap.org/wiki/API_v0.6#Query:_GET_.2Fapi.2F0.6.2Fchangesets">OSM API</a>.
         *
         * Example for an query string:<br>
         * <pre>
         *    uid=1234&amp;open=true
         * </pre>
         *
         * @param query the query string. If null, an empty query (identical to a query for all changesets) is
         * assumed
         * @return the changeset query
         * @throws ChangesetQueryUrlException if the query string doesn't represent a legal query for changesets
         */
        public ChangesetQuery parse(String query) throws ChangesetQueryUrlException {
            if (query == null)
                return new ChangesetQuery();
            query = query.trim();
            if (query.isEmpty())
                return new ChangesetQuery();
            Map<String,String> queryParams = createMapFromQueryString(query);
            return createFromMap(queryParams);
        }
    }
}
