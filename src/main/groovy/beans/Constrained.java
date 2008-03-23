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

package groovy.beans;

import org.codehaus.groovy.ast.GroovyASTTransformation;
import org.codehaus.groovy.control.Phases;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a groovy property, and indicates that it should be a constrained
 * property according to the JavaBeans spec, subject to listeners vetoing
 * the property change.
 *
 * It is a compilation error to place this annotation on a field (that is
 * nota property, i.e. has scope visibility modifiers).
 *
 * It is a compilation error to place this annotation on a property with
 * a user defined settter.
 *
 * @see ConstrainedASTTransformation
 *
 * @author Danno Ferrin (shemnon)
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
@GroovyASTTransformation(transformationClassName = "groovy.beans.ConstrainedASTTransformation", phase = Phases.CANONICALIZATION)
public @interface Constrained {
}