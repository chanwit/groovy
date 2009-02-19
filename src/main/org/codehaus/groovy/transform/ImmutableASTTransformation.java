/*
 * Copyright 2008 the original author or authors.
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

package org.codehaus.groovy.transform;

import groovy.lang.Immutable;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.util.HashCodeHelper;
import org.objectweb.asm.Opcodes;

import java.util.*;

/**
 * Handles generation of code for the @Immutable annotation.
 * This is experimental, use at your own risk.
 *
 * @author Paul King
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class ImmutableASTTransformation implements ASTTransformation, Opcodes {

    /*
      currently leaving BigInteger and BigDecimal in list but see:
      http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6348370
     */
    private static Class[] immutableList = {
            Boolean.class,
            Byte.class,
            Character.class,
            Double.class,
            Float.class,
            Integer.class,
            Long.class,
            Short.class,
            String.class,
            java.math.BigInteger.class,
            java.math.BigDecimal.class,
            java.awt.Color.class,
    };
    private static final Class MY_CLASS = Immutable.class;
    private static final ClassNode MY_TYPE = new ClassNode(MY_CLASS);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();
    private static final ClassNode OBJECT_TYPE = new ClassNode(Object.class);
    private static final ClassNode HASHMAP_TYPE = new ClassNode(HashMap.class);
    private static final ClassNode MAP_TYPE = new ClassNode(Map.class);
    private static final ClassNode DATE_TYPE = new ClassNode(Date.class);
    private static final ClassNode CLONEABLE_TYPE = new ClassNode(Cloneable.class);
    private static final ClassNode COLLECTION_TYPE = new ClassNode(Collection.class);
    private static final ClassNode HASHUTIL_TYPE = new ClassNode(HashCodeHelper.class);
    private static final ClassNode STRINGBUFFER_TYPE = new ClassNode(StringBuffer.class);
    private static final ClassNode DGM_TYPE = new ClassNode(DefaultGroovyMethods.class);
    private static final ClassNode SELF_TYPE = new ClassNode(ImmutableASTTransformation.class);
    private static final Token COMPARE_EQUAL = Token.newSymbol(Types.COMPARE_EQUAL, -1, -1);
    private static final Token COMPARE_NOT_EQUAL = Token.newSymbol(Types.COMPARE_NOT_EQUAL, -1, -1);
    private static final Token COMPARE_IDENTICAL = Token.newSymbol(Types.COMPARE_IDENTICAL, -1, -1);
    private static final Token ASSIGN = Token.newSymbol(Types.ASSIGN, -1, -1);

    public void visit(ASTNode[] nodes, SourceUnit source) {
        if (!(nodes[0] instanceof AnnotationNode) || !(nodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: wrong types: $node.class / $parent.class");
        }

        AnnotatedNode parent = (AnnotatedNode) nodes[1];
        AnnotationNode node = (AnnotationNode) nodes[0];
        if (!MY_TYPE.equals(node.getClassNode())) return;
        List<PropertyNode> newNodes = new ArrayList<PropertyNode>();

        if (parent instanceof ClassNode) {
            ClassNode cNode = (ClassNode) parent;
            String cName = cNode.getName();
            if (cNode.isInterface()) {
                throw new RuntimeException("Error processing interface '" + cName + "'. " + MY_TYPE_NAME + " not allowed for interfaces.");
            }
            if ((cNode.getModifiers() & ACC_FINAL) == 0) {
                throw new RuntimeException("Error processing class '" + cName + "'. " + MY_TYPE_NAME + " classes must be final.");
            }

            final List<PropertyNode> pList = cNode.getProperties();
            for (PropertyNode pNode : pList) {
                adjustPropertyForImmutability(pNode, newNodes);
            }
            for (PropertyNode pNode : newNodes) {
                pList.remove(pNode);
                addProperty(cNode, pNode);
            }
            final List<FieldNode> fList = cNode.getFields();
            for (FieldNode fNode : fList) {
                ensureNotPublic(cName, fNode);
            }
            createConstructor(cNode);
            createHashCode(cNode);
            createEquals(cNode);
            createToString(cNode);
        }
    }

    private void ensureNotPublic(String cNode, FieldNode fNode) {
        String fName = fNode.getName();
        // TODO: do we need to lock down things like: $ownClass
        if (fNode.isPublic() && !fName.contains("$")) {
            throw new RuntimeException("Public field '" + fName + "' not allowed for " + MY_TYPE_NAME + " class '" + cNode + "'.");
        }
    }

    private void createHashCode(ClassNode cNode) {
        final FieldNode hashField = cNode.addField("$hash$code", ACC_PRIVATE | ACC_SYNTHETIC, ClassHelper.int_TYPE, null);
        final BlockStatement body = new BlockStatement();
        final Expression hash = new FieldExpression(hashField);
        final List<PropertyNode> list = cNode.getProperties();

        body.addStatement(new IfStatement(
                isZeroExpr(hash),
                calculateHashStatements(hash, list),
                new EmptyStatement()
        ));

        body.addStatement(new ReturnStatement(hash));

        cNode.addMethod(new MethodNode("hashCode", ACC_PUBLIC, ClassHelper.int_TYPE, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, body));
    }

    private void createToString(ClassNode cNode) {
        final BlockStatement body = new BlockStatement();
        final List<PropertyNode> list = cNode.getProperties();
        // def _result = new StringBuffer()
        final Expression result = new VariableExpression("_result");
        final Expression init = new ConstructorCallExpression(STRINGBUFFER_TYPE, MethodCallExpression.NO_ARGUMENTS);
        body.addStatement(new ExpressionStatement(new DeclarationExpression(result, ASSIGN, init)));

        body.addStatement(append(result, new ConstantExpression(cNode.getName())));
        body.addStatement(append(result, new ConstantExpression("(")));
        boolean first = true;
        for (PropertyNode pNode : list) {
            if (first) {
                first = false;
            } else {
                body.addStatement(append(result, new ConstantExpression(", ")));
            }
            body.addStatement(new IfStatement(
                    new BooleanExpression(new FieldExpression(cNode.getField("$map$constructor"))),
                    toStringPropertyName(result, pNode.getName()),
                    new EmptyStatement()
            ));
            final FieldExpression fieldExpr = new FieldExpression(pNode.getField());
            body.addStatement(append(result, new MethodCallExpression(fieldExpr, "toString", MethodCallExpression.NO_ARGUMENTS)));
        }
        body.addStatement(append(result, new ConstantExpression(")")));
        body.addStatement(new ReturnStatement(new MethodCallExpression(result, "toString", MethodCallExpression.NO_ARGUMENTS)));
        cNode.addMethod(new MethodNode("toString", ACC_PUBLIC, ClassHelper.STRING_TYPE, Parameter.EMPTY_ARRAY, ClassNode.EMPTY_ARRAY, body));
    }

    private Statement toStringPropertyName(Expression result, String fName) {
        final BlockStatement body = new BlockStatement();
        body.addStatement(append(result, new ConstantExpression(fName)));
        body.addStatement(append(result, new ConstantExpression(":")));
        return body;
    }

    private ExpressionStatement append(Expression result, Expression expr) {
        return new ExpressionStatement(new MethodCallExpression(result, "append", expr));
    }

    private Statement calculateHashStatements(Expression hash, List<PropertyNode> list) {
        final BlockStatement body = new BlockStatement();
        // def _result = HashCodeHelper.initHash()
        final Expression result = new VariableExpression("_result");
        final Expression init = new StaticMethodCallExpression(HASHUTIL_TYPE, "initHash", MethodCallExpression.NO_ARGUMENTS);
        body.addStatement(new ExpressionStatement(new DeclarationExpression(result, ASSIGN, init)));

        // fields
        for (PropertyNode pNode : list) {
            // _result = HashCodeHelper.updateHash(_result, field)
            final Expression fieldExpr = new FieldExpression(pNode.getField());
            final Expression args = new TupleExpression(result, fieldExpr);
            final Expression current = new StaticMethodCallExpression(HASHUTIL_TYPE, "updateHash", args);
            body.addStatement(assignStatement(result, current));
        }
        // $hash$code = _result
        body.addStatement(assignStatement(hash, result));
        return body;
    }

    private void createEquals(ClassNode cNode) {
        final BlockStatement body = new BlockStatement();
        Expression other = new VariableExpression("other");

        // some short circuit cases for efficiency
        body.addStatement(returnFalseIfNull(other));
        body.addStatement(returnFalseIfWrongType(cNode, other));
        body.addStatement(returnTrueIfIdentical(VariableExpression.THIS_EXPRESSION, other));

        final List<PropertyNode> list = cNode.getProperties();
        // fields
        for (PropertyNode pNode : list) {
            body.addStatement(returnFalseIfPropertyNotEqual(pNode, other));
        }

        // default
        body.addStatement(new ReturnStatement(ConstantExpression.TRUE));

        Parameter[] params = {new Parameter(OBJECT_TYPE, "other")};
        cNode.addMethod(new MethodNode("equals", ACC_PUBLIC, ClassHelper.boolean_TYPE, params, ClassNode.EMPTY_ARRAY, body));
    }

    private Statement returnFalseIfWrongType(ClassNode cNode, Expression other) {
        return new IfStatement(
                identicalExpr(new ClassExpression(cNode), new ClassExpression(other.getType())),
                new ReturnStatement(ConstantExpression.FALSE),
                new EmptyStatement()
        );
    }

    private IfStatement returnFalseIfNull(Expression other) {
        return new IfStatement(
                equalsNullExpr(other),
                new ReturnStatement(ConstantExpression.FALSE),
                new EmptyStatement()
        );
    }

    private IfStatement returnTrueIfIdentical(Expression self, Expression other) {
        return new IfStatement(
                identicalExpr(self, other),
                new ReturnStatement(ConstantExpression.TRUE),
                new EmptyStatement()
        );
    }

    private Statement returnFalseIfPropertyNotEqual(PropertyNode pNode, Expression other) {
        return new IfStatement(
                notEqualsExpr(pNode, other),
                new ReturnStatement(ConstantExpression.FALSE),
                new EmptyStatement()
        );
    }

    private void addProperty(ClassNode cNode, PropertyNode pNode) {
        final FieldNode fn = pNode.getField();
        cNode.getFields().remove(fn);
        cNode.addProperty(pNode.getName(), pNode.getModifiers() | ACC_FINAL, pNode.getType(),
                pNode.getInitialExpression(), pNode.getGetterBlock(), pNode.getSetterBlock());
        final FieldNode newfn = cNode.getField(fn.getName());
        cNode.getFields().remove(newfn);
        cNode.addField(fn);
    }

    private void createConstructor(ClassNode cNode) {
        // pretty toString will remember how the user declared the params and print accordingly
        final FieldNode constructorField = cNode.addField("$map$constructor", ACC_PRIVATE | ACC_SYNTHETIC, ClassHelper.boolean_TYPE, null);
        final FieldExpression constructorStyle = new FieldExpression(constructorField);
        if (cNode.getDeclaredConstructors().size() != 0) {
            // TODO: allow constructors which call provided constructor?
            throw new RuntimeException("Explicit constructors not allowed for " + MY_TYPE_NAME + " class: " + cNode.getNameWithoutPackage());
        }

        List<PropertyNode> list = cNode.getProperties();
        boolean specialHashMapCase = list.size() == 1 && list.get(0).getField().getType().equals(HASHMAP_TYPE);
        if (specialHashMapCase) {
            createConstructorMapSpecial(cNode, constructorStyle, list);
        } else {
            createConstructorMap(cNode, constructorStyle, list);
            createConstructorOrdered(cNode, constructorStyle, list);
        }
    }

    private void createConstructorMapSpecial(ClassNode cNode, FieldExpression constructorStyle, List<PropertyNode> list) {
        final BlockStatement body = new BlockStatement();
        body.addStatement(createConstructorStatementMapSpecial(list.get(0).getField()));
        createConstructorMapCommon(cNode, constructorStyle, body);
    }

    private void createConstructorMap(ClassNode cNode, FieldExpression constructorStyle, List<PropertyNode> list) {
        final BlockStatement body = new BlockStatement();
        for (PropertyNode pNode : list) {
            body.addStatement(createConstructorStatement(pNode));
        }
        createConstructorMapCommon(cNode, constructorStyle, body);
    }

    private void createConstructorMapCommon(ClassNode cNode, FieldExpression constructorStyle, BlockStatement body) {
        final List<FieldNode> fList = cNode.getFields();
        for (FieldNode fNode : fList) {
            if (!fNode.isPublic() && !fNode.getName().contains("$") && (cNode.getProperty(fNode.getName()) == null)) {
                body.addStatement(createConstructorStatementDefault(fNode));
            }
        }
        body.addStatement(assignStatement(constructorStyle, ConstantExpression.TRUE));
        final Parameter[] params = new Parameter[]{new Parameter(HASHMAP_TYPE, "args")};
        cNode.addConstructor(new ConstructorNode(ACC_PUBLIC, params, ClassNode.EMPTY_ARRAY, new IfStatement(
                equalsNullExpr(new VariableExpression("args")),
                new EmptyStatement(),
                body)));
    }

    private void createConstructorOrdered(ClassNode cNode, FieldExpression constructorStyle, List<PropertyNode> list) {
        final MapExpression argMap = new MapExpression();
        final Parameter[] orderedParams = new Parameter[list.size()];
        int index = 0;
        for (PropertyNode pNode : list) {
            orderedParams[index++] = new Parameter(pNode.getField().getType(), pNode.getField().getName());
            argMap.addMapEntryExpression(new ConstantExpression(pNode.getName()), new VariableExpression(pNode.getName()));
        }
        final BlockStatement orderedBody = new BlockStatement();
        orderedBody.addStatement(new ExpressionStatement(
                new ConstructorCallExpression(ClassNode.THIS, new ArgumentListExpression(new CastExpression(HASHMAP_TYPE, argMap)))
        ));
        orderedBody.addStatement(assignStatement(constructorStyle, ConstantExpression.FALSE));
        cNode.addConstructor(new ConstructorNode(ACC_PUBLIC, orderedParams, ClassNode.EMPTY_ARRAY, orderedBody));
    }

    private Statement createConstructorStatement(PropertyNode pNode) {
        FieldNode fNode = pNode.getField();
        final ClassNode fieldType = fNode.getType();
        final Statement statement;
        if (fieldType.isArray() || implementsInterface(fieldType, CLONEABLE_TYPE)) {
            statement = createConstructorStatementArrayOrCloneable(fNode);
        } else if (fieldType.isDerivedFrom(DATE_TYPE)) {
            statement = createConstructorStatementDate(fNode);
        } else if (fieldType.isDerivedFrom(COLLECTION_TYPE) || fieldType.isDerivedFrom(MAP_TYPE)) {
            statement = createConstructorStatementCollection(fNode);
        } else if (isKnownImmutable(fieldType)) {
            statement = createConstructorStatementDefault(fNode);
        } else if (fieldType.isResolved()) {
            throw new RuntimeException(createErrorMessage(fNode.getName(), fieldType.getName(), "compiling"));
        } else {
            statement = createConstructorStatementGuarded(fNode);
        }
        return statement;
    }

    private boolean implementsInterface(ClassNode fieldType, ClassNode interfaceType) {
        return Arrays.asList(fieldType.getInterfaces()).contains(interfaceType);
    }

    private Statement createConstructorStatementGuarded(FieldNode fNode) {
        final FieldExpression fieldExpr = new FieldExpression(fNode);
        Expression initExpr = fNode.getInitialValueExpression();
        if (initExpr == null) initExpr = ConstantExpression.NULL;
        Expression unknown = findArg(fNode.getName());
        return new IfStatement(
                equalsNullExpr(unknown),
                new IfStatement(
                        equalsNullExpr(initExpr),
                        new EmptyStatement(),
                        assignStatement(fieldExpr, checkUnresolved(fNode, initExpr))),
                assignStatement(fieldExpr, checkUnresolved(fNode, unknown)));
    }

    private Expression checkUnresolved(FieldNode fNode, Expression value) {
        Expression args = new TupleExpression(new ConstantExpression(fNode.getName()), value);
        return new StaticMethodCallExpression(SELF_TYPE, "checkImmutable", args);
    }

    private Statement createConstructorStatementCollection(FieldNode fNode) {
        final FieldExpression fieldExpr = new FieldExpression(fNode);
        Expression initExpr = fNode.getInitialValueExpression();
        if (initExpr == null) initExpr = ConstantExpression.NULL;
        Expression collection = findArg(fNode.getName());
        return new IfStatement(
                equalsNullExpr(collection),
                new IfStatement(
                        equalsNullExpr(initExpr),
                        new EmptyStatement(),
                        assignStatement(fieldExpr, cloneCollectionExpr(initExpr))),
                assignStatement(fieldExpr, cloneCollectionExpr(collection)));
    }

    private Statement createConstructorStatementMapSpecial(FieldNode fNode) {
        final FieldExpression fieldExpr = new FieldExpression(fNode);
        Expression initExpr = fNode.getInitialValueExpression();
        if (initExpr == null) initExpr = ConstantExpression.NULL;
        Expression namedArgs = findArg(fNode.getName());
        Expression baseArgs = new VariableExpression("args");
        return new IfStatement(
                equalsNullExpr(baseArgs),
                new IfStatement(
                        equalsNullExpr(initExpr),
                        new EmptyStatement(),
                        assignStatement(fieldExpr, cloneCollectionExpr(initExpr))),
                new IfStatement(
                        equalsNullExpr(namedArgs),
                        new IfStatement(
                                isTrueExpr(new MethodCallExpression(baseArgs, "containsKey", new ConstantExpression(fNode.getName()))),
                                assignStatement(fieldExpr, namedArgs),
                                assignStatement(fieldExpr, cloneCollectionExpr(baseArgs))),
                        new IfStatement(
                                isOneExpr(new MethodCallExpression(baseArgs, "size", MethodCallExpression.NO_ARGUMENTS)),
                                assignStatement(fieldExpr, cloneCollectionExpr(namedArgs)),
                                assignStatement(fieldExpr, cloneCollectionExpr(baseArgs)))
                )
        );
    }

    private boolean isKnownImmutable(ClassNode fieldType) {
        if (!fieldType.isResolved()) return false;
        Class typeClass = fieldType.getTypeClass();
        return typeClass.isEnum() ||
                typeClass.isPrimitive() ||
                inImmutableList(typeClass);
    }

    private static boolean inImmutableList(Class typeClass) {
        return Arrays.asList(immutableList).contains(typeClass);
    }

    private Statement createConstructorStatementDefault(FieldNode fNode) {
        final FieldExpression fieldExpr = new FieldExpression(fNode);
        Expression initExpr = fNode.getInitialValueExpression();
        if (initExpr == null) initExpr = ConstantExpression.NULL;
        Expression value = findArg(fNode.getName());
        return new IfStatement(
                equalsNullExpr(value),
                new IfStatement(
                        equalsNullExpr(initExpr),
                        new EmptyStatement(),
                        assignStatement(fieldExpr, initExpr)),
                assignStatement(fieldExpr, value));
    }

    private Statement createConstructorStatementArrayOrCloneable(FieldNode fNode) {
        final FieldExpression fieldExpr = new FieldExpression(fNode);
        Expression initExpr = fNode.getInitialValueExpression();
        if (initExpr == null) initExpr = ConstantExpression.NULL;
        final Expression array = findArg(fNode.getName());
        return new IfStatement(
                equalsNullExpr(array),
                new IfStatement(
                        equalsNullExpr(initExpr),
                        assignStatement(fieldExpr, ConstantExpression.NULL),
                        assignStatement(fieldExpr, cloneArrayOrCloneableExpr(initExpr))),
                assignStatement(fieldExpr, cloneArrayOrCloneableExpr(array)));
    }

    private Statement createConstructorStatementDate(FieldNode fNode) {
        final FieldExpression fieldExpr = new FieldExpression(fNode);
        Expression initExpr = fNode.getInitialValueExpression();
        if (initExpr == null) initExpr = ConstantExpression.NULL;
        final Expression date = findArg(fNode.getName());
        return new IfStatement(
                equalsNullExpr(date),
                new IfStatement(
                        equalsNullExpr(initExpr),
                        assignStatement(fieldExpr, ConstantExpression.NULL),
                        assignStatement(fieldExpr, cloneDateExpr(initExpr))),
                assignStatement(fieldExpr, cloneDateExpr(date)));
    }

    private Expression cloneDateExpr(Expression origDate) {
        return new ConstructorCallExpression(DATE_TYPE,
                new MethodCallExpression(origDate, "getTime", MethodCallExpression.NO_ARGUMENTS));
    }

    private Statement assignStatement(Expression fieldExpr, Expression value) {
        return new ExpressionStatement(assignExpr(fieldExpr, value));
    }

    private Expression assignExpr(Expression fieldExpr, Expression value) {
        return new BinaryExpression(fieldExpr, ASSIGN, value);
    }

    private BooleanExpression equalsNullExpr(Expression argExpr) {
        return new BooleanExpression(new BinaryExpression(argExpr, COMPARE_EQUAL, ConstantExpression.NULL));
    }

    private BooleanExpression isTrueExpr(Expression argExpr) {
        return new BooleanExpression(new BinaryExpression(argExpr, COMPARE_EQUAL, ConstantExpression.TRUE));
    }

    private BooleanExpression isZeroExpr(Expression expr) {
        return new BooleanExpression(new BinaryExpression(expr, COMPARE_EQUAL, new ConstantExpression(Integer.valueOf(0))));
    }

    private BooleanExpression isOneExpr(Expression expr) {
        return new BooleanExpression(new BinaryExpression(expr, COMPARE_EQUAL, new ConstantExpression(Integer.valueOf(1))));
    }

    private BooleanExpression notEqualsExpr(PropertyNode pNode, Expression other) {
        final Expression fieldExpr = new FieldExpression(pNode.getField());
        final Expression otherExpr = new PropertyExpression(other, pNode.getField().getName());
        return new BooleanExpression(new BinaryExpression(fieldExpr, COMPARE_NOT_EQUAL, otherExpr));
    }

    private BooleanExpression identicalExpr(Expression self, Expression other) {
        return new BooleanExpression(new BinaryExpression(self, COMPARE_IDENTICAL, other));
    }

    private Expression findArg(String fName) {
        return new PropertyExpression(new VariableExpression("args"), fName);
    }

    private void adjustPropertyForImmutability(PropertyNode pNode, List<PropertyNode> newNodes) {
        final FieldNode fNode = pNode.getField();
        fNode.setModifiers((pNode.getModifiers() & (~ACC_PUBLIC)) | ACC_FINAL | ACC_PRIVATE);
        adjustPropertyNode(pNode, createGetterBody(fNode));
        newNodes.add(pNode);
    }

    private void adjustPropertyNode(PropertyNode pNode, Statement getterBody) {
        pNode.setSetterBlock(null);
        pNode.setGetterBlock(getterBody);
    }

    private Statement createGetterBody(FieldNode fNode) {
        BlockStatement body = new BlockStatement();
        final ClassNode fieldType = fNode.getType();
        final Statement statement;
        if (fieldType.isArray() || implementsInterface(fieldType, CLONEABLE_TYPE)) {
            statement = createGetterBodyArrayOrCloneable(fNode);
        } else if (fieldType.isDerivedFrom(DATE_TYPE)) {
            statement = createGetterBodyDate(fNode);
        } else {
            statement = createGetterBodyDefault(fNode);
        }
        body.addStatement(statement);
        return body;
    }

    private Statement createGetterBodyDefault(FieldNode fNode) {
        final Expression fieldExpr = new FieldExpression(fNode);
        return new ExpressionStatement(fieldExpr);
    }

    private static String createErrorMessage(String fieldName, String typeName, String mode) {
        return "Possible mutable field '" + fieldName +
                "' of type '" + typeName + "' found while " + mode + " " + MY_TYPE_NAME + " class.";
    }

    private Statement createGetterBodyArrayOrCloneable(FieldNode fNode) {
        final Expression fieldExpr = new FieldExpression(fNode);
        final Expression expression = cloneArrayOrCloneableExpr(fieldExpr);
        return safeExpression(fieldExpr, expression);
    }

    private Expression cloneArrayOrCloneableExpr(Expression fieldExpr) {
        return new MethodCallExpression(fieldExpr, "clone", MethodCallExpression.NO_ARGUMENTS);
    }

    private Expression cloneCollectionExpr(Expression fieldExpr) {
        return new StaticMethodCallExpression(DGM_TYPE, "asImmutable", fieldExpr);
    }

    private Statement createGetterBodyDate(FieldNode fNode) {
        final Expression fieldExpr = new FieldExpression(fNode);
        final Expression expression = cloneDateExpr(fieldExpr);
        return safeExpression(fieldExpr, expression);
    }

    private Statement safeExpression(Expression fieldExpr, Expression expression) {
        return new IfStatement(
                equalsNullExpr(fieldExpr),
                new ExpressionStatement(fieldExpr),
                new ExpressionStatement(expression));
    }

    public static Object checkImmutable(String fieldName, Object field) {
        if (field == null || field instanceof Enum || inImmutableList(field.getClass())) return field;
        if (field instanceof Collection) return DefaultGroovyMethods.asImmutable((Collection) field);
        if (field.getClass().getAnnotation(MY_CLASS) != null) return field;
        final String typeName = field.getClass().getName();
        throw new RuntimeException(createErrorMessage(fieldName, typeName, "constructing"));
    }

}
