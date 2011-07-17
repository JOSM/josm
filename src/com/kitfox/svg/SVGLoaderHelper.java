/*
 * SVGLoaderHelper.java
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
 * Created on February 18, 2004, 5:37 PM
 */

package com.kitfox.svg;

import java.net.*;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class SVGLoaderHelper
{
    /** This is the URL that this document is being loaded from */
//    public final URL docRoot;
//    public final URI docRoot;

    /** This is the universe of all currently loaded SVG documents */
    public final SVGUniverse universe;

    /** This is the diagram which the load process is currently loading */
    public final SVGDiagram diagram;

    public final URI xmlBase;

    /** Creates a new instance of SVGLoaderHelper */
    public SVGLoaderHelper(URI xmlBase, SVGUniverse universe, SVGDiagram diagram)
    {
        /*
        URI docURI = null;
        try
        {
            docURI = new URI(docRoot.toString());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
         */
        
        this.xmlBase = xmlBase;
//        this.docRoot = docURI;
        this.universe = universe;
        this.diagram = diagram;
    }

}
