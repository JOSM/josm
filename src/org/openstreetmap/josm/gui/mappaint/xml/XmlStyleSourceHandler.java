// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.xml;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;
import org.openstreetmap.josm.gui.mappaint.Range;
import org.openstreetmap.josm.tools.ColorHelper;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class XmlStyleSourceHandler extends DefaultHandler
{
    private boolean inDoc, inRule, inCondition, inLine, inLineMod, inIcon, inArea, inScaleMax, inScaleMin;
    private boolean hadLine, hadLineMod, hadIcon, hadArea;
    private RuleElem rule = new RuleElem();

    XmlStyleSource style;

    static class RuleElem {
        XmlCondition cond = new XmlCondition();
        Collection<XmlCondition> conditions;
        double scaleMax;
        double scaleMin;
        LinePrototype line = new LinePrototype();
        LinemodPrototype linemod = new LinemodPrototype();
        AreaPrototype area = new AreaPrototype();
        IconPrototype icon = new IconPrototype();
        public void init() {
            conditions = null;
            scaleMax = Double.POSITIVE_INFINITY;
            scaleMin = 0;
            line.init();
            cond.init();
            linemod.init();
            area.init();
            icon.init();
        }
    }

    public XmlStyleSourceHandler(XmlStyleSource style) {
        this.style = style;
        inDoc=inRule=inCondition=inLine=inIcon=inArea=false;
        rule.init();
    }

    Color convertColor(String colString) {
        int i = colString.indexOf('#');
        Color ret;
        if (i < 0) {
            ret = Main.pref.getColor("mappaint."+style.getPrefName()+"."+colString, Color.red);
        } else if(i == 0) {
            ret = ColorHelper.html2color(colString);
        } else {
            ret = Main.pref.getColor("mappaint."+style.getPrefName()+"."+colString.substring(0,i),
                    ColorHelper.html2color(colString.substring(i)));
        }
        return ret;
    }

    @Override public void startDocument() {
        inDoc = true;
    }

    @Override public void endDocument() {
        inDoc = false;
    }

    private void error(String message) {
        String warning = style.getDisplayString() + " (" + rule.cond.key + "=" + rule.cond.value + "): " + message;
        Main.warn(warning);
        style.logError(new Exception(warning));
    }

    private void startElementLine(String qName, Attributes atts, LinePrototype line) {
        for (int count=0; count<atts.getLength(); count++) {
            if(atts.getQName(count).equals("width")) {
                String val = atts.getValue(count);
                if (! (val.startsWith("+") || val.startsWith("-") || val.endsWith("%"))) {
                    line.setWidth(Integer.parseInt(val));
                }
            } else if (atts.getQName(count).equals("colour")) {
                line.color=convertColor(atts.getValue(count));
            } else if (atts.getQName(count).equals("realwidth")) {
                line.realWidth=Integer.parseInt(atts.getValue(count));
            } else if (atts.getQName(count).equals("dashed")) {
                Float[] dashed;
                try {
                    String[] parts = atts.getValue(count).split(",");
                    dashed = new Float[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        dashed[i] = (float) Integer.parseInt(parts[i]);
                    }
                } catch (NumberFormatException nfe) {
                    boolean isDashed = Boolean.parseBoolean(atts.getValue(count));
                    if(isDashed) {
                        dashed = new Float[]{9f};
                    } else {
                        dashed = null;
                    }
                }
                line.setDashed(dashed == null ? null : Arrays.asList(dashed));
            } else if (atts.getQName(count).equals("dashedcolour")) {
                line.dashedColor=convertColor(atts.getValue(count));
            } else if(atts.getQName(count).equals("priority")) {
                line.priority = Integer.parseInt(atts.getValue(count));
            } else if (!(atts.getQName(count).equals("mode") && line instanceof LinemodPrototype)){
                error("The element \"" + qName + "\" has unknown attribute \"" + atts.getQName(count) + "\"!");
            }
        }
    }

    private void startElementLinemod(String qName, Attributes atts, LinemodPrototype line) {
        startElementLine(qName, atts, line);
        for (int count=0; count<atts.getLength(); count++) {
            if (atts.getQName(count).equals("width")) {
                String val = atts.getValue(count);
                if (val.startsWith("+")) {
                    line.setWidth(Integer.parseInt(val.substring(1)));
                    line.widthMode = LinemodPrototype.WidthMode.OFFSET;
                } else if(val.startsWith("-")) {
                    line.setWidth(Integer.parseInt(val));
                    line.widthMode = LinemodPrototype.WidthMode.OFFSET;
                } else if(val.endsWith("%")) {
                    line.setWidth(Integer.parseInt(val.substring(0, val.length()-1)));
                    line.widthMode = LinemodPrototype.WidthMode.PERCENT;
                } else {
                    line.setWidth(Integer.parseInt(val));
                }
            } else if(atts.getQName(count).equals("mode")) {
                line.over = !atts.getValue(count).equals("under");
            }
        }
    }

    @Override public void startElement(String uri,String name, String qName, Attributes atts) {
        if (inDoc) {
            if (qName.equals("rule")) {
                inRule=true;
            } else if (qName.equals("rules")) {
                if (style.name == null) {
                    style.name = atts.getValue("name");
                }
                if (style.title == null) {
                    style.title = atts.getValue("shortdescription");
                }
                if (style.icon == null) {
                    style.icon = atts.getValue("icon");
                }
            } else if (qName.equals("scale_max")) {
                inScaleMax = true;
            } else if (qName.equals("scale_min")) {
                inScaleMin = true;
            } else if (qName.equals("condition") && inRule) {
                inCondition=true;
                XmlCondition c = rule.cond;
                if (c.key != null) {
                    if(rule.conditions == null) {
                        rule.conditions = new LinkedList<XmlCondition>();
                    }
                    rule.conditions.add(new XmlCondition(rule.cond));
                    c = new XmlCondition();
                    rule.conditions.add(c);
                }
                for (int count=0; count<atts.getLength(); count++) {
                    if (atts.getQName(count).equals("k")) {
                        c.key = atts.getValue(count);
                    } else if (atts.getQName(count).equals("v")) {
                        c.value = atts.getValue(count);
                    } else if(atts.getQName(count).equals("b")) {
                        c.boolValue = atts.getValue(count);
                    } else {
                        error("The element \"" + qName + "\" has unknown attribute \"" + atts.getQName(count) + "\"!");
                    }
                }
                if(c.key == null) {
                    error("The condition has no key!");
                }
            } else if (qName.equals("line")) {
                hadLine = inLine = true;
                startElementLine(qName, atts, rule.line);
            } else if (qName.equals("linemod")) {
                hadLineMod = inLineMod = true;
                startElementLinemod(qName, atts, rule.linemod);
            } else if (qName.equals("icon")) {
                inIcon = true;
                for (int count=0; count<atts.getLength(); count++) {
                    if (atts.getQName(count).equals("src")) {
                        IconReference icon = new IconReference(atts.getValue(count), style);
                        hadIcon = (icon != null);
                        rule.icon.icon = icon;
                    } else if (atts.getQName(count).equals("annotate")) {
                        rule.icon.annotate = Boolean.parseBoolean (atts.getValue(count));
                    } else if(atts.getQName(count).equals("priority")) {
                        rule.icon.priority = Integer.parseInt(atts.getValue(count));
                    } else {
                        error("The element \"" + qName + "\" has unknown attribute \"" + atts.getQName(count) + "\"!");
                    }
                }
            } else if (qName.equals("area")) {
                hadArea = inArea = true;
                for (int count=0; count<atts.getLength(); count++)
                {
                    if (atts.getQName(count).equals("colour")) {
                        rule.area.color=convertColor(atts.getValue(count));
                    } else if (atts.getQName(count).equals("closed")) {
                        rule.area.closed=Boolean.parseBoolean(atts.getValue(count));
                    } else if(atts.getQName(count).equals("priority")) {
                        rule.area.priority = Integer.parseInt(atts.getValue(count));
                    } else {
                        error("The element \"" + qName + "\" has unknown attribute \"" + atts.getQName(count) + "\"!");
                    }
                }
            } else {
                error("The element \"" + qName + "\" is unknown!");
            }
        }
    }

    @Override public void endElement(String uri,String name, String qName)
    {
        if (inRule && qName.equals("rule")) {
            if (hadLine) {
                style.add(rule.cond, rule.conditions,
                        new LinePrototype(rule.line, new Range(rule.scaleMin, rule.scaleMax)));
            }
            if (hadLineMod)
            {
                style.add(rule.cond, rule.conditions,
                        new LinemodPrototype(rule.linemod, new Range(rule.scaleMin, rule.scaleMax)));
            }
            if (hadIcon)
            {
                style.add(rule.cond, rule.conditions,
                        new IconPrototype(rule.icon, new Range(rule.scaleMin, rule.scaleMax)));
            }
            if (hadArea)
            {
                style.add(rule.cond, rule.conditions,
                        new AreaPrototype(rule.area, new Range(rule.scaleMin, rule.scaleMax)));
            }
            inRule = false;
            hadLine = hadLineMod = hadIcon = hadArea = false;
            rule.init();
        } else if (inCondition && qName.equals("condition")) {
            inCondition = false;
        } else if (inLine && qName.equals("line")) {
            inLine = false;
        } else if (inLineMod && qName.equals("linemod")) {
            inLineMod = false;
        } else if (inIcon && qName.equals("icon")) {
            inIcon = false;
        } else if (inArea && qName.equals("area")) {
            inArea = false;
        } else if (qName.equals("scale_max")) {
            inScaleMax = false;
        } else if (qName.equals("scale_min")) {
            inScaleMin = false;
        }
    }

    @Override public void characters(char[] ch, int start, int length) {
        if (inScaleMax) {
            rule.scaleMax = Long.parseLong(new String(ch, start, length));
        } else if (inScaleMin) {
            rule.scaleMin = Long.parseLong(new String(ch, start, length));
        }
    }
}
