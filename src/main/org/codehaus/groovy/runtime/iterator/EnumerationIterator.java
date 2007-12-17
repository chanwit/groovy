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

package org.codehaus.groovy.runtime.iterator;

import java.util.Enumeration;

/**
 * Iterator on top of Enumeration
 */
public class EnumerationIterator extends AbstractIterator {
    protected final Enumeration delegate;

    public EnumerationIterator(Enumeration delegate) {
        this.delegate = delegate;
    }

    protected void doCheckNext() {
        hasNext = delegate.hasMoreElements();
        if (!hasNext)
            return;
        lastAcquired = delegate.nextElement();
    }
}