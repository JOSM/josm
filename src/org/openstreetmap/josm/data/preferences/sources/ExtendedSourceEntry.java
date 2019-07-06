// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences.sources;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.tools.ImageResource;
import org.openstreetmap.josm.tools.Utils;

/**
 * Source entry with additional metadata.
 * @since 12649 (extracted from gui.preferences package)
 */
public class ExtendedSourceEntry extends SourceEntry implements Comparable<ExtendedSourceEntry> {
    /** file name used for display */
    public String simpleFileName;
    /** version used for display */
    public String version;
    /** author name used for display */
    public String author;
    /** icon used for display */
    public ImageResource icon;
    /** webpage link used for display */
    public String link;
    /** short description used for display */
    public String description;
    /** Style type: can only have one value: "xml". Used to filter out old XML styles. For MapCSS styles, the value is not set. */
    public String styleType;
    /** minimum JOSM version required to enable this source entry */
    public Integer minJosmVersion;

    /**
     * Constructs a new {@code ExtendedSourceEntry}.
     * @param type type of source entry
     * @param simpleFileName file name used for display
     * @param url URL that {@link org.openstreetmap.josm.io.CachedFile} understands
     * @since 12825
     */
    public ExtendedSourceEntry(SourceType type, String simpleFileName, String url) {
        super(type, url, null, null, true);
        this.simpleFileName = simpleFileName;
    }

    /**
     * @return string representation for GUI list or menu entry
     */
    public String getDisplayName() {
        return title == null ? simpleFileName : title;
    }

    private static void appendRow(StringBuilder s, String th, String td) {
        s.append("<tr><th>").append(th).append("</th><td>").append(Utils.escapeReservedCharactersHTML(td)).append("</td</tr>");
    }

    /**
     * Returns a tooltip containing available metadata.
     * @return a tooltip containing available metadata
     */
    public String getTooltip() {
        StringBuilder s = new StringBuilder();
        appendRow(s, tr("Short Description:"), getDisplayName());
        appendRow(s, tr("URL:"), url);
        if (author != null) {
            appendRow(s, tr("Author:"), author);
        }
        if (link != null) {
            appendRow(s, tr("Webpage:"), link);
        }
        if (description != null) {
            appendRow(s, tr("Description:"), description);
        }
        if (version != null) {
            appendRow(s, tr("Version:"), version);
        }
        if (minJosmVersion != null) {
            appendRow(s, tr("Minimum JOSM Version:"), Integer.toString(minJosmVersion));
        }
        return "<html><style>th{text-align:right}td{width:400px}</style>"
                + "<table>" + s + "</table></html>";
    }

    @Override
    public String toString() {
        return "<html><b>" + getDisplayName() + "</b>"
                + (author == null ? "" : " <span color=\"gray\">" + tr("by {0}", author) + "</color>")
                + "</html>";
    }

    @Override
    public int compareTo(ExtendedSourceEntry o) {
        if (url.startsWith("resource") && !o.url.startsWith("resource"))
            return -1;
        if (o.url.startsWith("resource"))
            return 1;
        else
            return getDisplayName().compareToIgnoreCase(o.getDisplayName());
    }
}
