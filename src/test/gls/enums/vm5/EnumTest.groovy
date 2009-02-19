/*
 * Copyright 2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gls.enums.vm5

/**
 * Tests various properties of enums.
 *
 * @author Paul King
 */
class EnumTest extends GroovyTestCase {

    void testValues() {
        assert UsCoin.values().size() == 4
        assert UsCoin.values().toList().sum{ it.value } == 41
    }

    void testNext() {
        def coin = UsCoin.penny
        def coins = [coin++, coin++, coin++, coin++, coin]
        assert coins == [UsCoin.penny, UsCoin.nickel, UsCoin.dime, UsCoin.quarter, UsCoin.penny]
    }

    void testPrevious() {
        def coin = UsCoin.quarter
        def coins = [coin--, coin--, coin--, coin--, coin]
        assert coins == [UsCoin.quarter, UsCoin.dime, UsCoin.nickel, UsCoin.penny, UsCoin.quarter]
    }

    void testRange() {
        def coinRange1 = UsCoin.penny..UsCoin.dime
        assert (UsCoin.nickel in coinRange1)
        assert !(UsCoin.quarter in coinRange1)
    }

    void testMinValue() {
        assert UsCoin.MIN_VALUE == UsCoin.penny
        shouldFail(MissingPropertyException) {
           EmptyEnum.MIN_VALUE
        }
    }

    void testMaxValue() {
        assert UsCoin.MAX_VALUE == UsCoin.quarter
        shouldFail(MissingPropertyException) {
           EmptyEnum.MAX_VALUE
        }
    }

    void testComparators() {
        assert UsCoin.nickel <=> UsCoin.penny  ==  1
        assert UsCoin.nickel <=> UsCoin.nickel ==  0
        assert UsCoin.nickel <=> UsCoin.dime   == -1
        assert UsCoin.nickel <=  UsCoin.nickel
        assert UsCoin.nickel <=  UsCoin.dime
        assert UsCoin.nickel >=  UsCoin.penny
        assert UsCoin.nickel >=  UsCoin.nickel
    }

    void testStepWithRange() {
        def coinRange2 = UsCoin.nickel..UsCoin.quarter
        def coins = coinRange2.toList()
        assert coins == [UsCoin.nickel, UsCoin.dime, UsCoin.quarter]
        coins = coinRange2.step(2)
        assert coins == [UsCoin.nickel, UsCoin.quarter]
        coins = coinRange2.step(3)
        assert coins == [UsCoin.nickel]
        coins = coinRange2.step(4)
        assert coins == [UsCoin.nickel]
        coins = coinRange2.step(-1)
        assert coins == [UsCoin.quarter, UsCoin.dime, UsCoin.nickel]
        coins = coinRange2.step(-2)
        assert coins == [UsCoin.quarter, UsCoin.nickel]
        coins = coinRange2.step(-3)
        assert coins == [UsCoin.quarter]
        coins = coinRange2.step(-4)
        assert coins == [UsCoin.quarter]
    }

    void testStepWithReverseRange() {
        def coinRange2 = UsCoin.quarter..UsCoin.nickel
        def coins = coinRange2.toList()
        assert coins == [UsCoin.quarter, UsCoin.dime, UsCoin.nickel]
        coins = coinRange2.step(2)
        assert coins == [UsCoin.quarter, UsCoin.nickel]
        coins = coinRange2.step(3)
        assert coins == [UsCoin.quarter]
        coins = coinRange2.step(4)
        assert coins == [UsCoin.quarter]
        coins = coinRange2.step(-1)
        assert coins == [UsCoin.nickel, UsCoin.dime, UsCoin.quarter]
        coins = coinRange2.step(-2)
        assert coins == [UsCoin.nickel, UsCoin.quarter]
        coins = coinRange2.step(-3)
        assert coins == [UsCoin.nickel]
        coins = coinRange2.step(-4)
        assert coins == [UsCoin.nickel]
    }

    void testEnumWithSingleListInConstructor() {
        def sh = new GroovyShell();
        def enumStr;
        
        enumStr = """
            enum ListEnum1 {
                ONE([111, 222])
                ListEnum1(Object listArg){
                    assert listArg == [111, 222]
                    assert listArg instanceof java.util.ArrayList
                }
            }
            println ListEnum1.ONE
        """
        sh.evaluate(enumStr);
            
        enumStr = """
            enum ListEnum2 {
                TWO([234, [567,12]])
                ListEnum2(Object listArg){
                    assert listArg == [234, [567, 12]]
                    assert listArg instanceof java.util.ArrayList
                }
            }
            println ListEnum2.TWO
        """
        sh.evaluate(enumStr);
    }
    
    void testSingleListDoesNoInfluenceMaps() {
        // the fix for GROOVY-2933 caused map["taku"]
        // to become map[(["take])] instead. -> GROOVY-3214
        assertScript """
            public enum FontFamily {
                ARIAL
            
                static public void obtainMyMap()
                {
                    Map map = [:]
                    map["taku"] = "dio"
                    assert map.taku == "dio"
                }
            }
            FontFamily.obtainMyMap()
        """
    }
 
    void testMutipleValuesDontGetWronglyWrappedInList() {
        // the fix for GROOVY-3214 caused multiple values passed in an enum const
        // to get wrapped in an extra ListExpression. -> GROOVY-3276
        assertScript """
            enum GROOVY3276 {
               A(1,2), B(3,4)
           
               GROOVY3276(int xx, int yy) { 
                   x=xx 
                   y=yy 
               }
               public int x
               public int y
           }
           
           assert GROOVY3276.A.x == 1
           assert GROOVY3276.B.y == 4
        """
    }

    // the fix for GROOVY-3161
    def void testStaticEnumFieldWithEnumValues() {
    	def allColors = GroovyColors3161.ALL_COLORS
    	assert allColors.size == 3
    	assert allColors[0] == GroovyColors3161.red
    	assert allColors[1] == GroovyColors3161.blue
    	assert allColors[2] == GroovyColors3161.green
    }

    // the fix for GROOVY-3283
    def void testImportStaticMoreThanOneEnum() {
        assertScript """
            enum Foo3283 { A,B }
            enum Bar3283 { X,Y }
            
            import static Foo3283.*
            import static Bar3283.*
            
            a = A
            x = X
	    """
    }

    void testCallBehaviorOnEnumForGROOVY3284() {
    	// test the usage in a non-script class first
        for (f in Foo3284) {
        	assert f() == "A"
        }
        assert Foo3284.A.call() == "A"
        assert Foo3284.A() == "A"
        def a = Foo3284.A
        assert a() == "A"

        // now test the usage in a script but this time type Closure not specified explicitly
        assertScript """
			enum Foo32842 {
			    B({ "B" })
			    Foo32842(c) {
			      call = c
			    }
			    def call
			}
	        for (f in Foo32842) {
	            assert f() == "B"
	        }
	
	        assert Foo32842.B.call() == "B"
	
	        assert Foo32842.B() == "B"
			b = Foo32842.B
			assert b() == "B"
        """
    }
}

enum UsCoin {
    penny(1), nickel(5), dime(10), quarter(25)
    UsCoin(int value) { this.value = value }
    private final int value
    int getValue() { value }
}

enum EmptyEnum{}

enum GroovyColors3161 {
	red, blue, green
	static def ALL_COLORS = [red, blue, green]
}

enum Foo3284 {
    A({ "A" })
    Foo3284(Closure c) {
      call = c
    }
    final Closure call
}