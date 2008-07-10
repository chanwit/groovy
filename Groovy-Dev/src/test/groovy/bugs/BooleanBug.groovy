package groovy.bugs

/**
 * @version $Revision$
 */
class BooleanBug extends GroovyTestCase {
    
    void testBug() {
        def x = new BooleanBean(name:'James', foo:true)
        def y = new BooleanBean(name:'Bob', foo:false)

        assert x.foo
        assert ! y.foo
        y.foo = true
        assert y.foo
    }
    
    void testBug2() {
        BooleanBean bean = new BooleanBean(name:'Gromit', foo:false)
        def value = isApplicableTo(bean)
        assert value
    }
    
    public boolean isApplicableTo(BooleanBean field) {
        return !field.isFoo();
    }

}

class BooleanBean {
    String name
    boolean foo
}