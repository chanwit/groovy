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

package groovy.security;

import java.security.BasicPermission;

/**
 * Permission required to explicitly specify a codebase for a groovy script whose
 * codebase cannot be determined.  Typically this permission is only
 * required by clients that want to associate a code source with a script which
 * is a String or an InputStream.
 *
 * @author Steve Goetze
 */
public final class GroovyCodeSourcePermission extends BasicPermission {

	public GroovyCodeSourcePermission(String name) {
		super(name);
	}

	public GroovyCodeSourcePermission(String name, String actions) {
		super(name, actions);
	}
}
