package jp.rdlabo.capacitor.plugin.codescanner

import androidx.fragment.app.FragmentActivity
import com.getcapacitor.JSObject
import com.google.android.gms.common.util.BiConsumer

class CodeScanner {
    fun present(
        activity: FragmentActivity,
        notifyListenersFunction: BiConsumer<String, JSObject>,
        listener: CodeScannerBottomSheetFragment.OnDismissListener?
    ) {
        val fragment: CodeScannerBottomSheetFragment =
            CodeScannerBottomSheetFragment.Companion.newInstance()
        fragment.setNotifyListenersFunction(notifyListenersFunction)
        fragment.setOnDismissListener(listener)
        fragment.show(activity.supportFragmentManager, "CodeScannerBottomSheet")
    }
}
