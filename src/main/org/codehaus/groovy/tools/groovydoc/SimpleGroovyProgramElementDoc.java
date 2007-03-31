/*
 *
 * Copyright 2007 Jeremy Rayner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.codehaus.groovy.tools.groovydoc;
import org.codehaus.groovy.groovydoc.*;

public class SimpleGroovyProgramElementDoc extends SimpleGroovyDoc implements GroovyProgramElementDoc {

	public SimpleGroovyProgramElementDoc(String name) {
		super(name);
	}
	
	//	public GroovyAnnotationDesc[] annotations() {/*todo*/return null;}
	public GroovyClassDoc containingClass() {/*todo*/return null;}
	public GroovyPackageDoc containingPackage() {/*todo*/return null;}
	public boolean isFinal() {/*todo*/return false;}
	public boolean isPackagePrivate() {/*todo*/return false;}
	public boolean isPrivate() {/*todo*/return false;}
	public boolean isProtected() {/*todo*/return false;}
	public boolean isPublic() {/*todo*/return false;}
	public boolean isStatic() {/*todo*/return false;}
	public String modifiers() {/*todo*/return null;}
	public int modifierSpecifier() {/*todo*/return 0;}
	public String qualifiedName() {/*todo*/return null;}
}