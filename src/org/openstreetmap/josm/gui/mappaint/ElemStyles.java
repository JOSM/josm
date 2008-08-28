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

public class ElemStyles
{
	private HashMap<String, IconElemStyle> icons;
	private HashMap<String, LineElemStyle> lines;
	private HashMap<String, AreaElemStyle> areas;
	private HashMap<String, LineElemStyle> modifiers;

	public ElemStyles()
	{
		icons = new HashMap<String, IconElemStyle>();
		lines = new HashMap<String, LineElemStyle>();
		modifiers = new HashMap<String, LineElemStyle>();
		areas = new HashMap<String, AreaElemStyle>();
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

	public void add(String k, String v, String b, LineElemStyle style)
	{
		lines.put(getKey(k,v,b), style);
	}

	public void addModifier(String k, String v, String b, LineElemStyle style)
	{
		modifiers.put(getKey(k,v,b), style);
	}

	public void add(String k, String v, String b, AreaElemStyle style)
	{
		areas.put(getKey(k,v,b), style);
	}

	public void add(String k, String v, String b, IconElemStyle style)
	{
		icons.put(getKey(k,v,b), style);
	}

	public IconElemStyle get(Node n)
	{
		IconElemStyle ret = null;
		if(n.keys != null)
		{
			Iterator<String> iterator = n.keys.keySet().iterator();
			while(iterator.hasNext())
			{
				String key = iterator.next();
				String val = n.keys.get(key);
				IconElemStyle style;
				if((style = icons.get("n" + key + "=" + val)) != null)
				{
					if(ret == null || style.priority > ret.priority)
						ret = style;
				}
				if((style = icons.get("n" + key + "=" + OsmUtils.getNamedOsmBoolean(val))) != null)
				{
					if(ret == null || style.priority > ret.priority)
						ret = style;
				}
				if((style = icons.get("x" + key)) != null)
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
		AreaElemStyle retArea = null;
		LineElemStyle retLine = null;
		List<LineElemStyle> over = new LinkedList<LineElemStyle>();
		if(w.keys != null)
		{
			Iterator<String> iterator = w.keys.keySet().iterator();
			while(iterator.hasNext())
			{
				String key = iterator.next();
				String val = w.keys.get(key);
				AreaElemStyle styleArea;
				LineElemStyle styleLine;
				String idx = "n" + key + "=" + val;
				if((styleArea = areas.get(idx)) != null && (retArea == null || styleArea.priority > retArea.priority))
					retArea = styleArea;
				if((styleLine = lines.get(idx)) != null && (retLine == null || styleLine.priority > retLine.priority))
					retLine = styleLine;
				if((styleLine = modifiers.get(idx)) != null)
					over.add(styleLine);
				idx = "b" + key + "=" + OsmUtils.getNamedOsmBoolean(val);
				if((styleArea = areas.get(idx)) != null && (retArea == null || styleArea.priority > retArea.priority))
					retArea = styleArea;
				if((styleLine = lines.get(idx)) != null && (retLine == null || styleLine.priority > retLine.priority))
					retLine = styleLine;
				if((styleLine = modifiers.get(idx)) != null)
					over.add(styleLine);
				idx = "x" + key;
				if((styleArea = areas.get(idx)) != null && (retArea == null || styleArea.priority > retArea.priority))
					retArea = styleArea;
				if((styleLine = lines.get(idx)) != null && (retLine == null || styleLine.priority > retLine.priority))
					retLine = styleLine;
				if((styleLine = modifiers.get(idx)) != null)
					over.add(styleLine);
			}
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
		if(w.keys != null)
		{
			Iterator<String> iterator = w.keys.keySet().iterator();
			while(iterator.hasNext())
			{
				String key = iterator.next();
				String val = w.keys.get(key);
				if(areas.containsKey("n" + key + "=" + val)
				|| areas.containsKey("n" + key + "=" + OsmUtils.getNamedOsmBoolean(val))
				|| areas.containsKey("x" + key))
					return true;
			}
		}
		return false;
	}
}
