package jp.rdlabo.capacitor.plugin.codescanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.camera.core.ExperimentalGetImage;

public class CodeScannerBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String TAG = "CodeScanner";
    private OnDismissListener dismissListener;
    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    private boolean isCameraStarted = false;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;

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
        
        // MLKitバーコードスキャナーの初期化
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_PDF417,
                        Barcode.FORMAT_DATA_MATRIX,
                        Barcode.FORMAT_AZTEC
                )
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);
        
        // カメラエグゼキュータの初期化
        cameraExecutor = Executors.newSingleThreadExecutor();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        // リソースの解放
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
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
                
                // ImageAnalysisを追加してバーコード検出
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                
                imageAnalysis.setAnalyzer(cameraExecutor, new BarcodeAnalyzer());
                
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
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

    // バーコード検出用のImageAnalysis.Analyzer
    @OptIn(markerClass = ExperimentalGetImage.class)
    private class BarcodeAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            InputImage image = InputImage.fromMediaImage(
                    imageProxy.getImage(),
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            if (rawValue != null) {
                                Log.d(TAG, "バーコード検出: " + rawValue + " (形式: " + barcode.getFormat() + ")");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "バーコード検出エラー: " + e.getMessage());
                    })
                    .addOnCompleteListener(task -> {
                        imageProxy.close();
                    });
        }
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