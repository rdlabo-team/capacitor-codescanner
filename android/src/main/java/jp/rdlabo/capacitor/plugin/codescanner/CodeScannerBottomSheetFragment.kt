package jp.rdlabo.capacitor.plugin.codescanner

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CodeScannerBottomSheetFragment : BottomSheetDialogFragment() {
    private var dismissListener: OnDismissListener? = null
    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isCameraStarted = false
    private var cameraExecutor: ExecutorService? = null
    private var barcodeScanner: BarcodeScanner? = null

    interface OnDismissListener {
        fun onDismiss()
    }

    fun setOnDismissListener(listener: OnDismissListener?) {
        this.dismissListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Material 3のBottom sheetスタイルを設定
        setStyle(STYLE_NORMAL, R.style.Material3BottomSheetDialog)


        // MLKitバーコードスキャナーの初期化
        val options = BarcodeScannerOptions.Builder()
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
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)


        // カメラエグゼキュータの初期化
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_code_scanner_bottom_sheet, container, false)

        val closeButton = view.findViewById<ImageButton>(R.id.close_button)
        closeButton.setOnClickListener { v: View? ->
            if (dismissListener != null) {
                dismissListener!!.onDismiss()
            }
            dismiss()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // モーダルが表示されたら自動的にカメラを開始
        if (!isCameraStarted) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        // モーダルが一時停止したらカメラを停止
        stopCamera()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ビューが破棄されたらカメラを停止
        stopCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        // リソースの解放
        if (barcodeScanner != null) {
            barcodeScanner!!.close()
        }
        if (cameraExecutor != null) {
            cameraExecutor!!.shutdown()
        }
    }

    private fun startCamera() {
        // カメラ権限をチェック
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "カメラ権限が必要です", Toast.LENGTH_SHORT).show()
            return
        }

        val cameraPreviewContainer = view!!.findViewById<FrameLayout>(R.id.camera_preview) ?: return

        previewView = PreviewView(requireContext())
        cameraPreviewContainer.removeAllViews()
        cameraPreviewContainer.addView(previewView)

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val preview =
                    Preview.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA


                // ImageAnalysisを追加してバーコード検出
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor!!, BarcodeAnalyzer())

                preview.surfaceProvider = previewView!!.surfaceProvider
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                isCameraStarted = true
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "カメラの起動に失敗しました", Toast.LENGTH_SHORT)
                    .show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun stopCamera() {
        if (cameraProvider != null) {
            cameraProvider!!.unbindAll()
            cameraProvider = null
        }
        isCameraStarted = false
    }

    // バーコード検出用のImageAnalysis.Analyzer
    @OptIn(ExperimentalGetImage::class)
    private inner class BarcodeAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            val image = InputImage.fromMediaImage(
                imageProxy.image!!,
                imageProxy.imageInfo.rotationDegrees
            )

            barcodeScanner!!.process(image)
                .addOnSuccessListener { barcodes: List<Barcode> ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (rawValue != null) {
                            Log.d(
                                TAG,
                                "バーコード検出: " + rawValue + " (形式: " + barcode.format + ")"
                            )
                        }
                    }
                }
                .addOnFailureListener { e: Exception ->
                    Log.e(
                        TAG,
                        "バーコード検出エラー: " + e.message
                    )
                }
                .addOnCompleteListener { task: Task<List<Barcode?>?>? ->
                    imageProxy.close()
                }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        stopCamera()
        if (dismissListener != null) {
            dismissListener!!.onDismiss()
        }
    }

    companion object {
        private const val TAG = "CodeScanner"
        fun newInstance(): CodeScannerBottomSheetFragment {
            return CodeScannerBottomSheetFragment()
        }
    }
}