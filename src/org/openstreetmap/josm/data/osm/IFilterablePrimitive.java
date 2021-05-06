// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

/**
 * An interface used to indicate that a primitive is filterable
 * @author Taylor Smock
 * @since xxx
 */
public interface IFilterablePrimitive {
    /**
     * Get binary property used internally by the filter mechanism.
     * @return {@code true} if this object has the "hidden type" flag enabled
     */
    boolean getHiddenType();

    /**
     * Get binary property used internally by the filter mechanism.
     * @return {@code true} if this object has the "disabled type" flag enabled
     */
    boolean getDisabledType();

    /**
     * Make the primitive disabled (e.g.&nbsp;if a filter applies).
     *
     * To enable the primitive again, use unsetDisabledState.
     * @param hidden if the primitive should be completely hidden from view or
     *             just shown in gray color.
     * @return true, any flag has changed; false if you try to set the disabled
     * state to the value that is already preset
     */
    boolean setDisabledState(boolean hidden);

    /**
     * Remove the disabled flag from the primitive.
     * Afterwards, the primitive is displayed normally and can be selected again.
     * @return {@code true} if a change occurred
     */
    boolean unsetDisabledState();

    /**
     * Set binary property used internally by the filter mechanism.
     * @param isExplicit new "disabled type" flag value
     */
    void setDisabledType(boolean isExplicit);

    /**
     * Set binary property used internally by the filter mechanism.
     * @param isExplicit new "hidden type" flag value
     */
    void setHiddenType(boolean isExplicit);
}
