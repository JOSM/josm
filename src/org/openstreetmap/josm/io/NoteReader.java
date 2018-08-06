// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.data.notes.NoteComment.Action;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.XmlUtils;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class to read Note objects from their XML representation. It can take
 * either API style XML which starts with an "osm" tag or a planet dump
 * style XML which starts with an "osm-notes" tag.
 */
public class NoteReader {

    private final InputSource inputSource;
    private List<Note> parsedNotes;

    /**
     * Notes can be represented in two XML formats. One is returned by the API
     * while the other is used to generate the notes dump file. The parser
     * needs to know which one it is handling.
     */
    private enum NoteParseMode {
        API,
        DUMP
    }

    /**
     * SAX handler to read note information from its XML representation.
     * Reads both API style and planet dump style formats.
     */
    private class Parser extends DefaultHandler {

        private NoteParseMode parseMode;
        private final StringBuilder buffer = new StringBuilder();
        private Note thisNote;
        private long commentUid;
        private String commentUsername;
        private Action noteAction;
        private Date commentCreateDate;
        private boolean commentIsNew;
        private List<Note> notes;
        private String commentText;

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            buffer.append(ch, start, length);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
            buffer.setLength(0);
            switch(qName) {
            case "osm":
                parseMode = NoteParseMode.API;
                notes = new ArrayList<>(100);
                return;
            case "osm-notes":
                parseMode = NoteParseMode.DUMP;
                notes = new ArrayList<>(10_000);
                return;
            }

            if (parseMode == NoteParseMode.API) {
                if ("note".equals(qName)) {
                    thisNote = parseNoteBasic(attrs);
                }
                return;
            }

            //The rest only applies for dump mode
            switch(qName) {
            case "note":
                thisNote = parseNoteFull(attrs);
                break;
            case "comment":
                commentUid = Long.parseLong(Optional.ofNullable(attrs.getValue("uid")).orElse("0"));
                commentUsername = attrs.getValue("user");
                noteAction = Action.valueOf(attrs.getValue("action").toUpperCase(Locale.ENGLISH));
                commentCreateDate = DateUtils.fromString(attrs.getValue("timestamp"));
                commentIsNew = Boolean.parseBoolean(Optional.ofNullable(attrs.getValue("is_new")).orElse("false"));
                break;
            default: // Do nothing
            }
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName) {
            if (notes != null && "note".equals(qName)) {
                notes.add(thisNote);
            }
            if ("comment".equals(qName)) {
                User commentUser = User.createOsmUser(commentUid, commentUsername);
                if (commentUid == 0) {
                    commentUser = User.getAnonymous();
                }
                if (parseMode == NoteParseMode.API) {
                    commentIsNew = false;
                }
                if (parseMode == NoteParseMode.DUMP) {
                    commentText = buffer.toString();
                }
                thisNote.addComment(new NoteComment(commentCreateDate, commentUser, commentText, noteAction, commentIsNew));
                commentUid = 0;
                commentUsername = null;
                commentCreateDate = null;
                commentIsNew = false;
                commentText = null;
            }
            if (parseMode == NoteParseMode.DUMP) {
                return;
            }

            //the rest only applies to API mode
            switch (qName) {
            case "id":
                thisNote.setId(Long.parseLong(buffer.toString()));
                break;
            case "status":
                thisNote.setState(Note.State.valueOf(buffer.toString().toUpperCase(Locale.ENGLISH)));
                break;
            case "date_created":
                thisNote.setCreatedAt(DateUtils.fromString(buffer.toString()));
                break;
            case "date_closed":
                thisNote.setClosedAt(DateUtils.fromString(buffer.toString()));
                break;
            case "date":
                commentCreateDate = DateUtils.fromString(buffer.toString());
                break;
            case "user":
                commentUsername = buffer.toString();
                break;
            case "uid":
                commentUid = Long.parseLong(buffer.toString());
                break;
            case "text":
                commentText = buffer.toString();
                buffer.setLength(0);
                break;
            case "action":
                noteAction = Action.valueOf(buffer.toString().toUpperCase(Locale.ENGLISH));
                break;
            case "note": //nothing to do for comment or note, already handled above
            case "comment":
                break;
            }
        }

        @Override
        public void endDocument() throws SAXException {
            parsedNotes = notes;
        }
    }

    static LatLon parseLatLon(Function<String, String> attrs) {
        double lat = Double.parseDouble(attrs.apply("lat"));
        double lon = Double.parseDouble(attrs.apply("lon"));
        return new LatLon(lat, lon);
    }

    static Note parseNoteBasic(Attributes attrs) {
        return parseNoteBasic(attrs::getValue);
    }

    static Note parseNoteBasic(Function<String, String> attrs) {
        return new Note(parseLatLon(attrs));
    }

    static Note parseNoteFull(Attributes attrs) {
        return parseNoteFull(attrs::getValue);
    }

    static Note parseNoteFull(Function<String, String> attrs) {
        Note note = parseNoteBasic(attrs);
        String id = attrs.apply("id");
        if (id != null) {
            note.setId(Long.parseLong(id));
        }
        String closedTimeStr = attrs.apply("closed_at");
        if (closedTimeStr == null) { //no closed_at means the note is still open
            note.setState(Note.State.OPEN);
        } else {
            note.setState(Note.State.CLOSED);
            note.setClosedAt(DateUtils.fromString(closedTimeStr));
        }
        String createdAt = attrs.apply("created_at");
        if (createdAt != null) {
            note.setCreatedAt(DateUtils.fromString(createdAt));
        }
        return note;
    }

    /**
     * Initializes the reader with a given InputStream
     * @param source - InputStream containing Notes XML
     */
    public NoteReader(InputStream source) {
        this.inputSource = new InputSource(source);
    }

    /**
     * Initializes the reader with a string as a source
     * @param source UTF-8 string containing Notes XML to parse
     */
    public NoteReader(String source) {
        this.inputSource = new InputSource(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Parses the InputStream given to the constructor and returns
     * the resulting Note objects
     * @return List of Notes parsed from the input data
     * @throws SAXException if any SAX parsing error occurs
     * @throws IOException if any I/O error occurs
     */
    public List<Note> parse() throws SAXException, IOException {
        DefaultHandler parser = new Parser();
        try {
            XmlUtils.parseSafeSAX(inputSource, parser);
        } catch (ParserConfigurationException e) {
            Logging.error(e); // broken SAXException chaining
            throw new SAXException(e);
        }
        return parsedNotes;
    }
}
