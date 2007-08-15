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

package org.codehaus.groovy.tools.shell

/**
 * Provides simple command aliasing.
 *
 * @version $Id$
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
class CommandAlias
    extends Command
{
    private final MessageSource messages = new MessageSource(this.class)
    
    final String target
    
    CommandAlias(final String name, final String shortcut, final String target, final Closure function) {
        super(name, shortcut, function)
        assert target
        
        this.target = target
    }
    
    String getDescription() {
        return messages.format('info.alias_to', target)
    }
    
    //
    // TODO: Would be really nice to have an execute here to handle the actual
    //       redirect... but we don't have the registry context to do it :-(
    //
}