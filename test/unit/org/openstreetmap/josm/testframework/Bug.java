// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.testframework;

/**
 * Annotation that indicate that a specific test case function was a bug.
 * @author Imi
 */
public @interface Bug {
	/**
	 * The revision this bug was detected. (Can be later than the actual first occourence.
	 * This number is just to have a revision where the bug actually happen.)
	 */
	int value();
}
