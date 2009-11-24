// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChangesetClosedException extends OsmTransferException {

    /** the changeset id */
    private long changesetId;
    /** the date on which the changeset was closed */
    private Date closedOn;

    protected void parseErrorHeader(String errorHeader) {
        String pattern = "The changeset (\\d+) was closed at (.*)";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(errorHeader);
        if (m.matches()) {
            changesetId = Long.parseLong(m.group(1));
            // Example: Tue Oct 15 10:00:00 UTC 2009. Always parsed with english locale regardless
            // of the current locale in JOSM
            DateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy",Locale.ENGLISH);
            try {
                closedOn = formatter.parse(m.group(2));
            } catch(ParseException ex) {
                System.err.println(tr("Failed to parse date ''{0}'' replied by server.", m.group(2)));
                ex.printStackTrace();
            }
        } else {
            System.err.println(tr("Unexpected format of error header for conflict in changeset update. Got ''{0}''", errorHeader));
        }
    }

    public ChangesetClosedException(String errorHeader) {
        super(errorHeader);
        parseErrorHeader(errorHeader);
    }

    public long getChangesetId() {
        return changesetId;
    }

    public Date getClosedOn() {
        return closedOn;
    }
}
