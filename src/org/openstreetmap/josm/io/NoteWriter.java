// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Class to write a collection of notes out to XML.
 * The format is that of the note dump file with the addition of one
 * attribute in the comment element to indicate if the comment is a new local
 * comment that has not been uploaded to the OSM server yet.
 * @since 7732
 */
public class NoteWriter extends XmlWriter {

    /**
     * Create a NoteWriter that will write to the given PrintWriter
     * @param out PrintWriter to write XML to
     */
    public NoteWriter(PrintWriter out) {
        super(out);
    }

    /**
     * Create a NoteWriter that will write to a given OutputStream.
     * @param out OutputStream to write XML to
     */
    public NoteWriter(OutputStream out) {
        super(new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))));
    }

    /**
     * Write notes to designated output target
     * @param data Note collection to write
     */
    public void write(NoteData data) {
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<osm-notes>");
        for (Note note : data.getNotes()) {
            LatLon ll = note.getLatLon();
            out.print("  <note ");
            out.print("id=\"" + note.getId() + "\" ");
            out.print("lat=\"" + LatLon.cDdHighPecisionFormatter.format(ll.lat()) + "\" ");
            out.print("lon=\"" + LatLon.cDdHighPecisionFormatter.format(ll.lon()) + "\" ");
            out.print("created_at=\"" + DateUtils.fromDate(note.getCreatedAt()) + "\" ");
            if (note.getClosedAt() != null) {
                out.print("closed_at=\"" + DateUtils.fromDate(note.getClosedAt()) + "\" ");
            }

            out.println(">");
            for (NoteComment comment : note.getComments()) {
                writeComment(comment);
            }
            out.println("  </note>");
        }

        out.println("</osm-notes>");
        out.flush();
    }

    private void writeComment(NoteComment comment) {
        out.print("    <comment");
        out.print(" action=\"" + comment.getNoteAction() + "\" ");
        out.print("timestamp=\"" + DateUtils.fromDate(comment.getCommentTimestamp()) + "\" ");
        if (comment.getUser() != null && !comment.getUser().equals(User.getAnonymous())) {
            out.print("uid=\"" + comment.getUser().getId() + "\" ");
            out.print("user=\"" + encode(comment.getUser().getName()) + "\" ");
        }
        out.print("is_new=\"" + comment.isNew() + "\" ");
        out.print(">");
        out.print(encode(comment.getText(), false));
        out.println("</comment>");
    }
}
