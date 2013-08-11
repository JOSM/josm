// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.Main;

/**
 * Constants for GPX handling.
 */
public interface GpxConstants {

    public static final String META_PREFIX = "meta.";
    public static final String META_AUTHOR_NAME = META_PREFIX + "author.name";
    public static final String META_AUTHOR_EMAIL = META_PREFIX + "author.email";
    public static final String META_AUTHOR_LINK = META_PREFIX + "author.link";
    public static final String META_COPYRIGHT_AUTHOR = META_PREFIX + "copyright.author";
    public static final String META_COPYRIGHT_LICENSE = META_PREFIX + "copyright.license";
    public static final String META_COPYRIGHT_YEAR = META_PREFIX + "copyright.year";
    public static final String META_DESC = META_PREFIX + "desc";
    public static final String META_KEYWORDS = META_PREFIX + "keywords";
    public static final String META_LINKS = META_PREFIX + "links";
    public static final String META_NAME = META_PREFIX + "name";
    public static final String META_TIME = META_PREFIX + "time";
    public static final String META_EXTENSIONS = META_PREFIX + "extensions";

    public static final String JOSM_EXTENSIONS_NAMESPACE_URI = Main.JOSM_WEBSITE + "/gpx-extensions-1.0";

    public static List<String> WPT_KEYS = Arrays.asList("ele", "time", "magvar", "geoidheight",
            "name", "cmt", "desc", "src", META_LINKS, "sym", "number", "type",
            "fix", "sat", "hdop", "vdop", "pdop", "ageofdgpsdata", "dgpsid", META_EXTENSIONS);

}
