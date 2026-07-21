// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
  alias(libs.plugins.hilt) apply false
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.detekt) apply false
}

// Suppress KSP's known background AWT thread NullPointerException when it attempts to call
// FileDocumentManager.getInstance() or BinaryFileTypeDecompilers after KSP has disposed the IntelliJ Application.
val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
if (originalHandler?.javaClass?.name?.contains("KspAwtErrorHandler") != true) {
  Thread.setDefaultUncaughtExceptionHandler(object : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
      val stackTraceStr = e.stackTraceToString()
      val isKspAwtError = (t.name.contains("AWT") || t.name.contains("AWT-EventQueue")) &&
        (stackTraceStr.contains("ksp.com.intellij") || stackTraceStr.contains("BinaryFileTypeDecompilers"))
      if (isKspAwtError) {
        // Silently ignore this known KSP background thread leak NPE
        return
      }
      if (originalHandler != null) {
        originalHandler.uncaughtException(t, e)
      } else {
        System.err.println("Exception in thread \"${t.name}\" ${e.javaClass.name}: ${e.message}")
        e.printStackTrace(System.err)
      }
    }
  })
}
