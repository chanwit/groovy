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

package groovy.util;

import groovy.xml.QName;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * Prints a node with all children in XML format.
 *
 * @author Christian Stein
 * @see groovy.util.NodePrinter
 */
public class XmlNodePrinter {

    protected final IndentPrinter out;
    private String quote;
    private boolean namespaceAware = true;

    public XmlNodePrinter(PrintWriter out) {
        this(out, "  ");
    }

    public XmlNodePrinter(PrintWriter out, String indent) {
        this(out, indent, "\"");
    }

    public XmlNodePrinter(PrintWriter out, String indent, String quote) {
        this(new IndentPrinter(out, indent), quote);
    }

    public XmlNodePrinter(IndentPrinter out, String quote) {
        if (out == null) {
            throw new IllegalArgumentException("Argument 'IndentPrinter out' must not be null!");
        }
        this.out = out;
        this.quote = quote;
    }

    public XmlNodePrinter() {
        this(new PrintWriter(new OutputStreamWriter(System.out)));
    }

    public String getNameOfNode(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("Node must not be null!");
        }
        Object name = node.name();
        if (name instanceof QName) {
            QName qname = (QName) name;
            if (!namespaceAware) {
                return qname.getLocalPart();
            }
            return qname.getQualifiedName();
        }
        return name.toString();
    }

    public boolean isEmptyElement(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("Node must not be null!");
        }
        if (!node.children().isEmpty()) {
            return false;
        }
        return node.text().length() == 0;
    }

    public void print(Node node) {
        print(node, new NamespaceContext());
    }

    /**
     * Check if namespace handling is enabled.
     *
     * @return true if namespace handling is enabled
     */
    public boolean isNamespaceAware() {
        return namespaceAware;
    }

    /**
     * Enable and/or disable namespace handling.
     *
     * @param namespaceAware the new desired value
     */
    public void setNamespaceAware(boolean namespaceAware) {
        this.namespaceAware = namespaceAware;
    }

    /**
     * Get Quote to use when printing attributes.
     *
     * @return the quote character
     */
    public String getQuote() {
        return quote;
    }

    /**
     * Set Quote to use when printing attributes.
     *
     * @param quote the quote character
     */
    public void setQuote(String quote) {
        this.quote = quote;
    }

    protected void print(Node node, NamespaceContext ctx) {
        /*
         * Handle empty elements like '<br/>', '<img/> or '<hr noshade="noshade"/>.
         */
        if (isEmptyElement(node)) {
            printLineBegin();
            out.print("<");
            out.print(getNameOfNode(node));
            if (ctx != null) {
                printNamespace(node, ctx);
            }
            printNameAttributes(node.attributes());
            out.print("/>");
            printLineEnd();
            out.flush();
            return;
        }

        /*
         * Hook for extra processing, e.g. GSP tag element!
         */
        if (printSpecialNode(node)) {
            out.flush();
            return;
        }

        /*
         * Handle normal element like <html> ... </html>.
         */
        Object value = node.value();
        if (value instanceof List) {
            printName(node, ctx, true);
            printList((List) value, ctx);
            printName(node, ctx, false);
            out.flush();
            return;
        }

        // treat as simple type - probably a String
        printName(node, ctx, true);
        printSimpleItemWithIndent(value);
        printName(node, ctx, false);
        out.flush();
    }

    protected void printLineBegin() {
        out.printIndent();
    }

    protected void printLineEnd() {
        printLineEnd(null);
    }

    protected void printLineEnd(String comment) {
        if (comment != null) {
            out.print(" <!-- ");
            out.print(comment);
            out.print(" -->");
        }
        out.print("\n");
    }

    protected void printList(List list, NamespaceContext ctx) {
        out.incrementIndent();
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            NamespaceContext context = new NamespaceContext(ctx);
            Object value = iter.next();
            /*
             * If the current value is a node, recurse into that node.
             */
            if (value instanceof Node) {
                print((Node) value, context);
                continue;
            }
            printSimpleItem(value);

        }
        out.decrementIndent();
    }

    private void printSimpleItemWithIndent(Object value) {
        out.incrementIndent();
        printSimpleItem(value);
        out.decrementIndent();
    }

    protected void printSimpleItem(Object value) {
        printLineBegin();
        out.print(InvokerHelper.toString(value));
        printLineEnd();
    }

    protected void printName(Node node, NamespaceContext ctx, boolean begin) {
        if (node == null) {
            throw new NullPointerException("Node must not be null.");
        }
        Object name = node.name();
        if (name == null) {
            throw new NullPointerException("Name must not be null.");
        }
        printLineBegin();
        out.print("<");
        if (!begin) {
            out.print("/");
        }
        out.print(getNameOfNode(node));
        if (ctx != null) {
            printNamespace(node, ctx);
        }
        if (begin) {
            printNameAttributes(node.attributes());
        }
        out.print(">");
        printLineEnd();
    }

    protected void printNameAttributes(Map attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }
        for (Iterator iter = attributes.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            out.print(" ");
            out.print(entry.getKey().toString());
            out.print("=");
            Object value = entry.getValue();
            out.print(quote);
            if (value instanceof String) {
                printEscaped((String) value);
            } else {
                printEscaped(InvokerHelper.toString(value));
            }
            out.print(quote);
        }
    }

    // For ' and " we only escape if needed. As far as XML is concerned,
    // we could always escape if we wanted to.
    private void printEscaped(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '<':
                    out.print("&lt;");
                    break;
                case '>':
                    out.print("&gt;");
                    break;
                case '&':
                    out.print("&amp;");
                    break;
                case '\'':
                    if (quote.equals("'"))
                        out.print("&apos;");
                    else
                        out.print(c);
                    break;
                case '"':
                    if (quote.equals("\""))
                        out.print("&quot;");
                    else
                        out.print(c);
                    break;
                default:
                    out.print(c);
            }
        }
    }

    protected boolean printSpecialNode(Node node) {
        return false;
    }

    protected void printNamespace(Node node, NamespaceContext ctx) {
        Object name = node.name();
        if (name instanceof QName && namespaceAware) {
            QName qname = (QName) name;
            String namespaceUri = qname.getNamespaceURI();
            if (namespaceUri != null) {
                String prefix = qname.getPrefix();
                if (!ctx.isNamespaceRegistered(namespaceUri)) {
                    ctx.registerNamespacePrefix(namespaceUri, prefix);
                    out.print(" ");
                    out.print("xmlns");
                    if (prefix.length() > 0) {
                        out.print(":");
                        out.print(prefix);
                    }
                    out.print("=" + quote);
                    out.print(namespaceUri);
                    out.print(quote);
                }
            }
        }
    }

    private class NamespaceContext {
        private final Map namespaceMap;

        private NamespaceContext() {
            namespaceMap = new HashMap();
        }

        private NamespaceContext(NamespaceContext context) {
            this();
            namespaceMap.putAll(context.namespaceMap);
        }

        public boolean isNamespaceRegistered(String uri) {
            return namespaceMap.containsKey(uri);
        }

        public void registerNamespacePrefix(String uri, String prefix) {
            if (!isNamespaceRegistered(uri)) {
                namespaceMap.put(uri, prefix);
            }
        }

        public String getNamespacePrefix(String uri) {
            Object prefix = namespaceMap.get(uri);
            return (prefix == null) ? null : prefix.toString();
        }

    }

}
