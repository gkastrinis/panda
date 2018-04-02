package org.codesimius.panda.system

// A sequence of source lines (due to #include)
// The first element is the "oldest" include, etc.
class SourceLocation {

	List<String> files = []
	List<Integer> lineNums = []

	void add(String file, int lineNum) {
		files << file
		lineNums << lineNum
	}

	String toString() { [files, lineNums].transpose().collect { f, l -> "\tat $f : $l" }.join("\n") }
}
