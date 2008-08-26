package org.openstreetmap.josm.gui.mappaint;
import java.awt.Color;

public class AreaElemStyle extends ElemStyle
{
	public Color colour;
	public LineElemStyle line = null;

	public AreaElemStyle (Color colour, long maxScale, long minScale) {
		this.colour = colour;
		this.maxScale = maxScale;
		this.minScale = minScale;
	}

	public AreaElemStyle(AreaElemStyle a, LineElemStyle l)
	{
		this.colour = a.colour;
		this.maxScale = a.maxScale;
		this.minScale = a.minScale;
		this.line = l;
	}

	@Override public String toString() {
		return "AreaElemStyle:   colour=" + colour;
	}
}
