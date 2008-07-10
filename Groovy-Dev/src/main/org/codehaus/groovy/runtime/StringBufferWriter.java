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

import java.io.IOException;
import java.io.Writer;

/**
 * This class codes around a silly limiation of StringWriter which doesn't allow a StringBuffer
 * to be passed in as a constructor for some bizzare reason.
 * So we replicate the behaviour of StringWriter here but allow a StringBuffer to be passed in.
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class StringBufferWriter extends Writer {

    private StringBuffer buffer;

    /**
     * Create a new string writer which will append the text to the given StringBuffer
     */
    public StringBufferWriter(StringBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * Write a single character.
     */
    public void write(int c) {
        buffer.append((char) c);
    }

    /**
     * Write a portion of an array of characters.
     *
     * @param text Array of characters
     * @param offset Offset from which to start writing characters
     * @param length Number of characters to write
     */
    public void write(char text[], int offset, int length) {
        if ((offset < 0) || (offset > text.length) || (length < 0) || ((offset + length) > text.length) || ((offset + length) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        else if (length == 0) {
            return;
        }
        buffer.append(text, offset, length);
    }

    /**
     * Write a string.
     */
    public void write(String text) {
        buffer.append(text);
    }

    /**
     * Write a portion of a string.
     *
     * @param text the text to be written
     * @param offset offset from which to start writing characters
     * @param length Number of characters to write
     */
    public void write(String text, int offset, int length) {
        buffer.append(text.substring(offset, offset + length));
    }

    /**
     * Return the buffer's current value as a string.
     */
    public String toString() {
        return buffer.toString();
    }

    /**
     * Flush the stream.
     */
    public void flush() {
    }

    /**
     * Closing a <tt>StringWriter</tt> has no effect. The methods in this
     * class can be called after the stream has been closed without generating
     * an <tt>IOException</tt>.
     */
    public void close() throws IOException {
    }
}
