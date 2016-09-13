package org.teamtators.vision.util

import org.slf4j.event.Level

fun runScript(scriptPath: String) {
    val runtime = Runtime.getRuntime()
    val process = runtime.exec(scriptPath);

    val outputLogger = InputStreamLogger(scriptPath, process.inputStream)
    val errorLogger = InputStreamLogger(scriptPath, process.errorStream, Level.WARN)
    outputLogger.start()
    errorLogger.start()

    process.waitFor()
    outputLogger.join()
    errorLogger.join()
}