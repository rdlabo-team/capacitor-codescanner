package jp.rdlabo.capacitor.plugin.codescanner

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import android.widget.RelativeLayout

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
    private var enableAutoLight = true
    private var enableCloseButton = true
    private var codeTypes: JSArray = JSArray.from(arrayOf("qr", "code39", "ean13"))
    private var detectionWidth: Float = 0.4f
    private var detectionHeight: Float = 1f
    
    // UI elements for detection area and code view
    private var detectionAreaView: View? = null
    private var codeView: View? = null
    private var darkOverlayView: DarkOverlayView? = null
    private var cameraPreviewContainer: FrameLayout? = null
    
    // 画像サイズを保存
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

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
        enableAutoLight: Boolean,
        enableCloseButton: Boolean,
        codeTypes: JSArray,
        detectionWidth: Float,
        detectionHeight: Float
    ) {
        this.isMulti = isMulti
        this.enableAutoLight = enableAutoLight
        this.enableCloseButton = enableCloseButton
        this.codeTypes = codeTypes
        this.detectionWidth = detectionWidth
        this.detectionHeight = detectionHeight
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
        if (enableCloseButton) {
            closeButton.visibility = View.VISIBLE
            closeButton.setOnClickListener { v: View? ->
                if (dismissListener != null) {
                    dismissListener!!.onDismiss()
                }
                dismiss()
            }
        } else {
            closeButton.visibility = View.GONE
        }

        // 検出範囲とコードビューの参照を取得
        detectionAreaView = view.findViewById(R.id.detection_area)
        codeView = view.findViewById(R.id.code_view)
        darkOverlayView = view.findViewById(R.id.dark_overlay)
        cameraPreviewContainer = view.findViewById(R.id.camera_preview)

        return view
    }

    override fun onStart() {
        super.onStart()
        
        // BottomSheetBehaviorを使用して高さを設定
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let { sheet ->
            val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
            
            // 段階的な高さ設定を無効化し、デフォルトの高さを使用
            behavior.isDraggable = true
            behavior.isHideable = true
            behavior.skipCollapsed = true // 段階的な高さ変更を無効化
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            
            // ドラッグ時の動作を制御
            behavior.addBottomSheetCallback(object : com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_DRAGGING -> {
                            // ドラッグ中は何もしない
                        }
                        com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_SETTLING -> {
                            // 設定中は何もしない
                        }
                        com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN -> {
                            // 完全に隠れた場合は閉じる
                            dismiss()
                        }
                        com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED -> {
                            // 折りたたまれた場合は元の高さに戻す
                            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                        }
                    }
                }
                
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // スライド中は何もしない
                }
            })
        }
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

        val cameraPreviewContainer = requireView().findViewById<FrameLayout>(R.id.camera_preview) ?: return

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

                // 検出範囲の枠線を設定
                setupDetectionArea()

                if (enableAutoLight && !this.isFlashOn()) {
                    this.turnOnFlash()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "カメラの起動に失敗しました", Toast.LENGTH_SHORT)
                    .show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun setupDetectionArea() {
        // 検出範囲の枠線を表示
        detectionAreaView?.let { view ->
            // ビューのサイズが確定するまで待つ
            view.post {
                val parent = view.parent as? ViewGroup ?: return@post
                
                // カメラプレビューコンテナの実際のサイズを使用
                val container = cameraPreviewContainer ?: return@post
                val previewWidth = container.width
                val previewHeight = container.height
                
                if (previewWidth == 0 || previewHeight == 0) {
                    Log.w(TAG, "プレビュービューのサイズが0です")
                    return@post
                }
                
                // 検出範囲の計算は、シートの高さではなく実際のプレビューサイズを基準にする
                // これにより、sheetScreenRatioの二重計算を回避
                val width = (previewWidth * detectionWidth).toInt()
                val height = (width * detectionHeight).toInt() // 幅に対する割合に変更
                val x = (previewWidth - width) / 2 // 中央配置
                val y = (previewHeight - height) / 2 // 中央配置
                
                val params = view.layoutParams as RelativeLayout.LayoutParams
                params.leftMargin = x
                params.topMargin = y
                params.width = width
                params.height = height
                view.layoutParams = params
                view.requestLayout()
                
                // オーバーレイの検出エリアを更新
                darkOverlayView?.setDetectionArea(x, y, x + width, y + height)
                
                Log.d(TAG, "検出範囲枠線設定: x=$x, y=$y, width=$width, height=$height")
                Log.d(TAG, "プレビューサイズ: width=$previewWidth, height=$previewHeight")
            }
        }
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
            
            // 画像サイズと回転を保存
            imageWidth = image.width
            imageHeight = image.height
            val rotation = imageProxy.imageInfo.rotationDegrees

            barcodeScanner!!.process(image)
                .addOnSuccessListener { barcodes: List<Barcode> ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (rawValue != null) {
                            Log.d(TAG, "バーコード検出: $rawValue (形式: ${barcode.format})")
                            
                            // 検出範囲内かどうかをチェック
                            val isInDetectionArea = isBarcodeInDetectionArea(barcode, rotation)
                            Log.d(TAG, "検出範囲チェック結果: $isInDetectionArea")
                            
                            if (isInDetectionArea) {
                                Log.d(TAG, "検出範囲内のバーコード: $rawValue")
                                // バイブレーション実行
                                vibrate()
                                // 検出されたバーコードの枠線を表示
                                showCodeDetectionFrame(barcode)
                                
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
                            } else {
                                Log.d(TAG, "検出範囲外のバーコード: $rawValue - 無視します")
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

    private fun isBarcodeInDetectionArea(barcode: Barcode, rotation: Int): Boolean {
        // バーコードの境界ボックスを取得
        val boundingBox = barcode.boundingBox ?: return false
        
        // カメラプレビューコンテナのサイズを取得
        val container = cameraPreviewContainer ?: return false
        
        // カメラプレビューコンテナの実際の表示サイズを取得
        val previewWidth = container.width
        val previewHeight = container.height
        
        if (previewWidth == 0 || previewHeight == 0) {
            Log.w(TAG, "プレビュービューのサイズが0です")
            return false
        }
        
        // 検出範囲の境界を計算（中央配置）
        // setupDetectionArea()と同じ計算ロジックを使用して一貫性を保つ
        val detectionWidthPx = (previewWidth * detectionWidth).toInt()
        val detectionHeightPx = (detectionWidthPx * detectionHeight).toInt() // 幅に対する割合
        val detectionLeft = (previewWidth - detectionWidthPx) / 2
        val detectionTop = (previewHeight - detectionHeightPx) / 2
        val detectionRight = detectionLeft + detectionWidthPx
        val detectionBottom = detectionTop + detectionHeightPx
        
        // MLKitの座標系からプレビュービューの座標系に変換
        if (imageWidth == 0 || imageHeight == 0) {
            Log.w(TAG, "画像サイズが0です")
            return false
        }
        
        // カメラの回転を考慮した座標変換（プレビュービュー内での相対座標）
        val barcodeCenterX: Int
        val barcodeCenterY: Int
        
        when (rotation) {
            0 -> {
                // 0度回転（通常の横向き）
                barcodeCenterX = (boundingBox.centerX() * previewWidth / imageWidth).toInt()
                barcodeCenterY = (boundingBox.centerY() * previewHeight / imageHeight).toInt()
            }
            90 -> {
                // 90度回転（縦向き）
                barcodeCenterX = ((imageHeight - boundingBox.centerY()) * previewWidth / imageHeight).toInt()
                barcodeCenterY = ((imageWidth - boundingBox.centerX()) * previewHeight / imageWidth).toInt()
                Log.d(TAG, "90度回転座標変換詳細:")
                Log.d(TAG, "  boundingBox.centerX()=${boundingBox.centerX()}, boundingBox.centerY()=${boundingBox.centerY()}")
                Log.d(TAG, "  imageWidth=$imageWidth, imageHeight=$imageHeight")
                Log.d(TAG, "  previewWidth=$previewWidth, previewHeight=$previewHeight")
                Log.d(TAG, "  X計算: (${imageHeight} - ${boundingBox.centerY()}) * ${previewWidth} / ${imageHeight} = ${(imageHeight - boundingBox.centerY()) * previewWidth / imageHeight}")
                Log.d(TAG, "  Y計算: (${imageWidth} - ${boundingBox.centerX()}) * ${previewHeight} / ${imageWidth} = ${(imageWidth - boundingBox.centerX()) * previewHeight / imageWidth}")
            }
            180 -> {
                // 180度回転
                barcodeCenterX = ((imageWidth - boundingBox.centerX()) * previewWidth / imageWidth).toInt()
                barcodeCenterY = ((imageHeight - boundingBox.centerY()) * previewHeight / imageHeight).toInt()
            }
            270 -> {
                // 270度回転
                barcodeCenterX = ((imageHeight - boundingBox.centerY()) * previewWidth / imageHeight).toInt()
                barcodeCenterY = ((imageWidth - boundingBox.centerX()) * previewHeight / imageWidth).toInt()
            }
            else -> {
                // その他の回転角度
                barcodeCenterX = (boundingBox.centerX() * previewWidth / imageWidth).toInt()
                barcodeCenterY = (boundingBox.centerY() * previewHeight / imageHeight).toInt()
            }
        }
        
        // デバッグ情報を出力
        Log.d(TAG, "プレビューサイズ: width=$previewWidth, height=$previewHeight")
        Log.d(TAG, "画像サイズ: width=$imageWidth, height=$imageHeight")
        Log.d(TAG, "回転角度: $rotation")
        Log.d(TAG, "検出範囲: left=$detectionLeft, top=$detectionTop, right=$detectionRight, bottom=$detectionBottom")
        Log.d(TAG, "バーコード中心(変換前): x=${boundingBox.centerX()}, y=${boundingBox.centerY()}")
        Log.d(TAG, "バーコード中心(変換後): x=$barcodeCenterX, y=$barcodeCenterY")
        Log.d(TAG, "バーコード境界: left=${boundingBox.left}, top=${boundingBox.top}, right=${boundingBox.right}, bottom=${boundingBox.bottom}")
        
        val isInRange = barcodeCenterX >= detectionLeft && 
               barcodeCenterX <= detectionRight && 
               barcodeCenterY >= detectionTop && 
               barcodeCenterY <= detectionBottom
        
        Log.d(TAG, "検出範囲内: $isInRange")
        Log.d(TAG, "座標チェック詳細:")
        Log.d(TAG, "  X座標: $barcodeCenterX >= $detectionLeft && $barcodeCenterX <= $detectionRight = ${barcodeCenterX >= detectionLeft && barcodeCenterX <= detectionRight}")
        Log.d(TAG, "  Y座標: $barcodeCenterY >= $detectionTop && $barcodeCenterY <= $detectionBottom = ${barcodeCenterY >= detectionTop && barcodeCenterY <= detectionBottom}")
        
        return isInRange
    }

    private fun showCodeDetectionFrame(barcode: Barcode) {
        // コード検出時の枠線は非表示にする
        codeView?.visibility = View.GONE
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        stopCamera()
        if (dismissListener != null) {
            dismissListener!!.onDismiss()
        }
        if (enableAutoLight && this.isFlashOn()) {
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

    /**
     * バイブレーション機能を実行
     */
    private fun vibrate() {
        try {
            val vibrator: Vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val vibrationEffect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator.vibrate(vibrationEffect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
                Log.d(TAG, "バイブレーション実行")
            } else {
                Log.w(TAG, "バイブレーション機能が利用できません")
            }
        } catch (e: Exception) {
            Log.e(TAG, "バイブレーション実行エラー: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "CodeScanner"
        fun newInstance(): CodeScannerBottomSheetFragment {
            return CodeScannerBottomSheetFragment()
        }
    }
}