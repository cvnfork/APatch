package me.bmax.apatch.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.LoadingIndicator
import me.bmax.apatch.ui.viewmodel.KPModel
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.reboot
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

private const val TAG = "Patches"

@Destination<RootGraph>
@Composable
fun Patches(
    mode: PatchesViewModel.PatchMode,
    navigator: DestinationsNavigator
) {
    val scope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = viewModel<PatchesViewModel>()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val selectFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.error = ""
                Log.d(TAG, "select boot.img, data: $result.data, uri: $uri")
                viewModel.copyAndParseBootimg(uri)
            }
        }
    }

    LaunchedEffect(selectedBootImage) {
        if (mode == PatchesViewModel.PatchMode.PATCH_ONLY &&
            selectedBootImage != null &&
            viewModel.kimgInfo.banner.isEmpty()
        ) {
            viewModel.error = ""
            viewModel.copyAndParseBootimg(selectedBootImage!!)
        }
    }

    LaunchedEffect(viewModel.patchLog) {
        if (viewModel.patching && viewModel.patchLog.isNotEmpty()) {
            scope.launch {
                if (scrollBehavior.state.heightOffset > scrollBehavior.state.heightOffsetLimit) {
                    scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
                }
                kotlinx.coroutines.yield()
                val lastIndex = listState.layoutInfo.totalItemsCount - 1
                if (lastIndex >= 0) {
                    listState.animateScrollToItem(lastIndex, 1000)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(context as Activity, toRequest.toTypedArray(), 1001)
        }
    }

    SideEffect {
        viewModel.prepare(mode)
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(mode.sId),
                largeTitle = stringResource(R.string.patch_config_title),
                scrollBehavior = scrollBehavior,
                onBack = dropUnlessResumed { navigator.popBackStack() }
            )
        },
        bottomBar = {
            BottomButtons(
                viewModel = viewModel,
                mode = mode,
                navigator = navigator,
                selectFileLauncher = selectFileLauncher
            )
        }
    ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // error info
            if (viewModel.error.isNotEmpty()) {
                item { ErrorView(viewModel.error) }
            }

            // kpming info
            if (viewModel.kpimgInfo.version.isNotEmpty()) {
                item { KernelPatchImageView(viewModel.kpimgInfo) }
            }

            // Slot info
            if (viewModel.bootSlot.isNotEmpty() || viewModel.bootDev.isNotEmpty()) {
                item { BootimgView(slot = viewModel.bootSlot, boot = viewModel.bootDev) }
            }

            // Kernel image
            if (viewModel.kimgInfo.banner.isNotEmpty()) {
                item { KernelImageView(viewModel.kimgInfo) }
            }

            // Superkey view
            if (mode != PatchesViewModel.PatchMode.UNPATCH && viewModel.kimgInfo.banner.isNotEmpty()) {
                item { SetSuperKeyView(viewModel) }
            }

            // Select slot
            if (mode != PatchesViewModel.PatchMode.UNPATCH) {
                if (mode == PatchesViewModel.PatchMode.PATCH_AND_INSTALL || mode == PatchesViewModel.PatchMode.INSTALL_TO_NEXT_SLOT) {
                    items(viewModel.existedExtras.toList()) { extra ->
                        ExtraItem(
                            extra = extra,
                            existed = true,
                            onDelete = { viewModel.existedExtras.remove(extra) })
                    }
                }

                // KPM item
                items(viewModel.newExtras.toList()) { extra ->
                    ExtraItem(extra = extra, existed = false, onDelete = {
                        val idx = viewModel.newExtras.indexOf(extra)
                        viewModel.newExtras.remove(extra)
                        viewModel.newExtrasFileName.removeAt(idx)
                    })
                }

                // Add KPM module
                if (viewModel.superkey.isNotEmpty() && !viewModel.patching && !viewModel.patchdone) {
                    item { AddKpmItem { uri -> viewModel.embedKPM(uri) } }
                }
            }

            // Patch log
            item {
                androidx.compose.animation.AnimatedVisibility(
                    visible = viewModel.patching || viewModel.patchdone,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    Card (
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SelectionContainer {
                            Text(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                text = viewModel.patchLog,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
        if (viewModel.running) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.surface.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        }
    }
}

@Composable
private fun BottomButtons(
    viewModel: PatchesViewModel,
    mode: PatchesViewModel.PatchMode,
    navigator: DestinationsNavigator,
    selectFileLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
) {
    val scope = rememberCoroutineScope()
    if (!viewModel.patching) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.surface.copy(alpha = 0.95f))
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            when {
                viewModel.needReboot -> {
                    TextButton(
                        text = stringResource(R.string.reboot),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = { scope.launch { withContext(Dispatchers.IO) { reboot() } } }
                    )
                }

                viewModel.patchdone -> {
                    TextButton(
                        text = stringResource(android.R.string.ok),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = { navigator.popBackStack() }
                    )
                }

                mode == PatchesViewModel.PatchMode.PATCH_ONLY && viewModel.kimgInfo.banner.isEmpty() -> {
                    TextButton(
                        text = stringResource(R.string.patch_select_bootimg_btn),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            val intent =
                                Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                            selectFileLauncher.launch(intent)
                        }
                    )
                }

                else -> {
                    val canPatch =
                        mode != PatchesViewModel.PatchMode.UNPATCH && viewModel.superkey.isNotEmpty()
                    val canUnpatch =
                        mode == PatchesViewModel.PatchMode.UNPATCH && viewModel.kimgInfo.banner.isNotEmpty()
                    if (canPatch || canUnpatch) {
                        TextButton(
                            text = stringResource(if (canUnpatch) R.string.patch_start_unpatch_btn else R.string.patch_start_patch_btn),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = !viewModel.running,
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                            onClick = {
                                if (canUnpatch)
                                    viewModel.doUnpatch()
                                else
                                    viewModel.doPatch(mode)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtraConfigDialog(
    kpmInfo: KPModel.KPMInfo,
    show: MutableState<Boolean>
) {
    var event by remember { mutableStateOf(kpmInfo.event) }
    var args by remember { mutableStateOf(kpmInfo.args) }

    WindowDialog(
        title = stringResource(R.string.kpm_control_dialog_title),
        show = show,
        onDismissRequest = { show.value = false },
    ) {
        TextField(
            value = event,
            label = stringResource(R.string.patch_item_extra_event),
            onValueChange = {
                event = it
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = args,
            label = stringResource(id = R.string.patch_item_extra_args),
            onValueChange = {
                args = it
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = {
                    show.value = false
                },
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(20.dp))

            TextButton(
                text = stringResource(android.R.string.ok),
                onClick = {
                    kpmInfo.event = event
                    kpmInfo.args = args
                    show.value = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
        }
    }
}

@Composable
private fun ExtraItem(extra: KPModel.IExtraInfo, existed: Boolean, onDelete: () -> Unit) {
    val showConfigDialog = remember { mutableStateOf(false) }
    val colorScheme = colorScheme

    if (extra is KPModel.KPMInfo && showConfigDialog.value) {
        ExtraConfigDialog(kpmInfo = extra, show = showConfigDialog)
    }

    Card {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (extra is KPModel.KPMInfo) extra.name else extra.type.toString(),
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(if (existed) R.string.patch_item_existed_extra_kpm else R.string.patch_item_new_extra_kpm),
                        color = colorScheme.primary
                    )
                }

                if (extra.type == KPModel.ExtraType.KPM) {
                    IconButton(onClick = { showConfigDialog.value = true }) {
                        Icon(Icons.Default.Settings, null, modifier = Modifier.size(20.dp))
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp))
                }
            }

            if (extra is KPModel.KPMInfo) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    DetailText(stringResource(R.string.patch_item_extra_version), extra.version)
                    DetailText(stringResource(R.string.patch_item_extra_kpm_license), extra.license)
                    DetailText(stringResource(R.string.patch_item_extra_author), extra.author)
                    DetailText(stringResource(R.string.patch_item_extra_kpm_desciption), extra.description)
                }
            }
        }
    }
}

@Composable
private fun DetailText(label: String, value: String) {
    Text(
        text = "$label $value",
        fontSize = 12.sp,
        fontWeight = FontWeight(550),
        color = colorScheme.onSurfaceVariantSummary,
    )
}

@Composable
private fun AddKpmItem(onSelected: (Uri) -> Unit) {
    val selectFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                Log.d(TAG, "select kpm, uri=$uri")
                onSelected(uri)
            }
        }
    }

    Card(
        modifier = Modifier
            .clickable {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                selectFileLauncher.launch(intent)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = MiuixIcons.Add,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer(rotationZ = -90f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.patch_embed_kpm_btn),
                style = MiuixTheme.textStyles.body1,
                color = colorScheme.primary
            )
        }
    }
}

