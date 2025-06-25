package jp.rdlabo.capacitor.plugin.codescanner

import android.Manifest
import androidx.fragment.app.FragmentActivity
import com.getcapacitor.JSObject
import com.getcapacitor.PermissionState
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback

@CapacitorPlugin(
    name = "CodeScanner", permissions = [Permission(
        alias = "camera", strings = [Manifest.permission.CAMERA
        ]
    )]
)
class CodeScannerPlugin : Plugin() {
    private val implementation = CodeScanner()

    @PluginMethod
    fun present(call: PluginCall) {
        // カメラ権限をチェック
        if (getPermissionState("camera") != PermissionState.GRANTED) {
            requestPermissionForAlias("camera", call, "cameraPermsCallback")
            return
        }

        // 権限がある場合はモーダルを表示
        showScannerModal(call)
    }

    @PermissionCallback
    fun cameraPermsCallback(call: PluginCall) {
        if (getPermissionState("camera") == PermissionState.GRANTED) {
            showScannerModal(call)
        } else {
            call.reject("Permission is required to take a picture")
        }
    }

    private fun showScannerModal(call: PluginCall) {
        // 現在のアクティビティを取得
        val activity: FragmentActivity? = activity
        if (activity == null) {
            call.reject("Activity not found")
            return
        }

        // BottomSheetDialogFragmentを表示
        implementation.present(activity, object : CodeScannerBottomSheetFragment.OnDismissListener {
            override fun onDismiss() {
                val ret = JSObject()
                ret.put("dismissed", true)
                call.resolve(ret)
            }
        })
    }
}
