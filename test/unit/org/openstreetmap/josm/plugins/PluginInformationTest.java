// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.plugins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.jar.JarInputStream;

import junit.framework.TestCase;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;

public class PluginInformationTest extends TestCase {

	@Override protected void setUp() throws Exception {
	    super.setUp();
	    Main.pref = new Preferences(){
        	@Override public Collection<String> getAllPossiblePreferenceDirs() {
        		return Arrays.asList(new String[]{getClass().getResource("..").getFile()});
            }
        };
	}

	public void testConstructorExtractsAttributesFromManifest() throws Exception {
		PluginInformation info = new PluginInformation(new File(getClass().getResource("simple.jar").getFile()));
		String s = getClass().getResource(".").getFile();
        assertEquals(4, info.libraries.size());
        assertEquals(s+"foo", info.libraries.get(1).getFile());
        assertEquals(s+"bar", info.libraries.get(2).getFile());
        assertEquals(s+"C:/Foo%20and%20Bar", info.libraries.get(3).getFile());
        
        assertEquals("imi", info.author);
        assertEquals("Simple", info.className);
        assertEquals("Simpler", info.description);
        assertEquals(true, info.early);
    }

	public void testConstructorRequiresJarWithManifest() throws Exception {
		try {
	        new PluginInformation(new File(getClass().getResource("no_manifest.jar").getFile()));
	        fail("Exception because missing manifest excpected");
        } catch (PluginException e) {
        }
    }
	
	public void testConstructorWithInputStream() throws Exception {
		JarInputStream f = new JarInputStream(getClass().getResourceAsStream("simple.jar"));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		f.getManifest().write(out);

		PluginInformation info = new PluginInformation(null, "simple", new ByteArrayInputStream(out.toByteArray()));
        assertEquals("Only the 3 external classpaths are added (as we are using bootstrap classpath for plugin",
        		3, info.libraries.size());
    }
	
	public void testLoadClassInstantiatePlugin() throws Exception {
		PluginInformation info = new PluginInformation(new File(getClass().getResource("working.jar").getFile()));
		ClassLoader cl = new URLClassLoader(new URL[]{getClass().getResource("working.jar")});
		assertNotNull(info.load(info.loadClass(cl)));
    }
	
	// This is so the bugtracker always detect coding problems as "plugin problems"
	public void testLoadThrowsPluginExceptionOnRuntimeException() throws Exception {
		PluginInformation info = new PluginInformation(new File(getClass().getResource("working.jar").getFile()));
		try {
	        info.load(null);
	        fail("Exception excpected because null-Class");
        } catch (PluginException e) {
        }
        try {
        	info.loadClass(null);
        	fail("Exception excpected because null-ClassLoader");
        } catch (PluginException e) {
        }
    }
	
	public void testFindPluginReturnsInformationFromBootstrapClasspath() throws Exception {
	    PluginInformation info = PluginInformation.findPlugin("test_simple");
	    assertEquals("Simpler", info.description);
    }
	
	public void testFindPluginReturnsFromPreferencesDirs() throws Exception {
	    PluginInformation info = PluginInformation.findPlugin("simple");
	    assertEquals("Simpler", info.description);
    }
	
	public void testFindPluginForUnknownReturnsNull() throws Exception {
		assertNull(PluginInformation.findPlugin("asdf"));
	}

	public void testPluginLocationsReturnModifiedPreferenceLocations() throws Exception {
	    setUp();
	    Collection<String> locations = PluginInformation.getPluginLocations();
	    assertEquals(1, locations.size());
	    assertTrue(locations.iterator().next().endsWith("/plugins"));
    }
}
