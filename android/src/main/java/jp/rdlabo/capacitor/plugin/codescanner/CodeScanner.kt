package jp.rdlabo.capacitor.plugin.codescanner

import androidx.fragment.app.FragmentActivity

class CodeScanner {
    fun present(
        activity: FragmentActivity,
        listener: CodeScannerBottomSheetFragment.OnDismissListener?
    ) {
        val fragment: CodeScannerBottomSheetFragment =
            CodeScannerBottomSheetFragment.Companion.newInstance()
        fragment.setOnDismissListener(listener)
        fragment.show(activity.supportFragmentManager, "CodeScannerBottomSheet")
    }
}
