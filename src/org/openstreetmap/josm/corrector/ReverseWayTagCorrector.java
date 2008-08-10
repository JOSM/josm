// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Way;

public class ReverseWayTagCorrector extends TagCorrector<Way> {

	private static class PrefixSuffixSwitcher {

		private final String a;

		private final String b;

		private final Pattern startPattern;

		private final Pattern endPattern;

		public PrefixSuffixSwitcher(String a, String b) {
			this.a = a;
			this.b = b;
			startPattern = Pattern.compile("^(" + a + "|" + b + "):.*",
			        Pattern.CASE_INSENSITIVE);
			endPattern = Pattern.compile(".*:(" + a + "|" + b + ")$",
			        Pattern.CASE_INSENSITIVE);
		}

		public String apply(String text) {
			Matcher m = startPattern.matcher(text);
			if (!m.matches())
				m = endPattern.matcher(text);

			if (m.matches()) {
				String leftRight = m.group(1).toLowerCase();

				return text.substring(0, m.start(1)).concat(
				        leftRight.equals(a) ? b : a).concat(
				        text.substring(m.end(1)));
			}
			return text;
		}
	}

	private static PrefixSuffixSwitcher[] prefixSuffixSwitchers = new PrefixSuffixSwitcher[] {
	        new PrefixSuffixSwitcher("left", "right"),
	        new PrefixSuffixSwitcher("forward", "backward") };

	@Override public Collection<ChangePropertyCommand> execute(Way way) {

		Map<OsmPrimitive, List<TagCorrection>> tagCorrectionsMap = new HashMap<OsmPrimitive, List<TagCorrection>>();

		ArrayList<OsmPrimitive> primitives = new ArrayList<OsmPrimitive>();
		primitives.add(way);
		primitives.addAll(way.nodes);

		for (OsmPrimitive primitive : primitives) {

			tagCorrectionsMap.put(primitive, new ArrayList<TagCorrection>());

			for (String key : primitive.keySet()) {
				String newKey = key;
				String value = primitive.get(key);
				String newValue = value;

				if (key.equals("oneway")) {
					if (value.equals("-1"))
						newValue = OsmUtils.trueval;
					else {
						Boolean boolValue = OsmUtils.getOsmBoolean(value);
						if (boolValue != null && boolValue.booleanValue()) {
							newValue = "-1";
						}
					}
				} else {
					for (PrefixSuffixSwitcher prefixSuffixSwitcher : prefixSuffixSwitchers) {
						newKey = prefixSuffixSwitcher.apply(key);
						if (!key.equals(newKey))
							break;
					}
				}

				if (!key.equals(newKey) || !value.equals(newValue))
					tagCorrectionsMap.get(primitive).add(
					        new TagCorrection(key, value, newKey, newValue));
			}
		}

		return applyCorrections(tagCorrectionsMap,
		        tr("When reverting this way, following changes to properties "
		                + "of the way and its nodes are suggested in order "
		                + "to maintain data consistency."));
	}
}
