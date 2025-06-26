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
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.google.android.gms.common.util.BiConsumer
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CodeScannerBottomSheetFragment : BottomSheetDialogFragment() {
    private var dismissListener: OnDismissListener? = null
    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var isCameraStarted = false
    private var isFlashOn = false
    private var cameraExecutor: ExecutorService? = null
    private var barcodeScanner: BarcodeScanner? = null
    private var notifyListenersFunction: BiConsumer<String, JSObject>? = null
    private var isMulti = false
    private var codeTypes: JSArray = JSArray.from(arrayOf("qr", "code39", "ean13"))
    private var detectionX: Int? = null
    private var detectionY: Int? = null
    private var detectionWidth: Int? = null
    private var detectionHeight: Int? = null

    interface OnDismissListener {
        fun onDismiss()
    }

    fun setNotifyListenersFunction(notifyFunction: BiConsumer<String, JSObject>) {
        this.notifyListenersFunction = notifyFunction;
    }

    fun setOnDismissListener(listener: OnDismissListener?) {
        this.dismissListener = listener
    }

    fun setCallSettings(
        isMulti: Boolean,
        codeTypes: JSArray,
        detectionX: Float,
        detectionY: Float,
        detectionWidth: Float,
        detectionHeight: Float
    ) {
        this.isMulti = isMulti
        this.codeTypes = codeTypes
        this.detectionX = detectionX.toInt()
        this.detectionY = detectionY.toInt()
        this.detectionWidth = detectionWidth.toInt()
        this.detectionHeight = detectionHeight.toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Material 3のBottom sheetスタイルを設定
        setStyle(STYLE_NORMAL, R.style.Material3BottomSheetDialog)

        // カメラエグゼキュータの初期化
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun createBarcodeScannerOptions(): BarcodeScannerOptions {
        val formats = mutableListOf<Int>()
        
        for (i in 0 until codeTypes.length()) {
            val codeType = codeTypes.getString(i)
            when (codeType) {
                "aztec" -> formats.add(Barcode.FORMAT_AZTEC)
                "code128" -> formats.add(Barcode.FORMAT_CODE_128)
                "code39" -> formats.add(Barcode.FORMAT_CODE_39)
                "code39Mod43" -> formats.add(Barcode.FORMAT_CODE_39)
                "code93" -> formats.add(Barcode.FORMAT_CODE_93)
                "dataMatrix" -> formats.add(Barcode.FORMAT_DATA_MATRIX)
                "ean13" -> formats.add(Barcode.FORMAT_EAN_13)
                "ean8" -> formats.add(Barcode.FORMAT_EAN_8)
                "interleaved2of5" -> formats.add(Barcode.FORMAT_ITF)
                "itf14" -> formats.add(Barcode.FORMAT_ITF)
                "pdf417" -> formats.add(Barcode.FORMAT_PDF417)
                "qr" -> formats.add(Barcode.FORMAT_QR_CODE)
                "upce" -> formats.add(Barcode.FORMAT_UPC_E)
                else -> Log.w(TAG, "未対応のバーコードタイプ: $codeType")
            }
        }
        
        // デフォルトのフォーマットを追加（何も指定されていない場合）
        if (formats.isEmpty()) {
            formats.addAll(listOf(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_EAN_13
            ))
        }
        
        val builder = BarcodeScannerOptions.Builder()
        if (formats.isNotEmpty()) {
            val firstFormat = formats[0]
            val remainingFormats = formats.drop(1).toIntArray()
            builder.setBarcodeFormats(firstFormat, *remainingFormats)
        }
        return builder.build()
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

        // MLKitバーコードスキャナーの初期化
        val options = createBarcodeScannerOptions()
        barcodeScanner = BarcodeScanning.getClient(options)

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
                camera = cameraProvider?.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                isCameraStarted = true

                if (!this.isFlashOn()) {
                    this.turnOnFlash()
                }
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
                            notifyListeners("CodeScannerCatchEvent", JSObject().put("code", rawValue))
                            Log.d(
                                TAG,
                                "バーコード検出: " + rawValue + " (形式: " + barcode.format + ")"
                            )
                            if (!isMulti) {
                                if (dismissListener != null) {
                                    dismissListener!!.onDismiss()
                                }
                                dismiss()
                            }
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
        if (this.isFlashOn()) {
            this.turnOffFlash()
        }
    }

    /**
     * フラッシュライトをオンにする
     */
    fun turnOnFlash() {
        if (camera != null && camera!!.cameraInfo.hasFlashUnit()) {
            val future: ListenableFuture<Void> = camera!!.cameraControl.enableTorch(true)
            future.addListener({
                try {
                    future.get()
                    isFlashOn = true
                    Log.d(TAG, "フラッシュライトがオンになりました")
                } catch (e: Exception) {
                    Log.e(TAG, "フラッシュライトのオンに失敗: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(requireContext()))
        } else {
            Log.w(TAG, "フラッシュライトが利用できません")
        }
    }

    /**
     * フラッシュライトをオフにする
     */
    fun turnOffFlash() {
        if (camera != null && camera!!.cameraInfo.hasFlashUnit()) {
            val future: ListenableFuture<Void> = camera!!.cameraControl.enableTorch(false)
            future.addListener({
                try {
                    future.get()
                    isFlashOn = false
                    Log.d(TAG, "フラッシュライトがオフになりました")
                } catch (e: Exception) {
                    Log.e(TAG, "フラッシュライトのオフに失敗: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(requireContext()))
        } else {
            Log.w(TAG, "フラッシュライトが利用できません")
        }
    }

    /**
     * フラッシュライトがオンかどうかを取得
     */
    private fun isFlashOn(): Boolean {
        return isFlashOn
    }

    protected fun notifyListeners(eventName: String, data: JSObject) {
        notifyListenersFunction?.accept(eventName, data)
    }

    companion object {
        private const val TAG = "CodeScanner"
        fun newInstance(): CodeScannerBottomSheetFragment {
            return CodeScannerBottomSheetFragment()
        }
    }
}