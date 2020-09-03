package org.codesimius.panda.system

import groovy.transform.Canonical

@Singleton
class SourceManager {

	// Stack of active files under process (due to include)
	static Stack<File> files = []
	// A list of include locations up to the current point
	static List<String> lines = []

	static void mainFile(File file) { files.push file }

	static void enterInclude(File toFile, int fromLine) {
		lines.push "${files.last()} : $fromLine}" as String
		files.push toFile
	}

	static void exitInclude() {
		lines.pop()
		files.pop()
	}

	static Map<Object, String> locations = [:]

	static String locate(int line) { (lines + ["${files.last()} : $line"]).collect { it -> "\tat $it" }.join("\n") }

	static void rec(Object o, String loc) { SourceManager.locations[o] = loc }

	static String loc(Object o) { SourceManager.locations[o] }


	// A C-Preprocessor line marker
	@Canonical
	@Deprecated
	static class LineMarker {
		int line       // the line that the last marker reports
		int actualLine // the line that the last marker is in the output file
		String file    // the source file tha the last marker reports
	}

	// A sequence of source lines (due to #include)
	// The first element is the "oldest" include, and so on.
	@Canonical
	@Deprecated
	static class Location {
		List<String> lines = []

		void add(String file, int lineNum) { lines << ("$file : $lineNum" as String) }

		String toString() { lines.collect { "\tat $it" }.join("\n") }
	}

	/////////////////////////////////////////////////////////////////

	Stack<LineMarker> markers = []
	String outputFile
	Map<Object, Location> locationsOLD = [:]

	void lineMarkerStart(int markerLine, int markerActualLine, String sourceFile) {
		markers.push(new LineMarker(markerLine, markerActualLine, sourceFile))
	}

	void lineMarkerEnd() { markers.pop() }

	static void recOLD(Object o, Location loc) { SourceManager.instance.locationsOLD[o] = loc }

	static Location locOLD(Object o) { SourceManager.instance.locationsOLD[o] }

	Location locateOLD(int outputLine) {
		def loc = new Location()
		if (markers.empty()) {
			loc.add(outputFile, outputLine)
		} else {
			def tmp = (0..<markers.size()).collect { null }
			def actualLine = outputLine
			// Iterate in reverse order, because the top of the stack is at the "end"
			for (int i = markers.size() - 1; i >= 0; --i) {
				def lm = markers.get(i)
				def sourceLine = (lm.line + actualLine - (lm.actualLine + 1))
				tmp[i] = [lm.file, sourceLine]
				actualLine = lm.actualLine
			}
			tmp.each { String f, int l -> loc.add(f, l) }
		}
		return loc
	}
}
