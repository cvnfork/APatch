package me.bmax.apatch.ui.screen

import android.os.Build
import android.system.Os
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.generated.destinations.ModeSelectScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.component.AStatusCard
import me.bmax.apatch.ui.component.WarningCard
import me.bmax.apatch.ui.component.DropdownItem
import me.bmax.apatch.ui.component.KStatusCard
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.util.LatestVersionInfo
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.checkNewVersion
import me.bmax.apatch.util.getSELinuxStatus
import me.bmax.apatch.util.reboot
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.extra.WindowListPopup
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

private val managerVersion = getManagerVersion()

@Composable
fun HomeScreen(
    bottomPadding: Dp,
    navigator: DestinationsNavigator
) {
    val scrollBehavior = MiuixScrollBehavior()

    val kpState by APApplication.kpStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val apState by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar(
                navigator,
                kpState,
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding
        ) {
            item {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BackupWarningCard()
                    KStatusCard(
                        kpState = kpState,
                        apState = apState,
                        navigator = navigator
                    )
                    if (kpState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_INSTALLED) {
                        AStatusCard(apState)
                    }
                    val checkUpdate =
                        APApplication.sharedPreferences.getBoolean("check_update", true)
                    if (checkUpdate) {
                        UpdateCard()
                    }
                    InfoCard(kpState, apState)
                    LearnMoreCard()
                }
                Spacer(Modifier.height(bottomPadding))
            }
        }
    }
}