@Composable
private fun SetSuperKeyView(viewModel: PatchesViewModel) {
    var skey by remember { mutableStateOf(viewModel.superkey) }
    var showWarn by remember { mutableStateOf(!viewModel.checkSuperKeyValidation(skey)) }
    var keyVisible by remember { mutableStateOf(false) }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.patch_item_skey),
                    style = MiuixTheme.textStyles.body1
                )
            }
            if (showWarn) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    color = Color.Red,
                    text = stringResource(id = R.string.patch_item_set_skey_label),
                    style = MiuixTheme.textStyles.body2
                )
            }

            Box (Modifier.padding(top = 6.dp)) {
                TextField(
                    value = skey,
                    label = stringResource(id = R.string.patch_set_superkey),
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    onValueChange = {
                        skey = it
                        if (viewModel.checkSuperKeyValidation(it)) {
                            viewModel.superkey = it
                            showWarn = false
                        } else {
                            viewModel.superkey = ""
                            showWarn = true
                        }
                    },
                )
                IconButton(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 5.dp),
                    onClick = { keyVisible = !keyVisible }
                ) {
                    Icon(
                        imageVector = if (keyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun KernelPatchImageView(kpImgInfo: KPModel.KPImgInfo) {
    if (kpImgInfo.version.isEmpty()) return
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.patch_item_kpimg),
                    style = MiuixTheme.textStyles.body1
                )
            }
            Text(
                text = stringResource(id = R.string.patch_item_kpimg_version) + " " + Version.uInt2String(
                    kpImgInfo.version.substring(2).toUInt(16)
                ), style = MiuixTheme.textStyles.body2
            )
            Text(
                text = stringResource(id = R.string.patch_item_kpimg_comile_time) + " " + kpImgInfo.compileTime,
                style = MiuixTheme.textStyles.body2
            )
            Text(
                text = stringResource(id = R.string.patch_item_kpimg_config) + " " + kpImgInfo.config,
                style = MiuixTheme.textStyles.body2
            )
        }
    }
}

@Composable
private fun BootimgView(slot: String, boot: String) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.patch_item_bootimg),
                    style = MiuixTheme.textStyles.body1
                )
            }
            if (slot.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.patch_item_bootimg_slot) + " " + slot,
                    style = MiuixTheme.textStyles.body2
                )
            }
            Text(
                text = stringResource(id = R.string.patch_item_bootimg_dev) + " " + boot,
                style = MiuixTheme.textStyles.body2
            )
        }
    }
}

@Composable
private fun KernelImageView(kImgInfo: KPModel.KImgInfo) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.patch_item_kernel),
                    style = MiuixTheme.textStyles.body2
                )
            }
            Text(text = kImgInfo.banner, style = MiuixTheme.textStyles.body2)
        }
    }
}

@Composable
private fun ErrorView(error: String) {
    if (error.isEmpty()) return
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.patch_item_error),
                style = MiuixTheme.textStyles.body2
            )
            Text(text = error, style = MiuixTheme.textStyles.body2)
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    largeTitle: String,
    scrollBehavior: ScrollBehavior,
    onBack: () -> Unit
) {
    TopAppBar(
        title = title,
        largeTitle = largeTitle,
        scrollBehavior = scrollBehavior,
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