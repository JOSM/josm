package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;

import org.openstreetmap.josm.tools.ColorHelper;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import org.openstreetmap.josm.Main;

public class ElemStyleHandler extends DefaultHandler
{
	boolean inDoc, inRule, inCondition, inElemStyle, inLine, inLineMod, inIcon, inArea, inScaleMax, inScaleMin;
	boolean hadLine, hadLineMod, hadIcon, hadArea;
	ElemStyles styles;
	String styleName;
	RuleElem rule = new RuleElem();

	class RuleElem {
		String key;
		String value;
		String boolValue;
		long scaleMax;
		long scaleMin;
		LineElemStyle line = new LineElemStyle();
		LineElemStyle linemod = new LineElemStyle();
		AreaElemStyle area = new AreaElemStyle();
		IconElemStyle icon = new IconElemStyle();
		public void init()
		{
			key = value = boolValue = null;
			scaleMax = 1000000000;
			scaleMin = 0;
			line.init();
			linemod.init();
			area.init();
			icon.init();
		}
	};

	public ElemStyleHandler(String name) {
		styleName = name;
		inDoc=inRule=inCondition=inElemStyle=inLine=inIcon=inArea=false;
		rule.init();
		styles = MapPaintStyles.getStyles();
	}

	Color convertColor(String colString)
	{
		int i = colString.indexOf("#");
		String colorString;
		if(i < 0) // name only
			colorString = Main.pref.get("color.mappaint."+colString);
		else if(i == 0) // value only
			colorString = colString;
		else // value and name
			colorString = Main.pref.get("color.mappaint."+colString.substring(0,i), colString.substring(i));
		return ColorHelper.html2color(colorString);
	}

	@Override public void startDocument() {
		inDoc = true;
	}

	@Override public void endDocument() {
		inDoc = false;
	}

	@Override public void startElement(String uri,String name, String qName, Attributes atts) {
		if (inDoc==true)
		{
			if (qName.equals("rule"))
				inRule=true;
			else if (qName.equals("scale_max"))
				inScaleMax = true;
			else if (qName.equals("scale_min"))
				inScaleMin = true;
			else if (qName.equals("condition") && inRule)
			{
				inCondition=true;
				for (int count=0; count<atts.getLength(); count++)
				{
					if(atts.getQName(count).equals("k"))
						rule.key = atts.getValue(count);
					else if(atts.getQName(count).equals("v"))
						rule.value = atts.getValue(count);
					else if(atts.getQName(count).equals("b"))
						rule.boolValue = atts.getValue(count);
				}
			}
			else if (qName.equals("line"))
			{
				hadLine = inLine = true;
				for (int count=0; count<atts.getLength(); count++)
				{
					if(atts.getQName(count).equals("width"))
						rule.line.width = Integer.parseInt(atts.getValue(count));
					else if (atts.getQName(count).equals("colour"))
						rule.line.color=convertColor(atts.getValue(count));
					else if (atts.getQName(count).equals("realwidth"))
						rule.line.realWidth=Integer.parseInt(atts.getValue(count));
					else if (atts.getQName(count).equals("dashed"))
						rule.line.dashed=Boolean.parseBoolean(atts.getValue(count));
					else if(atts.getQName(count).equals("priority"))
						rule.line.priority = Integer.parseInt(atts.getValue(count));
				}
			}
			else if (qName.equals("linemod"))
			{
				hadLineMod = inLine = true;
				for (int count=0; count<atts.getLength(); count++)
				{
					if(atts.getQName(count).equals("width"))
					{
						String val = atts.getValue(count);
						if(val.startsWith("+"))
						{
							rule.line.width = Integer.parseInt(val.substring(1));
							rule.line.widthMode = LineElemStyle.WidthMode.OFFSET;
						}
						else if(val.startsWith("-"))
						{
							rule.line.width = Integer.parseInt(val);
							rule.line.widthMode = LineElemStyle.WidthMode.OFFSET;
						}
						else if(val.endsWith("%"))
						{
							rule.line.width = Integer.parseInt(val.substring(0, val.length()-1));
							rule.line.widthMode = LineElemStyle.WidthMode.PERCENT;
						}
						else
							rule.line.width = Integer.parseInt(val);
					}
					else if (atts.getQName(count).equals("colour"))
						rule.line.color=convertColor(atts.getValue(count));
					else if (atts.getQName(count).equals("realwidth"))
						rule.line.realWidth=Integer.parseInt(atts.getValue(count));
					else if (atts.getQName(count).equals("dashed"))
						rule.line.dashed=Boolean.parseBoolean(atts.getValue(count));
					else if(atts.getQName(count).equals("priority"))
						rule.line.priority = Integer.parseInt(atts.getValue(count));
					else if(atts.getQName(count).equals("mode"))
						rule.line.over = !atts.getValue(count).equals("under");
				}
			}
			else if (qName.equals("icon"))
			{
				hadIcon = inIcon = true;
				for (int count=0; count<atts.getLength(); count++)
				{
					if (atts.getQName(count).equals("src"))
						rule.icon.icon = MapPaintStyles.getIcon(atts.getValue(count));
					else if (atts.getQName(count).equals("annotate"))
						rule.icon.annotate = Boolean.parseBoolean (atts.getValue(count));
					else if(atts.getQName(count).equals("priority"))
						rule.icon.priority = Integer.parseInt(atts.getValue(count));
				}
			}
			else if (qName.equals("area"))
			{
				hadArea = inArea = true;
				for (int count=0; count<atts.getLength(); count++)
				{
					if (atts.getQName(count).equals("colour"))
						rule.area.color=convertColor(atts.getValue(count));
					else if(atts.getQName(count).equals("priority"))
						rule.area.priority = Integer.parseInt(atts.getValue(count));
				}
			}
		}
	}

	@Override public void endElement(String uri,String name, String qName)
	{
		if (inRule && qName.equals("rule"))
		{
			if(hadLine)
				styles.add(styleName, rule.key, rule.value, rule.boolValue,
				new LineElemStyle(rule.line, rule.scaleMax, rule.scaleMin));
			if(hadLineMod)
				styles.addModifier(styleName, rule.key, rule.value, rule.boolValue,
				new LineElemStyle(rule.line, rule.scaleMax, rule.scaleMin));
			if(hadIcon)
				styles.add(styleName, rule.key, rule.value, rule.boolValue,
				new IconElemStyle(rule.icon, rule.scaleMax, rule.scaleMin));
			if(hadArea)
				styles.add(styleName, rule.key, rule.value, rule.boolValue,
				new AreaElemStyle(rule.area, rule.scaleMax, rule.scaleMin));
			inRule = false;
			hadLine = hadLineMod = hadIcon = hadArea = false;
			rule.init();
		}
		else if (inCondition && qName.equals("condition"))
			inCondition = false;
		else if (inLine && qName.equals("line"))
			inLine = false;
		else if (inIcon && qName.equals("icon"))
			inIcon = false;
		else if (inArea && qName.equals("area"))
			inArea = false;
		else if (qName.equals("scale_max"))
			inScaleMax = false;
		else if (qName.equals("scale_min"))
			inScaleMin = false;
	}

	@Override public void characters(char ch[], int start, int length)
	{
		if (inScaleMax == true)
			rule.scaleMax = Long.parseLong(new String(ch, start, length));
		else if (inScaleMin == true)
			rule.scaleMin = Long.parseLong(new String(ch, start, length));
	}
}
