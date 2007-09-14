// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.text.MessageFormat;

/**
 * Internationalisation support.
 * 
 * @author Immanuel.Scholz
 */
public class I18n {
	/**
	 * Set by MainApplication. Changes here later will probably mess up everything, because
	 * many strings are already loaded.
	 */
	public static org.xnap.commons.i18n.I18n i18n;

	public static String tr(String text, Object... objects) {
		if (i18n == null)
			return MessageFormat.format(text, objects);
		return i18n.tr(text, objects);
	}

	public static String tr(String text) {
		if (i18n == null)
			return text;
		return i18n.tr(text);
	}

	public static String trn(String text, String pluralText, long n, Object... objects) {
		if (i18n == null)
			return n == 1 ? tr(text, objects) : tr(pluralText, objects);
			return i18n.trn(text, pluralText, n, objects);
	}

	public static String trn(String text, String pluralText, long n) {
		if (i18n == null)
			return n == 1 ? tr(text) : tr(pluralText);
			return i18n.trn(text, pluralText, n);
	}
}
