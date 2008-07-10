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

import java.util.Map.Entry;

/**
 * Represents a property in an Expando object
 * 
 * @author John Stump
 * @version $Revision$
 */
public class MetaExpandoProperty extends MetaProperty {

	Object value = null;
	
    public MetaExpandoProperty(Entry entry) {
		super((String) entry.getKey(), Object.class);
		
		value = entry.getValue();
    }

    /**
     * @return the property of the given object
     * @throws Exception if the property could not be evaluated
     */
    public Object getProperty(Object object) {
		return value;
	}

    /**
     * Sets the property on the given object to the new value
     * 
     * @param object on which to set the property
     * @param newValue the new value of the property
     */
    public void setProperty(Object object, Object newValue) {
		value = newValue;
	}
}
