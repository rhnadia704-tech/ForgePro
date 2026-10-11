package com.example.ui

import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.AiRestructuringPlanEntity
import com.example.data.SystemGraphNodeEntity
import com.example.data.TrustAndArtArtifactEntity
import com.example.data.UnpackedRomEntity
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.viewmodel.RomForgeViewModel
import com.example.viewmodel.WorkbenchUiState

@Composable
fun RomForgeApp(viewModel: RomForgeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unpackedRoms by viewModel.unpackedRoms.collectAsStateWithLifecycle()
    val dagNodes by viewModel.allDagNodes.collectAsStateWithLifecycle()
    val trustArtifacts by viewModel.trustAndArtArtifacts.collectAsStateWithLifecycle()
    val aiPlans by viewModel.aiPlans.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    if (selectedTab != 0) {
        BackHandler {
            selectedTab = 0
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val seg = uri.lastPathSegment ?: "Custom_GSI_System.img"
            viewModel.updateInputFilePath(seg, uri)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Build else Icons.Outlined.Build,
                            contentDescription = stringResource(R.string.workbench_tab)
                        )
                    },
                    label = { Text(stringResource(R.string.workbench_tab)) },
                    modifier = Modifier.testTag("nav_tab_workbench")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.AccountTree else Icons.Outlined.AccountTree,
                            contentDescription = stringResource(R.string.dag_extract_tab)
                        )
                    },
                    label = { Text(stringResource(R.string.dag_extract_tab)) },
                    modifier = Modifier.testTag("nav_tab_dag")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.VerifiedUser else Icons.Outlined.VerifiedUser,
                            contentDescription = stringResource(R.string.trust_art_tab)
                        )
                    },
                    label = { Text(stringResource(R.string.trust_art_tab)) },
                    modifier = Modifier.testTag("nav_tab_trust")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings_tab)
                        )
                    },
                    label = { Text(stringResource(R.string.settings_tab)) },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (selectedTab) {
                0 -> WorkbenchAndMakeScreen(
                    uiState = uiState,
                    unpackedRoms = unpackedRoms,
                    onInputPathChange = { viewModel.updateInputFilePath(it, null) },
                    onPickFileClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                    onSelectUnpackedRom = viewModel::selectUnpackedRom,
                    onExtractImgClick = viewModel::onExtractImgClicked,
                    onExtractDeviceRootClick = viewModel::onExtractDeviceRootClicked,
                    onPortageClick = viewModel::onPortageClicked,
                    onSignClick = viewModel::onSignClicked,
                    onRepackClick = viewModel::onRepackClicked,
                    onMakeClick = viewModel::onMakeFullPipelineClicked
                )
                1 -> PorterExtractAndDagScreen(
                    uiState = uiState,
                    dagNodes = dagNodes,
                    onExtractDeviceRootClick = viewModel::onExtractDeviceRootClicked
                )
                2 -> TrustArtAndAiLabScreen(
                    uiState = uiState,
                    trustArtifacts = trustArtifacts,
                    aiPlans = aiPlans,
                    onPromptChange = viewModel::updateAiCustomPrompt,
                    onAskAiClick = viewModel::onAskAiArchitectClicked,
                    onSignRegenClick = viewModel::onSignClicked
                )
                3 -> SettingsAndCicdScreen(
                    uiState = uiState,
                    onThemeSelected = viewModel::setThemeMode
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkbenchAndMakeScreen(
    uiState: WorkbenchUiState,
    unpackedRoms: List<UnpackedRomEntity>,
    onInputPathChange: (String) -> Unit,
    onPickFileClick: () -> Unit,
    onSelectUnpackedRom: (Int) -> Unit,
    onExtractImgClick: () -> Unit,
    onExtractDeviceRootClick: () -> Unit,
    onPortageClick: () -> Unit,
    onSignClick: () -> Unit,
    onRepackClick: () -> Unit,
    onMakeClick: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val selectedRom = unpackedRoms.find { it.id == uiState.selectedUnpackedRomId } ?: unpackedRoms.firstOrNull()
    val cs = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            HeroFoundryBanner(uiState = uiState)
        }

        // Live Operation Status & Progress Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .testTag("operation_status_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Memory,
                                contentDescription = "Pipeline Status",
                                tint = if (uiState.isBusy) cs.primary else cs.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.activeOperationTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (uiState.isBusy) cs.primary else cs.secondary
                            )
                        }
                        Text(
                            text = "${(uiState.progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = cs.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { uiState.progressFraction },
                        color = cs.primary,
                        trackColor = cs.outline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.currentStepDetail,
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant
                    )
                }
            }
        }

        // MASTER "MAKE" BUTTON CARD (High-Thinking Source-Level Pipeline)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(listOf(cs.primary, cs.secondary)),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Gemini High Thinking MAKE",
                            tint = cs.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SOURCE-LEVEL AUTO PIPELINE (GEMINI 3.1 PRO HIGH THINKING)",
                            style = MaterialTheme.typography.labelLarge,
                            color = cs.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Unpacks & disassembles GSI to source level -> Extracts 100% of ${Build.DEVICE} into /sdcard/PORTER/EXTRACT -> Resolves full Dependency Graph & FOD/UDFPS mechanism -> Regenerates OAT/VDEX/fsv_meta & XML signatures -> Repacks & AVB2 Signs with Zero Bootloop.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onMakeClick,
                        enabled = !uiState.isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.primary,
                            contentColor = cs.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("make_master_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Run MAKE")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MAKE (UNPACK + EXTRACT + AI DAG PORT + OAT/SIGN)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // SECTION 1: UNPACK IMG (Field to choose file + EXTRACT button)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. UNPACK & SOURCE DISASSEMBLY (.img / super.img / payload.bin / .xz / .lz4 / .br)",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = uiState.selectedInputFilePath,
                            onValueChange = onInputPathChange,
                            label = { Text("Image or ROM File to Unpack") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = onPickFileClick,
                                    modifier = Modifier.testTag("pick_image_file_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderOpen,
                                        contentDescription = "Browse Image File",
                                        tint = cs.primary
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("unpack_file_input")
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val presets = listOf(
                            "/sdcard/Download/Generic_AOSP15_Arm64_ab_GSI.img.xz",
                            "/sdcard/Download/LineageOS22_TD_EROFS_system.img",
                            "/sdcard/Download/OTA_Full_Rom_payload.bin",
                            "/sdcard/Download/super_dynamic_partitions.img.lz4"
                        )
                        items(presets) { preset ->
                            FilterChip(
                                selected = uiState.selectedInputFilePath == preset,
                                onClick = { onInputPathChange(preset) },
                                label = {
                                    Text(
                                        text = preset.substringAfterLast("/"),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = cs.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = cs.primary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onExtractImgClick,
                        enabled = !uiState.isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.secondary,
                            contentColor = cs.onSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("extract_img_button")
                    ) {
                        Icon(Icons.Filled.Storage, contentDescription = "Extract Image")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_extract_img),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // SECTION 2: EXTRACT DEVICE (ROOT -> /sdcard/PORTER/EXTRACT)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.tertiary.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = "Root Hardware & FOD Extractor",
                            tint = cs.tertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. ZERO-OMISSION HOST EXTRACTION (-> /sdcard/PORTER/EXTRACT)",
                            style = MaterialTheme.typography.titleMedium,
                            color = cs.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Target: ${uiState.porterExtractPath}\nExtracts proprietary-files.txt, Android.bp, BoardConfig.mk, VINTF, SELinux CIL, and 100% of FOD/UDFPS + ELF transitive dependencies (0 omitted files).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onExtractDeviceRootClick,
                        enabled = !uiState.isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.tertiary,
                            contentColor = cs.onTertiary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("extract_device_root_button")
                    ) {
                        Icon(Icons.Filled.DeveloperBoard, contentDescription = "Extract Device with Root")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_extract_device),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // SECTION 3: DROPDOWN OF UNPACKED IMGS + PORTAGE + SIGN + REPACK BUTTONS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. UNPACKED IMG SELECTION, PORTAGE, TRUST SIGN & REPACK",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unpacked_img_dropdown_box")
                    ) {
                        OutlinedTextField(
                            value = selectedRom?.let {
                                "${it.name} [${it.imageFormat}] — DAG:${it.dagNodesCount}n"
                            } ?: "No unpacked IMG yet (Click EXTRACT IMG or MAKE)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unpacked System Image Workspace") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("unpacked_img_dropdown_field")
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            unpackedRoms.forEach { rom ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "${rom.name} (${rom.imageFormat})",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                            Text(
                                                text = "Ported:${rom.isPorted} | FOD:${rom.isFodSolved} | OAT/VDEX:${rom.isOatVdexRegenerated} | AVB:${rom.isSignedAvb}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = cs.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSelectUnpackedRom(rom.id)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedRom != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatusBadge("FOD: ${if (selectedRom.isFodSolved) "SOLVED" else "PENDING"}", selectedRom.isFodSolved)
                            StatusBadge("OAT/VDEX/fsv: ${if (selectedRom.isOatVdexRegenerated) "SYNCED" else "RAW"}", selectedRom.isOatVdexRegenerated)
                            StatusBadge("AVB2: ${if (selectedRom.isSignedAvb) "SIGNED" else "UNSIGNED"}", selectedRom.isSignedAvb)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onPortageClick,
                            enabled = !uiState.isBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = cs.primary,
                                contentColor = cs.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("portage_button")
                        ) {
                            Icon(Icons.Filled.Code, contentDescription = "Portage", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PORTAGE", style = MaterialTheme.typography.labelLarge)
                        }

                        Button(
                            onClick = onSignClick,
                            enabled = !uiState.isBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = cs.tertiary,
                                contentColor = cs.onTertiary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("sign_button")
                        ) {
                            Icon(Icons.Filled.Security, contentDescription = "Sign", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SIGN & OAT", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onRepackClick,
                        enabled = !uiState.isBusy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("repack_button")
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Repack Image", tint = cs.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_repack_img),
                            style = MaterialTheme.typography.labelLarge,
                            color = cs.secondary
                        )
                    }
                }
            }
        }

        // REAL-TIME TELEMETRY & EXECUTION CONSOLE
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(14.dp))
                    .testTag("terminal_console_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Terminal,
                            contentDescription = "Telemetry Console",
                            tint = cs.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE KERNEL / DAG / OAT-VDEX / AVB TELEMETRY LOG",
                            style = MaterialTheme.typography.labelLarge,
                            color = cs.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = cs.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.terminalLogs.takeLast(12).forEach { line ->
                        Text(
                            text = line,
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                line.contains("[MAKE-SUCCESS]") || line.contains("-OK]") -> cs.secondary
                                line.contains("[AI") || line.contains("[FOD") || line.contains("[THEME]") -> cs.primary
                                line.contains("[WARN]") -> cs.tertiary
                                else -> cs.onSurfaceVariant
                            },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.security_prototype_notice),
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeroFoundryBanner(uiState: WorkbenchUiState) {
    val cs = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cs.outline, RoundedCornerShape(18.dp))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_foundry_banner),
                contentDescription = "ROM Forge Studio Engineering Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x880A0D12),
                                Color(0xEE0A0D12)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomStart)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF00E5FF).copy(alpha = 0.22f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "DAG + FOD SOURCE ENGINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFF00E676).copy(alpha = 0.22f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ZERO-BOOTLOOP OAT/VDEX/AVB2",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00E676),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ROM Forge Studio — ${Build.MANUFACTURER} ${Build.MODEL}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = "Extract Path: ${uiState.porterExtractPath} | Unresolved Deps: ${uiState.unresolvedDependenciesCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFD0D9E6),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, active: Boolean) {
    val cs = MaterialTheme.colorScheme
    Surface(
        color = if (active) cs.secondary.copy(alpha = 0.16f) else cs.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(
            1.dp,
            if (active) cs.secondary.copy(alpha = 0.5f) else cs.outline,
            RoundedCornerShape(8.dp)
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) cs.secondary else cs.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PorterExtractAndDagScreen(
    uiState: WorkbenchUiState,
    dagNodes: List<SystemGraphNodeEntity>,
    onExtractDeviceRootClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var selectedSubsystem by rememberSaveable { mutableStateOf("ALL") }
    val subsystems = listOf("ALL", "FOD_UDFPS", "DISPLAY_HBM", "BIOMETRICS", "KEYMASTER_TRUST", "AUDIO", "CAMERA", "RIL_IMS", "CORE_SYSTEM")
    val filteredNodes = remember(dagNodes, selectedSubsystem) {
        if (selectedSubsystem == "ALL") dagNodes else dagNodes.filter { it.subsystem == selectedSubsystem }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "/sdcard/PORTER/EXTRACT — ZERO-OMISSION SOURCE TREE & DAG",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Physical Directory: ${uiState.porterExtractPath}\n" +
                            "DAG Nodes: ${dagNodes.size} | Edges: ${uiState.hostDagEdgesCount} | Unresolved Transitive Deps: ${uiState.unresolvedDependenciesCount} (0 Omitted Files)\n" +
                            "FOD Sensor Calibration: ${uiState.fodGeometrySummary}",
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onExtractDeviceRootClick,
                        enabled = !uiState.isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = cs.tertiary, contentColor = cs.onTertiary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dag_screen_extract_button")
                    ) {
                        Icon(Icons.Filled.DeveloperBoard, contentDescription = "Refresh Extract")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RESCAN & EXTRACT ALL TO /sdcard/PORTER/EXTRACT", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        if (uiState.generatedSourceFilesInPorter.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "GENERATED AOSP SOURCE & FOD ARTIFACTS IN PORTER/EXTRACT",
                            style = MaterialTheme.typography.labelLarge,
                            color = cs.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        uiState.generatedSourceFilesInPorter.forEach { filePath ->
                            Text(
                                text = "• $filePath",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subsystems) { sub ->
                    FilterChip(
                        selected = selectedSubsystem == sub,
                        onClick = { selectedSubsystem = sub },
                        label = { Text(sub, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cs.primary.copy(alpha = 0.22f),
                            selectedLabelColor = cs.primary
                        )
                    )
                }
            }
        }

        if (filteredNodes.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountTree,
                            contentDescription = "Empty DAG",
                            tint = cs.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No DAG nodes captured yet for $selectedSubsystem. Click EXTRACT DEVICE or MAKE to populate /sdcard/PORTER/EXTRACT.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredNodes, key = { it.nodeId }) { node ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "[${node.subsystem}] ${node.nodeType}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (node.subsystem == "FOD_UDFPS") cs.primary else cs.secondary
                            )
                            Text(
                                text = node.partition.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = node.pathOrSymbol,
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = node.disassembledSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Transitive Closure Deps: ${node.dtNeededDependenciesCsv.ifBlank { "None (Leaf Node)" }}\nSELinux: ${node.selinuxContext} | Mode: ${node.fsConfigMode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.primary.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun TrustArtAndAiLabScreen(
    uiState: WorkbenchUiState,
    trustArtifacts: List<TrustAndArtArtifactEntity>,
    aiPlans: List<AiRestructuringPlanEntity>,
    onPromptChange: (String) -> Unit,
    onAskAiClick: () -> Unit,
    onSignRegenClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val latestPlan = aiPlans.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.tertiary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TRUST-CHAIN APK/XML SIGNER & OAT / VDEX / FSV_META REGENERATOR",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.tertiary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Preserves sharedUserId=android.uid.system & 4096-byte page alignment on all APKs/JARs.\n" +
                            "• Synchronizes X.509 signatures inside plat_mac_permissions.xml & privapp-permissions.\n" +
                            "• Regenerates .oat, .odex, .vdex, .art, boot-framework.art & .fsv_meta (FS-Verity Merkle tree) with updated DEX CRC32 checksums.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onSignRegenClick,
                        enabled = !uiState.isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = cs.tertiary, contentColor = cs.onTertiary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("trust_tab_sign_regen_button")
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = "Regenerate OAT/VDEX & Sign")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("REGENERATE OAT/VDEX/FSV_META, SYNC XML & SIGN AVB2", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        items(trustArtifacts, key = { it.id }) { art ->
            Card(
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = art.artifactCategory,
                            style = MaterialTheme.typography.labelLarge,
                            color = cs.secondary
                        )
                        Text(
                            text = art.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = art.targetFile,
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "sharedUserId: ${art.sharedUserId} | Key: ${art.keyAlias} | Alignment: ${art.pageAlignmentBytes}B\n" +
                            "DEX CRC32: ${art.dexCrc32Hex} | Cert SHA256: ${art.certSha256}\n" +
                            "Regenerated Files: ${art.regeneratedPathsCsv}",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }
        }

        if (latestPlan != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cs.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "GEMINI 3.1 PRO (THINKING: ${latestPlan.thinkingLevel}) RESTRUCTURING BLUEPRINT",
                            style = MaterialTheme.typography.labelLarge,
                            color = cs.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Bootloop Safety Score: ${latestPlan.bootloopSafetyScore}/100 | Zero-Omission Verified: ${latestPlan.zeroOmissionVerified}",
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "FOD / UDFPS Mechanism Resolution:\n${latestPlan.fodMechanismAnalysis}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Synthesized Smali & SELinux CIL Patch:\n${latestPlan.generatedSmaliPatch}\n\n${latestPlan.generatedSelinuxCilPatch}",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(cs.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ASK GEMINI SYSTEM ARCHITECT (FOD / DAG / SELINUX / OAT)",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.aiCustomPromptInput,
                        onValueChange = onPromptChange,
                        label = { Text("Ask about FOD HBM sysfs, SELinux CIL, or OAT/VDEX...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_architect_prompt_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onAskAiClick,
                        enabled = !uiState.isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ask_ai_architect_button")
                    ) {
                        Text("ANALYZE WITH GEMINI", style = MaterialTheme.typography.labelLarge)
                    }
                    AnimatedVisibility(visible = uiState.aiInteractiveResponse.isNotBlank()) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = uiState.aiInteractiveResponse,
                                style = MaterialTheme.typography.labelMedium,
                                color = cs.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(cs.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsAndCicdScreen(
    uiState: WorkbenchUiState,
    onThemeSelected: (AppThemeMode) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.SettingsBrightness,
                            contentDescription = "Thème de l'application",
                            tint = cs.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "APPARENCE & THÈME MATERIAL 3",
                            style = MaterialTheme.typography.titleMedium,
                            color = cs.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Choisissez le thème de l'interface (Clair, Sombre ou Suivant le système). Le changement s'applique immédiatement à toute l'application.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    AppThemeMode.entries.forEach { mode ->
                        val isSelected = uiState.themeMode == mode
                        val icon = when (mode) {
                            AppThemeMode.LIGHT -> Icons.Filled.LightMode
                            AppThemeMode.DARK -> Icons.Filled.DarkMode
                            AppThemeMode.SYSTEM -> Icons.Filled.SettingsBrightness
                        }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) cs.primaryContainer else cs.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onThemeSelected(mode) }
                                .testTag("theme_option_${mode.name.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onThemeSelected(mode) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = icon,
                                    contentDescription = mode.labelFr,
                                    tint = if (isSelected) cs.onPrimaryContainer else cs.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.labelFr,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) cs.onPrimaryContainer else cs.onSurface
                                    )
                                    Text(
                                        text = mode.descriptionFr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) cs.onPrimaryContainer.copy(alpha = 0.8f) else cs.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // GitHub Actions CI/CD Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "GitHub Actions Ready",
                            tint = cs.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "COMPILATION GITHUB ACTIONS PRÊTE",
                            style = MaterialTheme.typography.titleMedium,
                            color = cs.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Le workflow CI/CD a été généré dans ${uiState.githubWorkflowPath} avec support JDK 17, Gradle 8.13, génération automatique du debug.keystore et publication de l'artéfact APK.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Workflow: .github/workflows/android-build.yml\nTrigger: push / pull_request (main, master) & workflow_dispatch\nArtifact: ROM-Forge-Studio-debug-apk",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cs.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
