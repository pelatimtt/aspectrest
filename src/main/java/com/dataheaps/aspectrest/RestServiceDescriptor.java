package com.dataheaps.aspectrest;

import lombok.AllArgsConstructor;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by matteopelati on 27/11/15.
 */
@AllArgsConstructor
class RestServiceDescriptor implements Comparable<RestServiceDescriptor> {

    @AllArgsConstructor
    static class ArgIndex {
        Set<RestSource> source = new HashSet<>();
        int index;
        String name;
        Class<?> argClass;
    }

    int priority;
    Pattern path;
    Object service;
    Method method;
    boolean authenticated;
    RestMethod verb;
    List<ArgIndex> argsIndexes;

    @Override
    public int compareTo(RestServiceDescriptor o) {
        int r = new Integer(priority).compareTo(o.priority);
        return r >= 0 ? 1 : -1;
    }
}
