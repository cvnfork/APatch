package me.bmax.apatch.ui.component

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.screen.MODULE_TYPE
import me.bmax.apatch.ui.viewmodel.APModuleViewModel
import me.bmax.apatch.util.ModuleParser

@Composable
fun ModuleInstallHandler(
    uri: Uri?,
    viewModel: APModuleViewModel,
    navigator: DestinationsNavigator,
    onReset: () -> Unit
) {
    val apmTitle = stringResource(R.string.apm)
    val context = LocalContext.current
    val currentUri by rememberUpdatedState(uri)
    val loadingDialog = rememberLoadingDialog()

    val confirmDialog = rememberConfirmDialog(
        callback = rememberConfirmCallback(
            onConfirm = {
                currentUri?.let { uri ->
                    navigator.navigate(InstallScreenDestination(uri, MODULE_TYPE.APM))
                }
            },
            onDismiss = {}
        )
    )

    var handledUriString by rememberSaveable { mutableStateOf<String?>(null) }   // remember uri
    val handledUri = handledUriString?.toUri()
    var moduleInstallDesc by remember { mutableStateOf("") }

    LaunchedEffect(uri) {
        val currentUri = uri ?: return@LaunchedEffect

        // skip install
        if (currentUri == handledUri) {
            onReset()
            return@LaunchedEffect
        }

        viewModel.fetchModuleList()
        val desc = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                ModuleParser.getModuleInstallDesc(context, uri, viewModel.moduleList)
            }
        }
        moduleInstallDesc = desc
        handledUriString = currentUri.toString()

        confirmDialog.showConfirm(
            title = apmTitle,
            content = moduleInstallDesc
        )
    }
}
