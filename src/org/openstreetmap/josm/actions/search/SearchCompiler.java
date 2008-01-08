// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.search;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.Relation;

/**
 * Implements a google-like search.
 * @author Imi
 */
public class SearchCompiler {

	private boolean caseSensitive = false;
	private PushbackTokenizer tokenizer;

	public SearchCompiler(boolean caseSensitive, PushbackTokenizer tokenizer) {
		this.caseSensitive = caseSensitive;
		this.tokenizer = tokenizer;
	}
	
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
		public KeyValue(String key, String value) {this.key = key; this.value = value; }
		@Override public boolean match(OsmPrimitive osm) {
			String value = null;
			if (key.equals("timestamp"))
				value = osm.getTimeStr();
			else
				value = osm.get(key);
			if (value == null)
				return false;
			String v1 = caseSensitive ? value : value.toLowerCase();
			String v2 = caseSensitive ? this.value : this.value.toLowerCase();
			return v1.indexOf(v2) != -1;
		}
		@Override public String toString() {return key+"="+value;}
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
			if (osm instanceof Way)
				return type.equals("way");
			if (osm instanceof Relation)
				return type.equals("relation");
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
            return osm.incomplete;
		}
		@Override public String toString() {return "incomplete";}
	}
	
	public static Match compile(String searchStr, boolean caseSensitive) {
		return new SearchCompiler(caseSensitive,
				new PushbackTokenizer(
					new PushbackReader(new StringReader(searchStr))))
			.parse();
	}

	public Match parse() {
		Match m = parseJuxta();
		if (!tokenizer.readIfEqual(null)) {
			throw new RuntimeException("Unexpected token: " + tokenizer.nextToken());
		}
		return m;
	}

	private Match parseJuxta() {
		Match juxta = new Always();

		Match m;
		while ((m = parseOr()) != null) {
			juxta = new And(m, juxta);
		}

		return juxta;
	}

	private Match parseOr() {
		Match a = parseNot();
		if (tokenizer.readIfEqual("|")) {
			Match b = parseNot();
			if (a == null || b == null) {
				throw new RuntimeException("Missing arguments for or.");
			}
			return new Or(a, b);
		}
		return a;
	}

	private Match parseNot() {
		if (tokenizer.readIfEqual("-")) {
			Match m = parseParens();
			if (m == null) {
				throw new RuntimeException("Missing argument for not.");
			}
			return new Not(m);
		}
		return parseParens();
	}

	private Match parseParens() {
		if (tokenizer.readIfEqual("(")) {
			Match m = parseJuxta();
			if (!tokenizer.readIfEqual(")")) {
				throw new RuntimeException("Expected closing paren");
			}
			return m;
		}
		return parsePat();
	}

	private Match parsePat() {
		String tok = tokenizer.readText();

		if (tokenizer.readIfEqual(":")) {
			String tok2 = tokenizer.readText();
			if (tok == null) tok = "";
			if (tok2 == null) tok2 = "";
			return parseKV(tok, tok2);
		}

		if (tok == null) {
			return null;
		} else if (tok.equals("modified")) {
			return new Modified();
		} else if (tok.equals("incomplete")) {
			return new Incomplete();
		} else if (tok.equals("selected")) {
			return new Selected();
		} else {
			return new Any(tok);
		}
	}

	private Match parseKV(String key, String value) {
		if (key.equals("type")) {
			return new ExactType(value);
		} else if (key.equals("id")) {
			try {
				return new Id(Long.parseLong(value));
			} catch (NumberFormatException x) {
				return new Id(0);
			}
		} else {
			return new KeyValue(key, value);
		}
	}
}
