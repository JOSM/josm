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

public class XmlStyleSourceHandler extends DefaultHandler {
    private boolean inDoc, inRule, inCondition, inLine, inLineMod, inIcon, inArea, inScaleMax, inScaleMin;
    private boolean hadLine, hadLineMod, hadIcon, hadArea;
    private RuleElem rule = new RuleElem();

    private XmlStyleSource style;

    static class RuleElem {
        private XmlCondition cond = new XmlCondition();
        private Collection<XmlCondition> conditions;
        private double scaleMax;
        private double scaleMin;
        private LinePrototype line = new LinePrototype();
        private LinemodPrototype linemod = new LinemodPrototype();
        private AreaPrototype area = new AreaPrototype();
        private IconPrototype icon = new IconPrototype();
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
        inDoc = inRule = inCondition = inLine = inIcon = inArea = false;
        rule.init();
    }

    private Color convertColor(String colString) {
        int i = colString.indexOf('#');
        Color ret;
        if (i < 0) {
            ret = Main.pref.getColor("mappaint."+style.getPrefName()+'.'+colString, Color.red);
        } else if (i == 0) {
            ret = ColorHelper.html2color(colString);
        } else {
            ret = Main.pref.getColor("mappaint."+style.getPrefName()+'.'+colString.substring(0, i),
                    ColorHelper.html2color(colString.substring(i)));
        }
        return ret;
    }

    @Override
    public void startDocument() {
        inDoc = true;
    }

    @Override
    public void endDocument() {
        inDoc = false;
    }

    private void error(String message) {
        String warning = style.getDisplayString() + " (" + rule.cond.key + '=' + rule.cond.value + "): " + message;
        Main.warn(warning);
        style.logError(new Exception(warning));
    }

    private void startElementLine(String qName, Attributes atts, LinePrototype line) {
        for (int count = 0; count < atts.getLength(); count++) {
            switch (atts.getQName(count)) {
            case "width":
                String val = atts.getValue(count);
                if (!(val.startsWith("+") || val.startsWith("-") || val.endsWith("%"))) {
                    line.setWidth(Integer.parseInt(val));
                }
                break;
            case "colour":
                line.color = convertColor(atts.getValue(count));
                break;
            case "realwidth":
                line.realWidth = Integer.valueOf(atts.getValue(count));
                break;
            case "dashed":
                Float[] dashed;
                try {
                    String[] parts = atts.getValue(count).split(",");
                    dashed = new Float[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        dashed[i] = (float) Integer.parseInt(parts[i]);
                    }
                } catch (NumberFormatException nfe) {
                    boolean isDashed = Boolean.parseBoolean(atts.getValue(count));
                    if (isDashed) {
                        dashed = new Float[]{9f};
                    } else {
                        dashed = null;
                    }
                }
                line.setDashed(dashed == null ? null : Arrays.asList(dashed));
                break;
            case "dashedcolour":
                line.dashedColor = convertColor(atts.getValue(count));
                break;
            case "priority":
                line.priority = Integer.parseInt(atts.getValue(count));
                break;
            case "mode":
                if (line instanceof LinemodPrototype)
                    break;
            default:
                error("The element \"" + qName + "\" has unknown attribute \"" + atts.getQName(count) + "\"!");
            }
        }
    }

    private void startElementLinemod(String qName, Attributes atts, LinemodPrototype line) {
        startElementLine(qName, atts, line);
        for (int count = 0; count < atts.getLength(); count++) {
            switch (atts.getQName(count)) {
            case "width":
                String val = atts.getValue(count);
                if (val.startsWith("+")) {
                    line.setWidth(Integer.parseInt(val.substring(1)));
                    line.widthMode = LinemodPrototype.WidthMode.OFFSET;
                } else if (val.startsWith("-")) {
                    line.setWidth(Integer.parseInt(val));
                    line.widthMode = LinemodPrototype.WidthMode.OFFSET;
                } else if (val.endsWith("%")) {
                    line.setWidth(Integer.parseInt(val.substring(0, val.length()-1)));
                    line.widthMode = LinemodPrototype.WidthMode.PERCENT;
                } else {
                    line.setWidth(Integer.parseInt(val));
                }
                break;
            case "mode":
                line.over = !"under".equals(atts.getValue(count));
                break;
            }
        }
    }

