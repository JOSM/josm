// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import org.junit.Test 

import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.OsmPrimitiveType
import org.openstreetmap.josm.data.osm.DataSet

import static org.junit.Assert.*;

class ParseWithChangesetReaderTest {
	
	/**
	 * A new node with a changeset id. Ignore it.
	 */
	@Test
	public void test_1() {
		String doc = """\
         <osm version="0.6">
        <node id="-1" lat="0.0" lon="0.0" changeset="1"/>
        </osm>
        """
		
		OsmReader reader = new OsmReader()
		DataSet ds = reader.parseDataSet(new StringBufferInputStream(doc), null)
		Node n = ds.getPrimitiveById(-1, OsmPrimitiveType.NODE)
		assert n != null
		assert n.uniqueId == -1
		assert n.changesetId == 0
	}
	
	/**
	 * A new node with an invalid changeset id. Ignore it.
	 */
	@Test
	public void test_11() {
		String doc = """\
         <osm version="0.6">
        <node id="-1" lat="0.0" lon="0.0" changeset="0">
		    <tag k="external-id" v="-1"/>
		</node>
        </osm>
        """
		
		OsmReader reader = new OsmReader()
		DataSet ds = reader.parseDataSet(new StringBufferInputStream(doc), null)
		Node n = ds.nodes.find {it.get("external-id") == "-1"}
		assert n != null
		assert n.changesetId == 0
	}
	
	/**
	 * A new node with an invalid changeset id. Ignore it.
	 */
	@Test
	public void test_12() {
		String doc = """\
         <osm version="0.6">
        <node id="-1" lat="0.0" lon="0.0" changeset="-1">
		    <tag k="external-id" v="-1"/>
        </node>
        </osm>
        """
		
		OsmReader reader = new OsmReader()
		DataSet ds = reader.parseDataSet(new StringBufferInputStream(doc), null)
		Node n = ds.nodes.find {it.get("external-id") == "-1"}
		assert n != null
		assert n.changesetId == 0
	}
	
	/**
	 * A new node with an invalid changeset id. Ignore it.
	 */
	@Test
	public void test_13() {
		String doc = """\
         <osm version="0.6">
        <node id="-1" lat="0.0" lon="0.0" changeset="aaa">
		    <tag k="external-id" v="-1"/>
        </node>
        </osm>
        """
		
		OsmReader reader = new OsmReader()
		DataSet ds = reader.parseDataSet(new StringBufferInputStream(doc), null)
		Node n = ds.nodes.find {it.get("external-id") == "-1"}
		assert n != null
		assert n.changesetId == 0
	}
	
	/**
	 * A new node with a missing changeset id. That's fine. The changeset id
	 * is reset to 0.
	 */
	@Test
	public void test_14() {
		String doc = """\
         <osm version="0.6">
        <node id="-1" lat="0.0" lon="0.0" >
		    <tag k="external-id" v="-1"/>
        </node>
        </osm>
        """
		
		OsmReader reader = new OsmReader()
		DataSet ds = reader.parseDataSet(new StringBufferInputStream(doc), null)
		Node n = ds.nodes.find {it.get("external-id") == "-1"}
		assert n != null
		assert n.changesetId == 0
	}
	
	
	/**
	 * An existing node with a missing changeset id. That's fine. The changeset id
	 * is reset to 0.
	 */
	@Test
	public void test_2() {
		String doc = """\
         <osm version="0.6">
        <node id="1" lat="0.0" lon="0.0" version="1"/>
        </osm>
        """
		
		OsmReader reader = new OsmReader()
		DataSet ds = reader.parseDataSet(new StringBufferInputStream(doc), null)
		Node n = ds.getPrimitiveById(1, OsmPrimitiveType.NODE)
		assert n != null
		assert n.uniqueId == 1
		assert n.changesetId == 0
	}
	
	/**
     * An existing node with a valid changeset id id. That's fine. The changeset id
     * is applied.
     */
	@Test
	public void test_3() {
		String doc = """\
         <osm version="0.6">
        <node id="1" lat="0.0" lon="0.0" version="1" changeset="4"/>
        </osm>
        """
		
		OsmReader reader = new OsmReader()
		DataSet ds = reader.parseDataSet(new StringBufferInputStream(doc), null)
		Node n = ds.getPrimitiveById(1, OsmPrimitiveType.NODE)
		assert n != null
		assert n.uniqueId == 1
		assert n.changesetId == 4
	}
	
	/**
	 * An existing node with an invalid changeset id. That's a problem. An exception
	 * is thrown.
	 */
	@Test
	public void test_4() {
		String doc = """\
         <osm version="0.6">
        <node id="1" lat="0.0" lon="0.0" version="1" changeset="-1"/>
        </osm>
        """
		
		final shouldFail = new GroovyTestCase().&shouldFail
		
		OsmReader reader = new OsmReader()
		shouldFail(IllegalDataException) {
		    DataSet ds = reader.parseDataSet(new StringBufferInputStream(doc), null)
		}
	}	
	/**
	 * An existing node with an invalid changeset id. That's a problem. An exception
	 * is thrown.
	 */
	@Test
	public void test_5() {
		String doc = """\
         <osm version="0.6">
        <node id="1" lat="0.0" lon="0.0" version="1" changeset="0"/>
        </osm>
        """
		
		final shouldFail = new GroovyTestCase().&shouldFail
		
		OsmReader reader = new OsmReader()
		shouldFail(IllegalDataException) {
			DataSet ds = reader.parseDataSet(new StringBufferInputStream(doc), null)
		}
	}   
	/**
	 * An existing node with an invalid changeset id. That's a problem. An exception
	 * is thrown.
	 */
	@Test
	public void test_6() {
		String doc = """\
	         <osm version="0.6">
	        <node id="1" lat="0.0" lon="0.0" version="1" changeset="abc"/>
	        </osm>
	        """
		
		final shouldFail = new GroovyTestCase().&shouldFail
		
		OsmReader reader = new OsmReader()
		shouldFail(IllegalDataException) {
			DataSet ds = reader.parseDataSet(new StringBufferInputStream(doc), null)
		}
	}   
}
