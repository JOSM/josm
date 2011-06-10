// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.actions.mapmode;

/**
 * TODO: rewrite to use awt modifers flag instead.
 *
 * @author Ole Jørgen Brønner (olejorgenb)
 */
public class ModifiersSpec {
    static public final int ON = 1, OFF = 0, UNKNOWN = 2;
    public int alt = UNKNOWN;
    public int shift = UNKNOWN;
    public int ctrl = UNKNOWN;

    /**
     *  'A' = Alt, 'S' = Shift, 'C' = Ctrl
     *  Lowercase signifies off and '?' means unknown/optional.
     *  Order is Alt, Shift, Ctrl
     * @param str
     */
    public ModifiersSpec(String str) {
        assert (str.length() == 3);
        char a = str.charAt(0);
        char s = str.charAt(1);
        char c = str.charAt(2);
        // @formatter:off
        alt   = (a == '?' ? UNKNOWN : (a == 'A' ? ON : OFF));
        shift = (s == '?' ? UNKNOWN : (s == 'S' ? ON : OFF));
        ctrl  = (c == '?' ? UNKNOWN : (c == 'C' ? ON : OFF));
        // @formatter:on
    }

    public ModifiersSpec(final int alt, final int shift, final int ctrl) {
        this.alt = alt;
        this.shift = shift;
        this.ctrl = ctrl;
    }

    public boolean matchWithKnown(final int knownAlt, final int knownShift, final int knownCtrl) {
        return match(alt, knownAlt) && match(shift, knownShift) && match(ctrl, knownCtrl);
    }

    public boolean matchWithKnown(final boolean knownAlt, final boolean knownShift, final boolean knownCtrl) {
        return match(alt, knownAlt) && match(shift, knownShift) && match(ctrl, knownCtrl);
    }

    private boolean match(final int a, final int knownValue) {
        assert (knownValue == ON | knownValue == OFF);
        return a == knownValue || a == UNKNOWN;
    }

    private boolean match(final int a, final boolean knownValue) {
        return a == (knownValue ? ON : OFF) || a == UNKNOWN;
    }
    // does java have built in 3-state support?
}
