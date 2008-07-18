// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Way;

public class ReverseWayTagCorrector extends TagCorrector<Way> {

	private static final Pattern leftRightStartRegex = Pattern.compile(
	        "^(left|right):.*", Pattern.CASE_INSENSITIVE);

	private static final Pattern leftRightEndRegex = Pattern.compile(
	        ".*:(left|right)$", Pattern.CASE_INSENSITIVE);

	@Override public boolean execute(Way way) {

		ArrayList<TagCorrection> tagCorrections = new ArrayList<TagCorrection>();

		for (String key : way.keySet()) {
			String newKey = key;
			String value = way.get(key);
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
				Matcher m = leftRightStartRegex.matcher(key);
				if (!m.matches())
					m = leftRightEndRegex.matcher(key);

				if (m.matches()) {
					String leftRight = m.group(1).toLowerCase();

					newKey = key.substring(0, m.start(1)).concat(
					        leftRight.equals("left") ? "right" : "left")
					        .concat(key.substring(m.end(1)));
				}
			}

			if (key != newKey || value != newValue)
				tagCorrections.add(new TagCorrection(key, value, newKey,
				        newValue));
		}

		return applyCorrections(tagCorrections, way,
		        tr("When reverting this way, following changes to the "
		                + "properties are suggested in order to maintain "
		                + "data consistency."));
	}
}
