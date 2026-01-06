@file:Suppress("unused")

package de.robv.android.xposed

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Helper methods for Xposed modules to find and hook classes/methods.
 */
object XposedHelpers {

    /**
     * Fallback ClassLoader when null is passed.
     */
    @JvmStatic
    val BOOTCLASSLOADER: ClassLoader = ClassLoader.getSystemClassLoader()

    /**
     * Map of primitive to wrapper types for method matching.
     */
    private val WRAPPER_TYPES = mapOf<Class<*>?, Class<*>>(
        Int::class.javaPrimitiveType to Int::class.javaObjectType,
        Long::class.javaPrimitiveType to Long::class.javaObjectType,
        Boolean::class.javaPrimitiveType to Boolean::class.javaObjectType,
        Float::class.javaPrimitiveType to Float::class.javaObjectType,
        Double::class.javaPrimitiveType to Double::class.javaObjectType,
        Byte::class.javaPrimitiveType to Byte::class.javaObjectType,
        Short::class.javaPrimitiveType to Short::class.javaObjectType,
        Char::class.javaPrimitiveType to Char::class.javaObjectType
    )

    /**
     * Find a class by name using the given class loader.
     */
    @JvmStatic
    fun findClass(className: String, classLoader: ClassLoader?): Class<*> {
        val loader = classLoader ?: BOOTCLASSLOADER
        return Class.forName(className, false, loader)
    }

    /**
     * Find a class by name, returning null if not found.
     */
    @JvmStatic
    fun findClassIfExists(className: String, classLoader: ClassLoader?): Class<*>? {
        return try {
            findClass(className, classLoader)
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    /**
     * Find and hook a method.
     */
    @JvmStatic
    fun findAndHookMethod(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypesAndCallback: Any
    ): XC_MethodHook.Unhook {
        val callback = parameterTypesAndCallback.last() as XC_MethodHook
        val parameterTypes = parameterTypesAndCallback.dropLast(1).map {
            when (it) {
                is Class<*> -> it
                is String -> findClass(it, clazz.classLoader)
                else -> throw IllegalArgumentException("Invalid parameter type: $it")
            }
        }.toTypedArray()

        val method = clazz.getDeclaredMethod(methodName, *parameterTypes)
        return XposedBridge.hookMethod(method, callback)
    }

    /**
     * Find and hook a method by class name.
     */
    @JvmStatic
    fun findAndHookMethod(
        className: String,
        classLoader: ClassLoader?,
        methodName: String,
        vararg parameterTypesAndCallback: Any
    ): XC_MethodHook.Unhook {
        val clazz = findClass(className, classLoader)
        return findAndHookMethod(clazz, methodName, *parameterTypesAndCallback)
    }

    /**
     * Find and hook a constructor.
     */
    @JvmStatic
    fun findAndHookConstructor(
        clazz: Class<*>,
        vararg parameterTypesAndCallback: Any
    ): XC_MethodHook.Unhook {
        val callback = parameterTypesAndCallback.last() as XC_MethodHook
        val parameterTypes = parameterTypesAndCallback.dropLast(1).map {
            when (it) {
                is Class<*> -> it
                is String -> findClass(it, clazz.classLoader)
                else -> throw IllegalArgumentException("Invalid parameter type: $it")
            }
        }.toTypedArray()

        val constructor = clazz.getDeclaredConstructor(*parameterTypes)
        return XposedBridge.hookMethod(constructor, callback)
    }

    /**
     * Get a field value from an object.
     */
    @JvmStatic
    fun getObjectField(obj: Any, fieldName: String): Any? {
        val field = findField(obj.javaClass, fieldName)
        return field.get(obj)
    }

    /**
     * Set a field value on an object.
     */
    @JvmStatic
    fun setObjectField(obj: Any, fieldName: String, value: Any?) {
        val field = findField(obj.javaClass, fieldName)
        field.set(obj, value)
    }

    /**
     * Get a static field value.
     */
    @JvmStatic
    fun getStaticObjectField(clazz: Class<*>, fieldName: String): Any? {
        val field = findField(clazz, fieldName)
        return field.get(null)
    }

    /**
     * Set a static field value.
     */
    @JvmStatic
    fun setStaticObjectField(clazz: Class<*>, fieldName: String, value: Any?) {
        val field = findField(clazz, fieldName)
        field.set(null, value)
    }

    /**
     * Find a field by name, searching through superclasses.
     */
    @JvmStatic
    fun findField(clazz: Class<*>, fieldName: String): Field {
        var currentClass: Class<*>? = clazz
        while (currentClass != null) {
            try {
                val field = currentClass.getDeclaredField(fieldName)
                field.isAccessible = true
                return field
            } catch (e: NoSuchFieldException) {
                currentClass = currentClass.superclass
            }
        }
        throw NoSuchFieldException("Field $fieldName not found in ${clazz.name}")
    }

    /**
     * Call a method on an object.
     */
    @JvmStatic
    fun callMethod(obj: Any, methodName: String, vararg args: Any?): Any? {
        val argTypes = args.map { it?.javaClass }.toTypedArray()
        val method = findMethodBestMatch(obj.javaClass, methodName, *argTypes)
        return method.invoke(obj, *args)
    }

    /**
     * Call a static method.
     */
    @JvmStatic
    fun callStaticMethod(clazz: Class<*>, methodName: String, vararg args: Any?): Any? {
        val argTypes = args.map { it?.javaClass }.toTypedArray()
        val method = findMethodBestMatch(clazz, methodName, *argTypes)
        return method.invoke(null, *args)
    }

    /**
     * Find the best matching method for the given argument types.
     */
    @JvmStatic
    fun findMethodBestMatch(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>?
    ): Method {
        var currentClass: Class<*>? = clazz
        while (currentClass != null) {
            for (method in currentClass.declaredMethods) {
                if (method.name == methodName && isAssignable(method.parameterTypes, parameterTypes)) {
                    method.isAccessible = true
                    return method
                }
            }
            currentClass = currentClass.superclass
        }
        throw NoSuchMethodException("Method $methodName not found in ${clazz.name}")
    }

    private fun isAssignable(paramTypes: Array<Class<*>>, argTypes: Array<out Class<*>?>): Boolean {
        if (paramTypes.size != argTypes.size) return false
        return paramTypes.zip(argTypes.toList()).all { (param, arg) ->
            arg == null || param.isAssignableFrom(arg) || isWrapperType(param, arg)
        }
    }

    private fun isWrapperType(param: Class<*>, arg: Class<*>): Boolean {
        return WRAPPER_TYPES[param] == arg || WRAPPER_TYPES.entries.find { it.value == param }?.key == arg
    }
}
