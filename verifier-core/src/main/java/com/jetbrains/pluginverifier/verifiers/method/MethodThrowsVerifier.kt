package com.jetbrains.pluginverifier.verifiers.method

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.checkClassExistsOrExternal
import com.jetbrains.pluginverifier.verifiers.createMethodLocation
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescr
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class MethodThrowsVerifier : MethodVerifier {
  @Suppress("UNCHECKED_CAST")
  override fun verify(clazz: ClassNode, method: MethodNode, ctx: VerificationContext) {
    val exceptions = method.exceptions as List<String>
    for (exception in exceptions) {
      val descr = exception.extractClassNameFromDescr() ?: continue
      ctx.checkClassExistsOrExternal(descr, { createMethodLocation(clazz, method) })
    }
  }
}
