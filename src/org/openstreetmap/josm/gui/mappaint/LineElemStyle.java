package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

public class LineElemStyle extends ElemStyle implements Comparable<LineElemStyle>
{
	public int width;
	public int realWidth; //the real width of this line in meter
	public Color color;
	public boolean dashed;

	public boolean over;
	public enum WidthMode { ABSOLUTE, PERCENT, OFFSET };
	public WidthMode widthMode;

	public List<LineElemStyle> overlays;

	public LineElemStyle(LineElemStyle s, long maxScale, long minScale) {
		this.width = s.width;
		this.realWidth = s.realWidth;
		this.color = s.color;
		this.dashed = s.dashed;
		this.over = s.over;
		this.widthMode = s.widthMode;

		this.priority = s.priority;
		this.maxScale = maxScale;
		this.minScale = minScale;
	}

	public LineElemStyle(LineElemStyle s, List<LineElemStyle> overlays) {
		this.width = s.width;
		this.realWidth = s.realWidth;
		this.color = s.color;
		this.dashed = s.dashed;
		this.over = s.over;
		this.widthMode = s.widthMode;

		this.priority = s.priority;
		this.maxScale = s.maxScale;
		this.minScale = s.minScale;

		this.overlays = overlays;
	}

	public LineElemStyle() { init(); }

	public void init()
	{
		width = 1;
		realWidth = 0;
		dashed = false;
		priority = 0;
		color = null;
		over = true; // only used for line modifications
		widthMode = WidthMode.ABSOLUTE;
		overlays = null;
	};

	// get width for overlays
	public int getWidth(int ref)
	{
		int res;
		if(widthMode == WidthMode.ABSOLUTE)
			res = width;
		else if(widthMode == WidthMode.OFFSET)
			res = ref + width;
		else
		{
			if(width < 0)
				res = 0;
			else
				res = ref*width/100;
		}
		return res <= 0 ? 1 : res;
	}

	public int compareTo(LineElemStyle s)
	{
		if(s.priority != priority)
			return s.priority > priority ? 1 : -1;
		if(!over && s.over)
			return -1;
		// we have no idea how to order other objects :-)
		return 0;
	}
}
