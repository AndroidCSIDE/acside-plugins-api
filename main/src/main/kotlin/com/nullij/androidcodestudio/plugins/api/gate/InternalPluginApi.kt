package com.nullij.androidcodestudio.plugins.api.gate

/**
 * Marks IDE-internal APIs that must not be called from plugin code.
 *
 * Calling any symbol annotated with [@InternalPluginApi] without explicitly
 * opting in will cause a compile-time error.
 */
@RequiresOptIn(
    level   = RequiresOptIn.Level.ERROR,
    message = "This is an internal IDE API. Plugin code must never call it."
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR
)
annotation class InternalPluginApi
