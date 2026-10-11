package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "unpacked_roms")
data class UnpackedRomEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val sourceFilePath: String,
    val workspaceDir: String,
    val imageFormat: String, // EXT4_RW, EROFS_RO, SPARSE_IMG, PAYLOAD_BIN, SUPER_DYNAMIC, COMPRESSED_ARCHIVE
    val sizeBytes: Long,
    val inodeCount: Int,
    val selinuxContextsCaptured: Int,
    val dagNodesCount: Int,
    val dagEdgesCount: Int,
    val isPorted: Boolean = false,
    val isFodSolved: Boolean = false,
    val isOatVdexRegenerated: Boolean = false,
    val isTrustChainVerified: Boolean = false,
    val isSignedAvb: Boolean = false,
    val repackedOutputPath: String = "",
    val vndkVersion: String = "34",
    val archAbi: String = "arm64-v8a",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "dag_nodes")
data class SystemGraphNodeEntity(
    @PrimaryKey val nodeId: String,
    val scope: String, // HOST_DEVICE or TARGET_IMG:<id>
    val nodeType: String, // KERNEL_SYSFS_NODE, INIT_RC_SERVICE, ELF_SHARED_LIB, ELF_BINARY, VINTF_HAL_INTERFACE, SELINUX_DOMAIN_RULE, FRAMEWORK_CLASS_DEX, RRO_RESOURCE_OVERLAY, FIRMWARE_BLOB, APK_CERTIFICATE_XML, ART_OAT_VDEX_FSV
    val subsystem: String, // FOD_UDFPS, DISPLAY_HBM, BIOMETRICS, AUDIO, CAMERA, RIL_IMS, KEYMASTER_TRUST, SELINUX_VINTF, ART_RUNTIME, CORE_SYSTEM
    val pathOrSymbol: String,
    val partition: String, // system, vendor, product, system_ext, odm, sysfs
    val sha256: String,
    val dtNeededDependenciesCsv: String, // Transitive ELF dependencies & .rodata referenced paths
    val disassembledSummary: String,
    val selinuxContext: String,
    val fsConfigMode: String, // UID:GID:MODE:CAPABILITIES
    val extractedToPath: String
)

@Serializable
@Entity(tableName = "trust_art_artifacts")
data class TrustAndArtArtifactEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val romId: Int,
    val targetFile: String, // e.g., /system/priv-app/SystemUI/SystemUI.apk or /system/framework/services.jar or /system/etc/selinux/plat_mac_permissions.xml
    val artifactCategory: String, // APK_SIGNATURE, XML_SIGNATURE_POLICY, OAT_ODEX_VDEX_ART, FSV_META_VERITY, AVB_VBMETA_FOOTER
    val sharedUserId: String, // android.uid.system, android.uid.phone, etc.
    val keyAlias: String, // platform, shared, media, networkstack, releasekey
    val certSha256: String,
    val dexCrc32Hex: String,
    val pageAlignmentBytes: Int, // 4096 (4K page aligned)
    val regeneratedPathsCsv: String, // e.g. oat/arm64/services.odex,oat/arm64/services.vdex,services.jar.fsv_meta
    val status: String // VERIFIED, REGENERATED, SYNCED_XML, SIGNED_AVB2
)

@Serializable
@Entity(tableName = "ai_restructuring_plans")
data class AiRestructuringPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val romId: Int,
    val modelUsed: String,
    val thinkingLevel: String,
    val bootloopSafetyScore: Int, // 0..100
    val zeroOmissionVerified: Boolean,
    val fodMechanismAnalysis: String,
    val dependencyGraphResolutionSummary: String,
    val trustAndOatRegenerationPlan: String,
    val generatedSmaliPatch: String,
    val generatedSelinuxCilPatch: String,
    val generatedInitRcBridge: String,
    val generatedRroOverlayXml: String,
    val generatedMacPermissionsXmlPatch: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface RomForgeDao {
    @Query("SELECT * FROM unpacked_roms ORDER BY timestamp DESC")
    fun observeUnpackedRoms(): Flow<List<UnpackedRomEntity>>

    @Query("SELECT * FROM unpacked_roms ORDER BY timestamp DESC")
    suspend fun getUnpackedRomsList(): List<UnpackedRomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnpackedRom(rom: UnpackedRomEntity): Long

    @Update
    suspend fun updateUnpackedRom(rom: UnpackedRomEntity)

    @Query("SELECT * FROM dag_nodes ORDER BY subsystem ASC, nodeType ASC")
    fun observeAllDagNodes(): Flow<List<SystemGraphNodeEntity>>

    @Query("SELECT * FROM dag_nodes WHERE scope = :scope")
    suspend fun getDagNodesByScope(scope: String): List<SystemGraphNodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDagNodes(nodes: List<SystemGraphNodeEntity>)

    @Query("SELECT * FROM trust_art_artifacts ORDER BY id DESC")
    fun observeTrustAndArtArtifacts(): Flow<List<TrustAndArtArtifactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrustAndArtArtifacts(items: List<TrustAndArtArtifactEntity>)

    @Query("SELECT * FROM ai_restructuring_plans ORDER BY timestamp DESC")
    fun observeAiPlans(): Flow<List<AiRestructuringPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiPlan(plan: AiRestructuringPlanEntity): Long
}

@Database(
    entities = [
        UnpackedRomEntity::class,
        SystemGraphNodeEntity::class,
        TrustAndArtArtifactEntity::class,
        AiRestructuringPlanEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RomForgeDatabase : RoomDatabase() {
    abstract fun dao(): RomForgeDao
}
