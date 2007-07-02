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
package groovy.lang;


/**
 * An exception occurred if a dynamic property dispatch fails with an unknown property.
 * 
 * Note that the Missing*Exception classes were named for consistency and
 * to avoid conflicts with JDK exceptions of the same name.
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class MissingPropertyException extends GroovyRuntimeException {

    private String property;
    private Class type;

    public MissingPropertyException(String property, Class type) {
        super("No such property: " + property + " for class: " + type.getName());
        this.property = property;
        this.type = type;
    }

    public MissingPropertyException(String property, Class type, Throwable e) {
        super("No such property: " + property + " for class: " + type.getName() + ". Reason: " + e, e);
        this.property = property;
        this.type = type;
    }

    public MissingPropertyException(String message, String property, Class type) {
        super(message);
        this.property = property;
        this.type = type;
    }

    /**
     * @return the name of the property that could not be found
     */
    public String getProperty() {
        return property;
    }

    /**
     * 
     * @return The type on which the property was attempted to be called
     */
    public Class getType() {
        return type;
    }
}