@Composable
fun AuthFailedTipDialog(showDialog: MutableState<Boolean>) {
    WindowDialog(
        title = stringResource(R.string.home_dialog_auth_fail_title),
        summary = stringResource(R.string.home_dialog_auth_fail_content),
        show = showDialog,
        onDismissRequest = { showDialog.value = false },
    ) {
        Spacer(Modifier.height(12.dp))

        Row {
            TextButton(
                stringResource(android.R.string.ok),
                onClick = { showDialog.value = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
        }
    }
}

val checkSuperKeyValidation: (superKey: String) -> Boolean = { superKey ->
    superKey.length in 8..63 && superKey.any { it.isDigit() } && superKey.any { it.isLetter() }
}

@Composable
fun AuthSuperKey(showDialog: MutableState<Boolean>, showFailedDialog: MutableState<Boolean>) {
    var key by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var enable by remember { mutableStateOf(false) }

    WindowDialog(
        show = showDialog,
        title = stringResource(R.string.home_auth_key_title),
        summary = stringResource(R.string.home_auth_key_desc),
        onDismissRequest = { showDialog.value = false }
    ) {

        Box(contentAlignment = Alignment.CenterEnd) {

            TextField(
                value = key,
                onValueChange = {
                    key = it
                    enable = checkSuperKeyValidation(key)
                },
                label = stringResource(R.string.super_key),
                visualTransformation =
                    if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            IconButton(
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 8.dp),
                onClick = { keyVisible = !keyVisible }
            ) {
                Icon(
                    imageVector = if (keyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {

            TextButton(
                stringResource(id = android.R.string.cancel),
                onClick = { showDialog.value = false },
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(20.dp))

            TextButton(
                stringResource(id = android.R.string.ok),
                onClick = {
                    showDialog.value = false
                    val ok = Natives.nativeReady(key)
                    if (ok) APApplication.superKey = key
                    else showFailedDialog.value = true
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                enabled = enable
            )
        }
    }
}

@Composable
private fun TopBar(
    navigator: DestinationsNavigator,
    kpState: APApplication.State,
    scrollBehavior: ScrollBehavior
) {
    val howDropdownReboot = remember { mutableStateOf(false) }

    val rebootItems = listOf(
        stringResource(R.string.reboot),
        stringResource(R.string.reboot_recovery),
        stringResource(R.string.reboot_bootloader),
        stringResource(R.string.reboot_download),
        stringResource(R.string.reboot_edl),
    )

    TopAppBar(
        title = stringResource(R.string.app_name),
        actions = {
            IconButton(onClick = dropUnlessResumed {
                navigator.navigate(ModeSelectScreenDestination())
            }) {
                Icon(
                    imageVector = Icons.Filled.InstallMobile,
                    contentDescription = stringResource(id = R.string.mode_select_page_title)
                )
            }

            if (kpState != APApplication.State.UNKNOWN_STATE) {
                IconButton(
                    modifier = Modifier.padding(end = 16.dp),
                    onClick = {
                        howDropdownReboot.value = true
                }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(id = R.string.reboot)
                    )

                    WindowListPopup(
                        show = howDropdownReboot,
                        alignment = PopupPositionProvider.Align.BottomStart,
                        onDismissRequest = { howDropdownReboot.value = false }
                    ) {
                        ListPopupColumn {
                            rebootItems.forEachIndexed { index, string ->
                                DropdownItem(
                                    text = string,
                                    optionSize = rebootItems.size,
                                    onSelectedIndexChange = {
                                        when (index) {
                                            0 -> reboot()
                                            1 -> reboot("recovery")
                                            2 -> reboot("bootloader")
                                            3 -> reboot("download")
                                            4 -> reboot("edl")
                                        }
                                        howDropdownReboot.value = false
                                    },
                                    index = index
                                )
                            }
                        }
                    }
                }
            }
        }, scrollBehavior = scrollBehavior
    )
}



@Composable
fun BackupWarningCard() {
    val show = rememberSaveable { mutableStateOf(apApp.getBackupWarningState()) }
    if (show.value) {
        Card(
            colors = CardDefaults.defaultColors(run {
                colorScheme.error
            })
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = "warning")
                }
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(id = R.string.patch_warnning),
                        )

                        Spacer(Modifier.width(12.dp))

                        Icon(
                            Icons.Outlined.Clear,
                            contentDescription = "",
                            modifier = Modifier.clickable {
                                apApp.updateBackupWarningState(false)
                                show.value = false
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun getSystemVersion(): String {
    return "${Build.VERSION.RELEASE} ${if (Build.VERSION.PREVIEW_SDK_INT != 0) "Preview" else ""} (API ${Build.VERSION.SDK_INT})"
}

private fun getDeviceInfo(): String {
    var manufacturer =
        Build.MANUFACTURER[0].uppercaseChar().toString() + Build.MANUFACTURER.substring(1)
    if (!Build.BRAND.equals(Build.MANUFACTURER, ignoreCase = true)) {
        manufacturer += " " + Build.BRAND[0].uppercaseChar() + Build.BRAND.substring(1)
    }
    manufacturer += " " + Build.MODEL + " "
    return manufacturer
}


@Composable
private fun InfoCard(
    kpState: APApplication.State,
    apState: APApplication.State
) {
    @Composable
    fun InfoText(
        title: String,
        content: String,
        bottomPadding: Dp = 24.dp
    ) {
        Text(
            text = title,
            fontSize = MiuixTheme.textStyles.headline1.fontSize,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface
        )
        Text(
            text = content,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
            color = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 2.dp, bottom = bottomPadding)
        )
    }
    Card {
        val uname = Os.uname()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (kpState != APApplication.State.UNKNOWN_STATE) {
                InfoText(
                    title = stringResource(R.string.home_kpatch_version),
                    content = Version.installedKPVString()
                )
                InfoText(
                    title = stringResource(R.string.home_su_path),
                    content = Natives.suPath()
                )
            }
            if (apState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_NOT_INSTALLED) {
                InfoText(
                    title = stringResource(R.string.home_apatch_version),
                    content = managerVersion.second.toString()
                )
            }
            InfoText(
                title = stringResource(R.string.home_device_info),
                content = getDeviceInfo(),
            )
            InfoText(
                title = stringResource(R.string.home_kernel),
                content = uname.release
            )
            InfoText(
                title = stringResource(R.string.home_system_version),
                content = getSystemVersion()
            )
            InfoText(
                title = stringResource(R.string.home_fingerprint),
                content =  Build.FINGERPRINT
            )
            InfoText(
                title = stringResource(R.string.home_selinux_status),
                content = getSELinuxStatus(),
                bottomPadding = 0.dp
            )
        }
    }
}

@Composable
fun UpdateCard() {
    val latestVersionInfo = LatestVersionInfo()
    val newVersion by produceState(initialValue = latestVersionInfo) {
        value = withContext(Dispatchers.IO) {
            checkNewVersion()
        }
    }
    val currentVersionCode = managerVersion.second
    val newVersionCode = newVersion.versionCode
    val newVersionUrl = newVersion.downloadUrl
    val changelog = newVersion.changelog

    val uriHandler = LocalUriHandler.current
    val title = stringResource(id = R.string.apm_changelog)
    val updateText = stringResource(id = R.string.apm_update)

    AnimatedVisibility(
        visible = newVersionCode > currentVersionCode,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val updateDialog = rememberConfirmDialog(onConfirm = { uriHandler.openUri(newVersionUrl) })
        WarningCard(
            message = stringResource(id = R.string.home_new_apatch_found).format(newVersionCode),
            colorScheme.outline
        ) {
            if (changelog.isEmpty()) {
                uriHandler.openUri(newVersionUrl)
            } else {
                updateDialog.showConfirm(
                    title = title, content = changelog, markdown = true, confirm = updateText
                )
            }
        }
    }
}

@Composable
fun LearnMoreCard() {
    val uriHandler = LocalUriHandler.current
    val url = "https://apatch.dev"

    Card (
        modifier = Modifier.fillMaxWidth()
    ) {
        BasicComponent(
            title =  stringResource(R.string.home_learn_apatch),
            summary = stringResource(R.string.home_click_to_learn_apatch),
            endActions = {
                Icon(
                    imageVector = MiuixIcons.Link,
                    tint = colorScheme.onSurface,
                    contentDescription = null
                )
            },
            onClick = {
                uriHandler.openUri(url)
            },
        )
    }
}