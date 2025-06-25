package jp.rdlabo.capacitor.plugin.codescanner;

import android.Manifest;
import android.content.pm.PackageManager;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import androidx.fragment.app.FragmentActivity;

@CapacitorPlugin(
    name = "CodeScanner",
    permissions = {
        @Permission(
            alias = "camera",
            strings = {
                Manifest.permission.CAMERA
            }
        )
    }
)
public class CodeScannerPlugin extends Plugin {

    private final CodeScanner implementation = new CodeScanner();

    @PluginMethod
    public void present(PluginCall call) {
        // カメラ権限をチェック
        if (getPermissionState("camera") != PermissionState.GRANTED) {
            requestPermissionForAlias("camera", call, "cameraPermsCallback");
            return;
        }

        // 権限がある場合はモーダルを表示
        showScannerModal(call);
    }

    @PermissionCallback
    public void cameraPermsCallback(PluginCall call) {
        if (getPermissionState("camera") == PermissionState.GRANTED) {
            showScannerModal(call);
        } else {
            call.reject("Permission is required to take a picture");
        }
    }

    private void showScannerModal(PluginCall call) {
        // 現在のアクティビティを取得
        FragmentActivity activity = getActivity();
        if (activity == null) {
            call.reject("Activity not found");
            return;
        }

        // BottomSheetDialogFragmentを表示
        implementation.present(activity, new CodeScannerBottomSheetFragment.OnDismissListener() {
            @Override
            public void onDismiss() {
                // モーダルが閉じられた時の処理
                JSObject ret = new JSObject();
                ret.put("dismissed", true);
                call.resolve(ret);
            }
        });
    }
}
