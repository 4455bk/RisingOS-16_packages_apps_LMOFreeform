package com.libremobileos.freeform.receiver

import android.app.ActivityOptions
import android.app.WindowConfiguration
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.Rect
import android.provider.Settings
import android.view.Display

import com.libremobileos.freeform.LMOFreeform
import com.libremobileos.freeform.LMOFreeformServiceManager
import com.libremobileos.freeform.utils.Debug
import com.libremobileos.freeform.utils.Logger

import kotlin.math.roundToInt

/**
 * @author KindBrave
 * @since 2023/9/19
 */
class StartFreeformReceiver : BroadcastReceiver() {

    private val logger = Logger(TAG)

    companion object {
        private const val TAG = "StartFreeformReceiver"
        private const val PACKAGE_NAME = "com.libremobileos.sidebar"
        private const val ACTION = "com.libremobileos.freeform.START_FREEFORM"
        private const val INITIAL_MAX_WIDTH = 600
        private const val INITIAL_MAX_HEIGHT = 600
        private const val WINDOWING_MODE_PINNED_WINDOW_EXT = 102 // Pop-Up View
        private const val WINDOWING_MODE_FREEFORM = WindowConfiguration.WINDOWING_MODE_FREEFORM

        // The fully qualified class name of AllAppActivity
        private const val ALL_APP_ACTIVITY = "com.libremobileos.sidebar.ui.all_app.AllAppActivity"
        // The package name for the sidebar app
        private const val SIDEBAR_PACKAGE = "com.libremobileos.sidebar"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION) {
            val freeformLaunchMode = Settings.System.getInt(
                context.contentResolver,
                "freeform_launch_mode",
                0
            )
            val packageName = intent.getStringExtra("packageName")
            when (freeformLaunchMode) {
                0 -> { // Pop-Up View
                    if (packageName == SIDEBAR_PACKAGE) {
                        launchAllAppActivityInPopupView(context)
                    } else {
                        launchAppInPopupView(context, intent)
                    }
                }
                1, 2 -> { // Native Freeform
                    if (packageName == SIDEBAR_PACKAGE) {
                        launchAllAppActivityInNativeFreeform(context)
                    } else {
                        launchAppInNativeFreeform(context, intent)
                    }
                }
                3 -> { // LMO Freeform
                    launchAppInLMOFreeform(context, intent)
                }
                else -> {
                    launchAppInLMOFreeform(context, intent)
                }
            }
        }
    }

    private fun launchAppInLMOFreeform(context: Context, intent: Intent) {
        if (Debug.isDebug) logger.d("onReceive ${intent.extras}")
            val packageName = intent.getStringExtra("packageName")
            val activityName = intent.getStringExtra("activityName")
            val userId = intent.getIntExtra("userId", 0)
            val taskId = intent.getIntExtra("taskId", -1)

            if (packageName != null && activityName != null) {
                val sp = context.getSharedPreferences(LMOFreeform.CONFIG, Context.MODE_PRIVATE)
                val screenWidth = context.resources.displayMetrics.widthPixels
                val screenHeight = context.resources.displayMetrics.heightPixels
                val screenDensityDpi = context.resources.displayMetrics.densityDpi
                
                val freeformWidth = sp.getInt("freeform_width", (screenWidth * 0.6f).roundToInt())
                    .coerceAtMost(INITIAL_MAX_WIDTH)
                val freeformHeight = sp.getInt("freeform_height", (screenHeight * 0.5f).roundToInt())
                    .coerceAtMost(INITIAL_MAX_HEIGHT)
                
                LMOFreeformServiceManager.createWindow(
                    packageName,
                    activityName,
                    userId,
                    taskId,
                    freeformWidth,
                    freeformHeight,
                    sp.getInt("freeform_dpi", screenDensityDpi)
                )
            }
    }

    private fun launchAllAppActivityInNativeFreeform(context: Context) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager

        val screenSize = Point()
        windowManager?.defaultDisplay?.getSize(screenSize)

        val centerX = screenSize.x / 2
        val centerY = screenSize.y / 2
        val width = (screenSize.x * 0.5).roundToInt()
        val height = (screenSize.y * 0.5).roundToInt()
        val launchBounds = Rect(centerX - width / 2, centerY - height / 2, centerX + width / 2, centerY + height / 2)

        val activityOptions = ActivityOptions.makeBasic().apply {
            setLaunchWindowingMode(WINDOWING_MODE_FREEFORM)
            setLaunchBounds(launchBounds)
            setTaskAlwaysOnTop(true)
        }

        val intent = Intent().apply {
            setClassName(SIDEBAR_PACKAGE, ALL_APP_ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent, activityOptions.toBundle())
        } catch (e: Exception) {
            logger.e("Error launching AllAppActivity in native freeform: $e")
        }
    }

    private fun launchAllAppActivityInPopupView(context: Context) {
        // Customize the popup bounds as needed
        val popupBounds = Rect(200, 200, 900, 1400)

        val activityOptions = ActivityOptions.makeBasic().apply {
            setLaunchWindowingMode(WINDOWING_MODE_PINNED_WINDOW_EXT)
            setLaunchBounds(popupBounds)
            // Do not setTaskAlwaysOnTop for popup view
        }

        val intent = Intent().apply {
            setClassName(SIDEBAR_PACKAGE, ALL_APP_ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent, activityOptions.toBundle())
        } catch (e: Exception) {
            logger.e("Error launching AllAppActivity in popup view: $e")
        }
    }

    private fun launchAppInNativeFreeform(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra("packageName") ?: return

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager

        val screenSize = Point()
        windowManager?.defaultDisplay?.getSize(screenSize)

        val centerX = screenSize.x / 2
        val centerY = screenSize.y / 2
        val width = (screenSize.x * 0.5).roundToInt()
        val height = (screenSize.y * 0.5).roundToInt()
        val launchBounds = Rect(centerX - width / 2, centerY - height / 2, centerX + width / 2, centerY + height / 2)

        val activityOptions = ActivityOptions.makeBasic().apply {
            setLaunchWindowingMode(WINDOWING_MODE_FREEFORM)
            setLaunchBounds(launchBounds)
            setTaskAlwaysOnTop(true)
        }

        val packageManager = context.packageManager
        val startAppIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startAppIntent?.let {
                context.startActivity(it, activityOptions.toBundle())
            }
        } catch (e: Exception) {
            logger.e("Error launching app in native freeform: $e")
        }
    }

    private fun launchAppInPopupView(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra("packageName") ?: return
        val packageManager = context.packageManager
        val startAppIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Customize the popup bounds as needed
        val popupBounds = Rect(200, 200, 900, 1400)

        val activityOptions = ActivityOptions.makeBasic().apply {
            setLaunchWindowingMode(WINDOWING_MODE_PINNED_WINDOW_EXT)
            setLaunchBounds(popupBounds)
            // Do not setTaskAlwaysOnTop for popup view
        }

        try {
            startAppIntent?.let {
                context.startActivity(it, activityOptions.toBundle())
            }
        } catch (e: Exception) {
            logger.e("Error launching app in popup view: $e")
        }
    }
}
