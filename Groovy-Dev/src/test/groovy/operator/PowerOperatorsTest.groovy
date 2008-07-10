package groovy.operator

/** 
 * Test Math Power Operation in Classic/New Groovy
 * 
 * @author Pilho Kim
 * @version $Revision: 4996 $
 */
class PowerOperatorsTest extends GroovyTestCase {

    void testConstantPowerOperation() {
        assert 2**5 == 32
        assert -2**5 == -32
        assert 3**4 == 81
        assert -3**4 == -81
        assert 3**-4 == 3.power(-4)
        assert -3**-4 == -3.power(-4)
        assert 7**2 - 7*3 + 2 == 30         //  49 - 21 + 2 = 30
        assert -7**2 - 7*3 + 2 == -68       // -49 - 21 + 2 = -68
        assert -(7**2) - 7*3 + 2 == -68     // -49 - 21 + 2 = -68
        assert (-7)**2 - 7*3 + 2 == 30     //  49 - 21 + 2 = 30
    }

    void testPowerOperation() {
        def x = 9
        --x
        assert x == 8
        println(--x)
        assert x == 7
        println(--x)
        assert x == 6
        println((--x)**3)
        assert x == 5
        assert (--x)**3 == 64
        assert (-x**3) == -64
        assert x == 4
        assert (++x)**3 == 125
        assert x == 5
        assert (x++)**3 == 125
        assert x == 6
        println((x++)**3)
        assert x == 7
        println(x)
        println("${x**2}")
        println("${-x**2}")
        assert x == 7
        println("${(--x)**2}")
        assert x == 6
        assert (--x)**2 + x*2 - 1 == 34      // 5**2 + 5*2 - 1 = 34
        assert x == 5
        assert (x--)**2 + x*2 - 1 == 32      // 5**2 + 4*2 - 1 = 32
        assert x == 4
    }

    void testConstantPowerAssignmentOperation() {
        def x = 5
        x **= 2
        assert x == 25
        assert x**2 == 625
        assert -x**2 != 625
        assert -x**2 == -625
    }

    void testPowerAssignmentOperation() {
        def x = 5
        def y = 2
        x **= y
        assert x == 25
        assert x**y == 625
        assert x**-1 == 1/25
        assert x**-y == 1/625
        assert x**-y == x**(-y)
    }
}