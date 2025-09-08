import Foundation
import Capacitor
import AVFoundation

class CodeScannerViewController: UIViewController {
    weak var scannerPlugin: CodeScannerPlugin?
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        // ビューが非表示になった時にcloseCameraを呼び出し
        scannerPlugin?.closeCamera()
    }
}

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(CodeScannerPlugin)
public class CodeScannerPlugin: CAPPlugin, AVCaptureMetadataOutputObjectsDelegate, CAPBridgedPlugin {
    public let identifier = "CodeScannerPlugin"
    public let jsName = "CodeScanner"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "present", returnType: CAPPluginReturnPromise)
    ]
    // セッションのインスタンス生成
    let captureSession = AVCaptureSession()
    var videoLayer: AVCaptureVideoPreviewLayer?

    var previewViewCtrl: UIViewController!
    var detectionArea: UIView!
    var codeView: UIView!

    var isReady = false
    var isMulti = false
    var enableAutoLight = true
    var currentCall: CAPPluginCall?
    var isClosing = false

    @objc func present(_ call: CAPPluginCall) {
        self.currentCall = call
        self.isMulti = call.getBool("isMulti", false)
        self.enableAutoLight = call.getBool("enableAutoLight", true)
        let enableCloseButton = call.getBool("enableCloseButton", true)
        DispatchQueue.main.async {
            if let rootViewController = UIApplication.shared.keyWindow?.rootViewController {
                // 検出エリア設定
                let width: CGFloat = CGFloat(call.getFloat("detectionWidth") ?? 0.4)
                let height: CGFloat = CGFloat(call.getFloat("detectionHeight") ?? 1)
                
                // シートの高さ比率設定
                let sheetScreenRatio: CGFloat = CGFloat(call.getFloat("sheetScreenRatio") ?? 1.0)

                // 入力（背面カメラ）
                if !self.isReady {
                    let videoDevice = AVCaptureDevice.default(for: .video)
                    let videoInput = try! AVCaptureDeviceInput(device: videoDevice!)
                    self.captureSession.addInput(videoInput)

                    // 出力（メタデータ）
                    let metadataOutput = AVCaptureMetadataOutput()
                    self.captureSession.addOutput(metadataOutput)

                    // コードを検出した際のデリゲート設定
                    metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)

                    // コードの認識を設定
                    let ObjectTypes = call.getArray("CodeTypes", String.self) ?? ["qr", "code39", "ean13"]
                    var metadataObjectTypes = [AVMetadataObject.ObjectType]()

                    for type in ObjectTypes {
                        switch type {
                        case "aztec":
                            metadataObjectTypes.append(AVMetadataObject.ObjectType.aztec)
                        case "code128":
                            metadataObjectTypes.append(AVMetadataObject.ObjectType.code128)
                        case "code39":
                            metadataObjectTypes.append(AVMetadataObject.ObjectType.code39)
                        case "code39Mod43":
                            metadataObjectTypes.append(AVMetadataObject.ObjectType.code39Mod43)
                        case "dataMatrix":
                            metadataObjectTypes.append(AVMetadataObject.ObjectType.dataMatrix)
                        case "ean13":
                            metadataObjectTypes.append(AVMetadataObject.ObjectType.ean13)
                        case "ean8":
                            metadataObjectTypes.append(AVMetadataObject.ObjectType.ean8)
                        case "face":
                            metadataObjectTypes.append(AVMetadataObject.ObjectType.face)
                        case "interleaved2of5":
                            metadataObjectTypes.append(AVMetadataObject.ObjectType.interleaved2of5)
                        case "itf14":
                            metadataObjectTypes.append(AVMetadataObject.ObjectType.itf14)
                        case "pdf417":
                            metadataObjectTypes.append(AVMetadataObject.ObjectType.pdf417)
                        case "qr":
                            metadataObjectTypes.append(AVMetadataObject.ObjectType.qr)
                        case "upce":
                            metadataObjectTypes.append(AVMetadataObject.ObjectType.upce)
                        case "catBody":
                            if #available(iOS 13.0, *) {
                                metadataObjectTypes.append(AVMetadataObject.ObjectType.catBody)
                            }
                        case "dogBody":
                            if #available(iOS 13.0, *) {
                                metadataObjectTypes.append(AVMetadataObject.ObjectType.dogBody)
                            }
                        case "humanBody":
                            if #available(iOS 13.0, *) {
                                metadataObjectTypes.append(AVMetadataObject.ObjectType.humanBody)
                            }
                        case "salientObject":
                            if #available(iOS 13.0, *) {
                                metadataObjectTypes.append(AVMetadataObject.ObjectType.salientObject)
                            }
                        default:
                            print(type)
                        }
                    }

                    metadataOutput.metadataObjectTypes = metadataObjectTypes
                    // 中央配置に基づいてrectOfInterestを計算（heightは幅に対する割合）
                    let centerX = 0.5 - width / 2
                    let aspectRatio = height // 幅に対する高さの割合
                    let centerY = 0.5 - (width * aspectRatio) / 2
                    metadataOutput.rectOfInterest = CGRect(x: centerY, y: centerX, width: width * aspectRatio, height: width)
                    self.isReady = true
                }

                // プレビュー表示
                let scannerViewController = CodeScannerViewController()
                scannerViewController.scannerPlugin = self
                self.previewViewCtrl = scannerViewController
                self.previewViewCtrl.modalPresentationStyle = .pageSheet
                
                // シートの背景を暗くする
                if #available(iOS 15.0, *) {
                    self.previewViewCtrl.sheetPresentationController?.prefersScrollingExpandsWhenScrolledToEdge = false
                    self.previewViewCtrl.sheetPresentationController?.prefersEdgeAttachedInCompactHeight = true
                }
                
                // シートの高さ比率を設定
                if #available(iOS 15.0, *) {
                    let screenHeight = UIScreen.main.bounds.height
                    let sheetHeight = screenHeight * sheetScreenRatio
                    if #available(iOS 16.0, *) {
                        self.previewViewCtrl.sheetPresentationController?.detents = [
                            UISheetPresentationController.Detent.custom { _ in
                                return sheetHeight
                            }
                        ]
                    } else {
                        // Fallback on earlier versions
                        if sheetScreenRatio <= 0.5 {
                            self.previewViewCtrl.sheetPresentationController?.detents = [.medium()]
                        } else if sheetScreenRatio <= 0.7 {
                            self.previewViewCtrl.sheetPresentationController?.detents = [.large()]
                        } else {
                            self.previewViewCtrl.sheetPresentationController?.detents = [.large()]
                        }
                    }
                    // ハンドルを表示
                    self.previewViewCtrl.sheetPresentationController?.prefersGrabberVisible = true
                }

                self.previewViewCtrl.view.backgroundColor = .black
                self.previewViewCtrl.view.frame = rootViewController.view.bounds
                self.previewViewCtrl.view.tag = 325973259 // rand
                self.bridge?.viewController?.present(self.previewViewCtrl, animated: true, completion: nil)

                self.videoLayer = AVCaptureVideoPreviewLayer.init(session: self.captureSession)
                self.videoLayer?.frame = rootViewController.view.bounds
                self.videoLayer?.videoGravity = AVLayerVideoGravity.resizeAspectFill
                self.previewViewCtrl.view.layer.addSublayer(self.videoLayer!)

                self.detectionArea = UIView()
                // SafeAreaとハンドル部分を考慮した有効エリアで検出エリアを中央配置で計算
                let safeAreaInsets = rootViewController.view.safeAreaInsets
                let handleHeight: CGFloat = 20 // ハンドル部分の高さ
                let availableWidth = rootViewController.view.frame.size.width - safeAreaInsets.left - safeAreaInsets.right
                let availableHeight = (UIScreen.main.bounds.height * sheetScreenRatio) - safeAreaInsets.top - safeAreaInsets.bottom - handleHeight
                
                let detectionWidth = availableWidth * width
                let detectionHeight = detectionWidth * height // 幅に対する割合に変更
                let x = safeAreaInsets.left + (availableWidth - detectionWidth) / 2
                let y = safeAreaInsets.top + handleHeight + (availableHeight - detectionHeight) / 2
                self.detectionArea.frame = CGRect(x: x, y: y, width: detectionWidth, height: detectionHeight)
                self.detectionArea.layer.cornerRadius = 8
                self.previewViewCtrl.view.addSubview(self.detectionArea)
                
                // 検出エリア以外を暗くするオーバーレイを作成
                let overlayView = UIView(frame: self.previewViewCtrl.view.bounds)
                overlayView.backgroundColor = UIColor.black.withAlphaComponent(0.2)
                overlayView.tag = 888 // 識別用のタグ
                
                // 検出エリア部分を透明にするマスクを作成（SafeAreaを考慮）
                let maskLayer = CAShapeLayer()
                let path = UIBezierPath(rect: overlayView.bounds)
                let detectionRect = CGRect(x: x, y: y, width: detectionWidth, height: detectionHeight)
                let transparentPath = UIBezierPath(roundedRect: detectionRect, cornerRadius: 8)
                path.append(transparentPath.reversing())
                maskLayer.path = path.cgPath
                maskLayer.fillRule = .evenOdd
                overlayView.layer.mask = maskLayer
                
                self.previewViewCtrl.view.addSubview(overlayView)

                // 検出ビュー
                self.codeView = UIView()
                self.codeView.layer.borderWidth = 4
                self.codeView.layer.borderColor = UIColor.red.cgColor
                self.codeView.frame = CGRect(x: 0, y: 0, width: 0, height: 0)

                self.previewViewCtrl.view.addSubview(self.codeView)

                // 閉じるボタン（enableCloseButtonがtrueの場合のみ表示）
                if enableCloseButton {
                    let btnClose = UIButton()
                    if #available(iOS 13.0, *) {
                        let closeImage = UIImage(systemName: "xmark")
                        btnClose.setImage(closeImage, for: .normal)
                        btnClose.tintColor = .white
                    } else {
                        btnClose.setTitle("✕", for: .normal)
                        btnClose.setTitleColor(.white, for: .normal)
                    }
                    btnClose.frame = CGRect(x: 20, y: 30, width: 44, height: 44)

                    self.previewViewCtrl.view.addSubview(btnClose)

                    let closeGesture = UITapGestureRecognizer(target: self, action: #selector(self.closeGesture))
                    btnClose.addGestureRecognizer(closeGesture)
                }

                DispatchQueue.global(qos: .userInitiated).async {
                    if !self.captureSession.isRunning {
                        self.captureSession.startRunning()
                        if self.enableAutoLight {
                            self.toggleLight(launch: true)
                        }
                    }
                }
            }
        }
    }

    @objc func closeGesture(sender: UITapGestureRecognizer) {
        self.closeCamera()
    }

    public func closeCamera() {
        print("closeCamera")
        // 重複実行を防ぐ
        guard !self.isClosing else { return }
        self.isClosing = true
        
        if self.enableAutoLight {
            self.toggleLight(launch: false)
        }
        DispatchQueue.main.async {
            if let rootViewController = UIApplication.shared.keyWindow?.rootViewController {
                if self.captureSession.isRunning {
                    self.captureSession.stopRunning()
                }
                // オーバーレイを削除
                if let overlayView = self.previewViewCtrl?.view.viewWithTag(888) {
                    overlayView.removeFromSuperview()
                }
                rootViewController.dismiss(animated: true, completion: {
                    // シートが閉じられた後にresolveを実行
                    if let call = self.currentCall {
                        call.resolve()
                        self.currentCall = nil
                    }
                    self.isClosing = false
                })
            } else {
                self.isClosing = false
            }
        }
    }

    public func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        for metadata in metadataObjects as! [AVMetadataMachineReadableCodeObject] {
            // コード内容の確認
            //            if Set(arrayLiteral: AVMetadataObject.ObjectType.qr, AVMetadataObject.ObjectType.code39, AVMetadataObject.ObjectType.ean13).contains(metadata.type) {
            if metadata.stringValue != nil {
                // 検出位置を取得
                let barCode = self.videoLayer?.transformedMetadataObject(for: metadata) as! AVMetadataMachineReadableCodeObject
                if barCode.bounds.height != 0 {
                    AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
                    self.codeView!.frame = barCode.bounds
                    self.notifyListeners("CodeScannerCatchEvent", data: [
                        "code": metadata.stringValue!
                    ])

                    if !self.isMulti {
                        self.closeCamera()
                    }
                }
            }
        }
    }

    public func toggleLight(launch: Bool) {
        DispatchQueue.main.async {
            let avCaptureDevice = AVCaptureDevice.default(for: AVMediaType.video)
            if avCaptureDevice!.hasTorch, avCaptureDevice!.isTorchAvailable {
                do {
                    try avCaptureDevice!.lockForConfiguration()

                    if launch {
                        print("light launch")
                        avCaptureDevice!.torchMode = .on
                    } else {
                        print("light dissmiss")
                        avCaptureDevice!.torchMode = .off
                    }
                    avCaptureDevice!.unlockForConfiguration()
                } catch let error {
                    print(error)
                }
            }
        }
    }
}
