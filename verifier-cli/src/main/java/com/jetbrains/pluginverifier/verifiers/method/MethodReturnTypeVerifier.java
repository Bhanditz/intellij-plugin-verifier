package com.jetbrains.pluginverifier.verifiers.method;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Sergey Patrikeev
 */
public class MethodReturnTypeVerifier implements MethodVerifier {
  @Override
  public void verify(ClassNode clazz, MethodNode method, Resolver resolver, VerificationContext ctx) {
    Type methodType = Type.getType(method.desc);
    Type returnType = methodType.getReturnType();

    String descriptor = returnType.getDescriptor();
    if ("V".equals(descriptor)) return; //void return type

    String returnTypeDesc = VerifierUtil.extractClassNameFromDescr(descriptor);
    if (returnTypeDesc == null) return;

    if (!VerifierUtil.classExists(ctx.getOptions(), resolver, returnTypeDesc)) {
      ctx.registerProblem(new ClassNotFoundProblem(returnTypeDesc), ProblemLocation.fromMethod(clazz.name, method.name + method.desc));
    }

  }
}
