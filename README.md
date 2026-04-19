# Android Code Studio - Plugin API Developer Guide

Hey! Welcome to the Android Code Studio Plugin API docs. This guide covers everything you need to know to build plugins that interact with the IDE. Whether you want to mess with the editor, launch processes, show custom UI, or even register your own language server, this is where you'll find it all.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [PluginApi - The Entry Point](#pluginapi---the-entry-point)
3. [PluginContext - Your Sandboxed Context](#plugincontext---your-sandboxed-context)
4. [IEditorApi - Working with the Editor](#ieditorapi---working-with-the-editor)
5. [IEnvironmentApi - Paths and Environment](#ienvironmentapi---paths-and-environment)
6. [ILspApi - Language Server Protocol](#ilspapi---language-server-protocol)
7. [IProcessApi - Launching Processes](#iprocessapi---launching-processes)
8. [ITemplateApi - Project and File Templates](#itemplateapi---project-and-file-templates)
9. [IUiApi - Custom Compose UI Overlays](#iuiapi---custom-compose-ui-overlays)
10. [Security Model](#security-model)
11. [Common Patterns and Examples](#common-patterns-and-examples)
12. [Things You Should Never Do](#things-you-should-never-do)

---

## Getting Started

Plugins interact with the IDE exclusively through the `PluginApi` object. You never get a direct reference to any internal IDE class. That's intentional, the API surface is designed to be stable, safe, and versioned.


#### Add the API to your plugin's Gradle build file
[![](https://jitpack.io/v/AndroidCSIDE/acside-plugins-api.svg)](https://jitpack.io/#AndroidCSIDE/acside-plugins-api)

For Groovy:
```groovy
// Plugins block
plugins {
    ...
    // For packaging and producing a .acp plugin file
    id 'io.github.nullij.acside-gradle-plugin' version '0.2.0'
}

dependencies {
    ...
    implementation 'com.github.AndroidCSIDE:acside-plugins-api:0.1.0'
}
```

If you're using the Gradle Kotlin DSL:
```kts
// Plugins block
plugins {
    ...
    id("io.github.nullij.acside-gradle-plugin").version("0.2.0")
}

dependencies {
    ...
    implementation("com.github.AndroidCSIDE:acside-plugins-api:0.1.0")
}
```

Ensure that JitPack is added to your Gradle settings file.

For Groovy:
```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ...
        // for acside-plugin-api
        maven { url 'https://jitpack.io' }
    }
}
```

For Kotlin DSL:
```kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ...
        maven { url = uri("https://jitpack.io") }
    }
}
```

The IDE wires everything up before your plugin action fires, so you don't need to do any setup. Just access what you need through `PluginApi`.

```kotlin
import com.nullij.androidcodestudio.plugins.api.PluginApi

// That's it. Just use it.
val editor = PluginApi.editor
val env = PluginApi.environment
```

> **Important:** All accessors throw `IllegalStateException` if the IDE hasn't initialized yet. In practice this won't happen during normal plugin execution, but if you're doing something unusual (like background threads that outlive your action), keep it in mind.

---

## PluginApi - The Entry Point

`PluginApi` is a Kotlin singleton (`object`) that gives you access to all the sub-APIs.

```kotlin
object PluginApi {
    val editor: IEditorApi
    val environment: IEnvironmentApi
    val lsp: ILspApi?          // null outside EditorActivity
    val templates: ITemplateApi
    val process: IProcessApi
    val ui: IUiApi?            // null outside EditorActivity
}
```

### Which ones can be null?

`lsp` and `ui` can be null. Both of them only exist when the user is inside `EditorActivity` (i.e. has a file open in the editor). If your plugin action can be triggered from the home screen or a project list, always null-check these two before using them.

```kotlin
// Safe pattern
PluginApi.lsp?.let { lsp ->
    // We're inside the editor, lsp is available
    lsp.startServer("kotlin")
}

// Also fine
val ui = PluginApi.ui ?: return  // bail early if not in editor
ui.showOverlay { handle ->
    MyDialog(onDismiss = { handle.dismiss() })
}
```

The rest (`editor`, `environment`, `templates`, `process`) are always available and never null once the IDE boots.

---

## PluginContext - Your Sandboxed Context

Your plugin entry point receives a `PluginContext` instead of a raw Android `Context`. This is on purpose. The IDE does not want plugins holding references to `Activity` objects, launching other activities, or binding to arbitrary services.

### What you can do with PluginContext

```kotlin
// Get the app package name
val pkg = context.packageName

// Post work to the main (UI) thread
context.runOnUiThread {
    // Update UI state, show a Toast, etc.
}

// Get a safe applicationContext (not the real one)
val appCtx = context.applicationContext
```

### System services you're allowed to use

`getSystemService()` is whitelisted. Only these work:

| Service | Constant |
|---|---|
| Clipboard | `Context.CLIPBOARD_SERVICE` |
| Connectivity | `Context.CONNECTIVITY_SERVICE` |
| Input Method | `Context.INPUT_METHOD_SERVICE` |
| Vibrator | `Context.VIBRATOR_SERVICE` |
| Notifications | `Context.NOTIFICATION_SERVICE` |

Anything else throws `SecurityException`. No sneaking around it by calling `applicationContext.getSystemService()` either, that's sandboxed too.

### What you cannot do (and why)

The `PluginSandboxContext` (which is what `applicationContext` returns) blocks a bunch of things:

- `startActivity()` - plugins can't launch activities
- `startService()` / `bindService()` - no binding to services
- `sendBroadcast()` - no sending broadcasts
- `registerReceiver()` - no registering broadcast receivers
- `getContentResolver()` - no raw content resolver access

If you need to interact with content or show new screens, use the dedicated IDE APIs instead.

---

## IEditorApi - Working with the Editor

Access via `PluginApi.editor`. All methods are thread-safe, the implementation handles marshaling internally.

### Checking if the editor is ready

Always do this first if there's any chance the editor might not be visible.

```kotlin
if (!PluginApi.editor.isAvailable()) {
    // No editor open, bail out or show a message
    return
}
```

### Reading and writing text

```kotlin
val editor = PluginApi.editor

// Get the full file content
val content = editor.getText()

// Replace the entire file content (goes into undo history)
editor.setText("fun main() {\n    println(\"Hello\")\n}\n")
```

### Cursor position

Cursor positions are 0-indexed internally. The `Display` variants give you 1-indexed values for showing in a status bar.

```kotlin
// 0-indexed
val line = editor.getCurrentLine()
val col = editor.getCurrentColumn()

// 1-indexed (for UI display)
val lineDisplay = editor.getCurrentLineDisplay()
val colDisplay = editor.getCurrentColumnDisplay()

// Move the cursor (0-indexed, clamped silently if out of bounds)
editor.setCursor(line = 5, column = 0)
```

### Selection

```kotlin
if (editor.hasSelection()) {
    val selected = editor.getSelectedText()
    // Do something with selected text
}

// Select everything
editor.selectAll()
```

### Clipboard operations

```kotlin
editor.copy()   // copies selection, no-op if nothing selected
editor.cut()    // cuts selection, no-op if nothing selected
editor.paste()  // pastes at cursor
```

### Undo / Redo

```kotlin
if (editor.canUndo()) editor.undo()
if (editor.canRedo()) editor.redo()
```

### Inserting and deleting text

```kotlin
// Insert at current cursor - preferred way to inject code snippets
editor.insertText("// TODO: implement this\n")

// Delete currently selected text (no-op if nothing selected)
editor.deleteSelection()
```

### Formatting

```kotlin
// Triggers the language server formatter asynchronously
editor.formatDocument()
```

---

## IEnvironmentApi - Paths and Environment

Access via `PluginApi.environment`. Gives you the file paths the IDE uses internally. Everything is read-only from the plugin side.

### Project directories

```kotlin
val env = PluginApi.environment

// The currently open project (null if no project open or outside EditorActivity)
val project = env.openProjectDir

// The main projects directory on external storage
val projects = env.projectsDir       // ~/AndroidCSProjects

// Internal storage projects root
val acsRoot = env.acsRootProjects    // $HOME/AndroidCSProjects (internal)
```

### SDK and toolchain paths

```kotlin
val sdk = env.androidSdkDir   // $HOME/Android/Sdk
val flutter = env.flutterDir  // $HOME/flutter
```

### App-private directories

```kotlin
val files = env.filesDir      // Context.filesDir
val home = env.homeDir        // $filesDir/home
val local = env.localDir      // $filesDir/localenv
val tmp = env.tmpDir          // $localDir/tmp
```

### Rootfs paths (for processes running inside proot)

These are string paths as they appear inside the proot environment, not host filesystem paths.

```kotlin
val sdkInRootfs = env.rootfsAndroidSdkPath   // Android SDK path inside rootfs
val javaHome = env.rootfsJavaHome            // JAVA_HOME inside rootfs
```

### Environment variables

```kotlin
// Get the full env map the IDE uses for subprocesses
val envMap = env.getEnvironment()

// Merge in your own overrides
val envMap = env.getEnvironment(
    additionalEnv = mapOf("MY_VAR" to "my_value")
)
```

### Checking initialization status

```kotlin
if (!env.isInitialized()) {
    // IDE environment isn't ready yet
    return
}
```

---

## ILspApi - Language Server Protocol

Access via `PluginApi.lsp`. **This is null outside of EditorActivity.** Always null-check it.

The LSP API lets you query and control language servers, register your own, and send document lifecycle events.

### Checking server status

```kotlin
val lsp = PluginApi.lsp ?: return

// Is there a server registered for this language?
lsp.hasServer("kotlin")      // true/false

// Is it actually running right now?
lsp.isServerRunning("kotlin") // true/false

// What servers exist / are running?
val available = lsp.getAvailableServers()   // Set<String>
val running = lsp.getRunningServers()        // Set<String>
```

### Starting and stopping servers

```kotlin
val started = lsp.startServer("kotlin")  // returns true if started or already running
lsp.stopServer("kotlin")
lsp.stopAllServers()
```

### Document events

These tell the registry what's happening with files. Usually the IDE calls these automatically, but plugins can trigger them too.

```kotlin
val file = File("/path/to/MyFile.kt")

lsp.openDocument(file)       // IDE will start the right server if needed
lsp.closeDocument(file)
lsp.documentChanged(file, content = "...", version = 2)  // version must be increasing
```

### Detecting language from a file

```kotlin
val langId = lsp.detectLanguage(file)         // from a File object
val langId2 = lsp.detectLanguage("Main.kt")  // from a filename string
// Returns null if unrecognized
```

### Registering your own language server

This is the main extension point for plugins that want to add support for a new language. Implement `PluginLanguageServerSpec` and register it.

```kotlin
class MyLspServer : PluginLanguageServerSpec {
    override val languageId = "mylang"

    override fun start(): Boolean {
        // Start your LSP process here
        return true
    }

    override fun stop() {
        // Stop it
    }

    override fun isRunning(): Boolean = /* ... */

    override fun getClient(): LanguageServerClient {
        // Return a LanguageServerClient wrapper around your actual client
        return myClient
    }

    override fun openDocument(file: File): Boolean {
        // Notify your server
        return true
    }

    override fun closeDocument(file: File) { /* ... */ }

    override fun documentChanged(file: File, content: String, version: Int) { /* ... */ }
}

// Register it
lsp.registerServer("mylang", MyLspServer())

// Associate file extensions
lsp.registerExtension("ml", "mylang")
lsp.registerExtension("mli", "mylang")

// Check current registrations
val extensions = lsp.getRegisteredExtensions()  // Map<String, String>

// Clean up
lsp.unregisterServer("mylang")
lsp.unregisterExtension("ml")
```

---

## IProcessApi - Launching Processes

Access via `PluginApi.process`. Used to launch executables inside the IDE's proot / acsenv environment. The IDE handles the environment setup, you just describe what to run.

### Basic usage

```kotlin
val process = PluginApi.process
    .builder()
    .command("/bin/bash-language-server", "start")
    .attachStorage()
    .withEnv(mapOf("HOME" to "/root"))
    .launch()
```

### Builder methods

```kotlin
val process = PluginApi.process
    .builder()

    // The executable + arguments, as they appear inside the rootfs
    .command("/usr/local/bin/pylsp")

    // Bind a host directory into the proot environment
    // If mountAt is omitted, mounts at the same path
    .attachDir(hostDir = File("/data/user/0/my.app/lsp-bins"), mountAt = "/root/lsp")

    // Bind the IDE's internal storage (needed for SDK, home dir, etc.)
    .attachStorage()

    // Merge extra environment variables
    .withEnv(mapOf(
        "LOG_LEVEL" to "debug",
        "SERVER_PORT" to "2087"
    ))

    // Actually launch it - returns a standard java.lang.Process
    .launch()
```

### Working with the returned Process

`launch()` returns a standard `java.lang.Process`, so you work with it the same way you would with any Java process:

```kotlin
val proc = PluginApi.process.builder()
    .command("/bin/my-server", "--stdio")
    .attachStorage()
    .launch()

// Write to stdin
proc.outputStream.bufferedWriter().use { writer ->
    writer.write(jsonRpcMessage)
}

// Read from stdout
val reader = proc.inputStream.bufferedReader()

// Wait for it to finish
val exitCode = proc.waitFor()

// Or kill it
proc.destroy()
```

---

## ITemplateApi - Project and File Templates

Access via `PluginApi.templates`. Lets you add new entries to the New Project / New File wizard.

### Registering a template

```kotlin
val handle = PluginApi.templates.registerTemplate(
    TemplateSpec(
        displayName = "My Custom Activity",
        templateType = "ACTIVITY",
        onCreate = { context, options, rawOptions ->
            // options is typed TemplateOptionsData
            // rawOptions is the raw object for extra fields
            
            val structure = PluginApi.templates.createStandardStructure(
                projectDir = options.saveLocation,
                packageId = options.packageId
            )
            
            // Write your template files
            PluginApi.templates.createFile(
                dir = structure.layoutDir,
                fileName = "activity_my_custom.xml",
                content = myLayoutXml
            )
        }
    )
)

// Later, when your plugin is being cleaned up
PluginApi.templates.unregisterTemplate(handle)
```

### Template types

Common values for `templateType`:

- `"ACTIVITY"` - a new Activity
- `"FRAGMENT"` - a new Fragment
- `"PROJECT"` - a full project

### TemplateOptionsData

When your `onCreate` lambda fires, you get a `TemplateOptionsData` with all the user's choices:

```kotlin
data class TemplateOptionsData(
    val projectName: String,    // What the user named the project
    val packageId: String,      // e.g. "com.example.myapp"
    val minSdk: Int,            // Minimum SDK version chosen
    val useKts: Boolean,        // Whether to use Kotlin Script build files
    val saveLocation: File,     // Where to create the project
    val languageType: String    // "KOTLIN" or "JAVA"
)
```

### File and directory helpers

```kotlin
val templates = PluginApi.templates

// Create a chain of directories, returns the leaf dir
val dir = templates.createDirectories(
    baseDir = someDir,
    "src", "main", "kotlin", "com", "example"
)

// Write a file
val file = templates.createFile(
    dir = dir,
    fileName = "MainActivity.kt",
    content = """
        package com.example
        
        class MainActivity : AppCompatActivity()
    """.trimIndent()
)

// Build the whole standard Android project structure at once
val structure = templates.createStandardStructure(
    projectDir = options.saveLocation,
    packageId = options.packageId
)
// structure.projectDir, .mainSrcDir, .javaDir, .resDir, .layoutDir,
// .valuesDir, .drawableDir, .manifestFile, .packageId, .packagePath
```

### ProjectStructure fields

```kotlin
data class ProjectStructure(
    val projectDir: File,
    val mainSrcDir: File,    // src/main
    val javaDir: File,       // src/main/java (or kotlin)
    val resDir: File,        // src/main/res
    val layoutDir: File,     // src/main/res/layout
    val valuesDir: File,     // src/main/res/values
    val drawableDir: File,   // src/main/res/drawable
    val manifestFile: File,  // src/main/AndroidManifest.xml
    val packageId: String,
    val packagePath: String  // e.g. "com/example/myapp"
)
```

### Reading extra options

If your plugin is invoked via `templates.json` (dispatch-based), the options come in as raw `Any`. Use `extractOptions` to get a typed object:

```kotlin
onCreate = { context, _, rawOptions ->
    val options = PluginApi.templates.extractOptions(rawOptions)
    // Now options is a TemplateOptionsData
}
```

---

## IUiApi - Custom Compose UI Overlays

Access via `PluginApi.ui`. **This is null outside of EditorActivity.** Always null-check.

Lets your plugin show arbitrary Jetpack Compose UI on top of the editor. You don't own the `ComposeView` or `Recomposer`, you just contribute a `@Composable` lambda that runs inside the existing composition.

### Showing an overlay

```kotlin
val ui = PluginApi.ui ?: return

val handle = ui.showOverlay { overlayHandle ->
    // This is a @Composable lambda
    MyPluginDialog(
        onConfirm = { /* do stuff */ overlayHandle.dismiss() },
        onCancel = { overlayHandle.dismiss() }
    )
}
```

### Dismissing from outside

```kotlin
// If you held on to the handle
handle.dismiss()

// Or dismiss everything at once
ui.dismissAll()
```

### OverlayHandle

```kotlin
interface OverlayHandle {
    fun dismiss()           // Remove from composition. Safe to call multiple times.
    val isShowing: Boolean  // True while still visible
}
```

### Threading

`showOverlay()` must be called from the main thread. If you're on a background thread, use `PluginContext.runOnUiThread`:

```kotlin
context.runOnUiThread {
    PluginApi.ui?.showOverlay { handle ->
        MyDialog(onDismiss = { handle.dismiss() })
    }
}
```

---

## Security Model

The plugin system has multiple layers of protection. Here's a quick rundown so you understand why certain things don't work.

### PluginContext sandboxing

Plugins never get a raw `Activity` or `Context`. The `PluginContext` and `PluginSandboxContext` wrappers block anything that could let a plugin escape its sandbox (launching activities, binding services, sending broadcasts, accessing content providers directly).

### @InternalPluginApi annotation

Some methods on the public interfaces are marked `@InternalPluginApi`. These are compile-time-blocked for plugin code. If you try to call one, your plugin won't compile. They're there for the IDE's internal bridge layer.

```kotlin
// This will fail to compile in plugin code
PluginApi.templates.callListenerMethod(...)  // compile error

// This too
PluginApi.wire(...)  // not even in your API surface
```

### Runtime ClassLoader check

Even if somehow you bypassed the compile-time annotation check, the `wire()`, `reset()`, `clearLsp()`, and `clearUi()` methods do a runtime ClassLoader check. If a plugin class is on the call stack, a `SecurityException` is thrown. This is the defense-in-depth layer.

### Thread-safety

`PluginApi` uses `@Volatile` fields and `@Synchronized` methods for the internal wiring. As a plugin developer, all the public-facing APIs are safe to call from any thread. The implementations handle thread marshaling.

---

## Common Patterns and Examples

### Check-then-act pattern for editor plugins

```kotlin
fun myPluginAction(context: PluginContext) {
    val editor = PluginApi.editor
    
    if (!editor.isAvailable()) return
    
    val selected = editor.getSelectedText()
    if (selected == null) {
        // Nothing selected, show a prompt or work on whole file
        val all = editor.getText()
        val processed = processText(all)
        editor.setText(processed)
    } else {
        // Work on the selection
        editor.deleteSelection()
        editor.insertText(transform(selected))
    }
}
```

### Starting a language server when a file opens

```kotlin
fun onFileOpened(file: File, context: PluginContext) {
    val lsp = PluginApi.lsp ?: return
    
    val langId = lsp.detectLanguage(file) ?: return
    
    if (!lsp.isServerRunning(langId)) {
        lsp.startServer(langId)
    }
    
    lsp.openDocument(file)
}
```

### Creating a process and reading its output

```kotlin
fun runLinter(file: File): String {
    val env = PluginApi.environment
    
    val proc = PluginApi.process
        .builder()
        .command("/usr/local/bin/my-linter", "--json", file.absolutePath)
        .attachStorage()
        .withEnv(mapOf("LANG" to "en_US.UTF-8"))
        .launch()
    
    val output = proc.inputStream.bufferedReader().readText()
    proc.waitFor()
    return output
}
```

### Showing a result in the editor with a dialog

```kotlin
fun analyzeAndShow(context: PluginContext) {
    val result = runSomeAnalysis()
    
    context.runOnUiThread {
        PluginApi.ui?.showOverlay { handle ->
            ResultDialog(
                result = result,
                onInsert = {
                    PluginApi.editor.insertText(result.suggestion)
                    handle.dismiss()
                },
                onDismiss = { handle.dismiss() }
            )
        }
    }
}
```

### Registering a template with full structure

```kotlin
fun registerMyTemplate() {
    PluginApi.templates.registerTemplate(
        TemplateSpec(
            displayName = "MVVM Activity",
            templateType = "ACTIVITY",
            onCreate = { context, options, rawOptions ->
                val structure = PluginApi.templates.createStandardStructure(
                    projectDir = options.saveLocation,
                    packageId = options.packageId
                )
                
                val pkg = options.packageId
                val name = options.projectName
                
                PluginApi.templates.createFile(
                    dir = structure.javaDir,
                    fileName = "${name}Activity.kt",
                    content = buildActivityContent(pkg, name)
                )
                
                PluginApi.templates.createFile(
                    dir = structure.javaDir,
                    fileName = "${name}ViewModel.kt",
                    content = buildViewModelContent(pkg, name)
                )
                
                PluginApi.templates.createFile(
                    dir = structure.layoutDir,
                    fileName = "activity_${name.lowercase()}.xml",
                    content = buildLayoutContent()
                )
            }
        )
    )
}
```

---

## Things You Should Never Do

Just a quick list of common mistakes to avoid.

**Don't call `@InternalPluginApi` methods.** They're not for you, they'll fail at compile time or throw `SecurityException` at runtime.

```kotlin
// Bad - won't compile
PluginApi.wire(...)
PluginApi.reset()
```

**Don't use `PluginApi.lsp` or `PluginApi.ui` without null-checking.** They are null outside `EditorActivity`.

```kotlin
// Bad - will throw NullPointerException if called from home screen
PluginApi.lsp.startServer("kotlin")

// Good
PluginApi.lsp?.startServer("kotlin")
```

**Don't try to cast `PluginContext.applicationContext` to anything.** You'll get a `PluginSandboxContext`, not a real `Application` or `Activity`.

```kotlin
// Bad - ClassCastException
val app = context.applicationContext as Application
```

**Don't call `getSystemService()` with services outside the whitelist.** It throws `SecurityException`.

```kotlin
// Bad
context.getSystemService(Context.WINDOW_SERVICE)  // not allowed
```

**Don't hold long-lived references to `PluginContext` beyond your action's scope.** The IDE clears things when activities are destroyed, and you don't want to hold a stale reference.

**Don't call `PluginApi.editor` or `PluginApi.environment` before the IDE boots.** In practice this won't happen during normal plugin actions, but don't call them from static initializers or module-level code that could run before the IDE is ready.

**Don't call `showOverlay()` from a background thread.** Use `context.runOnUiThread` to get onto the main thread first.

```kotlin
// Bad - calling from a background coroutine
PluginApi.ui?.showOverlay { ... }  // might crash if not on main thread

// Good
context.runOnUiThread {
    PluginApi.ui?.showOverlay { ... }
}
```

---

## API Quick Reference

| API | Access | Nullable | Available |
|---|---|---|---|
| `IEditorApi` | `PluginApi.editor` | No | Always |
| `IEnvironmentApi` | `PluginApi.environment` | No | Always |
| `ILspApi` | `PluginApi.lsp` | Yes | EditorActivity only |
| `ITemplateApi` | `PluginApi.templates` | No | Always |
| `IProcessApi` | `PluginApi.process` | No | Always |
| `IUiApi` | `PluginApi.ui` | Yes | EditorActivity only |

### IEditorApi methods

| Method | Description |
|---|---|
| `isAvailable()` | Check if editor is open and ready |
| `getText()` | Get full file content |
| `setText(text)` | Replace all file content |
| `getCurrentLine()` | 0-indexed cursor line |
| `getCurrentColumn()` | 0-indexed cursor column |
| `getCurrentLineDisplay()` | 1-indexed cursor line |
| `getCurrentColumnDisplay()` | 1-indexed cursor column |
| `setCursor(line, column)` | Move cursor (0-indexed) |
| `hasSelection()` | True if something is selected |
| `getSelectedText()` | Get selected text or null |
| `selectAll()` | Select everything |
| `copy()` | Copy selection to clipboard |
| `cut()` | Cut selection to clipboard |
| `paste()` | Paste at cursor |
| `canUndo()` / `canRedo()` | Check undo/redo state |
| `undo()` / `redo()` | Undo or redo |
| `insertText(text)` | Insert at cursor |
| `deleteSelection()` | Delete selected text |
| `formatDocument()` | Format via language server |

### IEnvironmentApi properties

| Property | Description |
|---|---|
| `openProjectDir` | Current open project (nullable) |
| `projectsDir` | External storage projects dir |
| `acsRootProjects` | Internal storage projects dir |
| `androidSdkDir` | Android SDK location |
| `flutterDir` | Flutter SDK location |
| `filesDir` | App internal files dir |
| `homeDir` | Home dir inside internal storage |
| `localDir` | Local env dir |
| `tmpDir` | Temp dir |
| `rootfsAndroidSdkPath` | SDK path inside proot |
| `rootfsJavaHome` | JAVA_HOME inside proot |

### ILspApi methods

| Method | Description |
|---|---|
| `hasServer(languageId)` | Is a server registered? |
| `isServerRunning(languageId)` | Is it currently running? |
| `getAvailableServers()` | All registered language IDs |
| `getRunningServers()` | Currently active language IDs |
| `startServer(languageId)` | Start a server |
| `stopServer(languageId)` | Stop a server |
| `stopAllServers()` | Stop everything |
| `openDocument(file)` | Notify file opened |
| `closeDocument(file)` | Notify file closed |
| `documentChanged(file, content, version)` | Notify file changed |
| `detectLanguage(file)` | Get language ID for file |
| `detectLanguage(fileName)` | Get language ID by filename |
| `registerServer(languageId, spec)` | Register your server |
| `unregisterServer(languageId)` | Remove your server |
| `registerExtension(ext, languageId)` | Map file extension to language |
| `unregisterExtension(ext)` | Unmap extension |
| `getRegisteredExtensions()` | Current extension map |

---

That's the whole API. If something isn't covered here, it's probably intentional (meaning it's either internal IDE code or not part of the stable plugin surface). When in doubt, work with what's in this guide.
You can always check the official AndroidCS Plugins repository (https://github.com/AndroidCSIDE/androidcs-plugins/tree/main/official) to see how templates and LSPs are registered and created, which can help you better understand the process.

