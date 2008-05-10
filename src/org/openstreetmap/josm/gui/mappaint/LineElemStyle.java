package org.openstreetmap.josm.gui.mappaint;
import java.awt.Color;

public class LineElemStyle extends ElemStyle
{
	public int width;
	public int realWidth = 0; //the real width of this line in meter
	public Color colour;
	public boolean dashed = false;

	public LineElemStyle (int width, int realWidth, Color colour, boolean dashed, long maxScale, long minScale) {
		this.width = width;
		this.realWidth = realWidth;
		this.colour = colour;
		this.dashed = dashed;
		this.maxScale = maxScale;
		this.minScale = minScale;
	}

	@Override public String toString() {
		return "LineElemStyle:  width= " + width + "realWidth= " + realWidth +  " colour=" + colour + " dashed=" + dashed;
	}
}
