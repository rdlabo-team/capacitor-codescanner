package jp.rdlabo.capacitor.plugin.codescanner

import androidx.fragment.app.FragmentActivity
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.google.android.gms.common.util.BiConsumer

class CodeScanner {
    fun present(
        isMulti: Boolean,
        enableAutoLight: Boolean,
        enableCloseButton: Boolean,
        codeTypes: JSArray,
        detectionWidth: Float,
        detectionHeight: Float,
        activity: FragmentActivity,
        notifyListenersFunction: BiConsumer<String, JSObject>,
        listener: CodeScannerBottomSheetFragment.OnDismissListener?
    ) {
        val fragment: CodeScannerBottomSheetFragment =
            CodeScannerBottomSheetFragment.Companion.newInstance()
        fragment.setNotifyListenersFunction(notifyListenersFunction)
        fragment.setOnDismissListener(listener)
        fragment.setCallSettings(isMulti, enableAutoLight, enableCloseButton, codeTypes,
            detectionWidth,
            detectionHeight)
        fragment.show(activity.supportFragmentManager, "CodeScannerBottomSheet")
    }
}
