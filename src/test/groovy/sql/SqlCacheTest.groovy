package groovy.sql

import java.sql.SQLException
import javax.sql.DataSource
import java.sql.Connection
import org.codehaus.groovy.runtime.InvokerHelper

/**
 * Unit test of Sql cache feature 
 * @author Marc DeXeT
 * @author Paul King
 */
class SqlCacheTest extends GroovyTestCase {
    Sql sql
    Connection wrappedCon
    Connection con
    DataSource ds
    int prepareStatementExpectedCall
    int prepareStatementCallCounter
    int createStatementExpectedCall
    int createStatementCallCounter

    void setUp() {
        ds = new org.hsqldb.jdbc.jdbcDataSource()
        ds.database = "jdbc:hsqldb:mem:foo" + getMethodName()
        ds.user = 'sa'
        ds.password = ''
        con = ds.getConnection()
        def methodOverride = [
                createStatement: {Object[] args ->
                    createStatementCallCounter++
                    assert !createStatementExpectedCall || createStatementCallCounter <= createStatementExpectedCall
                    InvokerHelper.invokeMethod(con, 'createStatement', args)
                },
                prepareStatement: {String sql ->
                    prepareStatementCallCounter++
                    assert !prepareStatementExpectedCall || prepareStatementCallCounter <= prepareStatementExpectedCall
                    con.prepareStatement(sql)
                }
        ]
        wrappedCon = ProxyGenerator.INSTANCE.instantiateDelegate(methodOverride, [Connection], con)
        sql = new Sql(wrappedCon)
        sql.execute("create table PERSON ( id integer, firstname varchar, lastname varchar )")
        sql.execute("create table FOOD ( id integer, type varchar, name varchar)")
        sql.execute("create table PERSON_FOOD ( personid integer, foodid integer)")

        // now let's populate the datasets
        def people = sql.dataSet("PERSON")
        people.add(id: 1, firstname: "James", lastname: "Strachan")
        people.add(id: 2, firstname: "Bob", lastname: "Mcwhirter")
        people.add(id: 3, firstname: "Sam", lastname: "Pullara")
        people.add(id: 4, firstname: "Jean", lastname: "Gabin")
        people.add(id: 5, firstname: "Lino", lastname: "Ventura")

        def food = sql.dataSet("FOOD")
        food.add(id: 1, type: "cheese", name: "edam")
        food.add(id: 2, type: "cheese", name: "brie")
        food.add(id: 3, type: "cheese", name: "cheddar")
        food.add(id: 4, type: "drink", name: "beer")
        food.add(id: 5, type: "drink", name: "coffee")

        def person_food = sql.dataSet("PERSON_FOOD")
        person_food.add(personid: 1, foodid: 1)
        person_food.add(personid: 1, foodid: 4)
        person_food.add(personid: 2, foodid: 2)
        person_food.add(personid: 3, foodid: 5)
        person_food.add(personid: 4, foodid: 1)
        person_food.add(personid: 4, foodid: 2)
        person_food.add(personid: 4, foodid: 3)
        person_food.add(personid: 99, foodid: 99)

        prepareStatementCallCounter = 0
    }

    /**
     * Validation of ConnectionWrapper expectation.
     */
    void testValidateWrappedConnectionStatementCall() {
        prepareStatementCallCounter = 0
        prepareStatementExpectedCall = 1
        try {
            wrappedCon.prepareStatement("SELECT * FROM PERSON")
            wrappedCon.prepareStatement("SELECT * FROM PERSON")
            fail("Exception must be raised")
        } catch (AssertionError e) {
            assert prepareStatementCallCounter == 2
        }
    }

    /**
     * Test with saveOn
     * Statements are prepared only if need be.
     * There's 3 different statements and only 3 calls to prepareStatement
     */
    void testCachePreparedStatements() {
        prepareStatementCallCounter = 0
        prepareStatementExpectedCall = 3
        sql.cacheStatements {
            sql.eachRow("SELECT * FROM PERSON", []) { r ->
                sql.eachRow("SELECT * FROM PERSON_FOOD WHERE personid = ?", [r["id"]]) { pf ->
                    sql.firstRow("SELECT * FROM FOOD WHERE id = ?", [pf["foodid"]])
                }
            }
        }
        assert prepareStatementCallCounter == 3
    }

    /**
     * test without saveOn.
     * Statements are prepared each time.
     * There's 3 different statements but more than 3 calls to
     * prepareStatement
     */
    void testNotCacheStatements() {
        prepareStatementCallCounter = 0
        sql.eachRow("SELECT * FROM PERSON", []) {r ->
            sql.eachRow("SELECT * FROM PERSON_FOOD WHERE personid = ?", [r["id"]]) {pf ->
                sql.firstRow("SELECT * FROM FOOD WHERE id = ?", [pf["foodid"]])
            }
        }
        assert prepareStatementCallCounter == 13
    }

