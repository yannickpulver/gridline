package com.yannickpulver.gridline.update

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Play in-app updates, flexible flow: when a newer version is on the Play Store, Play shows its own
 * "update available" sheet once per process; after the user accepts and the download finishes, the
 * update is applied the next time the app comes to the foreground. Builds not installed from Play
 * (dev, sideloaded) get no update info and stay silent.
 */
class InAppUpdate(private val activity: ComponentActivity) : DefaultLifecycleObserver {
    private val manager = AppUpdateManagerFactory.create(activity)
    private val launcher = activity.registerForActivityResult(StartIntentSenderForResult()) {}
    private var prompted = false

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                when {
                    info.installStatus() == InstallStatus.DOWNLOADED -> manager.completeUpdate()
                    !prompted &&
                        info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                        prompted = true
                        manager.startUpdateFlowForResult(
                            info,
                            launcher,
                            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        )
                    }
                }
            }
            .addOnFailureListener { Log.d("InAppUpdate", "In-app update check unavailable", it) }
    }
}
