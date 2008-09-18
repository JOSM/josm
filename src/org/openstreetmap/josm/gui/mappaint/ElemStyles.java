package org.openstreetmap.josm.gui.mappaint;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.Main;

public class ElemStyles
{
	private class StyleSet {
		HashMap<String, IconElemStyle> icons;
		HashMap<String, LineElemStyle> lines;
		HashMap<String, AreaElemStyle> areas;
		HashMap<String, LineElemStyle> modifiers;
		public StyleSet()
		{
			icons = new HashMap<String, IconElemStyle>();
			lines = new HashMap<String, LineElemStyle>();
			modifiers = new HashMap<String, LineElemStyle>();
			areas = new HashMap<String, AreaElemStyle>();
		}
	}
	HashMap<String, StyleSet> styleSet;

	public ElemStyles()
	{
		styleSet = new HashMap<String, StyleSet>();
	}

	private String getKey(String k, String v, String b)
	{
		if(v != null)
			return "n" + k + "=" + v;
		else if(b != null)
			return "b" + k  + "=" + OsmUtils.getNamedOsmBoolean(b);
		else
			return "x" + k;
	}

	public void add(String name, String k, String v, String b, LineElemStyle style)
	{
		getStyleSet(name, true).lines.put(getKey(k,v,b), style);
	}

	public void addModifier(String name, String k, String v, String b, LineElemStyle style)
	{
		getStyleSet(name, true).modifiers.put(getKey(k,v,b), style);
	}

	public void add(String name, String k, String v, String b, AreaElemStyle style)
	{
		getStyleSet(name, true).areas.put(getKey(k,v,b), style);
	}

	public void add(String name, String k, String v, String b, IconElemStyle style)
	{
		getStyleSet(name, true).icons.put(getKey(k,v,b), style);
	}

	private StyleSet getStyleSet(String name, boolean create)
	{
		if(name == null)
			name = Main.pref.get("mappaint.style", "standard");
		StyleSet s = styleSet.get(name);
		if(create && s == null)
		{
			s = new StyleSet();
			styleSet.put(name, s);
		}
		return s;
	}

	public IconElemStyle get(Node n)
	{
		StyleSet ss = getStyleSet(null, false);
		IconElemStyle ret = null;
		if(ss != null && n.keys != null)
		{
			Iterator<String> iterator = n.keys.keySet().iterator();
			while(iterator.hasNext())
			{
				String key = iterator.next();
				String val = n.keys.get(key);
				IconElemStyle style;
				if((style = ss.icons.get("n" + key + "=" + val)) != null)
				{
					if(ret == null || style.priority > ret.priority)
						ret = style;
				}
				if((style = ss.icons.get("b" + key + "=" + OsmUtils.getNamedOsmBoolean(val))) != null)
				{
					if(ret == null || style.priority > ret.priority)
						ret = style;
				}
				if((style = ss.icons.get("x" + key)) != null)
				{
					if(ret == null || style.priority > ret.priority)
						ret = style;
				}
			}
		}
		return ret;
	}

	public ElemStyle get(Way w)
	{
		StyleSet ss = getStyleSet(null, false);
		if(ss == null || w.keys == null)
			return null;
		AreaElemStyle retArea = null;
		LineElemStyle retLine = null;
		List<LineElemStyle> over = new LinkedList<LineElemStyle>();
		Iterator<String> iterator = w.keys.keySet().iterator();
		while(iterator.hasNext())
		{
			String key = iterator.next();
			String val = w.keys.get(key);
			AreaElemStyle styleArea;
			LineElemStyle styleLine;
			String idx = "n" + key + "=" + val;
			if((styleArea = ss.areas.get(idx)) != null && (retArea == null || styleArea.priority > retArea.priority))
				retArea = styleArea;
			if((styleLine = ss.lines.get(idx)) != null && (retLine == null || styleLine.priority > retLine.priority))
				retLine = styleLine;
			if((styleLine = ss.modifiers.get(idx)) != null)
				over.add(styleLine);
			idx = "b" + key + "=" + OsmUtils.getNamedOsmBoolean(val);
			if((styleArea = ss.areas.get(idx)) != null && (retArea == null || styleArea.priority > retArea.priority))
				retArea = styleArea;
			if((styleLine = ss.lines.get(idx)) != null && (retLine == null || styleLine.priority > retLine.priority))
				retLine = styleLine;
			if((styleLine = ss.modifiers.get(idx)) != null)
				over.add(styleLine);
			idx = "x" + key;
			if((styleArea = ss.areas.get(idx)) != null && (retArea == null || styleArea.priority > retArea.priority))
				retArea = styleArea;
			if((styleLine = ss.lines.get(idx)) != null && (retLine == null || styleLine.priority > retLine.priority))
				retLine = styleLine;
			if((styleLine = ss.modifiers.get(idx)) != null)
				over.add(styleLine);
		}
		if(over.size() != 0 && retLine != null)
		{
			Collections.sort(over);
			retLine = new LineElemStyle(retLine, over);
		}
		if(retArea != null)
		{
			if(retLine != null)
				return new AreaElemStyle(retArea, retLine);
			else
				return retArea;
		}
		return retLine;
	}

	public boolean isArea(Way w)
	{
		StyleSet ss = getStyleSet(null, false);
		if(ss != null && w.keys != null)
		{
			Iterator<String> iterator = w.keys.keySet().iterator();
			while(iterator.hasNext())
			{
				String key = iterator.next();
				String val = w.keys.get(key);
				if(ss.areas.containsKey("n" + key + "=" + val)
				|| ss.areas.containsKey("n" + key + "=" + OsmUtils.getNamedOsmBoolean(val))
				|| ss.areas.containsKey("x" + key))
					return true;
			}
		}
		return false;
	}
	public boolean hasAreas()
	{
		StyleSet ss = getStyleSet(null, false);
		return ss != null && ss.areas.size() > 0;
	}
}
