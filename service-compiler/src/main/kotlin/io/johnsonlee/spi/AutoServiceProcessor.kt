package io.johnsonlee.spi

import com.google.auto.common.AnnotationMirrors.getAnnotationValue
import com.google.auto.common.MoreElements.getAnnotationMirror
import com.google.auto.common.MoreTypes
import com.google.auto.service.AutoService
import com.google.auto.service.processor.AutoServiceProcessor
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import java.io.PrintWriter
import java.io.StringWriter
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedOptions
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleAnnotationValueVisitor8
import javax.tools.Diagnostic

/**a
 * Processes {@link AutoService} annotations and generates the service provider creator class
 *
 * @author johnsonlee
 */
@Suppress("UnstableApiUsage")
@SupportedOptions("debug", "verify")
class AutoServiceProcessor : AutoServiceProcessor() {

    private val providers = mutableMapOf<String, MutableList<String>>()

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        return super.process(annotations, roundEnv) && try {
            if (roundEnv.processingOver()) {
                generateProviderCreators()
            } else {
                processAnnotations(roundEnv)
            }
            true
        } catch (e: Exception) {
            processingEnv.fatalError(e)
            true
        }
    }

    private fun processAnnotations(roundEnv: RoundEnvironment) {
        roundEnv.getElementsAnnotatedWith(AutoService::class.java).also {
            processingEnv.debug(it.joinToString(", "))
        }.map {
            it as TypeElement
        }.forEach { implementer ->
            val annotation = getAnnotationMirror(implementer, AutoService::class.java).get()
            val providers = annotation.valueFieldOfClasses
            if (providers.isEmpty()) {
                processingEnv.error("No service interfaces provided for element!", implementer, annotation)
            } else {
                providers.forEach {
                    val provider = MoreTypes.asTypeElement(it)
                    if (processingEnv.checkImplementer(implementer, provider)) {
                        this.providers.getOrPut(provider.getBinaryName(implementer.simpleName.toString())) {
                            mutableListOf()
                        } += implementer.getBinaryName(implementer.simpleName.toString())
                    } else {
                        processingEnv.error("ServiceProviders must implement their service provider interface. ${implementer.qualifiedName} does not implement ${provider.qualifiedName}", implementer, annotation)
                    }
                }
            }
        }
    }

    /**
     * public final class Implementer_Creator implements Callable<Implementer>  {
     *     Implementer call() {
     *         return new Implementer();
     *     }
     * }
     */
    private fun generateProviderCreators() {
        val typeOfCallable = ClassName.get("java.util.concurrent", "Callable")

        this.providers.entries.flatMap { (k, v) ->
            v.map { k to it }
        }.parallelStream().forEach { (_, implementer) ->
            val pkg = implementer.substringBeforeLast('.')
            val name = implementer.substringAfterLast('.')
            val creator = "${name}_Creator"
            val typeOfImplementer = ClassName.get(pkg, name)
            val typeOfCallableOfImplementer = ParameterizedTypeName.get(typeOfCallable, typeOfImplementer)
            val implementerCreator = TypeSpec.classBuilder(creator)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addSuperinterface(typeOfCallableOfImplementer)
                    .addMethod(MethodSpec.methodBuilder("call")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .addStatement("return new " + '$' + "T()", typeOfImplementer)
                            .returns(typeOfImplementer)
                            .build())
                    .build()
            processingEnv.debug("Generating $creator ...")
            JavaFile.builder(pkg, implementerCreator).build().writeTo(processingEnv.filer)
        }
    }

}

fun TypeElement.getBinaryName(className: String): String {
    val enclosingElement = this.enclosingElement
    if (enclosingElement is PackageElement) {
        return if (enclosingElement.isUnnamed) {
            className
        } else {
            enclosingElement.qualifiedName.toString() + "." + className
        }
    }

    val typeElement = enclosingElement as TypeElement
    return typeElement.getBinaryName(typeElement.simpleName.toString() + "$" + className)
}

fun ProcessingEnvironment.checkImplementer(implementer: TypeElement, provider: TypeElement): Boolean {
    val verify: String? = this.options["verify"]
    if (verify?.toBoolean() != true) {
        return true
    }
    return typeUtils.isSubtype(implementer.asType(), provider.asType())
}

fun ProcessingEnvironment.debug(msg: String) {
    if (options.containsKey("debug")) {
        messager.printMessage(Diagnostic.Kind.NOTE, msg)
    }
}

fun ProcessingEnvironment.error(msg: String, element: Element, annotation: AnnotationMirror) {
    messager.printMessage(Diagnostic.Kind.ERROR, msg, element, annotation)
}

fun ProcessingEnvironment.fatalError(e: Throwable) {
    val stacktrace = StringWriter().use {
        e.printStackTrace(PrintWriter(it))
    }.toString()
    messager.printMessage(Diagnostic.Kind.ERROR, stacktrace)
}

val AnnotationMirror.valueFieldOfClasses: Set<DeclaredType>
    get() = getAnnotationValue(this, "value").accept(object : SimpleAnnotationValueVisitor8<Set<DeclaredType>, Any?>() {
        override fun visitType(typeMirror: TypeMirror, v: Any?): Set<DeclaredType>? {
            return setOf(MoreTypes.asDeclared(typeMirror))
        }

        override fun visitArray(values: List<AnnotationValue>, v: Any?): Set<DeclaredType>? {
            return values.flatMap { value ->
                value.accept(this, null)
            }.toSet()
        }
    }, null)
