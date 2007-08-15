package org.codehaus.groovy.runtime;

import java.io.File
import java.io.Reader

/** 
 * Test Writer append and left shift methods in Groovy
 * 
 * @author Joachim Baumann</a>
 * @version $Revision$
 */
class WriterAppendTest extends GroovyTestCase {
	/**
	 * The following instances are used in testing the file writes
	 */
	static text = """
			<groovy>
			  <things>
			    <thing>Jelly Beans</thing>
			  </things>
			  <music>
			    <tune>The 59th Street Bridge Song</tune>
			  </music>
			  <characters>
			    <character name="Austin Powers">
			       <enemy>Dr. Evil</enemy>
			       <enemy>Mini Me</enemy>
			    </character>
			  </characters>
			</groovy>
			"""
	static gPathResult = new XmlSlurper().parseText(text)
	static gPathWriteTo
	static defaultEncoding
	static UTF8_ENCODING

	static {
		StringWriter sw = new StringWriter()
		gPathResult.writeTo(sw)
		gPathWriteTo = sw.toString()
		UTF8_ENCODING = "UTF-8"
	    defaultEncoding = System.getProperty("file.encoding")
	}

	// Our file instance
	def File file;
	
	void setUp() {
		// Setup guarantees us that we use a non-existent file
		file = File.createTempFile("unitTest", ".txt") 
		assert file.exists() == true
		//println file.canonicalPath
		assert file.length() == 0L
	}
	void tearDown() {
		// we remove our temporary file
		def deleted = false
		while(deleted == false)
			deleted = file.delete()
		assert file.exists() == false
	}

	void testAppendStringWithEncoding(){
		def expected
		// test new
		file.withWriterAppend(UTF8_ENCODING) { writer ->
		    writer.write(text)
		}
		expected = text
		assert hasContents(file, expected, UTF8_ENCODING)
		
		// test existing
		file.withWriterAppend(UTF8_ENCODING) { writer ->
		    writer.write(text)
		}
		expected += text
		assert hasContents(file, expected, UTF8_ENCODING)
	}

	void testAppendWritableWithEncoding(){
		def expected
		
		// test new
		file.withWriterAppend(UTF8_ENCODING) { writer ->
		    writer.write(gPathResult)
		}
		expected = gPathWriteTo
		assert hasContents(file, expected, UTF8_ENCODING)
		
		// test existing
		file.withWriterAppend(UTF8_ENCODING) { writer ->
		    writer.write(gPathResult)
		}
		expected += gPathWriteTo
		assert hasContents(file, expected, UTF8_ENCODING)
	}


	void testLeftShiftStringWithEncoding(){
		def expected
		
		// test new
		file.withWriterAppend(UTF8_ENCODING) { writer ->
		    writer << text
		}
		expected = text
		assert hasContents(file, expected, UTF8_ENCODING)
		
		// test existing
		file.withWriterAppend(UTF8_ENCODING) { writer ->
		    writer << text
		}
		expected += text
		assert hasContents(file, expected, UTF8_ENCODING)
	}
			

	void testLeftShiftWritableWithEncoding(){
		def expected
		
		// test new
		file.withWriterAppend(UTF8_ENCODING) { writer ->
		    writer << gPathResult
		}
		expected = gPathWriteTo
		assert hasContents(file, expected, UTF8_ENCODING)
		
		// test existing
		file.withWriterAppend(UTF8_ENCODING) { writer ->
		    writer << gPathResult
		}
		expected += gPathWriteTo
		assert hasContents(file, expected, UTF8_ENCODING)
	}		 

	///////////////////////////////////////
	void testAppendStringDefaultEncoding(){
		def expected
		// test new
		file.withWriterAppend { writer ->
		    writer.write(text)
		}
		expected = text
		assert hasContents(file, expected, defaultEncoding)
		
		// test existing
		file.withWriterAppend { writer ->
		    writer.write(text)
		}
		expected += text
		assert hasContents(file, expected, defaultEncoding)
	}

	void testAppendWritableDefaultEncoding(){
		def expected
		
		// test new
		file.withWriterAppend { writer ->
		    writer.write(gPathResult)
		}
		expected = gPathWriteTo
		assert hasContents(file, expected, defaultEncoding)
		
		// test existing
		file.withWriterAppend { writer ->
		    writer.write(gPathResult)
		}
		expected += gPathWriteTo
		assert hasContents(file, expected, defaultEncoding)
	}


	void testLeftShiftStringDefaultEncoding(){
		def expected
		
		// test new
		file.withWriterAppend { writer ->
		    writer << text
		}
		expected = text
		assert hasContents(file, expected, defaultEncoding)
		
		// test existing
		file.withWriterAppend { writer ->
		    writer << text
		}
		expected += text
		assert hasContents(file, expected, defaultEncoding)
	}
			

	void testLeftShiftWritableDefaultEncoding(){
		def expected
		
		// test new
		file.withWriterAppend { writer ->
		    writer << gPathResult
		}
		expected = gPathWriteTo
		assert hasContents(file, expected, defaultEncoding)
		
		// test existing
		file.withWriterAppend { writer ->
		    writer << gPathResult
		}
		expected += gPathWriteTo
		assert hasContents(file, expected, defaultEncoding)
	}		 

	
	boolean hasContents(File f, String expected, String charSet)
	{
		// read contents the Java way
		byte[] buf = new byte[expected.length()];
		
		def fileIS = new FileInputStream(file)
		fileIS.read(buf)
		if (expected != new String(buf, charSet))
		    println "EX: " + expected + "------" + new String(buf, charSet) + "\n----"
		return expected == new String(buf, charSet)
	}
}