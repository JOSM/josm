// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.util.Bag;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Check cyclic ways for errors
 *
 * @author jrreid
 */
public class WronglyOrderedWays extends Test  {
    protected static int WRONGLY_ORDERED_COAST = 1001;
    protected static int WRONGLY_ORDERED_WATER = 1002;
    protected static int WRONGLY_ORDERED_LAND  = 1003;

    /** The already detected errors */
    Bag<Way, Way> _errorWays;

    /**
     * Constructor
     */
    public WronglyOrderedWays()
    {
        super(tr("Wrongly Ordered Ways."),
              tr("This test checks the direction of water, land and coastline ways."));
    }

    @Override
    public void startTest(ProgressMonitor monitor)
    {
        super.startTest(monitor);
        _errorWays = new Bag<Way, Way>();
    }

    @Override
    public void endTest()
    {
        _errorWays = null;
        super.endTest();
    }

    @Override
    public void visit(Way w)
    {
        String errortype = "";
        int type;

        if( !w.isUsable() )
            return;
        if (w.getNodesCount() <= 0)
            return;

        String natural = w.get("natural");
        if( natural == null)
            return;

        if( natural.equals("coastline") )
        {
            errortype = tr("Reversed coastline: land not on left side");
            type= WRONGLY_ORDERED_COAST;
        }
        else if(natural.equals("water") )
        {
            errortype = tr("Reversed water: land not on left side");
            type= WRONGLY_ORDERED_WATER;
        }
        else if( natural.equals("land") )
        {
            errortype = tr("Reversed land: land not on left side");
            type= WRONGLY_ORDERED_LAND;
        }
        else
            return;


        /**
         * Test the directionality of the way
         *
         * Assuming a closed non-looping way, compute twice the area
         * of the polygon using the formula 2*a = sum (Xn * Yn+1 - Xn+1 * Yn)
         * If the area is negative the way is ordered in a clockwise direction
         *
         */

        if(w.getNode(0) == w.getNode(w.getNodesCount()-1))
        {
            double area2 = 0;

            for (int node = 1; node < w.getNodesCount(); node++)
            {
                area2 += (w.getNode(node-1).getCoor().lon() * w.getNode(node).getCoor().lat()
                - w.getNode(node).getCoor().lon() * w.getNode(node-1).getCoor().lat());
            }

            if(((natural.equals("coastline") || natural.equals("land")) && area2 < 0.)
            || (natural.equals("water") && area2 > 0.))
            {
                List<OsmPrimitive> primitives = new ArrayList<OsmPrimitive>();
                primitives.add(w);
                errors.add( new TestError(this, Severity.OTHER, errortype, type, primitives) );
                _errorWays.add(w,w);
            }
        }
    }
}
