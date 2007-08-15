/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.groovy.tools.shell

import java.util.ResourceBundle

//
// TODO: Move to groovy.util once new groovysh bits are cleaned up and integrated
//

/**
 * Message source backed up by a {@link java.util.ResourceBundle}.
 *
 * @version $Id$
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
class MessageSource
{
    private final ResourceBundle bundle

    MessageSource(String name) {
        assert name

        bundle = ResourceBundle.getBundle(name)
    }

    MessageSource(Class type) {
        this(type.name)
    }

    MessageSource(Object obj) {
        this(obj.class)
    }
    
    String getMessage(String code) {
        assert code
        
        return bundle.getString(code)
    }

    String format(String code, Object... args) {
        assert args
        
        String pattern = getMessage(code)

        return sprintf(pattern, args)
    }

    Object getProperty(String name) {
        return getMessage(name)
    }
}