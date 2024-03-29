package groovy

class SwitchTest extends GroovyTestCase {

    void testSwitch() {
        callSwitch("foo", "foo")
        callSwitch("bar", "barfoo")
        callSwitch("barbar", "barfoo")
        callSwitch("dummy", "d*")
        callSwitch("xyz", "xyzDefault")
        callSwitch("zzz", "Default")
        callSwitch(4, "List")
        callSwitch(5, "List")
        callSwitch(6, "List")
        callSwitch("inList", "List")
        callSwitch(1, "Integer")
        callSwitch(1.2, "Number")
        callSwitch(null, "null")
        callSwitch([1, 2, 3], "ListInterface")
    }

    def callSwitch(x, expected) {
        def result = ""
        switch (x) {
            case null:
                result = "null"
                break

            case ~/d.*/:
                result = "d*"
                break

            case "barbar":
            case "bar":
                result = result + "bar"

            case "foo":
                result = result + "foo"
                break

            case [4, 5, 6, 'inList']:
                result = "List"
                break

            case Integer:
                result = "Integer"
                break

            case Number:
                result = "Number"
                break

            case List:
                result = "ListInterface"
                break

            case "xyz":
                result = result + "xyz"

            default:
                result = result + "Default"
                // unnecessary just testing compiler
                break
        }
        assert result == expected, "Expected $expected but found $result when calling switch with $x"
    }

    // test the continue in switch, which should jump to the the while start
    void testSwitchScope() {
        def i = 0
        def j = 0
        while (true) {
            ++i;
            switch (i) {
                case 4:
                    continue
                case 5:
                    break;
                default:
                    j += i;
                    break;
            }
            if (i == 5) break;
        }
        assert j == 6
    }

    void testSwitchWithClosure() {
        switch (0) {
            case {true}: break
            default: assert false
        }
        switch (0) {
            case {false}: assert false
        }
        switch (0) {
            case {it == 0}: break
            default: assert false
        }
        switch (0) {
            case {candidate -> candidate == 0}: break
            default: assert false
        }
    }

    /** older versions of groovy produced a ListExpression for a
        fall through. the result was that it worked in some cases
        and in other cases not. For example not for patterns */
    void testFallthroughToOtherCaseWithNoCode() {
        def a = ['FileName.java', 'AnotherFileName.groovy', 'foo']
        def i = 0
        a.each {
            switch (it) {
                case ~/.*java$/:
                case ~/.*groovy$/:
                    i++
                    break
                default:
                    i += 10
            }
        }
        assertEquals 12, i
    }

    void testFallthroughToOtherCaseWithCode() {
        def a = ['FileName.java', 'AnotherFileName.groovy', 'foo']
        def i = 0
        a.each {
            switch (it) {
                case ~/.*java$/:
                    i += 5
                case ~/.*groovy$/:
                    i++
                    break
                default:
                    i += 10
            }
        }
        assertEquals 17, i
    }

    void testFallthroughToDefaultWithNoCode() {
        def a = ['FileName.java', 'AnotherFileName.groovy', 'foo']
        def i = 0
        a.each {
            switch (it) {
                case ~/.*java$/:
                    i++
                    break
                case ~/.*groovy$/:
                default:
                    i += 10
            }
        }
        assertEquals 21, i
    }

    void testFallthroughToDefaultWithCode() {
        def a = ['FileName.java', 'AnotherFileName.groovy', 'foo']
        def i = 0
        a.each {
            switch (it) {
                case ~/.*java$/:
                    i++
                    break
                case ~/.*groovy$/:
                    i += 5
                default:
                    i += 10
            }
        }
        assertEquals 26, i
    }

    void testSwitchNoStatementsAtEnd() {
        def a = ['FileName.java', 'AnotherFileName.groovy', 'foo']
        def i = 0
        a.each {
            switch (it) {
                case ~/.*java$/:
                    i++
                    break
                case ~/.*groovy$/: break
            }
        }
        assertEquals 1, i
        i = 0
        a.each {
            switch (it) {
                case ~/.*java$/:
                    i++
                    break
                default: break
            }
        }
        assertEquals 1, i
    }
}