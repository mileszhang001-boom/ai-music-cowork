package com.music.localmusic.source

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile

object UsbStorageHelper {
    private const val TAG = "UsbStorageHelper"
    private const val PREFS_NAME = "usb_music_source"
    private const val KEY_USB_URI = "usb_root_uri"
    private const val KEY_USB_URI_PERMISSION = "usb_uri_permission_taken"
    
    fun saveUsbUri(context: Context, uri: Uri): Boolean {
        return try {
            val prefs = getPrefs(context)
            val editor = prefs.edit()
            editor.putString(KEY_USB_URI, uri.toString())
            editor.putBoolean(KEY_USB_URI_PERMISSION, true)
            editor.apply()
            Log.i(TAG, "USB URI 已保存: $uri")
            true
        } catch (e: Exception) {
            Log.e(TAG, "保存 USB URI 失败", e)
            false
        }
    }
    
    fun loadUsbUri(context: Context): Uri? {
        return try {
            val prefs = getPrefs(context)
            val uriString = prefs.getString(KEY_USB_URI, null)
            if (uriString != null) {
                Uri.parse(uriString)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载 USB URI 失败", e)
            null
        }
    }
    
    fun hasStoredUri(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.contains(KEY_USB_URI)
    }
    
    fun clearUsbUri(context: Context) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()
        editor.remove(KEY_USB_URI)
        editor.remove(KEY_USB_URI_PERMISSION)
        editor.apply()
        Log.i(TAG, "USB URI 已清除")
    }
    
    fun isUriAccessible(context: Context, uri: Uri): Boolean {
        return try {
            val doc = DocumentFile.fromTreeUri(context, uri)
            doc?.exists() == true && doc.canRead()
        } catch (e: Exception) {
            Log.e(TAG, "URI 不可访问", e)
            false
        }
    }
    
    fun takePersistablePermission(context: Context, uri: Uri) {
        try {
            val contentResolver = context.contentResolver
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                       android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flags)
            Log.i(TAG, "已获取持久化权限: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "获取持久化权限失败", e)
        }
    }
    
    fun releasePersistablePermission(context: Context, uri: Uri) {
        try {
            val contentResolver = context.contentResolver
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                       android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.releasePersistableUriPermission(uri, flags)
            Log.i(TAG, "已释放持久化权限: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "释放持久化权限失败", e)
        }
    }
    
    fun listPersistableUris(context: Context): List<Uri> {
        val contentResolver = context.contentResolver
        val permissions = contentResolver.persistedUriPermissions
        return permissions.map { it.uri }
    }
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
