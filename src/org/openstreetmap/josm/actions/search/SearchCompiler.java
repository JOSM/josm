// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.search;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Implements a google-like search.
 * @author Imi
 */
public class SearchCompiler {

	boolean caseSensitive = false;
	
	abstract public static class Match {
		abstract public boolean match(OsmPrimitive osm);
	}

	private static class Always extends Match {
		@Override public boolean match(OsmPrimitive osm) {
			return true;
		}
	}

	private static class Not extends Match {
		private final Match match;
		public Not(Match match) {this.match = match;}
		@Override public boolean match(OsmPrimitive osm) {
			return !match.match(osm);
		}
		@Override public String toString() {return "!"+match;}
	}

	private static class And extends Match {
		private Match lhs;
		private Match rhs;
		public And(Match lhs, Match rhs) {this.lhs = lhs; this.rhs = rhs;}
		@Override public boolean match(OsmPrimitive osm) {
			return lhs.match(osm) && rhs.match(osm);
		}
		@Override public String toString() {return lhs+" && "+rhs;}
	}

	private static class Or extends Match {
		private Match lhs;
		private Match rhs;
		public Or(Match lhs, Match rhs) {this.lhs = lhs; this.rhs = rhs;}
		@Override public boolean match(OsmPrimitive osm) {
			return lhs.match(osm) || rhs.match(osm);
		}
		@Override public String toString() {return lhs+" || "+rhs;}
	}

	private static class Id extends Match {
		private long id;
		public Id(long id) {this.id = id;}
		@Override public boolean match(OsmPrimitive osm) {
			return osm.id == id;
		}
		@Override public String toString() {return "id="+id;}
	}

	private class KeyValue extends Match {
		private String key;
		private String value;
		boolean notValue;
		public KeyValue(String key, String value, boolean notValue) {this.key = key; this.value = value; this.notValue = notValue;}
		@Override public boolean match(OsmPrimitive osm) {
			String value = null;
			if (key.equals("timestamp"))
				value = osm.getTimeStr();
			else
				value = osm.get(key);
			if (value == null)
				return notValue;
			String v1 = caseSensitive ? value : value.toLowerCase();
			String v2 = caseSensitive ? this.value : this.value.toLowerCase();
			return (v1.indexOf(v2) != -1) != notValue;
		}
		@Override public String toString() {return key+"="+(notValue?"!":"")+value;}
	}

	private class Any extends Match {
		private String s;
		public Any(String s) {this.s = s;}
		@Override public boolean match(OsmPrimitive osm) {
			if (osm.keys == null)
				return s.equals("");
			String search = caseSensitive ? s : s.toLowerCase();
			for (Entry<String, String> e : osm.keys.entrySet()) {
				String key = caseSensitive ? e.getKey() : e.getKey().toLowerCase();
				String value = caseSensitive ? e.getValue() : e.getValue().toLowerCase();
				if (key.indexOf(search) != -1 || value.indexOf(search) != -1)
					return true;
			}
			if (osm.user != null) {
				String name = osm.user.name;
				if (!caseSensitive)
					name = name.toLowerCase();
				if (name.indexOf(search) != -1)
					return true;
			}
			return false;
		}
		@Override public String toString() {return s;}
	}

	private static class ExactType extends Match {
		private String type;
		public ExactType(String type) {this.type = type;}
		@Override public boolean match(OsmPrimitive osm) {
			if (osm instanceof Node)
				return type.equals("node");
			if (osm instanceof Segment)
				return type.equals("segment");
			if (osm instanceof Way)
				return type.equals("way");
			throw new IllegalStateException("unknown class "+osm.getClass());
		}
		@Override public String toString() {return "type="+type;}
	}

	private static class Modified extends Match {
		@Override public boolean match(OsmPrimitive osm) {
			return osm.modified;
		}
		@Override public String toString() {return "modified";}
	}
	
	private static class Selected extends Match {
		@Override public boolean match(OsmPrimitive osm) {
			return osm.selected;
		}
		@Override public String toString() {return "selected";}
	}

