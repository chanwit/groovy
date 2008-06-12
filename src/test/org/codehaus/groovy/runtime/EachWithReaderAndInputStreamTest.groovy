package org.codehaus.groovy.runtime

/**
 * Test .each with Reader and InputStream
 * 
 * @author <a href="mailto:joachim.baumann@xinaris.de">Joachim Baumann</a>
 * @version $Revision: $
 */
class EachWithReaderAndInputStreamTest extends GroovyTestCase {
	/**
	 * The following instances are used in testing the file operations
	 */

	String multiLineVal = """
This text
as can be seen
has multiple lines
and not one punctuation mark
"""

	// Our file instance
	def File file;
	
	void setUpFile() {
		// Setup guarantees us that we use a non-existent file
		file = File.createTempFile("unitTest", ".txt") 
		assert file.exists() == true
		//println file.canonicalPath
		assert file.length() == 0L
		file << multiLineVal
	}
	void tearDownFile() {
		// we remove our temporary file
		def deleted = false
		while(deleted == false)
			deleted = file.delete()
		assert file.exists() == false
	}

	void testEachForStringBufferInputStream(){
		def ist = new StringBufferInputStream(multiLineVal)
		def readVal = ""
		ist.each {
            readVal += (char)it 
        }
		assert readVal == multiLineVal
	}
	 		
	void testEachForStringReader(){
		def ir = new StringReader(multiLineVal)
		def readVal = ""
		ir.each { readVal += it + "\n" }
		assert readVal == multiLineVal
	}

	void testEachForFileWithInputStream() {
		setUpFile()
		def readVal = ""
		file.withInputStream{ is ->
			is.each { readVal += (char)it }
		}
		tearDownFile()
		assert readVal == multiLineVal
	}

	void testEachForFileWithReader() {
		setUpFile()
		def readVal = ""
		file.withReader{ reader ->
			reader.each { readVal += it + "\n" }
		}
		tearDownFile()
		assert readVal == multiLineVal
	}
}
