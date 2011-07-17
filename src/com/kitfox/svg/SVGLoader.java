/*
 * SVGLoader.java
 *
 *
 *  The Salamander Project - 2D and 3D graphics libraries in Java
 *  Copyright (C) 2004 Mark McKay
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 *  Mark McKay can be contacted at mark@kitfox.com.  Salamander and other
 *  projects can be found at http://www.kitfox.com
 *
 * Created on February 18, 2004, 5:09 PM
 */

package com.kitfox.svg;


import java.util.*;
import java.net.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class SVGLoader extends DefaultHandler
{
    final HashMap nodeClasses = new HashMap();
    //final HashMap attribClasses = new HashMap();
    final LinkedList buildStack = new LinkedList();

    final HashSet ignoreClasses = new HashSet();

    final SVGLoaderHelper helper;

    /**
     * The diagram that represents the base of this SVG document we're loading.
     * Will be augmented to include node indexing info and other useful stuff.
     */
    final SVGDiagram diagram;

//    SVGElement loadRoot;

    //Used to keep track of document elements that are not part of the SVG namespace
    int skipNonSVGTagDepth = 0;
    int indent = 0;

    final boolean verbose;
    
    /** Creates a new instance of SVGLoader */
    public SVGLoader(URI xmlBase, SVGUniverse universe)
    {
        this(xmlBase, universe, false);
    }
    
    public SVGLoader(URI xmlBase, SVGUniverse universe, boolean verbose)
    {
        this.verbose = verbose;
        
        diagram = new SVGDiagram(xmlBase, universe);

        //Compile a list of important builder classes
        nodeClasses.put("a", A.class);
        nodeClasses.put("circle", Circle.class);
        nodeClasses.put("clippath", ClipPath.class);
        nodeClasses.put("defs", Defs.class);
        nodeClasses.put("desc", Desc.class);
        nodeClasses.put("ellipse", Ellipse.class);
        nodeClasses.put("filter", Filter.class);
        nodeClasses.put("font", Font.class);
        nodeClasses.put("font-face", FontFace.class);
        nodeClasses.put("g", Group.class);
        nodeClasses.put("glyph", Glyph.class);
        nodeClasses.put("hkern", Hkern.class);
        nodeClasses.put("image", ImageSVG.class);
        nodeClasses.put("line", Line.class);
        nodeClasses.put("lineargradient", LinearGradient.class);
        nodeClasses.put("marker", Marker.class);
        nodeClasses.put("metadata", Metadata.class);
        nodeClasses.put("missing-glyph", MissingGlyph.class);
        nodeClasses.put("path", Path.class);
        nodeClasses.put("pattern", PatternSVG.class);
        nodeClasses.put("polygon", Polygon.class);
        nodeClasses.put("polyline", Polyline.class);
        nodeClasses.put("radialgradient", RadialGradient.class);
        nodeClasses.put("rect", Rect.class);
        nodeClasses.put("shape", ShapeElement.class);
        nodeClasses.put("stop", Stop.class);
        nodeClasses.put("style", Style.class);
        nodeClasses.put("svg", SVGRoot.class);
        nodeClasses.put("symbol", Symbol.class);
        nodeClasses.put("text", Text.class);
        nodeClasses.put("title", Title.class);
        nodeClasses.put("tspan", Tspan.class);
        nodeClasses.put("use", Use.class);

        ignoreClasses.add("midpointstop");

        //attribClasses.put("clip-path", StyleUrl.class);
        //attribClasses.put("color", StyleColor.class);

        helper = new SVGLoaderHelper(xmlBase, universe, diagram);
    }

    private String printIndent(int indent, String indentStrn)
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < indent; i++)
        {
            sb.append(indentStrn);
        }
        return sb.toString();
    }
    
    public void startDocument() throws SAXException
    {
//        System.err.println("Start doc");

//        buildStack.clear();
    }

    public void endDocument() throws SAXException
    {
//        System.err.println("End doc");
    }

    public void startElement(String namespaceURI, String sName, String qName, Attributes attrs) throws SAXException
    {
        if (verbose)
        {
            System.err.println(printIndent(indent, " ") + "Starting parse of tag " + sName+ ": " + namespaceURI);
        }
        indent++;
        
        if (skipNonSVGTagDepth != 0 || (!namespaceURI.equals("") && !namespaceURI.equals(SVGElement.SVG_NS)))
        {
            skipNonSVGTagDepth++;
            return;
        }
        
        sName = sName.toLowerCase();

//javax.swing.JOptionPane.showMessageDialog(null, sName);

        Object obj = nodeClasses.get(sName);
        if (obj == null)
        {
            if (!ignoreClasses.contains(sName))
            {
                if (verbose)
                {
                    System.err.println("SVGLoader: Could not identify tag '" + sName + "'");
                }
            }
            return;
        }

//Debug info tag depth
//for (int i = 0; i < buildStack.size(); i++) System.err.print(" ");
//System.err.println("+" + sName);

        try {
            Class cls = (Class)obj;
            SVGElement svgEle = (SVGElement)cls.newInstance();

            SVGElement parent = null;
            if (buildStack.size() != 0) parent = (SVGElement)buildStack.getLast();
            svgEle.loaderStartElement(helper, attrs, parent);

            buildStack.addLast(svgEle);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new SAXException(e);
        }

    }

    public void endElement(String namespaceURI, String sName, String qName)
        throws SAXException
    {
        indent--;
        if (verbose)
        {
            System.err.println(printIndent(indent, " ") + "Ending parse of tag " + sName+ ": " + namespaceURI);
        }
        
        if (skipNonSVGTagDepth != 0)
        {
            skipNonSVGTagDepth--;
            return;
        }
        
        sName = sName.toLowerCase();

        Object obj = nodeClasses.get(sName);
        if (obj == null) return;

//Debug info tag depth
//for (int i = 0; i < buildStack.size(); i++) System.err.print(" ");
//System.err.println("-" + sName);

        try {
            SVGElement svgEle = (SVGElement)buildStack.removeLast();

            svgEle.loaderEndElement(helper);

            SVGElement parent = null;
            if (buildStack.size() != 0) parent = (SVGElement)buildStack.getLast();
            //else loadRoot = (SVGElement)svgEle;

            if (parent != null) parent.loaderAddChild(helper, svgEle);
            else diagram.setRoot((SVGRoot)svgEle);

        }
        catch (Exception e)
        {
e.printStackTrace();
            throw new SAXException(e);
        }
    }

    public void characters(char buf[], int offset, int len)
        throws SAXException
    {
        if (skipNonSVGTagDepth != 0)
        {
            return;
        }

        if (buildStack.size() != 0)
        {
            SVGElement parent = (SVGElement)buildStack.getLast();
            String s = new String(buf, offset, len);
            parent.loaderAddText(helper, s);
        }
    }

    public void processingInstruction(String target, String data)
        throws SAXException
    {
        //Check for external style sheet
    }
    
//    public SVGElement getLoadRoot() { return loadRoot; }
    public SVGDiagram getLoadedDiagram() { return diagram; }
}
