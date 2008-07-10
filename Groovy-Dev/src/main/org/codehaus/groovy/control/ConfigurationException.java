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

package org.codehaus.groovy.control;

import org.codehaus.groovy.GroovyExceptionInterface;




/**
 *  Thrown when configuration data is invalid.
 *
 *  @author <a href="mailto:cpoirier@dreaming.org">Chris Poirier</a>
 *
 *  @version $Id$
 */

public class ConfigurationException extends RuntimeException implements GroovyExceptionInterface
{
    
  //---------------------------------------------------------------------------
  // CONSTRUCTION AND SUCH

    protected Exception cause;   // The phase in which the failures occurred

    
   /**
    *  Initializes the exception from a cause exception.
    */
    
    public ConfigurationException( Exception cause ) 
    {
        super( cause.getMessage() );
        this.cause = cause;
    }
    
    
   /**
    *  Initializes the exception with just a message.
    */
    
    public ConfigurationException( String message )
    {
        super( message );
    }

    
    
   /**
    *  Returns the causing exception, if available.
    */
    
    public Throwable getCause()
    {
        return cause;
    }
    
    
   /**
    *  Its always fatal.
    */
    
    public boolean isFatal()
    {
        return true;
    }
    
    
    
   /**
    *  Set fatal is just ignored.
    */
    
    public void setFatal( boolean fatal )
    {
    }
    
}
