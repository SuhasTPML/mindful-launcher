package app.olauncher

import android.app.Application
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OlauncherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stack = sw.toString()
                Log.e("MindfulLauncher", "CRASH in thread ${thread.name}", throwable)

                val dir = getExternalFilesDir("logs") ?: filesDir
                if (!dir.exists()) dir.mkdirs()
                val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                val file = File(dir, "crash-$ts.log")
                file.writeText(stack)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}

