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
package org.codehaus.groovy;

public class GroovyException extends Exception implements GroovyExceptionInterface {
    private boolean fatal = true;

    public GroovyException() {
    }

    public GroovyException(String message) {
        super(message);
    }

    public GroovyException(String message, Throwable cause) {
        super(message, cause);
    }

    public GroovyException(boolean fatal) {
        super();
        this.fatal = fatal;
    }

    public GroovyException(String message, boolean fatal) {
        super(message);
        this.fatal = fatal;
    }

    public boolean isFatal() {
        return fatal;
    }

    public void setFatal(boolean fatal) {
        this.fatal = fatal;
    }
}
