package groovy

/** 
 * Demonstrates the use of the default named parameter in a closure
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
class ClosureWithDefaultParamTest extends GroovyTestCase {

    void methodWithDefaultParam(example='default'){
        assert 'default' == example
    }

    void testListCollect() {
        def list = [1, 2, 3, 4]
        def answer = list.collect { it * 2 }

        assert answer.size() == 4
        
        def expected = [2, 4, 6, 8]
        assert answer == expected
    }

    void testMapCollect() {
        def map = [1:2, 2:4, 3:6, 4:8]
        def answer = map.collect { it.key + it.value }
		
		// lest sort the results since maps are in hash code order
		answer = answer.sort()
		
        assert answer.size() == 4
        assert answer == [3, 6, 9, 12]
        assert answer.get(0) == 3
        assert answer.get(1) == 6
        assert answer.get(2) == 9
        assert answer.get(3) == 12
    }

    void testListFind() {
        def list = ["a", "b", "c"]
        def answer = list.find {it == "b" }
        assert answer == "b"
        
        answer = list.find {it == "z" }
        assert answer == null
    }
    
    void testMapFind() {
        def map = [1:2, 2:4, 3:6, 4:8]
        def answer = map.find {it.value == 6 }
        assert answer != null
        assert answer.key == 3
        assert answer.value == 6
        
        answer = map.find {it.value == 0 }
        assert answer == null
    }

    void testListFindAll() {
        def list = [20, 5, 40, 2]
        def answer = list.findAll {it < 10 }

        assert answer.size() == 2
        assert answer == [5, 2]
    }
    
    void testMapFindAll() {
        def map = [1:2, 2:4, 3:6, 4:8]
        def answer = map.findAll {it.value > 5 }

        assert answer.size() == 2
        
        def keys = answer.collect {it.key }
        def values = answer.collect {it.value }

        System.out.println("keys " + keys + " values " + values)
		
        // maps are in hash order so lets sort the results       
        keys.sort() 
        values.sort() 
        
        assert keys == [3, 4]
        assert values == [6, 8]
    }

    void testListEach() {
        def count = 0

        def list = [1, 2, 3, 4]
        list.each { count = count + it }
		
        assert count == 10

        list.each { count = count + it }
		
        assert count == 20
    }

    void testMapEach() {
        def count = 0

        def map = [1:2, 2:4, 3:6, 4:8]
        map.each { count = count + it.value }

        assert count == 20
    }
    
    void testListEvery() {
        assert [1, 2, 3, 4].every { it < 5 }
        assert [1, 2, 7, 4].every { it < 5 } == false
    }

    void testListAny() {
        assert [1, 2, 3, 4].any { it < 5 }
        assert [1, 2, 3, 4].any { it > 3 }
        assert [1, 2, 3, 4].any { it > 5 } == false
    }
    
    void testJoin() {
        def value = [1, 2, 3].join('-')
        assert value == "1-2-3"
    }
    
    void testListReverse() {
        def value = [1, 2, 3, 4].reverse()
        assert value == [4, 3, 2, 1]
    }
    
    void testEachLine() {
        def file = new File("src/test/groovy/Bar.groovy")
        
        System.out.println("Contents of file: " + file)
        
        file.eachLine { println(it) }
        
        println("")
    }
    
    void testReadLines() {
        def file = new File("src/test/groovy/Bar.groovy")

		def lines = file.readLines()
		
		assert lines != null
		assert lines.size() > 0

        System.out.println("File has number of lines: " + lines.size())
    }
    
    void testEachFile() {
        def file = new File("src/test/groovy")
        
        System.out.println("Contents of dir: " + file)
        
        file.eachFile { println(it.getName()) }
        
        println("")
    }
}
