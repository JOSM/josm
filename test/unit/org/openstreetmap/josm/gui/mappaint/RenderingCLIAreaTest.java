// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.CustomMatchers.isFP;

import java.util.ArrayList;
import java.util.Collection;

import org.CustomMatchers;
import org.CustomMatchers.ErrorMode;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests the method {@link RenderingCLI#determineRenderingArea(org.openstreetmap.josm.data.osm.DataSet)}.
 */
@RunWith(Parameterized.class)
public class RenderingCLIAreaTest {
    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();

    @Parameterized.Parameters
    public static Collection<Object[]> runs() {
        Collection<Object[]> runs = new ArrayList<>();

        final double SCALE_Z18 = 0.5971642834779395;
        final double SCALE_Z19 = 0.29858214173896974;

        // area of imagery tile z=19/x=292949/y=174587
        Bounds bTile = new Bounds(51.40091918770498, 21.152114868164077, 51.4013475612123, 21.15280151367189, false);

        // 0
        runs.add(new Object[] {"--zoom 19 --bounds " + param(bTile),
                CoreMatchers.is(SCALE_Z19),
                CoreMatchers.is(bTile)});

        Bounds bFeldberg = new Bounds(53.33, 13.43, 53.333, 13.44); // rectangular area in the city Feldberg
        double scaleFeldberg4000 = 1.7722056827012918;

        // 1
        runs.add(new Object[] {"--scale 4000 --bounds " + param(bFeldberg),
                CoreMatchers.is(scaleFeldberg4000),
                CoreMatchers.is(bFeldberg)});

        // 2
        runs.add(new Object[] {"--width-px 628 --bounds " + param(bFeldberg),
                isFP(scaleFeldberg4000, ErrorMode.RELATIVE, 1e-3),
                CoreMatchers.is(bFeldberg)});

        // 3
        runs.add(new Object[] {"--height-px 316 --bounds " + param(bFeldberg),
                isFP(scaleFeldberg4000, ErrorMode.RELATIVE, 1.5e-3),
                CoreMatchers.is(bFeldberg)});

        LatLon aFeldberg = bFeldberg.getMin();
        LatLon aFeldberg200mRight = new LatLon(aFeldberg.lat(), 13.433008399004041);
        LatLon aFeldberg150mUp = new LatLon(53.33134745249311, aFeldberg.lon());
        Assert.assertThat(aFeldberg.greatCircleDistance(aFeldberg200mRight), isFP(200.0, 0.01));
        Assert.assertThat(aFeldberg.greatCircleDistance(aFeldberg150mUp), isFP(150.0, 0.01));

        Bounds bFeldberg200x150m = new Bounds(
                bFeldberg.getMin(), new LatLon(aFeldberg150mUp.lat(), aFeldberg200mRight.lon()));

        // 4
        runs.add(new Object[] {"--width-m 200 --height-m 150 -z 18 --anchor " + param(aFeldberg),
                CoreMatchers.is(SCALE_Z18),
                CustomMatchers.is(bFeldberg200x150m, 1e-7)});
        // -> image size 561x421 px

        // 5
        runs.add(new Object[] {"--width-m 200 --height-m 150 --scale 4000 --anchor " + param(aFeldberg),
                isFP(scaleFeldberg4000, ErrorMode.RELATIVE, 1e-3),
                CustomMatchers.is(bFeldberg200x150m, 1e-7)});
        // -> image size 189x142 px

        // 6
        runs.add(new Object[] {"--width-px 561 --height-px 421 -z 18 --anchor " + param(aFeldberg),
                CoreMatchers.is(SCALE_Z18),
                CustomMatchers.is(bFeldberg200x150m, 1e-5)});

        // 7
        runs.add(new Object[] {"--width-px 189 --height-px 142 --scale 4000 --anchor " + param(aFeldberg),
                isFP(scaleFeldberg4000, ErrorMode.RELATIVE, 1e-3),
                CustomMatchers.is(bFeldberg200x150m, 1e-5)});

        // 8
        runs.add(new Object[] {"--width-px 561 --height-m 150 -z 18 --anchor " + param(aFeldberg),
                CoreMatchers.is(SCALE_Z18),
                CustomMatchers.is(bFeldberg200x150m, 1e-5)});

        // 9
        runs.add(new Object[] {"--width-px 189 --height-m 150 --scale 4000 --anchor " + param(aFeldberg),
                isFP(scaleFeldberg4000, ErrorMode.RELATIVE, 1e-3),
                CustomMatchers.is(bFeldberg200x150m, 1e-5)});

        // 10
        runs.add(new Object[] {"--width-m 200 --height-px 421 -z 18 --anchor " + param(aFeldberg),
                CoreMatchers.is(SCALE_Z18),
                CustomMatchers.is(bFeldberg200x150m, 1e-5)});

        // 11
        runs.add(new Object[] {"--width-m 200 --height-px 142 --scale 4000 --anchor " + param(aFeldberg),
                isFP(scaleFeldberg4000, ErrorMode.RELATIVE, 1e-3),
                CustomMatchers.is(bFeldberg200x150m, 1e-5)});

        // 12
        runs.add(new Object[] {"--width-m 200 --height-m 150 --width-px 561 --anchor " + param(aFeldberg),
                isFP(SCALE_Z18, ErrorMode.RELATIVE, 1e-3),
                CustomMatchers.is(bFeldberg200x150m, 1e-5)});

        // 13
        runs.add(new Object[] {"--width-m 200 --height-m 150 --height-px 421 --anchor " + param(aFeldberg),
                isFP(SCALE_Z18, ErrorMode.RELATIVE, 1e-3),
                CustomMatchers.is(bFeldberg200x150m, 1e-5)});

        // 14
        runs.add(new Object[] {"--width-px 561 --height-px 421 --width-m 200 --anchor " + param(aFeldberg),
                isFP(SCALE_Z18, ErrorMode.RELATIVE, 1e-3),
                CustomMatchers.is(bFeldberg200x150m, 1e-5)});

        // 15
        runs.add(new Object[] {"--width-px 561 --height-px 421 --height-m 150 --anchor " + param(aFeldberg),
                isFP(SCALE_Z18, ErrorMode.RELATIVE, 1e-3),
                CustomMatchers.is(bFeldberg200x150m, 1e-5)});

        return runs;
    }

    private static String param(Bounds b) {
        return b.getMinLon() + "," + b.getMinLat() + "," + b.getMaxLon() + "," + b.getMaxLat();
    }

    private static String param(LatLon ll) {
        return ll.lon() + "," + ll.lat();
    }

    private final String[] args;
    private final Matcher<Double> scaleMatcher;
    private final Matcher<Bounds> boundsMatcher;

    public RenderingCLIAreaTest(String args, Matcher<Double> scaleMatcher, Matcher<Bounds> boundsMatcher) {
        this.args = args.split("\\s+");
        this.scaleMatcher = scaleMatcher;
        this.boundsMatcher = boundsMatcher;
    }

    @Test
    public void testDetermineRenderingArea() {
        RenderingCLI cli = new RenderingCLI();
        cli.parseArguments(args);
        cli.initialize();
        RenderingCLI.RenderingArea ra = cli.determineRenderingArea(null);
        Assert.assertThat(ra.scale, scaleMatcher);
        Assert.assertThat(ra.bounds, boundsMatcher);
    }
}
