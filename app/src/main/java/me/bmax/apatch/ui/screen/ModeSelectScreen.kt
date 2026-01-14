package me.bmax.apatch.ui.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.PatchesDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.util.isABDevice
import me.bmax.apatch.util.rootAvailable
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

var selectedBootImage: Uri? = null

enum class ActionType {
    INSTALL, UNINSTALL
}

@Destination<RootGraph>
@Composable
fun ModeSelectScreen(
    navigator: DestinationsNavigator,
    type: ActionType
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            val titleRes = if (type == ActionType.INSTALL)
                R.string.mode_select_page_title
            else
                R.string.home_dialog_uninstall_title

            TopBar(
                title = stringResource(titleRes),
                onBack = dropUnlessResumed { navigator.popBackStack() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card {
                if (type == ActionType.INSTALL) {
                    SelectInstallMethod(navigator = navigator)
                } else {
                    SelectUninstallMethod(navigator = navigator)
                }
            }
        }
    }
}

@Composable
private fun SelectInstallMethod(navigator: DestinationsNavigator) {
    val rootAvailable = rootAvailable()
    val isAbDevice = isABDevice()

    val radioOptions = mutableListOf<InstallMethod>(InstallMethod.SelectFile())
    if (rootAvailable) {
        radioOptions.add(InstallMethod.DirectInstall)
        if (isAbDevice) {
            radioOptions.add(InstallMethod.DirectInstallToInactiveSlot)
        }
    }

    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                selectedBootImage = uri
                navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.PATCH_ONLY))
            }
        }
    }

    val confirmDialog = rememberConfirmDialog(onConfirm = {
        navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.INSTALL_TO_NEXT_SLOT))
    }, onDismiss = null)

    val dialogTitle = stringResource(id = android.R.string.dialog_alert_title)
    val dialogContent = stringResource(id = R.string.mode_select_page_install_inactive_slot_warning)

    Column {
        radioOptions.forEach { option ->
            SuperArrow(
                title = stringResource(id = option.label),
                onClick = {
                    when (option) {
                        is InstallMethod.SelectFile -> {
                            selectedBootImage = null
                            selectImageLauncher.launch(
                                Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "application/octet-stream"
                                }
                            )
                        }
                        is InstallMethod.DirectInstall -> {
                            navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.PATCH_AND_INSTALL))
                        }
                        is InstallMethod.DirectInstallToInactiveSlot -> {
                            confirmDialog.showConfirm(dialogTitle, dialogContent, true)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SelectUninstallMethod(navigator: DestinationsNavigator) {
    Column {
        SuperArrow(
            title = stringResource(R.string.home_dialog_uninstall_all),
            onClick = {
                APApplication.uninstallApatch()
                navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.UNPATCH))
            }
        )
        SuperArrow(
            title = stringResource(R.string.home_dialog_restore_image),
            onClick = {
                navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.UNPATCH))
            }
        )
        SuperArrow(
            title = stringResource(R.string.home_dialog_uninstall_ap_only),
            onClick = {
                APApplication.uninstallApatch()
            }
        )
    }
}

sealed class InstallMethod {
    data class SelectFile(@param:StringRes override val label: Int = R.string.mode_select_page_select_file) : InstallMethod()
    data object DirectInstall : InstallMethod() { override val label = R.string.mode_select_page_patch_and_install }
    data object DirectInstallToInactiveSlot : InstallMethod() { override val label = R.string.mode_select_page_install_inactive_slot }
    abstract val label: Int
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    SmallTopAppBar(
        title = title,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(MiuixIcons.Back,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 20.dp)
                )
            }
        },
    )
}