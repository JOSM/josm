// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link NoteLayer} class.
 */
public class NoteLayerTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/13208">#13208</a>.
     */
    @Test
    public void testTicket13208() {
        assertEquals("0 notes", new NoteLayer().getToolTipText());
    }

    /**
     * Unit test of {@link NoteLayer#insertLineBreaks}.
     */
    @Test
    public void testInsertLineBreaks() {
        // empty string
        assertEquals("", NoteLayer.insertLineBreaks(""));
        // CHECKSTYLE.OFF: LineLength
        // https://www.openstreetmap.org/note/278197: long text with periods
        assertEquals("<html>Note 278197<hr/>Klumbumbus on Nov 29, 2014:<br>Hier soll eine zusätzliche Rechtsabbiegerspur entstehen.<br>Müsste dann bei Fertigstellung nochmal geprüft und die lanes angepoasst werden.<br><a href=\"http://www.sachsen-fernsehen.de/Aktuell/Chemnitz/Artikel/1370077/Bauarbeiten-auf-Chemnitzer-Hartmannstrasse-beginnen/\">http://www.sachsen-fernsehen.de/Aktuell/Chemnitz/Artikel/1370077/Bauarbeiten-auf-Chemnitzer-Hartmannstrasse-beginnen/</a><hr/>Saxonyking on May 4, 2015:<br>Danke<br>eingetragen</html>",
                NoteLayer.insertLineBreaks(
                     "<html>Note 278197<hr/>Klumbumbus on Nov 29, 2014:<br>Hier soll eine zusätzliche Rechtsabbiegerspur entstehen. Müsste dann bei Fertigstellung nochmal geprüft und die lanes angepoasst werden. <a href=\"http://www.sachsen-fernsehen.de/Aktuell/Chemnitz/Artikel/1370077/Bauarbeiten-auf-Chemnitzer-Hartmannstrasse-beginnen/\">http://www.sachsen-fernsehen.de/Aktuell/Chemnitz/Artikel/1370077/Bauarbeiten-auf-Chemnitzer-Hartmannstrasse-beginnen/</a><hr/>Saxonyking on May 4, 2015:<br>Danke<br>eingetragen</html>"));
        // https://www.openstreetmap.org/note/1196942: long text without periods (question marks instead)
        assertEquals("<html>Note 1196942<hr/>Mateusz Konieczny on Nov 6, 2017:<br><a href=\"https://www.openstreetmap.org/way/51661050#map=17/50.9326393/14.0821931layers=N\">https://www.openstreetmap.org/way/51661050#map=17/50.9326393/14.0821931layers=N</a> Why this is not tagged as highway=steps?<br>What is the meaning of steps=yes here?<br>See <a href=\"http://overpass-turbo.eu/s/sLv\">http://overpass-turbo.eu/s/sLv</a> for more cases (I considered armchair mapping it to highway=steps but I think that verification from local mappers is preferable)</html>",
                NoteLayer.insertLineBreaks(
                     "<html>Note 1196942<hr/>Mateusz Konieczny on Nov 6, 2017:<br><a href=\"https://www.openstreetmap.org/way/51661050#map=17/50.9326393/14.0821931layers=N\">https://www.openstreetmap.org/way/51661050#map=17/50.9326393/14.0821931layers=N</a> Why this is not tagged as highway=steps? What is the meaning of steps=yes here? See <a href=\"http://overpass-turbo.eu/s/sLv\">http://overpass-turbo.eu/s/sLv</a> for more cases (I considered armchair mapping it to highway=steps but I think that verification from local mappers is preferable)</html>"));
        // https://www.openstreetmap.org/note/1029364: several spaces between sentence mark and next sentence
        assertEquals("<html>Note 1029364<hr/>SaGm on Jun 13, 2017:<br>HOW CAN I CONNECT THIS ROUD TO THE MAIN ROUDS?<br><br>if i make a GPX trail its SKIP this way.<br><hr/>dsh4 on Jun 14, 2017:<br>To connect roads, they need to have a node in common.<br>This is already the case here: the footway has nodes in common with Idan and with Katsenelson.<br>Thus, in that respect, the map is correct.<br>If your router doesn't use the new footway, it would be better to ask about that elsewhere, see <a href=\"http://wiki.openstreetmap.org/wiki/Contact_channels\">http://wiki.openstreetmap.org/wiki/Contact_channels</a> .<br>(Notes are only really suited for discussing errors in the map, and there isn't an error in the map here.)<br><br>The footway is also a member of two turn restrictions that forbid turning from Katsenelson onto the footway.<br>Presumably pedestrians are allowed to enter the footway from Katsenelson, so those restrictions are wrong and should be deleted.<br><br>Cheers!<hr/>SaGm on Jun 14, 2017:<br>Thanks Dsh4<br>How can i delet it?<br><hr/>dsh4 on Jun 14, 2017:<br>That depends on what editor program you use.<br>In JOSM, for example, you would click on the footway, then double-click on the turn restriction in the &quot;Tags / Memberships&quot; window on the right, and then click the trash can button in the pop-up window to delete the entire relation (not just the membership of the footway in the relation).<br>A web search ought to find instructions for your favourite editor as well.</html>",
                NoteLayer.insertLineBreaks(
                     "<html>Note 1029364<hr/>SaGm on Jun 13, 2017:<br>HOW CAN I CONNECT THIS ROUD TO THE MAIN ROUDS?<br><br>if i make a GPX trail its SKIP this way.<br><hr/>dsh4 on Jun 14, 2017:<br>To connect roads, they need to have a node in common.  This is already the case here: the footway has nodes in common with Idan and with Katsenelson.  Thus, in that respect, the map is correct.  If your router doesn't use the new footway, it would be better to ask about that elsewhere, see <a href=\"http://wiki.openstreetmap.org/wiki/Contact_channels\">http://wiki.openstreetmap.org/wiki/Contact_channels</a> .  (Notes are only really suited for discussing errors in the map, and there isn't an error in the map here.)<br><br>The footway is also a member of two turn restrictions that forbid turning from Katsenelson onto the footway.  Presumably pedestrians are allowed to enter the footway from Katsenelson, so those restrictions are wrong and should be deleted.<br><br>Cheers!<hr/>SaGm on Jun 14, 2017:<br>Thanks Dsh4<br>How can i delet it?<br><hr/>dsh4 on Jun 14, 2017:<br>That depends on what editor program you use.  In JOSM, for example, you would click on the footway, then double-click on the turn restriction in the &quot;Tags / Memberships&quot; window on the right, and then click the trash can button in the pop-up window to delete the entire relation (not just the membership of the footway in the relation).  A web search ought to find instructions for your favourite editor as well.</html>"));
        // https://www.openstreetmap.org/note/230617: ideographic full stops
        assertEquals("<html>Note 230617<hr/>deckkun on Aug 27, 2014:<br>筑紫が丘への抜け道？<hr/>Rakkka on Jul 20, 2017:<br>地図を修正するためのメモではないように見えますが、解決してもよろしいでしょうか？<hr/>&lt;anonymous&gt; on Jul 21, 2017:<br>そうです。<br>一部未舗装があり、急な坂があります。<br>細い部分もあるので自動車は無理です。<br><hr/>Rakkka on Jul 21, 2017:<br>この地点から北西へ伸びている道路のことですよね。<br>自動車道になっているので、自動車が通れないなら直さないといけませんが、通れない部分がわからないので、メモは残しておきます。<br><hr/>&lt;anonymous&gt; on Jul 21, 2017:<br>そうです。<br>マーク地点からですと白百合学園までは行けますが、そこから筑紫が丘六丁目7に出る付近は資材置き場があり、狭く、もしかしたら一部私有地内の通行になるのかも。<br>軽自動車なら行ける幅だと思いますが。<br>ストリートマップでイメージは掴めると思います。<br></html>",
                NoteLayer.insertLineBreaks(
                     "<html>Note 230617<hr/>deckkun on Aug 27, 2014:<br>筑紫が丘への抜け道？<hr/>Rakkka on Jul 20, 2017:<br>地図を修正するためのメモではないように見えますが、解決してもよろしいでしょうか？<hr/>&lt;anonymous&gt; on Jul 21, 2017:<br>そうです。一部未舗装があり、急な坂があります。細い部分もあるので自動車は無理です。<hr/>Rakkka on Jul 21, 2017:<br>この地点から北西へ伸びている道路のことですよね。自動車道になっているので、自動車が通れないなら直さないといけませんが、通れない部分がわからないので、メモは残しておきます。<hr/>&lt;anonymous&gt; on Jul 21, 2017:<br>そうです。マーク地点からですと白百合学園までは行けますが、そこから筑紫が丘六丁目7に出る付近は資材置き場があり、狭く、もしかしたら一部私有地内の通行になるのかも。軽自動車なら行ける幅だと思いますが。ストリートマップでイメージは掴めると思います。</html>"));
        // CHECKSTYLE.ON: LineLength
    }

    /**
     * Unit test of {@link NoteLayer#replaceLinks}.
     */
    @Test
    public void testReplaceLinks() {
        // empty string
        assertEquals("", NoteLayer.replaceLinks(""));
        // no link
        assertEquals("no http link", NoteLayer.replaceLinks("no http link"));
        // just one link
        assertEquals("<a href=\"https://www.example.com/test\">https://www.example.com/\u200btest</a>",
                NoteLayer.replaceLinks("https://www.example.com/test"));
        // link with dot
        assertEquals("<a href=\"https://www.example.com\">https://www.example.com</a>.",
                NoteLayer.replaceLinks("https://www.example.com."));
        // CHECKSTYLE.OFF: LineLength
        // text with several links (with and without slash)
        assertEquals("foo <a href=\"https://foo.example.com/test\">https://foo.example.com/\u200btest</a> bar <a href=\"https://bar.example.com\">https://bar.example.com</a> baz",
                NoteLayer.replaceLinks("foo https://foo.example.com/test bar https://bar.example.com baz"));
        // CHECKSTYLE.ON: LineLength
    }
}
