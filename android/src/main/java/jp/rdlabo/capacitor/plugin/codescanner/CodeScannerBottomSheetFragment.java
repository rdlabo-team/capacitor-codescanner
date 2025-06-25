package jp.rdlabo.capacitor.plugin.codescanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.common.util.concurrent.ListenableFuture;

public class CodeScannerBottomSheetFragment extends BottomSheetDialogFragment {

    private OnDismissListener dismissListener;
    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    private boolean isCameraStarted = false;

    public interface OnDismissListener {
        void onDismiss();
    }

    public static CodeScannerBottomSheetFragment newInstance() {
        return new CodeScannerBottomSheetFragment();
    }

    public void setOnDismissListener(OnDismissListener listener) {
        this.dismissListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Material 3のBottom sheetスタイルを設定
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.Material3BottomSheetDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_code_scanner_bottom_sheet, container, false);
        
        ImageButton closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> {
            if (dismissListener != null) {
                dismissListener.onDismiss();
            }
            dismiss();
        });
        
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // モーダルが表示されたら自動的にカメラを開始
        if (!isCameraStarted) {
            startCamera();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // モーダルが一時停止したらカメラを停止
        stopCamera();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // ビューが破棄されたらカメラを停止
        stopCamera();
    }

    private void startCamera() {
        // カメラ権限をチェック
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "カメラ権限が必要です", Toast.LENGTH_SHORT).show();
            return;
        }

        FrameLayout cameraPreviewContainer = getView().findViewById(R.id.camera_preview);
        if (cameraPreviewContainer == null) return;

        previewView = new PreviewView(requireContext());
        cameraPreviewContainer.removeAllViews();
        cameraPreviewContainer.addView(previewView);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview);
                isCameraStarted = true;
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "カメラの起動に失敗しました", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
        isCameraStarted = false;
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        stopCamera();
        if (dismissListener != null) {
            dismissListener.onDismiss();
        }
    }
} 