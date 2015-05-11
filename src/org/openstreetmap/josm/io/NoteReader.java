// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.notes.NoteComment;
import org.openstreetmap.josm.data.notes.NoteComment.Action;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.tools.Utils;
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

    private InputSource inputSource;
    private List<Note> parsedNotes;

    /**
     * Notes can be represented in two XML formats. One is returned by the API
     * while the other is used to generate the notes dump file. The parser
     * needs to know which one it is handling.
     */
    private enum NoteParseMode {API, DUMP}

    /**
     * SAX handler to read note information from its XML representation.
     * Reads both API style and planet dump style formats.
     */
    private class Parser extends DefaultHandler {

        private NoteParseMode parseMode;
        private StringBuffer buffer = new StringBuffer();
        private Note thisNote;
        private long commentUid;
        private String commentUsername;
        private Action noteAction;
        private Date commentCreateDate;
        private Boolean commentIsNew;
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
                notes = new ArrayList<Note>(100);
                return;
            case "osm-notes":
                parseMode = NoteParseMode.DUMP;
                notes = new ArrayList<Note>(10000);
                return;
            }

            if (parseMode == NoteParseMode.API) {
                if("note".equals(qName)) {
                    double lat = Double.parseDouble(attrs.getValue("lat"));
                    double lon = Double.parseDouble(attrs.getValue("lon"));
                    LatLon noteLatLon = new LatLon(lat, lon);
                    thisNote = new Note(noteLatLon);
                }
                return;
            }

            //The rest only applies for dump mode
            switch(qName) {
            case "note":
                double lat = Double.parseDouble(attrs.getValue("lat"));
                double lon = Double.parseDouble(attrs.getValue("lon"));
                LatLon noteLatLon = new LatLon(lat, lon);
                thisNote = new Note(noteLatLon);
                thisNote.setId(Long.parseLong(attrs.getValue("id")));
                String closedTimeStr = attrs.getValue("closed_at");
                if(closedTimeStr == null) { //no closed_at means the note is still open
                    thisNote.setState(Note.State.open);
                } else {
                    thisNote.setState(Note.State.closed);
                    thisNote.setClosedAt(DateUtils.fromString(closedTimeStr));
                }
                thisNote.setCreatedAt(DateUtils.fromString(attrs.getValue("created_at")));
                break;
            case "comment":
                String uidStr = attrs.getValue("uid");
                if(uidStr == null) {
                    commentUid = 0;
                } else {
                    commentUid = Long.parseLong(uidStr);
                }
                commentUsername = attrs.getValue("user");
                noteAction = Action.valueOf(attrs.getValue("action"));
                commentCreateDate = DateUtils.fromString(attrs.getValue("timestamp"));
                String isNew = attrs.getValue("is_new");
                if(isNew == null) {
                    commentIsNew = false;
                } else {
                    commentIsNew = Boolean.valueOf(isNew);
                }
                break;
            }
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName) {
            if("note".equals(qName)) {
                notes.add(thisNote);
            }
            if("comment".equals(qName)) {
                User commentUser = User.createOsmUser(commentUid, commentUsername);
                if (commentUid == 0) {
                    commentUser = User.getAnonymous();
                }
                if(parseMode == NoteParseMode.API) {
                    commentIsNew = false;
                }
                if(parseMode == NoteParseMode.DUMP) {
                    commentText = buffer.toString();
                }
                thisNote.addComment(new NoteComment(commentCreateDate, commentUser, commentText, noteAction, commentIsNew));
                commentUid = 0;
                commentUsername = null;
                commentCreateDate = null;
                commentIsNew = null;
                commentText = null;
            }
            if(parseMode == NoteParseMode.DUMP) {
                return;
            }

            //the rest only applies to API mode
            switch (qName) {
            case "id":
                thisNote.setId(Long.parseLong(buffer.toString()));
                break;
            case "status":
                thisNote.setState(Note.State.valueOf(buffer.toString()));
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
                noteAction = Action.valueOf(buffer.toString());
                break;
            case "note": //nothing to do for comment or note, already handled above
            case "comment":
                break;
            }
        }

        @Override
        public void endDocument() throws SAXException  {
            parsedNotes = notes;
        }
    }

    /**
     * Initializes the reader with a given InputStream
     * @param source - InputStream containing Notes XML
     * @throws IOException
     */
    public NoteReader(InputStream source) throws IOException {
        this.inputSource = new InputSource(source);
    }

    /**
     * Initializes the reader with a string as a source
     * @param source UTF-8 string containing Notes XML to parse
     * @throws IOException
     */
    public NoteReader(String source) throws IOException {
        this.inputSource = new InputSource(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Parses the InputStream given to the constructor and returns
     * the resulting Note objects
     * @return List of Notes parsed from the input data
     * @throws SAXException
     * @throws IOException
     */
    public List<Note> parse() throws SAXException, IOException {
        DefaultHandler parser = new Parser();
        try {
            Utils.parseSafeSAX(inputSource, parser);
        } catch (ParserConfigurationException e) {
            Main.error(e); // broken SAXException chaining
            throw new SAXException(e);
        }
        return parsedNotes;
    }
}
