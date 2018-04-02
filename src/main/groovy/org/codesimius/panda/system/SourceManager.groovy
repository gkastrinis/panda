package org.codesimius.panda.system

import groovy.transform.Canonical

@Singleton
class SourceManager {

	// A C-Preprocessor line marker
	@Canonical
	static class LineMarker {
		int line       // the line that the last marker reports
		int actualLine // the line that the last marker is in the output file
		String file    // the source file tha the last marker reports
	}

	Stack<LineMarker> markers = []
	String outputFile

	void lineMarkerStart(int markerLine, int markerActualLine, String sourceFile) {
		markers.push(new LineMarker(markerLine, markerActualLine, sourceFile))
	}

	void lineMarkerEnd() {
		markers.pop()
	}

	/////////////////////////////////////////////////////////////////

	Map<Object, SourceLocation> locations = [:]

	SourceLocation record(Object o, int outputLine) { locations[o] = locate(outputLine) }

	SourceLocation recall(Object o) { locations[o] }

	static SourceLocation recallStatic(Object o) { SourceManager.instance.recall(o) }

	SourceLocation locate(int outputLine) {
		def loc = new SourceLocation()
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
