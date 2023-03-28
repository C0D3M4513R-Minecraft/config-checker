package com.c0d3m4513r.config;

import com.c0d3m4513r.config.iface.key.IConfigLoadable;
import com.c0d3m4513r.config.iface.key.IConfigLoadableSaveable;
import com.c0d3m4513r.config.iface.key.IConfigSavable;
import com.c0d3m4513r.config.qual.LoadableNonSaveable;
import com.c0d3m4513r.config.qual.LoadableSaveable;
import com.c0d3m4513r.config.qual.NonLoadableNonSaveable;
import com.c0d3m4513r.config.qual.NonLoadableSavable;
import com.sun.source.tree.*;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import javax.lang.model.type.TypeMirror;
import java.util.Iterator;
import java.util.List;

public class LoaderSaverVisitor extends BaseTypeVisitor<BaseAnnotatedTypeFactory> {
    public LoaderSaverVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public void visitAnnotatedType(@Nullable List<? extends AnnotationTree> list, Tree tree){
        if (list == null) return;
        boolean isLoadableSavable = false;
        boolean isLoadableNonSavable = false;
        boolean isNonLoadableSavable = false;
        boolean isNonLoadableNonSavable = false;
        Iterator<? extends Class<?>> classStream = list.stream()
                .map(TreeUtils::annotationFromAnnotationTree)
                .map(a -> a.getAnnotationType().asElement().asType())
                .map(TypesUtils::getClassFromType).iterator();
        while(classStream.hasNext() && !(isLoadableSavable && isLoadableNonSavable && isNonLoadableSavable && isNonLoadableNonSavable)){
            Class<?> a = classStream.next();
            isNonLoadableNonSavable |= a.isAssignableFrom(NonLoadableNonSaveable.class);
            isLoadableNonSavable |= a.isAssignableFrom(LoadableNonSaveable.class);
            isNonLoadableSavable |= a.isAssignableFrom(NonLoadableSavable.class);
            isLoadableSavable |= a.isAssignableFrom(LoadableSaveable.class);
        }
        final boolean addAnnotation = !isLoadableSavable && !isLoadableNonSavable && !isNonLoadableSavable && !isNonLoadableNonSavable;
        if (isLoadableSavable && isLoadableNonSavable) {
            checker.reportError(tree, "loadable.savable.conflict", "LoadableSavable", "LoadableNonSaveable");
        }else if (isLoadableSavable && isNonLoadableSavable) {
            checker.reportError(tree, "loadable.savable.conflict", "LoadableSavable", "NonLoadableSavable");
        }else if (isLoadableSavable && isNonLoadableNonSavable) {
            checker.reportError(tree, "loadable.savable.conflict", "LoadableSavable", "NonLoadableNonSaveable");
        }else if (isLoadableNonSavable && isNonLoadableNonSavable) {
            checker.reportError(tree, "loadable.savable.conflict", "LoadableNonSaveable", "NonLoadableNonSaveable");
        }else if (isLoadableNonSavable && isNonLoadableSavable) {
            checker.reportError(tree, "loadable.savable.conflict", "LoadableSavable", "NonLoadableSavable");
        }else if (isNonLoadableSavable && isNonLoadableNonSavable) {
            checker.reportError(tree, "loadable.savable.conflict", "NonLoadableSavable", "NonLoadableNonSaveable");
        }

        final boolean loadable = isLoadableSavable || isLoadableNonSavable;
        final boolean savable = isLoadableSavable || isNonLoadableSavable;

        //general structure of this loop was stolen from the BaseTypeVisitor
        //https://github.com/typetools/checker-framework/blob/3d37a42774946f21ab8a7a27e3251278961a3e40/framework/src/main/java/org/checkerframework/common/basetype/BaseTypeVisitor.java#L2591
        Tree t = tree;
        while (true) {
            switch (t.getKind()) {

                // Recurse for compound types whose top level is not at the far left.
                case ARRAY_TYPE:
                    t = ((ArrayTypeTree) t).getType();
                    continue;
                case MEMBER_SELECT:
                    t = ((MemberSelectTree) t).getExpression();
                    continue;
                case PARAMETERIZED_TYPE:
                    t = ((ParameterizedTypeTree) t).getType();
                    continue;

                    // Base cases
                case ANNOTATED_TYPE:
                    AnnotatedTypeTree at = (AnnotatedTypeTree) t;
                    TypeMirror typeAnnotatedType = TreeUtils.typeOf(t);
                    if (addAnnotation && typeAnnotatedType instanceof AnnotatedTypeMirror) ((AnnotatedTypeMirror) typeAnnotatedType).addAnnotation(NonLoadableNonSaveable.class);
                    t = at.getUnderlyingType();
                case PRIMITIVE_TYPE:
                case IDENTIFIER:
                    TypeMirror type = TreeUtils.typeOf(t);
                    if (addAnnotation && type instanceof AnnotatedTypeMirror) ((AnnotatedTypeMirror) type).addAnnotation(NonLoadableNonSaveable.class);
                    final Class<?> targetType = TypesUtils.getClassFromType(type);
                    final boolean assignableLS = IConfigLoadableSaveable.class.isAssignableFrom(targetType);
                    final boolean assignableL = IConfigLoadable.class.isAssignableFrom(targetType);
                    final boolean assignableS = IConfigSavable.class.isAssignableFrom(targetType);
                    if (!assignableLS && assignableL && assignableS)
                        checker.reportError(t, "loadableSaveable.improperimpl", targetType.getCanonicalName(), IConfigLoadable.class.getCanonicalName(), IConfigSavable.class.getCanonicalName(), IConfigLoadableSaveable.class.getCanonicalName());
                    if (assignableLS && (!loadable || !savable || !isLoadableSavable))
                        checker.reportError(t, "loadableSavable.more", targetType.getCanonicalName(), IConfigLoadableSaveable.class.getCanonicalName(), LoadableSaveable.class.getCanonicalName());
                    if (assignableL && !loadable){
                        checker.reportError(t, "loadableSavable.more", targetType.getCanonicalName(), IConfigLoadable.class.getCanonicalName(), LoadableNonSaveable.class.getCanonicalName());
                    }
                    if (assignableS && !savable) {
                        checker.reportError(t, "loadableSavable.more", targetType.getCanonicalName(), IConfigSavable.class.getCanonicalName(), NonLoadableSavable.class.getCanonicalName());
                    }
                    if (loadable && savable && !(assignableLS || (assignableL && assignableS))) {
                        checker.reportError(t, "loadableSavable.assignment", LoadableSaveable.class.getCanonicalName(), IConfigLoadableSaveable.class.getCanonicalName(), targetType.getCanonicalName());
                    }
                    if (!loadable && savable && !assignableS)
                        checker.reportError(t, "loadableSavable.assignment", NonLoadableSavable.class.getCanonicalName(), IConfigSavable.class.getCanonicalName(), targetType.getCanonicalName());
                    if (loadable && !savable && !assignableL)
                        checker.reportError(t, "loadableSavable.assignment", LoadableNonSaveable.class.getCanonicalName(), IConfigLoadable.class.getCanonicalName(), targetType.getCanonicalName());
                    return;
                default:
                    return;
            }
        }
    }
}
