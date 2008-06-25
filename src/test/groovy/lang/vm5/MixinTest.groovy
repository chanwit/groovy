package groovy.lang.vm5

import java.util.concurrent.locks.ReentrantLock

class MixinTest extends GroovyTestCase {

    protected void setUp() {
    }

    protected void tearDown() {
        ArrayList.metaClass = null
        List.metaClass = null
    }

    void testOneClass () {
        List.mixin ListExt
        ArrayList.mixin ArrayListExt
        assertEquals 1, [0,1].swap () [0]
        assertEquals 0, [0,1].swap () [1]
        assertEquals 0, [0,1].swap ().unswap () [0]
        assertEquals 1, [0,1].swap ().unswap () [1]
    }

    void testWithList () {
        ArrayList.mixin ArrayListExt, ListExt
        assertEquals 1, [0,1].swap () [0]
        assertEquals 0, [0,1].swap () [1]
        assertEquals 0, [0,1].swap ().unswap () [0]
        assertEquals 1, [0,1].swap ().unswap () [1]
    }

    void testCombined () {
        ArrayList.mixin Combined
        assertEquals 1, [0,1].swap () [0]
        assertEquals 0, [0,1].swap () [1]
        assertEquals 0, [0,1].swap ().unswap () [0]
        assertEquals 1, [0,1].swap ().unswap () [1]
    }

    void testWithEmc () {
        ArrayList.metaClass.unswap = {
            [delegate[1], delegate[0]]
        }
        ArrayList.mixin ArrayListExt
        assertEquals 1, [0,1].swap () [0]
        assertEquals 0, [0,1].swap () [1]
        assertEquals 0, [0,1].swap ().unswap () [0]
        assertEquals 1, [0,1].swap ().unswap () [1]
    }

    void testGroovyObject () {
        def obj = new ObjToTest ()
        assertEquals "original", obj.value
        obj.metaClass.mixin ObjToTestCategory
        assertEquals "changed by category", obj.value
        assertEquals "original", new ObjToTest ().value
    }

    void testGroovyObjectWithEmc () {
        ObjToTest.metaClass.getValue = { ->
            "emc changed"
        }
        ObjToTest obj = new ObjToTest ()
        assertEquals "emc changed", obj.getValue()
        obj.metaClass.mixin ObjToTestCategory
        assertEquals "changed by category", obj.value
        assertEquals "emc changed", new ObjToTest ().value
    }

    void testFlatten () {
        Object.metaClass.mixin DeepFlattenToCategory
        assertEquals ([8,9,3,2,1,4], [[8,9] as Object [], [3,2,[2:1,3:4]],[2,3]].flattenTo () as List)

        def x = [-2,-2,-3,-3]
        x.metaClass.mixin NoFlattenArrayListCategory
        assertEquals ([x, 8,9,3,2,1,4], [x, [8,9] as Object [], [3,2,[2:1,3:4]],[2,3]].flattenTo () as List)

        x.metaClass = null
        x.metaClass.flattenTo = { Set set -> set << delegate }
        assertEquals ([x, 8,9,3,2,1,4], [x, [8,9] as Object [], [3,2,[2:1,3:4]],[2,3]].flattenTo () as List)

        x.metaClass = null
        Object.metaClass.flattenTo(ArrayList){ ->
            LinkedHashSet set = new LinkedHashSet()
            delegate.flattenTo(set)
            return set
        }
        Object.metaClass.flattenTo(ArrayList){ Set set ->
            set << "oops"
            return Collection.metaClass.invokeMethod(delegate, "flattenTo", set)
        }
        assertEquals (["oops", -2, -3, 8,9,3,2,1,4], [x, [8,9] as Object [], [3,2,[2:1,3:4]],[2,3]].flattenTo () as List)

        Object.metaClass{
            flattenTo(ArrayList) { ->
                LinkedHashSet set = new LinkedHashSet()
                delegate.flattenTo(set)
                return set
            }

            flattenTo(ArrayList) { Set set ->
                set << "oopsssss"
                return Collection.metaClass.invokeMethod(delegate, "flattenTo", set)
            }

            asList { ->
                delegate as List
            }
        }
        assertEquals (["oopsssss", -2, -3, 8,9,3,2,1,4], [x, [8,9] as Object [], [3,2,[2:1,3:4]],[2,3]].flattenTo ().asList() )

        Object.metaClass{
            define(ArrayList) {
                flattenTo{ ->
                    LinkedHashSet set = new LinkedHashSet()
                    delegate.flattenTo(set)
                    return set
                }

                flattenTo{ Set set ->
                    set << "ssoops"
                    return Collection.metaClass.invokeMethod(delegate, "flattenTo", set)
                }
            }

            asList { ->
                delegate as List
            }
        }
        assertEquals (["ssoops", -2, -3, 8,9,3,2,1,4], [x, [8,9] as Object [], [3,2,[2:1,3:4]],[2,3]].flattenTo ().asList() )

        Object.metaClass = null
    }

    void testMixingLockable () {
        Object.metaClass.mixin ReentrantLock
        def name = "abcdef"
        name.lock ()
        try {
            assertTrue name.isLocked ()
        }
        finally {
            name.unlock ()
        }
        Object.metaClass = null
    }
}

class ArrayListExt {
    static def swap (ArrayList self) {
        [self[1], self[0]]
    }
}

class ListExt {
    static def unswap (List self) {
        [self[1], self[0]]
    }
}

class Combined {
    static def swap (ArrayList self) {
        [self[1], self[0]]
    }

    static def unswap (List self) {
        [self[1], self[0]]
    }
}

class ObjToTest {
    def getValue () {
        "original"
    }
}

class ObjToTestCategory {
    def static getValue (ObjToTest self) {
        "changed by category"
    }
}

class DeepFlattenToCategory {
    static Set flattenTo(element) {
        LinkedHashSet set = new LinkedHashSet()
        element.flattenTo(set)
        return set
    }

    // Object - put to result set
    static void flattenTo(element, Set addTo) {
        addTo << element;
    }

    // Collection - flatten each element
    static void flattenTo(Collection elements, Set addTo) {
        elements.each { element ->
           element.flattenTo(addTo);
        }
    }

    // Map - flatten each value
    static void flattenTo(Map elements, Set addTo) {
       elements.values().flattenTo(addTo);
    }

    // Array - flatten each element
    static void flattenTo(Object [] elements, Set addTo) {
        elements.each { element ->
           element.flattenTo(addTo);
        }
    }
}

class NoFlattenArrayListCategory {
    // Object - put to result set
    static void flattenTo(ArrayList element, Set addTo) {
        addTo << element;
    }
}