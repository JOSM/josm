// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

/**
 * Interface defining string constants (MapCSS property keys).
 * 
 * For the implementation of the <code>@supports</code> feature, the list of
 * supported keys is loaded from this interface using reflection.
 * @see org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource#evalSupportsDeclCondition(java.lang.String, java.lang.Object)
 */
public interface StyleKeys {

    String COLOR = "color";
    String DASHES = "dashes";
    String DASHES_BACKGROUND_COLOR = "dashes-background-color";
    String DASHES_BACKGROUND_OPACITY = "dashes-background-opacity";
    String DASHES_OFFSET = "dashes-offset";
    String FILL_COLOR = "fill-color";
    String FILL_EXTENT = "fill-extent";
    String FILL_EXTENT_THRESHOLD = "fill-extent-threshold";
    String FILL_IMAGE = "fill-image";
    String FILL_OPACITY = "fill-opacity";
    String FONT_FAMILY = "font-family";
    String FONT_SIZE = "font-size";
    String FONT_STYLE = "font-style";
    String FONT_WEIGHT = "font-weight";
    String ICON_IMAGE = "icon-image";
    String ICON_HEIGHT = "icon-height";
    String ICON_OFFSET_X = "icon-offset-x";
    String ICON_OFFSET_Y = "icon-offset-y";
    String ICON_OPACITY = "icon-opacity";
    String ICON_ROTATION = "icon-rotation";
    String ICON_WIDTH = "icon-width";
    /**
     * Position of icons on area.
     */
    String ICON_POSITION = "icon-position";
    String LINECAP = "linecap";
    String LINEJOIN = "linejoin";
    String MAJOR_Z_INDEX = "major-z-index";
    String MITERLIMIT = "miterlimit";
    String MODIFIER = "modifier";
    String OBJECT_Z_INDEX = "object-z-index";
    String OFFSET = "offset";
    String OPACITY = "opacity";
    String REAL_WIDTH = "real-width";
    String REPEAT_IMAGE = "repeat-image";
    String REPEAT_IMAGE_ALIGN = "repeat-image-align";
    String REPEAT_IMAGE_HEIGHT = "repeat-image-height";
    String REPEAT_IMAGE_OFFSET = "repeat-image-offset";
    String REPEAT_IMAGE_OPACITY = "repeat-image-opacity";
    String REPEAT_IMAGE_PHASE = "repeat-image-phase";
    String REPEAT_IMAGE_SPACING = "repeat-image-spacing";
    String REPEAT_IMAGE_WIDTH = "repeat-image-width";
    String TEXT = "text";
    String TEXT_ANCHOR_HORIZONTAL = "text-anchor-horizontal";
    String TEXT_ANCHOR_VERTICAL = "text-anchor-vertical";
    String TEXT_COLOR = "text-color";
    String TEXT_HALO_COLOR = "text-halo-color";
    String TEXT_HALO_OPACITY = "text-halo-opacity";
    String TEXT_HALO_RADIUS = "text-halo-radius";
    String TEXT_OFFSET = "text-offset";
    String TEXT_OFFSET_X = "text-offset-x";
    String TEXT_OFFSET_Y = "text-offset-y";
    String TEXT_OPACITY = "text-opacity";
    String TEXT_POSITION = "text-position";
    String WAY_DIRECTION_ARROWS = "way-direction-arrows";
    String WIDTH = "width";
    String Z_INDEX = "z-index";

}
