package org.kepler.objectmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.kepler.build.modules.Module;
import org.kepler.build.modules.ModuleTree;
import org.kepler.util.FileUtil;

import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Workspace;
import ptolemy.moml.MoMLParser;

/**
 * The idea here is to run through a bunch of actors and deserialize them
 * into ActorMetadata objects and see if they work.
 * Also, this class tests just the MoMLParser on all momls.
 * 
 * In order to run this test you must set your classpath to include everything
 * that's in the classpath for all modules.
 * 
 * @author Aaron Schultz
 */
public class ActorMetadataTest extends TestCase  {
	
	private final int maxLoops = 1000;
	
	public ActorMetadataTest(String name) {
		super(name);
	}

	/**
	 * Create a suite of tests to be run together
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTest(new ActorMetadataTest("initialize"));
		//suite.addTest(new ActorMetadataTest("testMomlParser"));
		suite.addTest(new ActorMetadataTest("testActorMetadata"));
		return suite;
	}

	/**
	 * Run an initial test that always passes to check that the test harness is
	 * working.
	 */
	public void initialize() {
		assertTrue(1 == 1);
	}
	
	/**
	 * This test goes through all of the resources/kar directories
	 * and tries to parse using ActorMetadata found there through the MoMLParser.
	 * No errors should be found.
	 */
	public void testActorMetadata() {
		System.out.println();
		System.out.println("*************************************************");
		System.out.println("testActorMetadata()");
		
		//Vector<List<File>> momlLists = getAllMoMLs();
		Vector<List<File>> momlLists = specifyMoMLs();
		
		Hashtable<File,String> failedToParse = new Hashtable<File,String>();
		
		int attempts = 0;
		int loops = 0;
		for (List<File> momlList : momlLists) {
			for (File f : momlList) {
				attempts++;
				System.out.println();
				System.out.println(f.toString());
				try {
					InputStream is = new FileInputStream(f);
					ActorMetadata am = new ActorMetadata(is);
					NamedObj obj = am.getActor();
					if (obj == null) {
						throw new Exception("Actor is null");
					}
				} catch (Exception e) {
					failedToParse.put(f, e.getMessage());
				}
				if (loops++ >= maxLoops) break;
			}
		}
		System.out.println("Attempted to parse " + attempts + " moml files");
		handleErrors(failedToParse);
	}
	
	/**
	 * This test goes through all of the resources/kar directories
	 * and tries to run the momls found there through the MoMLParser.
	 * No errors should be found.
	 */
	public void testMomlParser() {
		System.out.println();
		System.out.println("*************************************************");
		System.out.println("testMomlParser()");
		
		Vector<List<File>> momlLists = getAllMoMLs();
		
		Hashtable<File,String> failedToParse = new Hashtable<File,String>();
		
		int attempts = 0;
		int loops = 0;
		for (List<File> momlList : momlLists) {
			for (File f : momlList) {
				attempts++;
				//System.out.println(f.toString());
				try {
					InputStream is = new FileInputStream(f);
					String momlStr = FileUtil.convertStreamToString(is);
					MoMLParser parser = new MoMLParser(new Workspace());
					parser.reset();
		
					NamedObj obj = parser.parse(null, momlStr);
					//System.out.println(obj.getName() + " " + obj.getClassName());
				} catch (Exception e) {
					failedToParse.put(f, e.getMessage());
				}
				if (loops++ >= maxLoops) break;
			}
		}
		
		System.out.println("Attempted to parse " + attempts + " moml files");
		handleErrors(failedToParse);
	}

	/**
	 * @throws java.lang.Exception
	 */
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	public void tearDown() throws Exception {
	}

	private void handleErrors(Hashtable<File,String> failedToParse) {
		if (failedToParse.size() > 0) {
			System.out.println(failedToParse.size() + " momls failed to parse");
			for (File f : failedToParse.keySet()) {
				String err = failedToParse.get(f);
				System.out.println();
				System.out.println(f.toString());
				System.out.println(err);
			}
			fail();
		} else {
			System.out.println("0 failures");
		}
	}
	
	private Vector<List<File>> specifyMoMLs() {
		Vector<List<File>> momlLists = new Vector<List<File>>();
		
		List<File> momlList = new Vector<File>();
		
		Vector<String> f = new Vector<String>();
		
		//String p1 = "C:/kepler/suites/kepler/actors/resources/kar/CoreActors/";
		//String p2 = "C:/Documents and Settings/Aaron/KeplerData/";
		
		String test1 = "org/kepler/objectmanager/SomeExpression.xml";
		File tf1 = FileUtil.getResourceAsFile(this, test1);
		momlList.add(tf1);
		
		// Add file paths here
		//f.add( p1 + "CopyToNode.xml");
		//f.add( p2 + "SomeExpression.xml");
		
		for (String s : f) {
			momlList.add(new File(s));
		}
		
		momlLists.add(momlList);

		return momlLists;
	}
	
	private Vector<List<File>> getAllMoMLs() {
		ModuleTree moduleTree = ModuleTree.instance();

		Vector<List<File>> momlLists = new Vector<List<File>>();
		
		for (Module m : moduleTree.getModuleList()) {
			if (m != null) {
				System.out.println(m.getName());
				List<File> momls = getMoMLsForModule(m);
				if (momls.size() > 0) {
					System.out.println(momls.size() + " momls found");
					momlLists.add(momls);
				}
			} else {
				System.out.println("null");
			}
		}
		return momlLists;
	}
	
	private List<File> getMoMLsForModule(Module mod) {
		Vector<File> files = new Vector<File>();
		File resDir = mod.getKarResourcesDir();
		if (!resDir.exists()) return files;
		String[] dirs = resDir.list();
		for (int i = 0; i < dirs.length; i++) {
			String dir = dirs[i];
			File karDir = new File(resDir,dir);
			if (!karDir.exists()) return files;
			if (karDir.isDirectory()) {
				String[] xmls = karDir.list();
				for (int j = 0; j < xmls.length; j++) {
					String xml = xmls[j];
					File xmlFile = new File(karDir,xml);
					if (!xmlFile.exists()) return files;
					if (isMoml(xmlFile)) {
						files.add(xmlFile);
					}
				}
			}
		}
		
		return files;
	}
	
	private boolean isMoml(File f) {
		boolean isMoml = false;
		
		String fs = f.toString();
		String fsLC = fs.toLowerCase();
		
		if (fsLC.endsWith(".xml"))
			isMoml = true;
		if (fsLC.endsWith(".moml"))
			isMoml = true;
		
		return isMoml;
	}
	
}
