package groovy.servlet

import javax.servlet.http.HttpSession
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.ServletContext
import javax.servlet.ServletOutputStream

/**
* This test case tests the ServletBinding class. 
* 
* @author Hamlet D'Arcy
*/ 
class ServletBindingTest extends GroovyTestCase {

	def session = {} as HttpSession
	def response = {} as HttpServletResponse
	def context = {} as ServletContext
	
	void setUp() {
		super.setUp()
	}
	
	def makeDefaultBinding = { request ->
		new ServletBinding(
		    request as HttpServletRequest, 
		    response as HttpServletResponse, 
		    context as ServletContext
		)
	}
	
	def makeDefaultRequest  = {
		[ getSession: {session}, 
		getParameterNames: {new Vector().elements()}, 
		getHeaderNames: {new Vector().elements()} ] as HttpServletRequest
	}
	
	/**
	* Tests that the constructor binds the correct default variables. 
	*/
	void testConstructor_VariableBindings() {
		def request = makeDefaultRequest()
		def binding = makeDefaultBinding(request)
		
		assert request == binding.getVariable("request")
		assert response == binding.getVariable("response")
		assert context == binding.getVariable("context")
		assert context == binding.getVariable("application")
		assert session == binding.getVariable("session")
		assertTrue binding.getVariable("params").isEmpty()
		assertTrue binding.getVariable("headers").isEmpty()
	}
	
	/**
	* Tests that the constructor binds request parameter names correctly. 
	*/ 
	void testConstructor_ParameterNameBindings() {

		def parmNames = new Vector()
		parmNames.add("name1")
		parmNames.add("name2")

		def request = [ 
		    getSession: {session}, 
			getHeaderNames: {new Vector().elements()}, 
		    getParameterNames: {parmNames.elements()}, 
			getParameterValues: { 
		        //prepend string parm to known value to simulate attribute map
		        String[] arr = new String[1]; 
		        arr[0] = "value_for_" + it
		        return arr} ] as HttpServletRequest

		def binding = makeDefaultBinding(request)

		def variables = binding.getVariable("params")
		assert 2 == variables.size()
		assert "value_for_name1" == variables.get("name1")
		assert "value_for_name2" == variables.get("name2")	
	}
	
	/**
	* Tests that the constructor binds request header values correctly. 
	*/ 
	void testConstructor_HeaderBindings() {
		def headerNames = new Vector()
		headerNames.add("name1")
		headerNames.add("name2")

		def request = [ 
		    getSession: {session}, 
		    getParameterNames: {new Vector().elements()}, 
		    getHeaderNames: {headerNames.elements()}, 
		    getHeader: { 
		        //prepend string parm to known value to simulate attribute map
		        "value_for_" + it
		        } ] as HttpServletRequest

		def binding = makeDefaultBinding(request)

		def variables = binding.getVariable("headers")
		assert 2 == variables.size()
		assert "value_for_name1" == variables.get("name1")
		assert "value_for_name2" == variables.get("name2")		
	}
	
	/**
	* Tests the argument contract on getVariable. 
	*/ 
	void testGetVariable_Contract() {
		
		def request = makeDefaultRequest()
		def binding = makeDefaultBinding(request)
		
		shouldFail(IllegalArgumentException) { binding.getVariable(null) }
		shouldFail(IllegalArgumentException) { binding.getVariable("") }
	}

	/**
	* Tests that getVariable works for the special key names. 
	*/ 
	void testGetVariable_ImplicitKeyNames() {
		
		def writer = new PrintWriter(new StringWriter())
		def outputStream = new OutputStreamStub()

		def response = [
		    getWriter: {writer}, 
		    getOutputStream: {outputStream} ] as HttpServletResponse

		def request = makeDefaultRequest()

		def binding = new ServletBinding(
		    request as HttpServletRequest, 
		    response as HttpServletResponse, 
		    context as ServletContext
		)

		assert writer == binding.getVariable("out")
		assert binding.getVariable("html") instanceof groovy.xml.MarkupBuilder
		assert outputStream == binding.getVariable("sout")
	}
	
	/**
	* Tests the contract on setVarible(). 
	*/ 
	void testSetVariable_Contract() {
		
		def request = makeDefaultRequest()
		def binding = makeDefaultBinding(request)
		
		shouldFail(IllegalArgumentException) { binding.setVariable(null, null) }
		shouldFail(IllegalArgumentException) { binding.setVariable("", null) }
		shouldFail(IllegalArgumentException) { binding.setVariable("out", null) }
		shouldFail(IllegalArgumentException) { binding.setVariable("sout", null) }
		shouldFail(IllegalArgumentException) { binding.setVariable("html", null) }		
	}
	
	/**
	* Tests setVariable. 
	*/ 
	void testSetVariable() {
		def request = makeDefaultRequest()
		def binding = makeDefaultBinding(request)
		
		binding.setVariable("var_name", "var_value")
		def variables = binding.getVariables()
		assert "var_value" == variables.get("var_name")
	}
}

/**
* Test specific sub class to stub out the ServletOutputStream class. 
*/ 
class OutputStreamStub extends ServletOutputStream {
    void write(int x) {   }
}