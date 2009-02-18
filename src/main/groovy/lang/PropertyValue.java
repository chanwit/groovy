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

public class PropertyValue {
	// the owner of the property
	private Object bean;
	
	// the description of the property
	private MetaProperty mp;
	
	public PropertyValue(Object bean, MetaProperty mp) {
		this.bean = bean;
		this.mp = mp;
	}
	
	public String getName() {
		return mp.getName();
	}
	
	public Class getType() {
		return mp.getType();
	}
	
	public Object getValue() {
		return mp.getProperty(bean);
	}
	
	public void setValue(Object value)  {
		mp.setProperty(bean, value);
	}
}

