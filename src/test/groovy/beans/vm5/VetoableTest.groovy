/*
 * Copyright 2008 the original author or authors.
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

package groovy.beans.vm5

import org.codehaus.groovy.control.CompilationFailedException

/**
 * @author Danno Ferrin (shemnon)
 */
class VetoableTest extends GroovyTestCase {

    public void testSimpleConstrainedProperty() {
        GroovyShell shell = new GroovyShell()
        shell.evaluate("""
            import groovy.beans.Vetoable

            class VetoableTestBean1 {
                @Vetoable String name
            }

            sb = new VetoableTestBean1()
            sb.name = "foo"
            changed = false
            sb.vetoableChange = { pce ->
                if (changed) {
                    throw new java.beans.PropertyVetoException("Twice, even!", pce)
                } else {
                    changed = true
                }
            }
            sb.name = "foo"
            sb.name = "bar"
            try {
                sb.name = "baz"
                changed = false
            } catch (java.beans.PropertyVetoException pve) {
                // yep, we were vetoed
            }
        """)
        assert shell.changed
    }

    public void testBindableVetoableProperty() {
        GroovyShell shell = new GroovyShell()
        shell.evaluate("""
            import groovy.beans.Bindable
            import groovy.beans.Vetoable

            class VetoableTestBean2 {
                @Bindable @Vetoable String name
            }

            sb = new VetoableTestBean2()
            sb.name = "foo"
            vetoCheck = false
            changed = false
            sb.vetoableChange = { vetoCheck = true }
            sb.propertyChange = { changed = true }
            sb.name = "foo"
            assert !vetoCheck
            assert !changed
            sb.name = "bar"
        """)
        assert shell.changed
        assert shell.vetoCheck
    }

    public void testMultipleProperties() {
        GroovyShell shell = new GroovyShell()
        shell.evaluate("""
            import groovy.beans.Bindable
            import groovy.beans.Vetoable

            class VetoableTestBean3 {
                String u1
                @Bindable String b1
                @Vetoable String c1
                @Bindable @Vetoable String bc1
                String u2
                @Bindable String b2
                @Vetoable String c2
                @Bindable @Vetoable String bc2
            }

            sb = new VetoableTestBean3(u1:'a', b1:'b', c1:'c', bc1:'d', u2:'e', b2:'f', c2:'g', bc2:'h')
            changed = 0
            sb.vetoableChange = { changed++ }
            sb.propertyChange = { changed++ }
            sb.u1  = 'i'
            sb.b1  = 'j'
            sb.c1  = 'k'
            sb.bc1 = 'l'
            sb.u2  = 'm'
            sb.b2  = 'n'
            sb.c2  = 'o'
            sb.bc2 = 'p'
        """)
        assert shell.changed == 8
    }

    public void testExisingSetter() {
        GroovyShell shell = new GroovyShell()
        shouldFail(CompilationFailedException) {
            shell.evaluate("""
                class VetoableTestBean4 {
                    @groovy.beans.Vetoable String name
                    void setName() { }
                }
            """)
        }
    }

    public void testOnField() {
        GroovyShell shell = new GroovyShell()
        shouldFail(CompilationFailedException) {
            shell.evaluate("""
                class VetoableTestBean5 {
                    public @groovy.beans.Vetoable String name
                    void setName() { }
                }
            """)
        }
    }

    public void testInheritance() {
        for (int i = 0; i < 15; i++) {
            boolean bindParent = i & 1
            boolean bindChild  = i & 2
            boolean vetoParent = i & 4
            boolean vetoChild  = i & 8
            int count = (bindParent?1:0) + (bindChild?1:0) + (vetoParent?1:0) + (vetoChild?1:0)
            String script = """
                    import groovy.beans.Bindable
                    import groovy.beans.Vetoable

                    class InheritanceParentBean$i {
                        ${bindParent?'@Bindable':''} String bp
                        ${vetoParent?'@Vetoable':''} String vp
                    }

                    class InheritanceChildBean$i extends InheritanceParentBean$i {
                        ${bindChild?'@Bindable':''} String bc
                        ${vetoChild?'@Vetoable':''} String vc
                    }

                    cb = new InheritanceChildBean$i(bp:'a', vp:'b', bc:'c', vc:'d')
                    changed = 0
                    ${bindParent|bindChild?'cb.propertyChange = { changed++ }':''}
                    ${vetoParent|vetoChild?'cb.vetoableChange = { changed++ }':''}
                    cb.bp = 'e'
                    cb.vp = 'f'
                    cb.bc = 'g'
                    cb.vc = 'h'
                    assert changed == $count
                """
            try {
                GroovyShell shell = new GroovyShell()
                shell.evaluate(script);
            } catch (Throwable t) {
                System.out.println("Failed Script: $script")
                throw t
            }
        }
    }

    public void testClassMarkers() {
        for (int i = 0; i < 15; i++) {
            boolean bindField  = i & 1
            boolean bindClass  = i & 2
            boolean vetoField  = i & 4
            boolean vetoClass  = i & 8
            int vetoCount = vetoClass?4:(vetoField?2:0);
            int bindCount = bindClass?4:(bindField?2:0);

            String script = """
                    import groovy.beans.Bindable
                    import groovy.beans.Vetoable

                    ${vetoClass?'@Vetoable ':''}${bindClass?'@Bindable ':''}class ClassMarkerBean$i {
                        String neither

                        ${vetoField?'@Vetoable ':''}String veto

                        ${bindField?'@Bindable ':''}String bind

                        ${vetoField?'@Vetoable ':''}${bindField?'@Bindable ':''}String both
                    }

                    cb = new ClassMarkerBean$i(neither:'a', veto:'b', bind:'c', both:'d')
                    vetoCount = 0
                    bindCount = 0
                    ${bindClass|bindField?'cb.propertyChange = { bindCount++ }':''}
                    ${vetoClass|vetoField?'cb.vetoableChange = { vetoCount++ }':''}
                    cb.neither = 'e'
                    cb.bind = 'f'
                    cb.veto = 'g'
                    cb.both = 'h'
                    assert vetoCount == $vetoCount
                    assert bindCount == $bindCount
                """
            try {
                GroovyShell shell = new GroovyShell()
                shell.evaluate(script);
            } catch (Throwable t) {
                System.out.println("Failed Script: $script")
                throw t
            }
        }
    }
}