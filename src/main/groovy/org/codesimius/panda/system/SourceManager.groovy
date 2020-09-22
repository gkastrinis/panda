package org.codesimius.panda.system

class SourceManager {
	// Stack of active files under process (due to include)
	Stack<File> activeFiles = []
	// A list of include locations up to the current point
	List<String> includeLines = []
	// Map each object of interest to a location in the compiled file(s)
	Map<Object, String> locations = [:]

	SourceManager(File mainFile) { activeFiles.push mainFile }

	void enterInclude(File toFile, int fromLine) {
		includeLines.push "${activeFiles.last()} : $fromLine}" as String
		activeFiles.push toFile
	}

	void exitInclude() {
		includeLines.pop()
		activeFiles.pop()
	}

	String rec(Object o, int line) {
		def location = (includeLines + ["${activeFiles.last()} : $line"]).collect { it -> "\tat $it" }.join("\n")
		locations[o] = location
		return location
	}

	String loc(Object o) { locations[o] }
}
