// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.net.InetAddress.Cache;
import org.junit.Test 


import static org.junit.Assert.*;

class ChangesetCacheTest {
	
	@Test
	public void test_Constructor() {
	    ChangesetCache cache = ChangesetCache.getInstance()
		assert cache != null
	}
	
	@Test
	public void test_addAndRemoveListeners() {
		ChangesetCache cache = ChangesetCache.getInstance()
		cache.clear()
		
		// should work
		cache.addChangesetCacheListener null
		
		// should work 
		def listener = new ChangesetCacheListener() {
			public void changesetCacheUpdated(ChangesetCacheEvent event) {} 
		}		
		cache.addChangesetCacheListener listener
		// adding a second time - should work too
		cache.addChangesetCacheListener listener
		assert cache.@listeners.size() == 1 // ... but only added once 
		
		cache.removeChangesetCacheListener null
		
		cache.removeChangesetCacheListener listener 
		assert cache.@listeners.size() == 0
	}
	
	@Test
	public void update_get_remove_cycle() {
		ChangesetCache cache = ChangesetCache.getInstance()
		cache.clear()
		
		cache.update new Changeset(1)
		assert cache.size() == 1
		assert cache.get(1) != null
		assert cache.get(1).id == 1
		cache.remove(1)
		assert cache.size() == 0
	}
	
	@Test
	public void updateTwice() {
		ChangesetCache cache = ChangesetCache.getInstance()
		cache.clear()

		Changeset cs = new Changeset(1)
		cs.incomplete = false
		cs.put "key1", "value1"
		cs.open = true
		cache.update cs
		
		Changeset cs2 = new Changeset(cs)
		assert cs2 != null
		cs2.put "key2", "value2"
		cs2.open = false
		cache.update(cs2)
		
		assert cache.size() == 1
		assert cache.get(1) != null
		
		cs = cache.get(1)
		assert cs.get("key1") == "value1"
		assert cs.get("key2") == "value2"
		assert !cs.open		
	}
	
	
	@Test
	public void contains() {
		ChangesetCache cache = ChangesetCache.getInstance()
		cache.@listeners.clear()
		cache.clear()
		
		
		Changeset cs = new Changeset(1)
		cache.update cs
		
		assert cache.contains(1)
		assert cache.contains(cs)
		assert cache.contains(new Changeset(cs))
		
		assert ! cache.contains(2)
		assert ! cache.contains(new Changeset(2))
		assert ! cache.contains(null)
	}
	
	@Test
	public void fireingEvents_AddAChangeset() {
		ChangesetCache cache = ChangesetCache.getInstance()
		cache.clear()
		cache.@listeners.clear()
		
		// should work 
		def listener = new ChangesetCacheListener() {
					public void changesetCacheUpdated(ChangesetCacheEvent event) {
					    assert event != null
						assert event.getAddedChangesets().size() == 1
						assert event.getRemovedChangesets().empty
						assert event.getUpdatedChangesets().empty
						assert event.getSource() == cache 
					} 
		}   
		cache.addChangesetCacheListener listener
		cache.update(new Changeset(1))
		cache.removeChangesetCacheListener listener
	}
	
	@Test
	public void fireingEvents_UpdateChangeset() {
		ChangesetCache cache = ChangesetCache.getInstance()
		cache.clear()
		cache.@listeners.clear()
		
		// should work 
		def listener = new ChangesetCacheListener() {
					public void changesetCacheUpdated(ChangesetCacheEvent event) {
						assert event != null
						assert event.getAddedChangesets().empty
						assert event.getRemovedChangesets().empty
						assert event.getUpdatedChangesets().size() == 1
						assert event.getSource() == cache 
					} 
				}   
		cache.update(new Changeset(1))
		
		cache.addChangesetCacheListener listener
		cache.update(new Changeset(1))
		cache.removeChangesetCacheListener listener
	}
	
	@Test
	public void fireingEvents_RemoveChangeset() {
		ChangesetCache cache = ChangesetCache.getInstance()
		cache.clear()
		cache.@listeners.clear()
		
		// should work 
		def listener = new ChangesetCacheListener() {
					public void changesetCacheUpdated(ChangesetCacheEvent event) {
						assert event != null
						assert event.getAddedChangesets().empty
						assert event.getRemovedChangesets().size() == 1
						assert event.getUpdatedChangesets().empty
						assert event.getSource() == cache 
					} 
				}   
		cache.update(new Changeset(1))
		
		cache.addChangesetCacheListener listener
		cache.remove 1
		cache.removeChangesetCacheListener listener
	}
	
}
