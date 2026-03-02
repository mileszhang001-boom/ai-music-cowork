package com.music.appmain.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionResult(
    val permission: String,
    val isGranted: Boolean,
    val shouldShowRationale: Boolean
)

data class PermissionsState(
    val camera: Boolean = false,
    val recordAudio: Boolean = false,
    val accessFineLocation: Boolean = false,
    val readExternalStorage: Boolean = false,
    val allGranted: Boolean = false,
    val deniedPermissions: List<String> = emptyList()
)

class PermissionManager(private val activity: ComponentActivity) {

    companion object {
        val REQUIRED_PERMISSIONS = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    private val _permissionsState = MutableStateFlow(PermissionsState())
    val permissionsState: StateFlow<PermissionsState> = _permissionsState.asStateFlow()

    private val _permissionResults = MutableStateFlow<List<PermissionResult>>(emptyList())
    val permissionResults: StateFlow<List<PermissionResult>> = _permissionResults.asStateFlow()
    private var onPermissionsResultCallback: ((List<PermissionResult>) -> Unit)? = null

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val results = permissions.map { (permission, isGranted) ->
            PermissionResult(
                permission = permission,
                isGranted = isGranted,
                shouldShowRationale = !isGranted && activity.shouldShowRequestPermissionRationale(permission)
            )
        }
        _permissionResults.value = results
        updatePermissionsState()
        onPermissionsResultCallback?.invoke(results)
    }

    fun checkPermissions(): PermissionsState {
        val camera = checkPermission(Manifest.permission.CAMERA)
        val recordAudio = checkPermission(Manifest.permission.RECORD_AUDIO)
        val accessFineLocation = checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val readExternalStorage = checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)

        val deniedPermissions = mutableListOf<String>()
        if (!camera) deniedPermissions.add(Manifest.permission.CAMERA)
        if (!recordAudio) deniedPermissions.add(Manifest.permission.RECORD_AUDIO)
        if (!accessFineLocation) deniedPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!readExternalStorage) deniedPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)

        val state = PermissionsState(
            camera = camera,
            recordAudio = recordAudio,
            accessFineLocation = accessFineLocation,
            readExternalStorage = readExternalStorage,
            allGranted = deniedPermissions.isEmpty(),
            deniedPermissions = deniedPermissions
        )
        _permissionsState.value = state
        return state
    }

    fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermissions(callback: ((List<PermissionResult>) -> Unit)? = null) {
        onPermissionsResultCallback = callback
        permissionLauncher.launch(REQUIRED_PERMISSIONS.toTypedArray())
    }

    fun requestSpecificPermissions(
        permissions: List<String>,
        callback: ((List<PermissionResult>) -> Unit)? = null
    ) {
        onPermissionsResultCallback = callback
        permissionLauncher.launch(permissions.toTypedArray())
    }

    fun getDeniedPermissionsWithRationale(): List<PermissionResult> {
        return REQUIRED_PERMISSIONS.map { permission ->
            val isGranted = checkPermission(permission)
            PermissionResult(
                permission = permission,
                isGranted = isGranted,
                shouldShowRationale = !isGranted && activity.shouldShowRequestPermissionRationale(permission)
            )
        }.filter { !it.isGranted }
    }

    fun hasAllPermissions(): Boolean {
        return checkPermissions().allGranted
    }

    fun getPermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "相机"
            Manifest.permission.RECORD_AUDIO -> "录音"
            Manifest.permission.ACCESS_FINE_LOCATION -> "位置"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "存储"
            else -> permission.substringAfterLast(".")
        }
    }

    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "用于拍摄照片和视频"
            Manifest.permission.RECORD_AUDIO -> "用于语音识别和录音功能"
            Manifest.permission.ACCESS_FINE_LOCATION -> "用于获取您的精确位置信息"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "用于读取本地音乐文件"
            else -> "应用需要此权限"
        }
    }

    private fun updatePermissionsState() {
        checkPermissions()
    }
}
