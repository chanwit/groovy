package org.codehaus.groovy.runtime.callsite;

import groovy.lang.GroovyObject;

import java.util.concurrent.atomic.AtomicInteger;

public interface CallSite {
    CallSiteArray getArray();
    int getIndex();
    String getName();
    AtomicInteger getUsage();

    Object getProperty(Object receiver) throws Throwable;
    Object callGetPropertySafe (Object receiver) throws Throwable;
    Object callGetProperty (Object receiver) throws Throwable;
    Object callGroovyObjectGetProperty (Object receiver) throws Throwable;
    Object callGroovyObjectGetPropertySafe (Object receiver) throws Throwable;

    Object call (Object receiver, Object[] args) throws Throwable;
    Object call (Object receiver) throws Throwable;
    Object call (Object receiver, Object arg1) throws Throwable;
    Object call (Object receiver, Object arg1, Object arg2) throws Throwable;
    Object call (Object receiver, Object arg1, Object arg2, Object arg3) throws Throwable;
    Object call (Object receiver, Object arg1, Object arg2, Object arg3, Object arg4) throws Throwable;

    Object callSafe (Object receiver, Object[] args) throws Throwable;
    Object callSafe (Object receiver) throws Throwable;
    Object callSafe (Object receiver, Object arg1) throws Throwable;
    Object callSafe (Object receiver, Object arg1, Object arg2) throws Throwable;
    Object callSafe (Object receiver, Object arg1, Object arg2, Object arg3) throws Throwable;
    Object callSafe (Object receiver, Object arg1, Object arg2, Object arg3, Object arg4) throws Throwable;

    Object callCurrent (GroovyObject receiver, Object [] args) throws Throwable;
    Object callCurrent (GroovyObject receiver) throws Throwable;
    Object callCurrent (GroovyObject receiver, Object arg1) throws Throwable;
    Object callCurrent (GroovyObject receiver, Object arg1, Object arg2) throws Throwable;
    Object callCurrent (GroovyObject receiver, Object arg1, Object arg2, Object arg3) throws Throwable;
    Object callCurrent (GroovyObject receiver, Object arg1, Object arg2, Object arg3, Object arg4) throws Throwable;

    Object callStatic (Class receiver, Object [] args) throws Throwable;
    Object callStatic (Class receiver) throws Throwable;
    Object callStatic (Class receiver, Object arg1) throws Throwable;
    Object callStatic (Class receiver, Object arg1, Object arg2) throws Throwable;
    Object callStatic (Class receiver, Object arg1, Object arg2, Object arg3) throws Throwable;
    Object callStatic (Class receiver, Object arg1, Object arg2, Object arg3, Object arg4) throws Throwable;

    Object callConstructor (Object receiver, Object [] args) throws Throwable;
    Object callConstructor (Object receiver) throws Throwable;
    Object callConstructor (Object receiver, Object arg1) throws Throwable;
    Object callConstructor (Object receiver, Object arg1, Object arg2) throws Throwable;
    Object callConstructor (Object receiver, Object arg1, Object arg2, Object arg3) throws Throwable;
    Object callConstructor (Object receiver, Object arg1, Object arg2, Object arg3, Object arg4) throws Throwable;
}
