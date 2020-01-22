package io.johnsonlee.booster.transform.serviceloader

import com.didiglobal.booster.kotlinx.stream
import com.didiglobal.booster.transform.TransformContext
import com.didiglobal.booster.transform.asm.ClassTransformer
import com.didiglobal.booster.transform.asm.findAll
import com.didiglobal.booster.util.search
import com.google.auto.service.AutoService
import io.johnsonlee.spi.ShadowServiceLoader
import jdk.internal.org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
import java.util.ServiceLoader
import java.util.jar.JarFile
import kotlin.streams.toList

/**
 * Represents a transformer which used to replace [java.util.ServiceLoader] with [ShadowServiceLoader]
 *
 * @author johnsonlee
 */
@AutoService(ClassTransformer::class)
class ServiceLoaderTransformer : ClassTransformer {

    private val registry = mutableMapOf<String, MutableList<String>>()

    override fun onPreTransform(context: TransformContext) {
        context.compileClasspath.parallelStream().map { file ->
            when {
                // search from directory
                file.isDirectory -> file.search {
                    it.parentFile.name == "services" && it.parentFile.parentFile.name == "META-INF"
                }.map {
                    it.name to it.readLines().filter(::checkImpl)
                }
                // search from jar files
                file.isFile && file.extension == "jar" -> JarFile(file).use { jar ->
                    jar.entries().iterator().stream().filter {
                        it.name.length > META_INFO_SERVICES.length && it.name.startsWith(META_INFO_SERVICES)
                    }.map {
                        it.name.substringAfter(META_INFO_SERVICES) to jar.getInputStream(it).bufferedReader().use { reader ->
                            reader.readLines().filter(::checkImpl)
                        }
                    }.toList()
                }
                else -> emptyList()
            }
        }.flatMap {
            it.stream()
        }.forEach { (service, providers) ->
            registry.computeIfAbsent(service) {
                mutableListOf()
            } += providers
        }
    }

    override fun transform(context: TransformContext, klass: ClassNode) = when {
        klass.name.substringBefore('/') in PACKAGES_IGNORED -> klass
        klass.name == SERVICE_REGISTRY -> context.transformServiceRegistry(klass)
        klass.name == SHADOW_SERVICE_LOADER -> klass
        else -> context.transformAppClass(klass)
    }

    /**
     * Register provider creator into ServiceRegistry
     *
     * ```java
     * static {
     *    ......
     *    registry(A.class, new AImpl_Creator())
     *    registry(B.class, new BImpl_Creator())
     *    ......
     *    registry(X.class, new XImpl_Creator())
     * }
     * ```
     */
    private fun TransformContext.transformServiceRegistry(klass: ClassNode): ClassNode {
        val clinit = klass.methods.find {
            it.name == "<clinit>" && it.desc == "()V"
        } ?: MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC, SERVICE_REGISTRY, "<clinit>", "()V", arrayOf()).apply {
            this.instructions.add(InsnNode(Opcodes.RETURN))
            klass.methods.add(this)
        }

        clinit.instructions.findAll(Opcodes.RETURN).forEach { ret ->
            registry.flatMap { (k, v) ->
                v.map { k to it }
            }.map { (k, v) ->
                k.replace('.', '/') to v.replace('.', '/') + "_Creator"
            }.forEach { (provider, creator) ->
                clinit.instructions.insertBefore(ret, InsnList().apply {
                    add(LdcInsnNode(Type.getType("L${provider};")))
                    add(TypeInsnNode(Opcodes.NEW, creator))
                    add(InsnNode(Opcodes.DUP))
                    add(MethodInsnNode(Opcodes.INVOKESPECIAL, creator, "<init>", "()V"))
                    add(MethodInsnNode(Opcodes.INVOKESTATIC, SERVICE_REGISTRY, "register", "(Ljava/lang/Class;Ljava/util/concurrent/Callable;)V", false))
                })
            }
        }

        return klass
    }

    private fun TransformContext.transformAppClass(klass: ClassNode): ClassNode {
        klass.fields.forEach { field ->
            field.apply {
                this.desc = desc.shadow()
                this.signature?.let {
                    this.signature = it.shadow()
                }
            }
        }

        klass.methods.forEach { method ->
            method.apply {
                this.desc = desc.shadow()
                this.signature?.let {
                    this.signature = it.shadow()
                }
                this.instructions.iterator().forEach { insn ->
                    insn.apply {
                        when (this) {
                            is FieldInsnNode -> {
                                this.desc = this.desc.shadow()
                            }
                            is MethodInsnNode -> {
                                this.owner = this.owner.shadow()
                                this.desc = this.desc.shadow()
                            }
                            is TypeInsnNode -> {
                                this.desc = this.desc.shadow()
                            }
                        }

                    }
                }
            }
        }

        return klass
    }

}

private fun checkImpl(line: String) = line.isNotBlank() && !line.startsWith("#")

private val PACKAGES_IGNORED = setOf("android", "androidx", "kotlin")

private const val META_INFO_SERVICES = "META-INF/services/"

internal val SERVICE_LOADER = ServiceLoader::class.java.name.replace('.', '/')

internal const val SERVICE_REGISTRY = "io/johnsonlee/spi/ServiceRegistry"

internal val SHADOW_SERVICE_LOADER = ShadowServiceLoader::class.java.name.replace('.', '/')

private fun String.shadow() = this.replace(SERVICE_LOADER, SHADOW_SERVICE_LOADER)
