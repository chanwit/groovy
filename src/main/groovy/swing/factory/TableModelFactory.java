/*
 * $Id:  $
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
 *
 */

package groovy.swing.factory;

import groovy.lang.Closure;
import groovy.model.DefaultTableModel;
import groovy.model.ValueHolder;
import groovy.model.ValueModel;
import groovy.swing.SwingBuilder;
import java.util.ArrayList;
import java.util.Map;
import javax.swing.table.TableModel;

/**
 *
 * @author Danno Ferrin
 */
public class TableModelFactory implements Factory {
    
    public Object newInstance(SwingBuilder builder, Object name, Object value, Map properties) throws InstantiationException, IllegalAccessException {
        if (value instanceof TableModel) {
            return value;
        } else if (properties.get(name) instanceof TableModel) {
            return properties.remove(name);
        } else {
            ValueModel model = (ValueModel) properties.remove("model");
            if (model == null) {
                Object list = properties.remove("list");
                if (list == null) {
                    list = new ArrayList();
                }
                model = new ValueHolder(list);
            }
            return new DefaultTableModel(model);
        }
    }

    public static class PropertyColumnFactory implements Factory {
    
        public Object newInstance(SwingBuilder builder, Object name, Object value, Map properties) throws InstantiationException, IllegalAccessException {
            Object current = builder.getCurrent();
            if (current instanceof DefaultTableModel) {
                DefaultTableModel model = (DefaultTableModel) current;
                Object header = properties.remove("header");
                if (header == null) {
                    header = "";
                }
                String property = (String) properties.remove("propertyName");
                if (property == null) {
                    throw new IllegalArgumentException("Must specify a property for a propertyColumn");
                }
                Class type = (Class) properties.remove("type");
                if (type == null) {
                    type = Object.class;
                }
                return model.addPropertyColumn(header, property, type);
            } else {
                throw new RuntimeException("propertyColumn must be a child of a tableModel");
            }
        }
    }

    public static class ClosureColumnFactory implements Factory {
    
        public Object newInstance(SwingBuilder builder, Object name, Object value, Map properties) throws InstantiationException, IllegalAccessException {
            Object current = builder.getCurrent();
            if (current instanceof DefaultTableModel) {
                DefaultTableModel model = (DefaultTableModel) current;
                Object header = properties.remove("header");
                if (header == null) {
                    header = "";
                }
                Closure readClosure = (Closure) properties.remove("read");
                if (readClosure == null) {
                    throw new IllegalArgumentException("Must specify 'read' Closure property for a closureColumn");
                }
                Closure writeClosure = (Closure) properties.remove("write");
                Class type = (Class) properties.remove("type");
                if (type == null) {
                    type = Object.class;
                }
                return model.addClosureColumn(header, readClosure, writeClosure, type);
            } else {
                throw new RuntimeException("closureColumn must be a child of a tableModel");
            }
        }
    }

}