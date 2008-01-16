/*
 * Copyright 2003-2008 the original author or authors.
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

import groovy.lang.IntRange;
import groovy.lang.EmptyRange;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

/**
 * Support methods for DefaultGroovyMethods and PluginDefaultMethods.
 */
public class DefaultGroovyMethodsSupport {

    // helper method for getAt and putAt
    protected static RangeInfo subListBorders(int size, IntRange range) {
        int from = DefaultGroovyMethods.normaliseIndex(DefaultTypeTransformation.intUnbox(range.getFrom()), size);
        int to = DefaultGroovyMethods.normaliseIndex(DefaultTypeTransformation.intUnbox(range.getTo()), size);
        boolean reverse = range.isReverse();
        if (from > to) {
            // support list[1..-1]
            int tmp = to;
            to = from;
            from = tmp;
            reverse = !reverse;
        }
        return new RangeInfo(from, to + 1, reverse);
    }

    // helper method for getAt and putAt
    protected static RangeInfo subListBorders(int size, EmptyRange range) {
        int from = DefaultGroovyMethods.normaliseIndex(DefaultTypeTransformation.intUnbox(range.getFrom()), size);
        return new RangeInfo(from, from, false);
    }

    protected static class RangeInfo {
        public int from, to;
        public boolean reverse;

        public RangeInfo(int from, int to, boolean reverse) {
            this.from = from;
            this.to = to;
            this.reverse = reverse;
        }
    }
}