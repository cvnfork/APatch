package me.bmax.apatch.ui.component

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.screen.MODULE_TYPE
import me.bmax.apatch.ui.viewmodel.APModuleViewModel
import me.bmax.apatch.util.InstallPreview
import me.bmax.apatch.util.ModuleParser
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun ModuleInstallHandler(
    uri: Uri?,
    viewModel: APModuleViewModel,
    navigator: DestinationsNavigator,
    onReset: () -> Unit
) {
    val apmTitle = stringResource(R.string.apm)
    val context = LocalContext.current
    val loadingDialog = rememberLoadingDialog()

    val showDialog = remember { mutableStateOf(false) }
    var previewData by remember { mutableStateOf<InstallPreview?>(null) }
    var moduleIcon by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var handledUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val handledUri = handledUriString?.toUri()

    LaunchedEffect(uri) {
        val currentUri = uri ?: return@LaunchedEffect
        if (currentUri == handledUri) {
            onReset()
            return@LaunchedEffect
        }

        viewModel.fetchModuleList()
        val preview = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                ModuleParser.getModuleInstallPreview(context, currentUri)
            }
        }

        moduleIcon = withContext(Dispatchers.Default) {
            preview.icon?.let { bytes ->
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }

        previewData = preview
        showDialog.value = true
    }

    if (showDialog.value && previewData != null) {
        val data = previewData!!

        WindowDialog(
            show = showDialog.value,
            title = apmTitle,
            onDismissRequest = {
                showDialog.value = false
                onReset()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Module icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (moduleIcon != null) {
                        Image(
                            bitmap = moduleIcon!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Module name
                Text(
                    text = data.name,
                    style = MiuixTheme.textStyles.title4,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight(550),
                    textAlign = TextAlign.Center
                )

                // Module id
                Text(
                    text = data.id,
                    style = MiuixTheme.textStyles.body1,
                    color = colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // more info
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InfoItem(label = stringResource(R.string.module_info_author), value = data.author)
                    InfoItem(label = stringResource(R.string.module_info_version), value = data.version)
                    InfoItem(label = stringResource(R.string.module_info_version_code), value = data.versionCode.toString())
                }

                Spacer(Modifier.height(16.dp))

                // Module description
                Text(
                    text = data.description,
                    style = MiuixTheme.textStyles.body2,
                    color = colorScheme.onSurface,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                )

                // error msg
                data.errorMessage?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error, color = colorScheme.error,
                        style = MiuixTheme.textStyles.body1,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(28.dp))
            }

            // Bottom button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = { showDialog.value = false; onReset() },
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(20.dp))

                TextButton(
                    text = stringResource(R.string.apm_install),
                    onClick = {
                        showDialog.value = false
                        uri?.let { navigator.navigate(InstallScreenDestination(it, MODULE_TYPE.APM)) }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = colorScheme.onSurfaceVariantSummary,
        )
        Text(
            text = value ?: "",
            style = MiuixTheme.textStyles.body1,
            color = colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}