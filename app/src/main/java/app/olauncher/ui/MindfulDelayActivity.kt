package app.olauncher.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.CountDownTimer
import android.os.UserHandle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import app.olauncher.R
import app.olauncher.data.Prefs
import app.olauncher.helper.getUserHandleFromString
import app.olauncher.helper.showToast

class MindfulDelayActivity : AppCompatActivity() {
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var progress: ProgressBar
    private lateinit var btnOpenNow: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mindful_delay)

        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tvCountdown = findViewById(R.id.tvCountdown)
        progress = findViewById(R.id.progress)
        btnOpenNow = findViewById(R.id.btnOpenNow)
        btnCancel = findViewById(R.id.btnCancel)

        val packageName = intent.getStringExtra("packageName") ?: run { finish(); return }
        val activityClassName = intent.getStringExtra("activityClassName") ?: ""
        val userString = intent.getStringExtra("userString") ?: ""
        val delayMs = intent.getIntExtra("delayMs", Prefs(this).mindfulDelayMs)

        // Resolve label
        val appLabel = try {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (_: Exception) { packageName }

        tvTitle.text = getString(R.string.mindful_pause)
        tvSubtitle.text = getString(R.string.opening_app, appLabel)

        val totalSeconds = (delayMs / 1000).coerceAtLeast(0)
        progress.max = totalSeconds
        progress.progress = 0

        // Open Now initially disabled for 2 seconds
        btnOpenNow.isEnabled = false
        var openEnableRemaining = 2

        val userHandle: UserHandle = getUserHandleFromString(this, userString)

        val timer = object : CountDownTimer((totalSeconds * 1000).toLong(), 1000) {
            var elapsed = 0
            override fun onTick(millisUntilFinished: Long) {
                elapsed++
                val remaining = (millisUntilFinished / 1000).toInt()
                tvCountdown.text = getString(R.string.starting_in_seconds, remaining)
                progress.progress = totalSeconds - remaining

                if (openEnableRemaining > 0) {
                    openEnableRemaining--
                    if (openEnableRemaining == 0) btnOpenNow.isEnabled = true
                }
            }

            override fun onFinish() {
                launchTarget(packageName, activityClassName, userHandle)
            }
        }

        timer.start()

        btnOpenNow.setOnClickListener {
            timer.cancel()
            launchTarget(packageName, activityClassName, userHandle)
        }
        btnCancel.setOnClickListener {
            timer.cancel()
            finish()
        }
    }

    private fun launchTarget(pkg: String, act: String?, user: UserHandle) {
        val launcher = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activityInfo = launcher.getActivityList(pkg, user)
        val component = if (act.isNullOrBlank()) {
            when (activityInfo.size) {
                0 -> {
                    showToast(getString(R.string.app_not_found))
                    finish(); return
                }
                1 -> ComponentName(pkg, activityInfo[0].name)
                else -> ComponentName(pkg, activityInfo[activityInfo.size - 1].name)
            }
        } else {
            ComponentName(pkg, act)
        }
        try {
            launcher.startMainActivity(component, user, null, null)
        } catch (e: SecurityException) {
            try {
                launcher.startMainActivity(component, android.os.Process.myUserHandle(), null, null)
            } catch (e: Exception) {
                showToast(getString(R.string.unable_to_open_app))
            }
        } catch (e: Exception) {
            showToast(getString(R.string.unable_to_open_app))
        }
        finish()
    }
}

