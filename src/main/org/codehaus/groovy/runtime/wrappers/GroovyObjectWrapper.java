/*
 * Copyright 2003-2007 the original author or authors.
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

package org.codehaus.groovy.runtime.wrappers;

import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;

/**
 * @author John Wilson
 *
 */

public class GroovyObjectWrapper extends Wrapper {
  protected final GroovyObject wrapped;
  
  public GroovyObjectWrapper(final GroovyObject wrapped, final Class constrainedType) {
    super(constrainedType);
    this.wrapped = wrapped;
  }
  
  public Object unwrap() {
    return this.wrapped;
  }
  
  /**
   * Note the rest of these method will only be used post 1.0
   */

  /* (non-Javadoc)
   * @see groovy.lang.GroovyObject#getProperty(java.lang.String)
   */
  public Object getProperty(final String property) {
    return this.wrapped.getProperty(property);
  }

  /* (non-Javadoc)
   * @see groovy.lang.GroovyObject#invokeMethod(java.lang.String, java.lang.Object)
   */
  public Object invokeMethod(final String name, final Object args) {
    return this.wrapped.invokeMethod(name, args);
  }

  /* (non-Javadoc)
   * @see groovy.lang.GroovyObject#setMetaClass(groovy.lang.MetaClass)
   */
  public void setMetaClass(final MetaClass metaClass) {
    this.wrapped.setMetaClass(metaClass);
  }

  /* (non-Javadoc)
   * @see groovy.lang.GroovyObject#setProperty(java.lang.String, java.lang.Object)
   */
  public void setProperty(final String property, final Object newValue) {
    this.wrapped.setProperty(property, newValue);
  }

  protected Object getWrapped() {
    return this.wrapped;
  }

  protected MetaClass getDelegatedMetaClass() {
    return this.wrapped.getMetaClass();
  }
}
