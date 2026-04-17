package com.nullij.androidcodestudio.plugins.api.gate

/**
 * Marks IDE-internal APIs that plugin developers must never call.
 *
 * Any code that calls a symbol annotated with @InternalPluginApi without
 * explicitly opting in will fail to compile:
 *
 *   error: This declaration is opt-in and its usage must be marked with
 *   @InternalPluginApi or @OptIn(InternalPluginApi::class)
 *
 * The opt-in mechanism is a compile-time barrier. It is paired with a
 * runtime ClassLoader check inside PluginApi.wire() for defence-in-depth.
 *
 * ╔══════════════════════════════════════════════════════════╗
 * ║  Plugin developers: DO NOT call anything marked with     ║
 * ║  @InternalPluginApi. It will break without notice.       ║
 * ╚══════════════════════════════════════════════════════════╝
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
