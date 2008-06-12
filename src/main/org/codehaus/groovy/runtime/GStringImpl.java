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

package org.codehaus.groovy.runtime;

import groovy.lang.GString;

/**
 * Default implementation of a GString used by the compiler. A GString
 * consist of a list of values and strings which can be combined to 
 * create a new String.
 * 
 * @see groovy.lang.GString
 *  
 * @author Jochen Theodorou
 */
public class GStringImpl extends GString {
  private String[] strings;
	
   /**
    * Create a new GString with values and strings.
    * <p>
    * Each value is prefixed by a string, after the last value 
    * an additional String might be used. This means 
    * <code>strings.length==values.length  || strings.length==values.length+1</code>.
    * </p><p>
    * <b>NOTE:</b> The lengths are <b>not</b> checked. Using different lengths might result 
    * in unpredictable behaviour.
    * </p>
    *   
    * @param values the value parts
    * @param strings the string parts
    */
	public GStringImpl(Object[] values, String[] strings) {
		super(values);
		this.strings = strings;
	}
	
	/**
	 * Get the strings of this GString.
	 * <p>
	 * This methods returns the same array as used in the constructor. Changing
	 * the values will result in changes of the GString. It is not recommended 
	 * to do so.
	 * </p>
	 */
	public String[] getStrings() {
		return strings;
	}
	
}