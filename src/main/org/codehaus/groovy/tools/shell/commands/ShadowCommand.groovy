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

package org.codehaus.groovy.tools.shell.commands

import org.codehaus.groovy.tools.shell.ComplexCommandSupport
import org.codehaus.groovy.tools.shell.Shell

/**
 * The 'shadow' command.
 *
 * @version $Id$
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
class ShadowCommand
    extends ComplexCommandSupport
{
    ShadowCommand(final Shell shell) {
        super(shell, 'shadow', '\\&')
        
        this.functions = [ 'debug', 'verbose', 'this' ]
        
        //
        // NOTE: For now don't hid this guy
        //
        
        // this.hidden = true
    }
    
    def do_debug = {
        def flag = !log.debug
        
        io.out.println("Toggling logging debug to: $flag")
        
        log.debug = flag
    }
    
    def do_verbose = {
        def flag = !io.verbose
        
        io.out.println("Toggling logging verbose to: $flag")
        
        io.verbose = flag
    }
    
    def do_this = {
        return this
    }
}

