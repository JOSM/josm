package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import java.awt.Toolkit;
import java.io.File;
import java.net.URL;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.tools.ColorHelper;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class ElemStyleHandler extends DefaultHandler
{
	boolean inDoc, inRule, inCondition, inElemStyle, inLine, inIcon, inArea, inScaleMax, inScaleMin;
	String curKey = null;
	String curValue = null;
	int curLineWidth = -1;
	int curLineRealWidth = 0;
	boolean curLineDashed = false;
	Color curLineColour = null;
	Color curAreaColour = null;
	ImageIcon curIcon = null;
	boolean curIconAnnotate = true;
	long curScaleMax = 1000000000;
	long curScaleMin = 0;

	public ElemStyleHandler() {
		inDoc=inRule=inCondition=inElemStyle=inLine=inIcon=inArea=false;
	}

	/*
    ElemStyles getElemStyles()
    {
        return styles;
    }
	*/

	@Override public void startDocument() {
		inDoc = true;
	}

	@Override public void endDocument() {
		inDoc = false;
	}

	@Override public void startElement(String uri,String name, String qName, 
			Attributes atts) {
		if (inDoc==true)	{
			if (qName.equals("rule")) {
				inRule=true;
			}
			else if (qName.equals("condition") && inRule) {
				inCondition=true;
				for (int count=0; count<atts.getLength(); count++) {
					if(atts.getQName(count).equals("k"))
						curKey = atts.getValue(count);        
					else if(atts.getQName(count).equals("v"))
						curValue = atts.getValue(count);        
				}
			} else if (qName.equals("line")) {
				inLine = true;
				for (int count=0; count<atts.getLength(); count++) {
					if(atts.getQName(count).equals("width"))
						curLineWidth = Integer.parseInt(atts.getValue(count));
					else if (atts.getQName(count).equals("colour"))
						curLineColour=ColorHelper.html2color(atts.getValue(count));
					else if (atts.getQName(count).equals("realwidth"))
						curLineRealWidth=Integer.parseInt(atts.getValue(count));
					else if (atts.getQName(count).equals("dashed"))
						curLineDashed=Boolean.parseBoolean(atts.getValue(count));
				}
			} else if (qName.equals("scale_max")) {
				inScaleMax = true;
			} else if (qName.equals("scale_min")) {
				inScaleMin = true;
			} else if (qName.equals("icon")) {
				inIcon = true;
				for (int count=0; count<atts.getLength(); count++) {
					if (atts.getQName(count).equals("src")) {
						String imageFile = MapPaintStyles.getStyleDir()+"icons/"+atts.getValue(count); 
						File f = new File(imageFile);
						if (f.exists()) {
							//open icon from user directory
							curIcon = new ImageIcon(imageFile);
						} else {
							try {
								URL path = getClass().getResource("/styles/standard/icons/"+atts.getValue(count));
								if (path == null) {
									/* icon not found, using default */
									System.out.println("Mappaint: Icon " + atts.getValue(count) + " not found, using default icon");
									path = getClass().getResource("/styles/standard/icons/misc/no_icon.png");
									curIcon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(path));
								} else {
									curIcon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(path));
								}
							}
							catch (Exception e){
								URL path = getClass().getResource("/styles/standard/icons/amenity.png");
								curIcon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(path));
							}
						}
					} else if (atts.getQName(count).equals("annotate")) {
						curIconAnnotate = Boolean.parseBoolean (atts.getValue(count));
					}
				}
			}
			else if (qName.equals("area"))
			{
				inArea = true;
				for (int count=0; count<atts.getLength(); count++)
				{
					if (atts.getQName(count).equals("colour"))
						curAreaColour=ColorHelper.html2color(atts.getValue(count));
				}
			}
		}
	}

	@Override public void endElement(String uri,String name, String qName)
	{
		if (inRule && qName.equals("rule")) {
			ElemStyle newStyle;
			inRule = false;
			if (curLineWidth != -1) {
				newStyle = new LineElemStyle(curLineWidth, curLineRealWidth, curLineColour, 
						curLineDashed, curScaleMax, curScaleMin);
				MapPaintStyles.add(curKey, curValue, newStyle);
				curLineWidth	= -1;
				curLineRealWidth= 0;
				curLineDashed   = false;
				curLineColour 	= null;
			}
			
			if (curIcon != null) {
				newStyle = new IconElemStyle(curIcon, curIconAnnotate, curScaleMax, curScaleMin);
				MapPaintStyles.add(curKey, curValue, newStyle);
				curIcon 		= null;
				curIconAnnotate = true;
			}
			if (curAreaColour != null) {
				newStyle = new AreaElemStyle (curAreaColour, curScaleMax, curScaleMin);
				MapPaintStyles.add(curKey, curValue, newStyle);
				curAreaColour 	= null;
			}
			curScaleMax = 1000000000;
			curScaleMin = 0;

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

	@Override public void characters(char ch[], int start, int length) {
		if (inScaleMax == true) {
			String content = new String(ch, start, length);
			curScaleMax = Long.parseLong(content);
		}
		if (inScaleMin == true) {
			String content = new String(ch, start, length);
			curScaleMin = Long.parseLong(content);
		}
	}
}




