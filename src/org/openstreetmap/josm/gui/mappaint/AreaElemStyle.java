package org.openstreetmap.josm.gui.mappaint;
import java.awt.Color;

public class AreaElemStyle extends ElemStyle
{
	Color colour;

	public AreaElemStyle (Color colour, long maxScale, long minScale) {
		this.colour = colour;
		this.maxScale = maxScale;
		this.minScale = minScale;
	}

	public Color getColour() {
		return colour;
	}

	@Override public String toString() {
		return "AreaElemStyle:   colour=" + colour;
	}
}