    /**
     * We here use a wrapper for counting java.sql.Connection.prepareStatement(java.lang.String)
     * calls.
     * When caching is on, same request must not cause a new prepareStatement call :
     * prepareStatementCallCounter must not be increased.
     *
     * When caching is off, same request causes a new prepareStatement call :
     * prepareStatementCallCounter must be increased.
     *
     */
    void testCaching() {
        sql.cacheStatements = true
        sql.firstRow("SELECT * FROM PERSON WHERE lastname NOT like ? ", ['%a%'])
        assert prepareStatementCallCounter == 1
        sql.firstRow("SELECT * FROM PERSON WHERE lastname NOT like ? ", ['%a%'])
        assert prepareStatementCallCounter == 1
        sql.firstRow("SELECT * FROM FOOD WHERE id = ?", [3])
        assert prepareStatementCallCounter == 2
        sql.firstRow("SELECT * FROM PERSON WHERE lastname NOT like ? ", ['%a%'])
        assert prepareStatementCallCounter == 2

        // Stop caching
        sql.cacheStatements = false
        sql.firstRow("SELECT * FROM PERSON WHERE lastname NOT like ? ", ['%a%'])
        assert prepareStatementCallCounter == 3
        sql.firstRow("SELECT * FROM PERSON WHERE lastname NOT like ? ", ['%a%'])
        assert prepareStatementCallCounter == 4

        // Statements
        sql.cacheStatements = true
        createStatementCallCounter = 0
        sql.firstRow("SELECT * FROM PERSON")
        assert createStatementCallCounter == 1
        sql.firstRow("SELECT * FROM PERSON")
        assert createStatementCallCounter == 1
    }

    /**
     * @see #testCaching()
     */
    void testNoCaching() {
        // preparedStatements
        sql.firstRow("SELECT * FROM PERSON WHERE lastname NOT like ? ", ['%a%'])
        assert prepareStatementCallCounter == 1
        sql.firstRow("SELECT * FROM PERSON WHERE lastname NOT like ? ", ['%a%'])
        assert prepareStatementCallCounter == 2

        // Statements
        createStatementCallCounter = 0
        sql.firstRow("SELECT * FROM PERSON")
        assert createStatementCallCounter == 1
        sql.firstRow("SELECT * FROM PERSON")
        assert createStatementCallCounter == 2
    }

    /**
     * When caching is on, data source connection must be kept and not released.
     * Use a wrapped delegate for counting javax.sql.DataSource.getConnection() calls.
     * When caching is off, javax.sql.DataSource.getConnection() must be called each time.
     */
    void testCachingWithDataSource() {
        def connectionCallNumber = 0
        def methodOverride = [getConnection:{connectionCallNumber++; ds.getConnection()}]
        DataSource wrappedDs = ProxyGenerator.INSTANCE.instantiateDelegate(methodOverride, [DataSource], ds)
        sql = new Sql(wrappedDs)
        sql.cacheStatements = true
        sql.firstRow("SELECT * FROM PERSON WHERE lastname NOT like ? ", ['%a%'])
        assert connectionCallNumber == 1
        sql.firstRow("SELECT * FROM PERSON WHERE lastname NOT like ? ", ['%a%'])
        assert connectionCallNumber == 1
        sql.firstRow("SELECT * FROM FOOD WHERE id = ?", [3])
        assert connectionCallNumber == 1
        sql.firstRow("SELECT * FROM PERSON WHERE lastname NOT like ? ", ['%a%'])
        // Stop caching
        sql.cacheStatements = false
        sql.firstRow("SELECT * FROM PERSON WHERE lastname NOT like ? ", ['%a%'])
        assert connectionCallNumber == 2
        sql.firstRow("SELECT * FROM PERSON WHERE lastname NOT like ? ", ['%a%'])
        assert connectionCallNumber == 3
    }

    /**
     * Tests #1 Exception is not swallowed
     */
    void testException() {
        try {
            sql.cacheStatements {
                sql.eachRow("SELECT * FROM PERSON", []) {
                    throw new Exception('test.exception')
                }
            }
            fail('Exception must be raised !')
        } catch (Exception e) {
            assert e.message == 'test.exception'
            assert !sql.cacheStatements
        }
    }

    /**
     * Tests #1 SQLException is not swallowed
     */
    void testSQLException() {
        try {
            sql.cacheStatements {
                sql.eachRow("SELECT * FROM PERSON", []) {
                    throw new SQLException('test.exception')
                }
            }
            fail('Exception must be raised !')
        } catch (Exception e) {
            assert e.message == 'test.exception'
            assert !sql.cacheStatements
        }
    }

}
