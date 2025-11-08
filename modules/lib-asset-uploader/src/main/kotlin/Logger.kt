package dev.adamko.githubassetpublish.lib

interface Logger {
  fun error(message: String)
  fun warn(message: String)
  fun info(message: String)
  fun debug(message: String)
  fun trace(message: String)

  object Default : Logger {
    override fun error(message: String): Unit = System.err.println(message)
    override fun warn(message: String): Unit = println(message)
    override fun info(message: String): Unit = println(message)
    override fun debug(message: String): Unit = println(message)
    override fun trace(message: String): Unit = println(message)
  }

  companion object {
    fun Logger.warn(message: String, throwable: Throwable) {
      warn(
        buildString {
          appendLine(message)
          append(throwable.stackTraceToString())
        }
      )
    }
  }
}
