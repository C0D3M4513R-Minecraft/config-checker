package com.c0d3m4513r.config;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

public class LoaderSaverChecker extends BaseTypeChecker {
    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new LoaderSaverVisitor(this);
    }
}
