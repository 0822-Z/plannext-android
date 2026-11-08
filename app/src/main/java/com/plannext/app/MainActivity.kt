package com.plannext.app

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.plannext.app.alarm.PlanAlarmScheduler
import com.plannext.app.data.PlanRepository
import com.plannext.app.ui.PlanNextRoot
import com.plannext.app.ui.theme.PlanNextTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private var openDateRequest by mutableStateOf<DateRequest?>(null)
    private var permissionSnapshot by mutableStateOf(readPermissions())

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionSnapshot = readPermissions()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openDateRequest = dateRequestFrom(intent)

        val repository = PlanRepository.getInstance(this)
        PlanAlarmScheduler(this).scheduleAll(repository.snapshot())

        setContent {
            PlanNextTheme {
                PlanNextRoot(
                    repository = repository,
                    openDateRequest = openDateRequest,
                    onDateRequestHandled = { openDateRequest = null },
                    permissions = permissionSnapshot,
                    onRequestNotifications = ::requestNotifications,
                    onOpenExactAlarmSettings = ::openExactAlarmSettings,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openDateRequest = dateRequestFrom(intent)
    }

    override fun onResume() {
        super.onResume()
        permissionSnapshot = readPermissions()
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            openNotificationSettings()
            return
        }
        val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            openNotificationSettings()
            return
        }

        val preferences = getSharedPreferences("plannext_permissions", MODE_PRIVATE)
        val alreadyRequested = preferences.getBoolean("notification_requested", false)
        if (alreadyRequested) {
            openNotificationSettings()
        } else {
            preferences.edit().putBoolean("notification_requested", true).apply()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
        )
    }

    private fun openExactAlarmSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:$packageName"),
            )
        } else {
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName"),
            )
        }
        runCatching { startActivity(intent) }
            .onFailure {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:$packageName"),
                    ),
                )
            }
    }

    private fun readPermissions(): PermissionSnapshot {
        val notificationsEnabled = getSystemService(NotificationManager::class.java)
            .areNotificationsEnabled()
        return PermissionSnapshot(
            notificationsEnabled = notificationsEnabled,
            exactAlarmsEnabled = PlanAlarmScheduler(this).canScheduleExactAlarms(),
        )
    }

    private fun dateRequestFrom(intent: Intent?): DateRequest? {
        val value = intent?.getStringExtra(EXTRA_OPEN_DATE) ?: return null
        val date = runCatching { LocalDate.parse(value) }.getOrNull() ?: return null
        return DateRequest(date, System.nanoTime())
    }

    companion object {
        const val EXTRA_OPEN_DATE = "com.plannext.app.extra.OPEN_DATE"
    }
}

data class DateRequest(
    val date: LocalDate,
    val nonce: Long,
)

data class PermissionSnapshot(
    val notificationsEnabled: Boolean,
    val exactAlarmsEnabled: Boolean,
)

