package org.codesimius.panda.system

class SourceManager {
	// Stack of active files under process (due to include)
	static Stack<File> files
	// A list of include locations up to the current point
	static List<String> lines
	// Map each object of interest to a location in the compiled file(s)
	static Map<Object, String> locations

	// Note: Hack for multiple compilations
	// TODO: Find a better approach
	static void init() {
		files = []
		lines = []
		locations = [:]
	}

	static void mainFile(File file) { files.push file }

	static void enterInclude(File toFile, int fromLine) {
		lines.push "${files.last()} : $fromLine}" as String
		files.push toFile
	}

	static void exitInclude() {
		lines.pop()
		files.pop()
	}

	static String locate(int line) { (lines + ["${files.last()} : $line"]).collect { it -> "\tat $it" }.join("\n") }

	static void rec(Object o, String loc) { locations[o] = loc }

	static String loc(Object o) { locations[o] }
}