	private static class Incomplete extends Match {
		@Override public boolean match(OsmPrimitive osm) {
			return osm instanceof Way && ((Way)osm).isIncomplete();
		}
		@Override public String toString() {return "incomplete";}
	}
	
	public static Match compile(String searchStr, boolean caseSensitive) {
		SearchCompiler searchCompiler = new SearchCompiler();
		searchCompiler.caseSensitive = caseSensitive;
		return searchCompiler.parse(new PushbackReader(new StringReader(searchStr)));
	}


	/**
	 * The token returned is <code>null</code> or starts with an identifier character:
	 * - for an '-'. This will be the only character
	 * : for an key. The value is the next token
	 * | for "OR"
	 * ' ' for anything else.
	 * @return The next token in the stream.
	 */
	private String nextToken(PushbackReader search) {
		try {
			int next;
			char c = ' ';
			while (c == ' ' || c == '\t' || c == '\n') {
				next = search.read();
				if (next == -1)
					return null;
				c = (char)next;
			}
			StringBuilder s;
			switch (c) {
			case '-':
				return "-";
			case '"':
				s = new StringBuilder(" ");
				for (int nc = search.read(); nc != -1 && nc != '"'; nc = search.read())
					s.append((char)nc);
				int nc = search.read();
				if (nc != -1 && (char)nc == ':')
					return ":"+s.toString();
				if (nc != -1)
					search.unread(nc);
				return s.toString();
			default:
				s = new StringBuilder();
			for (;;) {
				s.append(c);
				next = search.read();
				if (next == -1) {
					if (s.toString().equals("OR"))
						return "|";
					return " "+s.toString();
				}
				c = (char)next;
				if (c == ' ' || c == '\t' || c == ':' || c == '"') {
					if (c == ':')
						return ":"+s.toString();
					search.unread(next);
					if (s.toString().equals("OR"))
						return "|";
					return " "+s.toString();
				}
			}
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}		
	}


	private boolean notKey = false;
	private boolean notValue = false;
	private boolean or = false;
	private String key = null;
	String token = null;
	private Match build() {
		String value = token.substring(1);
		if (key == null) {
			Match c = null;
			if (value.equals("modified"))
				c = new Modified();
			else if (value.equals("incomplete"))
				c = new Incomplete();
			else if (value.equals("selected"))
				c = new Selected();
			else
				c = new Any(value);
			return notValue ? new Not(c) : c;
		}
		Match c;
		if (key.equals("type"))
			c = new ExactType(value);
		else if (key.equals("property")) {
			String realKey = "", realValue = value;
			int eqPos = value.indexOf("=");
			if (eqPos != -1) {
				realKey = value.substring(0,eqPos);
				realValue = value.substring(eqPos+1);
			}
			c = new KeyValue(realKey, realValue, notValue);
		} else if (key.equals("id")) {
			try {
				c = new Id(Long.parseLong(value));
			} catch (NumberFormatException x) {
				c = new Id(0);
			}
			if (notValue)
				c = new Not(c);
		} else
			c = new KeyValue(key, value, notValue);
		if (notKey)
			return new Not(c);
		return c;
	}

	private Match parse(PushbackReader search) {
		Match result = null;
		for (token = nextToken(search); token != null; token = nextToken(search)) {
			if (token.equals("-"))
				notValue = true;
			else if (token.equals("|")) {
				if (result == null)
					continue;
				or = true;
				notValue = false;
			} else if (token.startsWith(":")) {
				if (key == null) {
					key = token.substring(1);
					notKey = notValue;
					notValue = false;
				} else
					key += token.substring(1);
			} else {
				Match current = build();
				if (result == null)
					result = current;
				else
					result = or ? new Or(result, current) : new And(result, current);
					key = null;
					notKey = false;
					notValue = false;
					or = false;
			}
		}
		// if "key:" was the last search
		if (key != null) {
			token = " ";
			Match current = build();
			result = (result == null) ? current : new And(result, current);
		}
		return result == null ? new Always() : result;
	}
}
