
// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Script to extract declared help topics from JOSM source files.
 * 
 * Help topics are declared with the method ht(...). Example: ht("/Action/New")
 * 
 * Run with 
 *   groovy org.openstreetmap.josm.HelpTopicExtractor [options] fileOrDirs
 * 
 */
public class HelpTopicExtractor {
	
	private boolean recursive = false
	private List<String> files = []
	private String outputFile = null
	private PrintWriter outputWriter = null

	/**
	 * extracts declared help topics from file and writes them to the output stream
	 * 
	 * @param file the file
	 */
	def extractHelpTopics(File file) {
		def packageName;
		def linenr = 0
		file.eachLine {line ->
			linenr++
			def m = line =~ /^\s*package\s+(\S+)/
			if (m.matches()) {
				packageName = m[0][1].replace(";", "")
			}
			
			m = line =~ /.*[^a-zA-Z]ht\s*\(\s*"([^"]+)"\s*\).*/
			if (m.matches()) {
				def topic = m[0][1]
				outputWriter.println "${file.getPath()}\t${file.getName()}\t${linenr}\t${packageName}\t${topic}"
			}
		}
	}

	/**
	 * display help information 
	 */
	def showHelp() {
		println "groovy org.openstreetmap.josm.HelpTopicExtractor [options] fileOrDirs"
		println "Options:"
		println "  --help|-h        show help information"
		println "  --recurive|-r    recursively extracts help topics from java files in the specified directories"
		println "  -output-file|-o file  write to output file"
	}

	/**
	 * processes a file or directory. If recursive processing is enabled and file is a directory, 
	 * recursively processes child directories.
	 * 
	 * @param file the file 
	 */
	def process(File file) {
		if (file.isFile()) {
			extractHelpTopics(file)
		} else if (file.isDirectory()) {
			file.listFiles().grep(~/.*\.java$/).each { 
			    File child ->
				extractHelpTopics(child)
			}
			if (recursive) {
				file.listFiles().grep { File child ->
					child.isDirectory() && child.getName() != "." && child.getName() != ".."
				}.each { File child ->
					process(child)
				}
			}
		}
	}
	
	def run(String[] args) {
		def i = 0
		while(i<args.length) {
			def arg = args[i]
			if (arg == "-h" || arg == "--help") {
				showHelp()
				System.exit(0)
			} else if (arg == "-r" || arg == "--recursive") {
				recursive = true
				i++;
			} else if (arg == "-o" || arg == "--output-file") {
				i++
				if (i >= args.length) {
					System.err.println "Missing argument for option '${args[i-1]}'"
					System.exit(1)
				}
				outputFile = args[i]
				outputWriter = new PrintWriter(new FileWriter(outputFile))
				i++
			} else {
				files.add(arg)
				i++
			}
			
		}
		if (outputFile == null) {
			outputWriter = new PrintWriter(new OutputStreamWriter(System.out))
		}
		files.each {file -> process(new File(file)) }
		outputWriter.flush()
		if (outputFile != null) {
		    outputWriter.close()
		}
	}
	
	public static void main(String[] args) {
		new HelpTopicExtractor().run(args)
	}
}