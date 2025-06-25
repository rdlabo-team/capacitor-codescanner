package jp.rdlabo.capacitor.plugin.codescanner;

import android.app.Activity;
import androidx.fragment.app.FragmentActivity;

public class CodeScanner {

    public void present(FragmentActivity activity, CodeScannerBottomSheetFragment.OnDismissListener listener) {
        CodeScannerBottomSheetFragment fragment = CodeScannerBottomSheetFragment.newInstance();
        fragment.setOnDismissListener(listener);
        fragment.show(activity.getSupportFragmentManager(), "CodeScannerBottomSheet");
    }
}
