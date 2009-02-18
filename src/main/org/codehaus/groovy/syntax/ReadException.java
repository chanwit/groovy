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

package org.codehaus.groovy.syntax;

import org.codehaus.groovy.GroovyException;

import java.io.IOException;

/**
 * Encapsulates non-specific i/o exceptions.
 */

public class ReadException extends GroovyException {
    private final IOException cause;

    public ReadException(IOException cause) {
        super();
        this.cause = cause;
    }

    public ReadException(String message, IOException cause) {
        super(message);
        this.cause = cause;
    }

    public IOException getIOCause() {
        return this.cause;
    }

    public String toString() {
        String message = super.getMessage();
        if (message == null || message.trim().equals("")) {
            message = cause.getMessage();
        }

        return message;
    }

    public String getMessage() {
        return toString();
    }
}
