package xyz.jekyllex.ui.activities.home

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import xyz.jekyllex.appContainer
import xyz.jekyllex.services.ProcessService
import xyz.jekyllex.ui.navigation.JekyllExNav
import xyz.jekyllex.ui.theme.JekyllExTheme
import xyz.jekyllex.utils.Commands.echo
import xyz.jekyllex.utils.Constants.requiredBinaries
import xyz.jekyllex.utils.NativeUtils
import xyz.jekyllex.utils.Setting

class HomeActivity : ComponentActivity() {
    private var serviceBound = false
    private lateinit var viewModel: HomeViewModel
    private lateinit var pickFileLauncher: ActivityResultLauncher<String>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            val processService = (binder as ProcessService.LocalBinder).service
            appContainer.process.attach(processService)
            processService.exec(echo("Welcome to JekyllEx!"))
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            appContainer.process.detach()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!NativeUtils.areUsable(requiredBinaries)) {
            NativeUtils.launchInstaller(this)
            return
        }

        startService(Intent(this, ProcessService::class.java))
        Intent(this, ProcessService::class.java).also { intent ->
            serviceBound = bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        val container = appContainer
        val settings = container.settings
        viewModel = viewModels<HomeViewModel>(
            factoryProducer = {
                HomeViewModel.Factory(
                    container.files,
                    container.process,
                    contentResolver,
                    container.settings.get(Setting.REDUCE_ANIMATIONS)
                )
            }
        ).value

        pickFileLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                viewModel.onFilePicked(uri)
            } ?: run {
                Toast.makeText(this, "No file selected!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted) {
                settings.set(Setting.ASK_NOTIF_PERM, false)
                val message = "Notifications set up successfully!"
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            } else {
                if (!settings.get<Boolean>(Setting.ASK_NOTIF_PERM)) {
                    Toast.makeText(
                        this,
                        "You will have to enable notifications from the settings!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && settings.get(Setting.ASK_NOTIF_PERM)) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                viewModel.setNotifRationale(true)
                settings.set(Setting.ASK_NOTIF_PERM, false)
            } else if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            JekyllExTheme {
                JekyllExNav(
                    homeViewModel = viewModel,
                    process = container.process,
                    pickFileLauncher = pickFileLauncher,
                    requestPermissionLauncher = requestPermissionLauncher,
                )
            }
        }
    }

    override fun onRestart() {
        super.onRestart()

        if (::viewModel.isInitialized)
            viewModel.setSkipAnimation(
                appContainer.settings.get(Setting.REDUCE_ANIMATIONS)
            )

        if (::viewModel.isInitialized) viewModel.refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            appContainer.process.detach()
            unbindService(connection)
        }
    }
}
