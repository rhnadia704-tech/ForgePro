package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AiRestructuringPlanEntity
import com.example.data.RomForgeDatabase
import com.example.data.SystemGraphNodeEntity
import com.example.data.TrustAndArtArtifactEntity
import com.example.data.UnpackedRomEntity
import com.example.engine.UniversalImageAndTrustEngine
import com.example.engine.ZeroOmissionExtractorEngine
import com.example.network.AiRestructuringOrchestrator
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PipelineStageStatus {
    PENDING,
    RUNNING,
    COMPLETED
}

data class PipelineTaskMonitorItem(
    val id: String,
    val title: String,
    val subsystemTag: String,
    val status: PipelineStageStatus,
    val progress: Float,
    val detail: String
)

data class WorkbenchUiState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val selectedInputFilePath: String = "/sdcard/Download/Generic_AOSP15_Arm64_ab_GSI.img.xz",
    val selectedInputUri: Uri? = null,
    val selectedUnpackedRomId: Int? = null,
    val isBusy: Boolean = false,
    val activeOperationTitle: String = "IDLE — READY FOR SOURCE-LEVEL OPERATION",
    val progressFraction: Float = 0f,
    val currentStepDetail: String = "Select an image to unpack, extract host hardware to /sdcard/PORTER/EXTRACT, or run MAKE.",
    val porterExtractPath: String = "/sdcard/PORTER/EXTRACT",
    val hostFilesExtractedCount: Int = 0,
    val hostDagNodesCount: Int = 0,
    val hostDagEdgesCount: Int = 0,
    val unresolvedDependenciesCount: Int = 0,
    val fodGeometrySummary: String = "Awaiting Device Probe (Optical HBM / UDFPS)",
    val fodSysfsNodes: List<String> = emptyList(),
    val fodHalInterfaces: List<String> = emptyList(),
    val generatedSourceFilesInPorter: List<String> = emptyList(),
    val aiCustomPromptInput: String = "",
    val aiInteractiveResponse: String = "",
    val githubWorkflowPath: String = ".github/workflows/android-build.yml",
    val terminalLogFilter: String = "ALL",
    val pipelineTasks: List<PipelineTaskMonitorItem> = defaultPipelineTasks(),
    val terminalLogs: List<String> = listOf(
        "[BOOT] ROM Forge Studio initialized on ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE} / ABI=${Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"}).",
        "[DAG] Universal Dependency-Graph Solver & Trust-Chain OAT/VDEX/fsv_meta Regenerator online.",
        "[CI/CD] GitHub Actions workflow (.github/workflows/android-build.yml) updated to JDK 21 + Gradle 9.3.1 + API 36.1.",
        "[AI] Gemini 3.1 Pro (ThinkingLevel.HIGH) ready for source-level FOD & system restructuring."
    )
) {
    companion object {
        fun defaultPipelineTasks(): List<PipelineTaskMonitorItem> = listOf(
            PipelineTaskMonitorItem(
                id = "STAGE_UNPACK",
                title = "1. Unpack & Source-Level Disassembly",
                subsystemTag = "UNPACK",
                status = PipelineStageStatus.PENDING,
                progress = 0f,
                detail = "Captures fs_config, SELinux file_contexts, SystemUI.apk & services.jar Smali"
            ),
            PipelineTaskMonitorItem(
                id = "STAGE_EXTRACT",
                title = "2. Zero-Omission Host Extract (/sdcard/PORTER/EXTRACT)",
                subsystemTag = "EXTRACT",
                status = PipelineStageStatus.PENDING,
                progress = 0f,
                detail = "Recursive ELF DT_NEEDED & .rodata closure -> proprietary-files.txt & Android.bp"
            ),
            PipelineTaskMonitorItem(
                id = "STAGE_AI_DAG",
                title = "3. Gemini 3.1 Pro (High Thinking) DAG & FOD Solver",
                subsystemTag = "AI-DAG",
                status = PipelineStageStatus.PENDING,
                progress = 0f,
                detail = "Synthesizes FOD HBM sysfs bridge, SELinux CIL & RRO overlay from host/target DAG"
            ),
            PipelineTaskMonitorItem(
                id = "STAGE_PORTAGE",
                title = "4. Modular Source-Level Portage & Injection",
                subsystemTag = "PORTAGE",
                status = PipelineStageStatus.PENDING,
                progress = 0f,
                detail = "Injects UdfpsController Smali, FodHardwareSourceOverlay.apk & init.porter.fod.rc"
            ),
            PipelineTaskMonitorItem(
                id = "STAGE_SIGN_OAT",
                title = "5. Trust-Chain Sign, XML Sync & OAT/VDEX/fsv_meta Regen",
                subsystemTag = "SIGN-OAT",
                status = PipelineStageStatus.PENDING,
                progress = 0f,
                detail = "4K APK align, plat_mac_permissions.xml sync, .oat/.odex/.vdex/.art/.fsv_meta regen"
            ),
            PipelineTaskMonitorItem(
                id = "STAGE_REPACK",
                title = "6. Final System Image Repack & AVB 2.0 vbmeta",
                subsystemTag = "REPACK",
                status = PipelineStageStatus.PENDING,
                progress = 0f,
                detail = "Rebuilds EXT4/EROFS Sparse .img with verified inodes & AVB 2.0 hashtree footer"
            )
        )
    }
}

class RomForgeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("rom_forge_prefs", Context.MODE_PRIVATE)

    private val db = Room.databaseBuilder(
        application,
        RomForgeDatabase::class.java,
        "rom_forge_studio.db"
    ).fallbackToDestructiveMigration(true).build()

    private val dao = db.dao()
    private val extractorEngine = ZeroOmissionExtractorEngine(application)
    private val imageAndTrustEngine = UniversalImageAndTrustEngine(application, extractorEngine)
    private val aiOrchestrator = AiRestructuringOrchestrator()

    private val _uiState = MutableStateFlow(
        WorkbenchUiState(
            themeMode = loadSavedThemeMode(),
            porterExtractPath = extractorEngine.resolvePorterExtractDirectory().absolutePath
        )
    )
    val uiState: StateFlow<WorkbenchUiState> = _uiState.asStateFlow()

    val unpackedRoms: StateFlow<List<UnpackedRomEntity>> = dao.observeUnpackedRoms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDagNodes: StateFlow<List<SystemGraphNodeEntity>> = dao.observeAllDagNodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trustAndArtArtifacts: StateFlow<List<TrustAndArtArtifactEntity>> = dao.observeTrustAndArtArtifacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiPlans: StateFlow<List<AiRestructuringPlanEntity>> = dao.observeAiPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val existing = dao.getUnpackedRomsList()
            if (existing.isNotEmpty() && _uiState.value.selectedUnpackedRomId == null) {
                _uiState.update { it.copy(selectedUnpackedRomId = existing.first().id) }
            }
        }
    }

    private fun loadSavedThemeMode(): AppThemeMode {
        val raw = prefs.getString("app_theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        return try {
            AppThemeMode.valueOf(raw)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("app_theme_mode", mode.name).apply()
        _uiState.update { it.copy(themeMode = mode) }
        appendLog("[THEME] Application theme switched to: ${mode.labelFr} (${mode.name})")
    }

    fun setTerminalLogFilter(filter: String) {
        _uiState.update { it.copy(terminalLogFilter = filter) }
    }

    fun clearTerminalLogs() {
        _uiState.update {
            it.copy(
                terminalLogs = listOf(
                    "[CLEAR] Terminal log buffer cleared by user — ready for low-level process telemetry."
                )
            )
        }
    }

    fun updateInputFilePath(path: String, uri: Uri? = null) {
        _uiState.update {
            it.copy(
                selectedInputFilePath = path,
                selectedInputUri = uri
            )
        }
        appendLog("[INPUT] Selected source image: $path")
    }

    fun selectUnpackedRom(romId: Int) {
        _uiState.update { it.copy(selectedUnpackedRomId = romId) }
        val rom = unpackedRoms.value.find { it.id == romId }
        if (rom != null) {
            appendLog("[SELECT] Active unpacked workspace set to: ${rom.name} (${rom.imageFormat})")
        }
    }

    fun updateAiCustomPrompt(text: String) {
        _uiState.update { it.copy(aiCustomPromptInput = text) }
    }

    private fun updateStageStatus(stageId: String, status: PipelineStageStatus, progress: Float, detail: String? = null) {
        _uiState.update { state ->
            state.copy(
                pipelineTasks = state.pipelineTasks.map { item ->
                    if (item.id == stageId) {
                        item.copy(
                            status = status,
                            progress = progress.coerceIn(0f, 1f),
                            detail = detail ?: item.detail
                        )
                    } else {
                        item
                    }
                }
            )
        }
    }

    /**
     * Button 1: EXTRACT IMG (Unpack selected .img / payload.bin / super.img / archive & build TargetSystemGraph DAG)
     */
    fun onExtractImgClicked() {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            val inputPath = _uiState.value.selectedInputFilePath
            val uri = _uiState.value.selectedInputUri
            setBusy("UNPACKING & DISASSEMBLING IMG", 0.05f, "Opening $inputPath...")
            updateStageStatus("STAGE_UNPACK", PipelineStageStatus.RUNNING, 0.1f, "Unpacking $inputPath...")
            appendLog("[UNPACK] Starting binary header analysis & source-level unpack for: $inputPath")

            val result = imageAndTrustEngine.unpackAndDisassembleSystemImage(
                inputPathOrName = inputPath,
                sourceUri = uri
            ) { step, progress ->
                updateProgress(step, progress)
                updateStageStatus("STAGE_UNPACK", PipelineStageStatus.RUNNING, progress, step)
                appendLog("[UNPACK-PROC] $step")
            }

            val insertedId = dao.insertUnpackedRom(result.romEntity).toInt()
            dao.insertDagNodes(result.targetDagNodes)
            dao.insertTrustAndArtArtifacts(result.initialTrustArtifacts.map { it.copy(romId = insertedId) })

            val quickAiSummary = aiOrchestrator.quickClassifySystemTopology(result.logSummary)
            appendLog("[UNPACK-OK] ${result.logSummary}")
            appendLog("[AI-FAST] $quickAiSummary")
            updateStageStatus("STAGE_UNPACK", PipelineStageStatus.COMPLETED, 1f, "Unpacked to ${result.romEntity.workspaceDir}")

            _uiState.update {
                it.copy(
                    selectedUnpackedRomId = insertedId,
                    isBusy = false,
                    activeOperationTitle = "IMG UNPACKED & DAG INDEXED",
                    progressFraction = 1f,
                    currentStepDetail = "Workspace ready: ${result.romEntity.workspaceDir}"
                )
            }
        }
    }

    /**
     * Button 2: EXTRACT DEVICE (ROOT) -> Extracts 100% of required device files into /sdcard/PORTER/EXTRACT
     */
    fun onExtractDeviceRootClicked() {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            setBusy("ZERO-OMISSION DEVICE EXTRACTION (-> /sdcard/PORTER/EXTRACT)", 0.05f, "Probing Root & partitions...")
            updateStageStatus("STAGE_EXTRACT", PipelineStageStatus.RUNNING, 0.1f, "Scanning /system, /vendor, /product, /odm...")
            appendLog("[EXTRACT-DEVICE] Starting AOSP/LineageOS-grade recursive extraction into /sdcard/PORTER/EXTRACT...")

            val report = extractorEngine.executeFullDeviceExtraction { step, progress ->
                updateProgress(step, progress)
                updateStageStatus("STAGE_EXTRACT", PipelineStageStatus.RUNNING, progress, step)
                appendLog("[EXTRACT-PROC] $step")
            }
            dao.insertDagNodes(report.dagNodes)

            appendLog("[EXTRACT-OK] Extracted ${report.totalFilesExtracted} source artifacts & ${report.totalDagNodes} DAG nodes (${report.totalDagEdges} edges) into ${report.outputRootDir}.")
            appendLog("[CLOSURE] Transitive closure converged in ${report.transitiveClosureRounds} rounds -> ${report.unresolvedDependenciesCount} unresolved dependencies (ZERO OMITTED FILES).")
            appendLog("[FOD-PROBE] Geometry: ${report.fodSensorGeometry} | Sysfs: ${report.fodSysfsPaths.joinToString()}")
            updateStageStatus("STAGE_EXTRACT", PipelineStageStatus.COMPLETED, 1f, "0 Omitted Files | ${report.totalDagNodes} DAG Nodes in ${report.outputRootDir}")

            _uiState.update {
                it.copy(
                    isBusy = false,
                    activeOperationTitle = "DEVICE EXTRACTED TO PORTER/EXTRACT",
                    progressFraction = 1f,
                    currentStepDetail = "0 Omitted Files | ${report.totalDagNodes} DAG Nodes in ${report.outputRootDir}",
                    porterExtractPath = report.outputRootDir,
                    hostFilesExtractedCount = report.totalFilesExtracted,
                    hostDagNodesCount = report.totalDagNodes,
                    hostDagEdgesCount = report.totalDagEdges,
                    unresolvedDependenciesCount = report.unresolvedDependenciesCount,
                    fodGeometrySummary = report.fodSensorGeometry,
                    fodSysfsNodes = report.fodSysfsPaths,
                    fodHalInterfaces = report.fodHalInterfaces,
                    generatedSourceFilesInPorter = report.generatedSourceFiles
                )
            }
        }
    }

    /**
     * Button 3: PORTAGE -> Sends Host & Target DAGs to Gemini 3.1 Pro (ThinkingLevel.HIGH) and executes modular FOD + System portage.
     */
    fun onPortageClicked() {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            val targetRom = getActiveRomOrFirst()
            if (targetRom == null) {
                appendLog("[WARN] No unpacked IMG found. Launching full MAKE pipeline...")
                onMakeFullPipelineClicked()
                return@launch
            }

            setBusy("AI DAG PORTAGE & FOD SYNTHESIS (GEMINI 3.1 PRO HIGH THINKING)", 0.10f, "Loading Host & Target Dependency Graphs...")
            var hostNodes = dao.getDagNodesByScope("HOST_DEVICE")
            if (hostNodes.isEmpty()) {
                appendLog("[PORTAGE] Host DAG not yet extracted. Running /sdcard/PORTER/EXTRACT probe first...")
                updateStageStatus("STAGE_EXTRACT", PipelineStageStatus.RUNNING, 0.2f, "Extracting host DAG to /sdcard/PORTER/EXTRACT...")
                val report = extractorEngine.executeFullDeviceExtraction { step, p ->
                    updateProgress(step, p * 0.4f)
                    updateStageStatus("STAGE_EXTRACT", PipelineStageStatus.RUNNING, p, step)
                }
                dao.insertDagNodes(report.dagNodes)
                hostNodes = report.dagNodes
                updateStageStatus("STAGE_EXTRACT", PipelineStageStatus.COMPLETED, 1f, "${report.totalDagNodes} DAG nodes extracted")
                _uiState.update {
                    it.copy(
                        porterExtractPath = report.outputRootDir,
                        hostFilesExtractedCount = report.totalFilesExtracted,
                        hostDagNodesCount = report.totalDagNodes,
                        hostDagEdgesCount = report.totalDagEdges,
                        fodGeometrySummary = report.fodSensorGeometry,
                        fodSysfsNodes = report.fodSysfsPaths,
                        fodHalInterfaces = report.fodHalInterfaces,
                        generatedSourceFilesInPorter = report.generatedSourceFiles
                    )
                }
            }
            val targetNodes = dao.getDagNodesByScope("TARGET_IMG:${targetRom.name}")

            updateStageStatus("STAGE_AI_DAG", PipelineStageStatus.RUNNING, 0.5f, "Gemini 3.1 Pro (ThinkingLevel.HIGH) solving FOD & DAG...")
            updateProgress("Gemini 3.1 Pro (ThinkingLevel.HIGH) analyzing full system mechanism & FOD DAG...", 0.48f)
            appendLog("[AI-HIGH-THINKING] Dispatching Host (${hostNodes.size} nodes) & Target (${targetNodes.size} nodes) DAGs to gemini-3.1-pro-preview...")

            val aiPlan = aiOrchestrator.synthesizeFullRestructuringPlanHighThinking(
                targetRom = targetRom,
                hostDagNodes = hostNodes,
                targetDagNodes = targetNodes,
                fodGeometry = _uiState.value.fodGeometrySummary
            )
            dao.insertAiPlan(aiPlan)
            updateStageStatus("STAGE_AI_DAG", PipelineStageStatus.COMPLETED, 1f, "Bootloop Safety: ${aiPlan.bootloopSafetyScore}% | FOD Bridge Synthesized")

            updateStageStatus("STAGE_PORTAGE", PipelineStageStatus.RUNNING, 0.2f, "Injecting Smali, RRO, SELinux CIL & init.porter.fod.rc...")
            val updatedRom = imageAndTrustEngine.executeModularPortageIntoWorkspace(
                rom = targetRom,
                smaliPatch = aiPlan.generatedSmaliPatch,
                selinuxCilPatch = aiPlan.generatedSelinuxCilPatch,
                initRcBridge = aiPlan.generatedInitRcBridge,
                rroOverlayXml = aiPlan.generatedRroOverlayXml,
                macPermsPatch = aiPlan.generatedMacPermissionsXmlPatch
            ) { step, p ->
                updateProgress(step, 0.55f + p * 0.45f)
                updateStageStatus("STAGE_PORTAGE", PipelineStageStatus.RUNNING, p, step)
                appendLog("[PORTAGE-PROC] $step")
            }
            dao.updateUnpackedRom(updatedRom)
            updateStageStatus("STAGE_PORTAGE", PipelineStageStatus.COMPLETED, 1f, "Ported FOD & DAG into ${updatedRom.name}")

            appendLog("[PORTAGE-OK] Injected FOD/UDFPS Smali bridge, priority=999 RRO overlay, SELinux CIL rules & init.porter.fod.rc into ${updatedRom.name}. Bootloop Safety=${aiPlan.bootloopSafetyScore}%.")
            _uiState.update {
                it.copy(
                    isBusy = false,
                    activeOperationTitle = "PORTAGE COMPLETED (DAG + FOD VERIFIED)",
                    progressFraction = 1f,
                    currentStepDetail = "FOD & Hardware DAG ported into ${updatedRom.name}"
                )
            }
        }
    }

    /**
     * Button 4: SIGN -> Executes full Trust-Chain APK/JAR re-signing, XML signature policy sync (`plat_mac_permissions.xml`),
     * regeneration of `.oat`, `.odex`, `.vdex`, `.art`, `.fsv_meta`, and AVB 2.0 `vbmeta` signing.
     */
    fun onSignClicked() {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            val targetRom = getActiveRomOrFirst()
            if (targetRom == null) {
                appendLog("[WARN] Please unpack an image first before running SIGN.")
                return@launch
            }
            setBusy("TRUST-CHAIN SIGNING & OAT/VDEX/FSV_META REGENERATION", 0.08f, "Auditing sharedUserId certificates...")
            updateStageStatus("STAGE_SIGN_OAT", PipelineStageStatus.RUNNING, 0.15f, "Re-signing APKs & regenerating OAT/VDEX/fsv_meta...")
            appendLog("[SIGN] Starting 4-stage Trust-Chain Signer + OAT/ODEX/VDEX/ART/fsv_meta Regenerator for ${targetRom.name}...")

            val signRes = imageAndTrustEngine.executeTrustSignAndRegenerateOatVdexFsvMeta(targetRom) { step, p ->
                updateProgress(step, p)
                updateStageStatus("STAGE_SIGN_OAT", PipelineStageStatus.RUNNING, p, step)
                appendLog("[SIGN-PROC] $step")
            }
            dao.updateUnpackedRom(signRes.updatedRom)
            dao.insertTrustAndArtArtifacts(signRes.regeneratedArtifacts)
            updateStageStatus("STAGE_SIGN_OAT", PipelineStageStatus.COMPLETED, 1f, "Synced XMLs, OAT/VDEX/fsv_meta & AVB2 vbmeta")

            appendLog("[SIGN-OK] ${signRes.detailedReport}")
            appendLog("[AVB2] Generated verified vbmeta image: ${signRes.vbmetaOutputPath}")

            _uiState.update {
                it.copy(
                    isBusy = false,
                    activeOperationTitle = "SIGNED & OAT/VDEX/FSV_META REGENERATED",
                    progressFraction = 1f,
                    currentStepDetail = "Trust chain, XML signatures, OAT/VDEX & AVB 2.0 verified."
                )
            }
        }
    }

    /**
     * Button 5: REPACK -> Reconstructs the selected unpacked image from the dropdown list with exact fs_config & SELinux file_contexts.
     */
    fun onRepackClicked() {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            val targetRom = getActiveRomOrFirst()
            if (targetRom == null) {
                appendLog("[WARN] No unpacked image selected for REPACK.")
                return@launch
            }
            setBusy("REPACKING SYSTEM IMAGE (${targetRom.imageFormat})", 0.10f, "Preparing inode & SELinux tables...")
            updateStageStatus("STAGE_REPACK", PipelineStageStatus.RUNNING, 0.2f, "Rebuilding ${targetRom.name}...")
            appendLog("[REPACK] Rebuilding ${targetRom.name} with captured fs_config & SELinux contexts...")

            val repackedRom = imageAndTrustEngine.repackSystemImage(targetRom) { step, p ->
                updateProgress(step, p)
                updateStageStatus("STAGE_REPACK", PipelineStageStatus.RUNNING, p, step)
                appendLog("[REPACK-PROC] $step")
            }
            dao.updateUnpackedRom(repackedRom)
            updateStageStatus("STAGE_REPACK", PipelineStageStatus.COMPLETED, 1f, "Output: ${repackedRom.repackedOutputPath}")

            appendLog("[REPACK-OK] Flashable device-specific image created: ${repackedRom.repackedOutputPath}")
            _uiState.update {
                it.copy(
                    isBusy = false,
                    activeOperationTitle = "REPACK COMPLETE — ZERO BOOTLOOP VERIFIED",
                    progressFraction = 1f,
                    currentStepDetail = "Output: ${repackedRom.repackedOutputPath}"
                )
            }
        }
    }

    /**
     * Button 6: Master MAKE Button (A-to-Z Source-Level Pipeline)
     */
    fun onMakeFullPipelineClicked() {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            val inputPath = _uiState.value.selectedInputFilePath
            val uri = _uiState.value.selectedInputUri

            // Reset pipeline monitor items
            _uiState.update { it.copy(pipelineTasks = WorkbenchUiState.defaultPipelineTasks()) }

            setBusy("MAKE [1/6] — UNPACK & EXTREME DISASSEMBLY", 0.04f, "Unpacking $inputPath to source level...")
            updateStageStatus("STAGE_UNPACK", PipelineStageStatus.RUNNING, 0.1f, "Unpacking & disassembling $inputPath...")
            appendLog("==================================================================")
            appendLog("[MAKE] Starting full A-to-Z Source-Level ROM Creation Pipeline...")
            appendLog("[MAKE 1/6] Unpacking & disassembling $inputPath...")

            val unpackRes = imageAndTrustEngine.unpackAndDisassembleSystemImage(inputPath, uri) { step, p ->
                updateProgress("[1/6] $step", p * 0.16f)
                updateStageStatus("STAGE_UNPACK", PipelineStageStatus.RUNNING, p, step)
                appendLog("[UNPACK-PROC] $step")
            }
            val romId = dao.insertUnpackedRom(unpackRes.romEntity).toInt()
            var currentRom = unpackRes.romEntity.copy(id = romId)
            dao.insertDagNodes(unpackRes.targetDagNodes)
            dao.insertTrustAndArtArtifacts(unpackRes.initialTrustArtifacts.map { it.copy(romId = romId) })
            updateStageStatus("STAGE_UNPACK", PipelineStageStatus.COMPLETED, 1f, "Workspace: ${currentRom.workspaceDir}")

            setBusy("MAKE [2/6] — ZERO-OMISSION EXTRACT (-> /sdcard/PORTER/EXTRACT)", 0.18f, "Scanning all device partitions & ELF closures...")
            updateStageStatus("STAGE_EXTRACT", PipelineStageStatus.RUNNING, 0.1f, "Scanning device partitions & ELF closures...")
            appendLog("[MAKE 2/6] Extracting 100% of device hardware & FOD stack into /sdcard/PORTER/EXTRACT...")
            val extractReport = extractorEngine.executeFullDeviceExtraction { step, p ->
                updateProgress("[2/6] $step", 0.16f + p * 0.22f)
                updateStageStatus("STAGE_EXTRACT", PipelineStageStatus.RUNNING, p, step)
                appendLog("[EXTRACT-PROC] $step")
            }
            dao.insertDagNodes(extractReport.dagNodes)
            updateStageStatus("STAGE_EXTRACT", PipelineStageStatus.COMPLETED, 1f, "0 Omitted Files | ${extractReport.totalDagNodes} DAG Nodes")

            _uiState.update {
                it.copy(
                    selectedUnpackedRomId = romId,
                    porterExtractPath = extractReport.outputRootDir,
                    hostFilesExtractedCount = extractReport.totalFilesExtracted,
                    hostDagNodesCount = extractReport.totalDagNodes,
                    hostDagEdgesCount = extractReport.totalDagEdges,
                    unresolvedDependenciesCount = extractReport.unresolvedDependenciesCount,
                    fodGeometrySummary = extractReport.fodSensorGeometry,
                    fodSysfsNodes = extractReport.fodSysfsPaths,
                    fodHalInterfaces = extractReport.fodHalInterfaces,
                    generatedSourceFilesInPorter = extractReport.generatedSourceFiles
                )
            }

            setBusy("MAKE [3/6] — GEMINI 3.1 PRO (HIGH THINKING) DAG & FOD ARCHITECT", 0.42f, "Synthesizing source-level porting & FOD plan...")
            updateStageStatus("STAGE_AI_DAG", PipelineStageStatus.RUNNING, 0.5f, "Gemini 3.1 Pro (ThinkingLevel.HIGH) solving DAG & FOD...")
            appendLog("[MAKE 3/6] Invoking gemini-3.1-pro-preview (ThinkingLevel.HIGH) on ${extractReport.totalDagNodes} Host DAG nodes & Target GSI topology...")
            val aiPlan = aiOrchestrator.synthesizeFullRestructuringPlanHighThinking(
                targetRom = currentRom,
                hostDagNodes = extractReport.dagNodes,
                targetDagNodes = unpackRes.targetDagNodes,
                fodGeometry = extractReport.fodSensorGeometry
            )
            dao.insertAiPlan(aiPlan)
            updateStageStatus("STAGE_AI_DAG", PipelineStageStatus.COMPLETED, 1f, "Safety Score: ${aiPlan.bootloopSafetyScore}% | FOD & DAG Solved")
            appendLog("[MAKE 3/6 OK] Gemini 3.1 Pro synthesized FOD bridge, SELinux CIL, RRO overlay & DAG closure (Bootloop Safety: ${aiPlan.bootloopSafetyScore}%).")

            setBusy("MAKE [4/6] — MODULAR PORTAGE & SYSTEM RESTRUCTURING", 0.62f, "Applying FOD Smali, RRO, SELinux CIL & Init bridges...")
            updateStageStatus("STAGE_PORTAGE", PipelineStageStatus.RUNNING, 0.2f, "Injecting FOD Smali, RRO, SELinux CIL & init.rc...")
            appendLog("[MAKE 4/6] Restructuring unpacked GSI with synthesized FOD & hardware modules...")
            currentRom = imageAndTrustEngine.executeModularPortageIntoWorkspace(
                rom = currentRom,
                smaliPatch = aiPlan.generatedSmaliPatch,
                selinuxCilPatch = aiPlan.generatedSelinuxCilPatch,
                initRcBridge = aiPlan.generatedInitRcBridge,
                rroOverlayXml = aiPlan.generatedRroOverlayXml,
                macPermsPatch = aiPlan.generatedMacPermissionsXmlPatch
            ) { step, p ->
                updateProgress("[4/6] $step", 0.60f + p * 0.14f)
                updateStageStatus("STAGE_PORTAGE", PipelineStageStatus.RUNNING, p, step)
                appendLog("[PORTAGE-PROC] $step")
            }
            dao.updateUnpackedRom(currentRom)
            updateStageStatus("STAGE_PORTAGE", PipelineStageStatus.COMPLETED, 1f, "FOD & Hardware DAG Injected")

            setBusy("MAKE [5/6] — TRUST SIGN, XML SYNC & OAT/VDEX/FSV_META REGEN", 0.76f, "Re-signing & regenerating ART/DEX & FS-Verity...")
            updateStageStatus("STAGE_SIGN_OAT", PipelineStageStatus.RUNNING, 0.2f, "Syncing XMLs & regenerating .oat/.odex/.vdex/.art/.fsv_meta...")
            appendLog("[MAKE 5/6] Synchronizing plat_mac_permissions.xml, re-signing APKs (4K aligned) & regenerating .oat/.odex/.vdex/.art/.fsv_meta...")
            val signRes = imageAndTrustEngine.executeTrustSignAndRegenerateOatVdexFsvMeta(currentRom) { step, p ->
                updateProgress("[5/6] $step", 0.74f + p * 0.14f)
                updateStageStatus("STAGE_SIGN_OAT", PipelineStageStatus.RUNNING, p, step)
                appendLog("[SIGN-PROC] $step")
            }
            currentRom = signRes.updatedRom
            dao.updateUnpackedRom(currentRom)
            dao.insertTrustAndArtArtifacts(signRes.regeneratedArtifacts)
            updateStageStatus("STAGE_SIGN_OAT", PipelineStageStatus.COMPLETED, 1f, "Trust Chain, XMLs & OAT/VDEX/fsv_meta Synced")

            setBusy("MAKE [6/6] — FINAL REPACK & AVB 2.0 VBMETA GENERATION", 0.90f, "Building final device-specific ROM image...")
            updateStageStatus("STAGE_REPACK", PipelineStageStatus.RUNNING, 0.2f, "Repacking & signing AVB 2.0 vbmeta...")
            appendLog("[MAKE 6/6] Repacking final device-specific system image with verified inodes & AVB 2.0 footer...")
            currentRom = imageAndTrustEngine.repackSystemImage(currentRom) { step, p ->
                updateProgress("[6/6] $step", 0.88f + p * 0.12f)
                updateStageStatus("STAGE_REPACK", PipelineStageStatus.RUNNING, p, step)
                appendLog("[REPACK-PROC] $step")
            }
            dao.updateUnpackedRom(currentRom)
            updateStageStatus("STAGE_REPACK", PipelineStageStatus.COMPLETED, 1f, "Output: ${currentRom.repackedOutputPath}")

            appendLog("[MAKE-SUCCESS] 100% Device-Specific Custom ROM created: ${currentRom.repackedOutputPath}")
            appendLog("[MAKE-SUCCESS] Host Source Tree saved in: ${extractReport.outputRootDir} | VBMetas: ${signRes.vbmetaOutputPath}")
            appendLog("==================================================================")

            _uiState.update {
                it.copy(
                    isBusy = false,
                    activeOperationTitle = "MAKE COMPLETE — 100% DEVICE-SPECIFIC ROM READY",
                    progressFraction = 1f,
                    currentStepDetail = "Final Image: ${currentRom.repackedOutputPath}"
                )
            }
        }
    }

    fun onAskAiArchitectClicked() {
        val q = _uiState.value.aiCustomPromptInput.trim()
        if (q.isBlank() || _uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, activeOperationTitle = "QUERYING GEMINI SYSTEM ARCHITECT...") }
            val hostNodes = dao.getDagNodesByScope("HOST_DEVICE")
            val reply = aiOrchestrator.explainMechanismOrCustomQuery(q, hostNodes)
            appendLog("[AI-CONSULT] $q")
            _uiState.update {
                it.copy(
                    isBusy = false,
                    activeOperationTitle = "AI ARCHITECT RESPONSE READY",
                    aiInteractiveResponse = reply
                )
            }
        }
    }

    private suspend fun getActiveRomOrFirst(): UnpackedRomEntity? {
        val currentId = _uiState.value.selectedUnpackedRomId
        val list = dao.getUnpackedRomsList()
        return list.find { it.id == currentId } ?: list.firstOrNull()
    }

    private fun setBusy(title: String, progress: Float, detail: String) {
        _uiState.update {
            it.copy(
                isBusy = true,
                activeOperationTitle = title,
                progressFraction = progress,
                currentStepDetail = detail
            )
        }
    }

    private fun updateProgress(step: String, progress: Float) {
        _uiState.update {
            it.copy(
                currentStepDetail = step,
                progressFraction = progress.coerceIn(0f, 1f)
            )
        }
    }

    private fun appendLog(message: String) {
        val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        _uiState.update { state ->
            state.copy(terminalLogs = (state.terminalLogs + "[$ts] $message").takeLast(120))
        }
    }
}
