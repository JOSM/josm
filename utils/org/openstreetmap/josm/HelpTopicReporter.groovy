// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import java.io.BufferedReader;

/**
 * Creates a help topics report for the JOSM trac wiki.
 * 
 */
public class HelpTopicReporter {

    private PrintWriter  writer
    private List<String> files = []
    private String outputFile = null
    private PrintWriter outputWriter = null

    def String formatLine(String line) {
        def fields = line.split("\t")
        def repositoryPath = fields[3].replace(".", "/") + "/" + fields[1]
        // each line is a table row according to the simple trac table syntax
        // 
        return "||[wiki:/Help${fields[4]} ${fields[4]}]||${fields[3]}||[source:/trunk/src/${repositoryPath}#L${fields[2]} ${fields[1]}]||"
    }
    
    def process(BufferedReader reader) {
        reader.eachLine { 
            String line ->
            writer.println formatLine(line)
        }
    }

    def showHelp() {
        println "groovy org.openstreetmap.josm.HelpTopicReporter [options] file*"
        println "Options:"
        println "  --help|-h        show help information"
        println "  -output-file|-o file  write to output file"
        println "Reads from stdin if no input file(s) in the argument list"
    }

    def run(String[] args) {
        def i = 0
        while(i<args.length) {
            def arg = args[i]
            if (arg == "-h" || arg == "--help") {
                showHelp()
                System.exit(0)
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

        if (! files.isEmpty()) {
            files.each { process(new BufferedReader(new FileReader(new File(it)))) }
        } else {
            process(new BufferedReader(new InputStreamReader(System.in)))
        }
        outputWriter.flush()
        if (outputFile != null) {
            outputWriter.close()
        }
    }
    
    static public void main(String[] args) {
        new HelpTopicReporter().run(args)
    }
}
