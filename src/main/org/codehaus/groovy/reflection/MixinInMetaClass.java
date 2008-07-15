package org.codehaus.groovy.reflection;

import groovy.lang.*;

import java.lang.reflect.Modifier;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.codehaus.groovy.runtime.MetaClassHelper;
import org.codehaus.groovy.runtime.HandleMetaClass;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.metaclass.NewInstanceMetaMethod;
import org.codehaus.groovy.runtime.metaclass.MixedInMetaClass;
import org.codehaus.groovy.runtime.metaclass.MixinInstanceMetaMethod;
import org.codehaus.groovy.runtime.metaclass.MixinInstanceMetaProperty;
import org.codehaus.groovy.util.ConcurrentWeakMap;

public class MixinInMetaClass extends ConcurrentWeakMap {
    final ExpandoMetaClass emc;
    final CachedClass mixinClass;
    final CachedConstructor constructor;

    public MixinInMetaClass(ExpandoMetaClass emc, CachedClass mixinClass) {
        this.emc = emc;
        this.mixinClass = mixinClass;

        constructor = findDefaultConstructor(mixinClass);
        emc.addMixinClass(this);
    }

    private CachedConstructor findDefaultConstructor(CachedClass mixinClass) {
        for(CachedConstructor constr : mixinClass.getConstructors()) {
            if (!Modifier.isPublic(constr.getModifiers()))
              continue;

            CachedClass[] classes = constr.getParameterTypes();
            if (classes.length == 0)
               return constr;
        }

        throw new GroovyRuntimeException("No default constructor for class " + mixinClass.getName() + "! Can't be mixed in.");
    }

    public synchronized Object getMixinInstance (Object object) {
        Object mixinInstance = get(object);
        if (mixinInstance == null) {
            mixinInstance = constructor.invoke(MetaClassHelper.EMPTY_ARRAY);
            new MixedInMetaClass(mixinInstance, object);
            put (object, mixinInstance);
        }
        return mixinInstance;
    }

    public synchronized void setMixinInstance (Object object, Object mixinInstance) {
        if (mixinInstance == null) {
            remove(object);
        }
        else {
          put (object, mixinInstance);
        }
    }

    public CachedClass getInstanceClass() {
        return emc.getTheCachedClass();
    }

    public CachedClass getMixinClass() {
        return mixinClass;
    }

    public static void mixinClassesToMetaClass(MetaClass self, List<Class> categoryClasses) {
        final Class selfClass = self.getTheClass();

        if (self instanceof HandleMetaClass) {
            self = (MetaClass) ((HandleMetaClass)self).replaceDelegate();
        }

        if (!(self instanceof ExpandoMetaClass)) {
            if (self instanceof DelegatingMetaClass && ((DelegatingMetaClass) self).getAdaptee() instanceof ExpandoMetaClass) {
                self = ((DelegatingMetaClass) self).getAdaptee();
            } else {
                throw new GroovyRuntimeException("Can't mixin methods to meta class: " + self);
            }
        }

        ExpandoMetaClass mc = (ExpandoMetaClass)self;

        ArrayList<MetaMethod> arr = new ArrayList<MetaMethod> ();
        for (Class categoryClass : categoryClasses) {

            final CachedClass cachedCategoryClass = ReflectionCache.getCachedClass(categoryClass);
            final MixinInMetaClass mixin = new MixinInMetaClass(mc, cachedCategoryClass);

            final MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(categoryClass);
            final List<MetaProperty> propList = (List<MetaProperty>) metaClass.getProperties();
            for (MetaProperty prop : propList)
              if (self.getMetaProperty(prop.getName()) == null) {
                  mc.registerBeanProperty(prop.getName(), new MixinInstanceMetaProperty(prop, mixin));
              }

            for (MetaProperty prop : cachedCategoryClass.getFields())
              if (self.getMetaProperty(prop.getName()) == null) {
                  mc.registerBeanProperty(prop.getName(), new MixinInstanceMetaProperty(prop, mixin));
              }

            for (MetaMethod method : (List<MetaMethod>)metaClass.getMethods()) {
                final int mod = method.getModifiers();

                if (!Modifier.isPublic(mod))
                   continue;

                if (method instanceof CachedMethod && ((CachedMethod)method).getCachedMethod().isSynthetic())
                  continue;

                if (Modifier.isStatic(mod)) {
                    if (method instanceof CachedMethod)
                      staticMethod(self, arr, (CachedMethod) method);
                }
                else {
//                    if(self.pickMethod(method.getName(), method.getNativeParameterTypes()) == null) {
                        final MixinInstanceMetaMethod metaMethod = new MixinInstanceMetaMethod(method, mixin);
                        arr.add(metaMethod);
//                    }
                }
            }
        }

        for (Object res : arr) {
            final MetaMethod metaMethod = (MetaMethod) res;
            if (metaMethod.getDeclaringClass().isAssignableFrom(selfClass))
              mc.registerInstanceMethod(metaMethod);
            else {
              mc.registerSubclassInstanceMethod(metaMethod);
            }
        }
    }

    private static void staticMethod(final MetaClass self, ArrayList<MetaMethod> arr, final CachedMethod method) {
        CachedClass[] paramTypes = method.getParameterTypes();

        if (paramTypes.length == 0)
            return;

        NewInstanceMetaMethod metaMethod;
        if (paramTypes[0].isAssignableFrom(self.getTheClass())) {
            if (paramTypes[0].getTheClass() == self.getTheClass())
                metaMethod = new NewInstanceMetaMethod(method);
            else
                metaMethod = new NewInstanceMetaMethod(method) {
                    public CachedClass getDeclaringClass() {
                        return ReflectionCache.getCachedClass(self.getTheClass());
                    }
                };
            arr.add(metaMethod);
        }
        else {
            if (self.getTheClass().isAssignableFrom(paramTypes[0].getTheClass())) {
                metaMethod = new NewInstanceMetaMethod(method);
                arr.add(metaMethod);
            }
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MixinInMetaClass)) return false;
        if (!super.equals(o)) return false;

        MixinInMetaClass that = (MixinInMetaClass) o;

        if (mixinClass != null ? !mixinClass.equals(that.mixinClass) : that.mixinClass != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (emc != null ? emc.hashCode() : 0);
        result = 31 * result + (mixinClass != null ? mixinClass.hashCode() : 0);
        result = 31 * result + (constructor != null ? constructor.hashCode() : 0);
        return result;
    }
}