    @Override
    public void startElement(String uri, String name, String qName, Attributes atts) {
        if (inDoc) {
            switch(qName) {
            case "rule":
                inRule = true;
                break;
            case "rules":
                if (style.name == null) {
                    style.name = atts.getValue("name");
                }
                if (style.title == null) {
                    style.title = atts.getValue("shortdescription");
                }
                if (style.icon == null) {
                    style.icon = atts.getValue("icon");
                }
                break;
            case "scale_max":
                inScaleMax = true;
                break;
            case "scale_min":
                inScaleMin = true;
                break;
            case "condition":
                if (inRule) {
                    inCondition = true;
                    XmlCondition c = rule.cond;
                    if (c.key != null) {
                        if (rule.conditions == null) {
                            rule.conditions = new LinkedList<>();
                        }
                        rule.conditions.add(new XmlCondition(rule.cond));
                        c = new XmlCondition();
                        rule.conditions.add(c);
                    }
                    for (int count = 0; count < atts.getLength(); count++) {
                        switch (atts.getQName(count)) {
                        case "k":
                            c.key = atts.getValue(count);
                            break;
                        case "v":
                            c.value = atts.getValue(count);
                            break;
                        case "b":
                            c.boolValue = atts.getValue(count);
                            break;
                        default:
                            error("The element \"" + qName + "\" has unknown attribute \"" + atts.getQName(count) + "\"!");
                        }
                    }
                    if (c.key == null) {
                        error("The condition has no key!");
                    }
                }
                break;
            case "line":
                hadLine = inLine = true;
                startElementLine(qName, atts, rule.line);
                break;
            case "linemod":
                hadLineMod = inLineMod = true;
                startElementLinemod(qName, atts, rule.linemod);
                break;
            case "icon":
                inIcon = true;
                for (int count = 0; count < atts.getLength(); count++) {
                    switch (atts.getQName(count)) {
                    case "src":
                        IconReference icon = new IconReference(atts.getValue(count), style);
                        hadIcon = (icon != null);
                        rule.icon.icon = icon;
                        break;
                    case "annotate":
                        rule.icon.annotate = Boolean.valueOf(atts.getValue(count));
                        break;
                    case "priority":
                        rule.icon.priority = Integer.parseInt(atts.getValue(count));
                        break;
                    default:
                        error("The element \"" + qName + "\" has unknown attribute \"" + atts.getQName(count) + "\"!");
                    }
                }
                break;
            case "area":
                hadArea = inArea = true;
                for (int count = 0; count < atts.getLength(); count++) {
                    switch (atts.getQName(count)) {
                    case "colour":
                        rule.area.color = convertColor(atts.getValue(count));
                        break;
                    case "closed":
                        rule.area.closed = Boolean.parseBoolean(atts.getValue(count));
                        break;
                    case "priority":
                        rule.area.priority = Integer.parseInt(atts.getValue(count));
                        break;
                    default:
                        error("The element \"" + qName + "\" has unknown attribute \"" + atts.getQName(count) + "\"!");
                    }
                }
                break;
            default:
                error("The element \"" + qName + "\" is unknown!");
            }
        }
    }

    @Override
    public void endElement(String uri, String name, String qName) {
        if (inRule && "rule".equals(qName)) {
            if (hadLine) {
                style.add(rule.cond, rule.conditions,
                        new LinePrototype(rule.line, new Range(rule.scaleMin, rule.scaleMax)));
            }
            if (hadLineMod) {
                style.add(rule.cond, rule.conditions,
                        new LinemodPrototype(rule.linemod, new Range(rule.scaleMin, rule.scaleMax)));
            }
            if (hadIcon) {
                style.add(rule.cond, rule.conditions,
                        new IconPrototype(rule.icon, new Range(rule.scaleMin, rule.scaleMax)));
            }
            if (hadArea) {
                style.add(rule.cond, rule.conditions,
                        new AreaPrototype(rule.area, new Range(rule.scaleMin, rule.scaleMax)));
            }
            inRule = false;
            hadLine = hadLineMod = hadIcon = hadArea = false;
            rule.init();
        } else if (inCondition && "condition".equals(qName)) {
            inCondition = false;
        } else if (inLine && "line".equals(qName)) {
            inLine = false;
        } else if (inLineMod && "linemod".equals(qName)) {
            inLineMod = false;
        } else if (inIcon && "icon".equals(qName)) {
            inIcon = false;
        } else if (inArea && "area".equals(qName)) {
            inArea = false;
        } else if ("scale_max".equals(qName)) {
            inScaleMax = false;
        } else if ("scale_min".equals(qName)) {
            inScaleMin = false;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (inScaleMax) {
            rule.scaleMax = Long.parseLong(new String(ch, start, length));
        } else if (inScaleMin) {
            rule.scaleMin = Long.parseLong(new String(ch, start, length));
        }
    }
}
