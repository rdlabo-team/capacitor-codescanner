import Foundation
import Capacitor
import AVFoundation

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(CodeScannerPlugin)
public class CodeScannerPlugin: CAPPlugin, AVCaptureMetadataOutputObjectsDelegate, CAPBridgedPlugin {
    public let identifier = "CodeScannerPlugin" 
    public let jsName = "CodeScanner" 
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "present", returnType: CAPPluginReturnPromise),
    ] 
   // セッションのインスタンス生成
    let captureSession = AVCaptureSession()
    var videoLayer: AVCaptureVideoPreviewLayer?

    var previewViewCtrl: UIViewController!
    var detectionArea: UIView!
    var codeView: UIView!

    var isReady = false
    var isMulti = false

    @objc func present(_ call: CAPPluginCall) {
        self.isMulti = call.getBool("isMulti", false)
        DispatchQueue.main.async {
            if let rootViewController = UIApplication.shared.keyWindow?.rootViewController {
                // 検出エリア設定
                let x: CGFloat = CGFloat(call.getFloat("detectionX") ?? 0.2)
                let y: CGFloat = CGFloat(call.getFloat("detectionY") ?? 0.35)
                let width: CGFloat = CGFloat(call.getFloat("detectionWidth") ?? 0.6)
                let height: CGFloat = CGFloat(call.getFloat("detectionHeight") ?? 0.15)

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
                    metadataOutput.rectOfInterest = CGRect(x: y,y: 1-x-width,width: height,height: width)
                    self.isReady = true
                }

                // プレビュー表示
                self.previewViewCtrl = UIViewController()
                self.previewViewCtrl.modalPresentationStyle = .pageSheet

                self.previewViewCtrl.view.backgroundColor = .black
                self.previewViewCtrl.view.frame = rootViewController.view.bounds
                self.previewViewCtrl.view.tag = 325973259 // rand
                self.bridge?.viewController?.present(self.previewViewCtrl, animated: true, completion: nil)

                self.videoLayer = AVCaptureVideoPreviewLayer.init(session: self.captureSession)
                self.videoLayer?.frame = rootViewController.view.bounds
                self.videoLayer?.videoGravity = AVLayerVideoGravity.resizeAspectFill
                self.previewViewCtrl.view.layer.addSublayer(self.videoLayer!)

                self.detectionArea = UIView()
                self.detectionArea.frame = CGRect(x: rootViewController.view.frame.size.width * x, y: rootViewController.view.frame.size.height * y, width: rootViewController.view.frame.size.width * width, height: rootViewController.view.frame.size.height * height)
                self.detectionArea.layer.borderColor = UIColor.red.cgColor
                self.detectionArea.layer.borderWidth = 3
                self.previewViewCtrl.view.addSubview(self.detectionArea)

                // 検出ビュー
                self.codeView = UIView()
                self.codeView.layer.borderWidth = 4
                self.codeView.layer.borderColor = UIColor.red.cgColor
                self.codeView.frame = CGRect(x: 0, y: 0, width: 0, height: 0)

                self.previewViewCtrl.view.addSubview(self.codeView)

                // 閉じるボタン
                let btnClose = UIButton()
                btnClose.titleLabel?.textAlignment = .center
                btnClose.setTitle("✕", for: .normal)
                btnClose.setTitleColor(.white, for: .normal)
                btnClose.frame = CGRect(x: 20, y: 30, width: 30, height: 30)
                btnClose.layer.cornerRadius = btnClose.bounds.midY
                btnClose.backgroundColor = .black

                self.previewViewCtrl.view.addSubview(btnClose)

                let closeGesture = UITapGestureRecognizer(target: self, action: #selector(self.closeGesture))
                btnClose.addGestureRecognizer(closeGesture)


                DispatchQueue.global(qos: .userInitiated).async {
                    if !self.captureSession.isRunning {
                        self.captureSession.startRunning()
                        self.toggleLight(launch: true)
                    }
                }

                call.resolve()
            }
        }
    }


    @objc func closeGesture(sender:UITapGestureRecognizer) {
        self.closeCamera()
    }

    public func closeCamera() {
        self.toggleLight(launch: false)
        DispatchQueue.main.async {
            if let rootViewController = UIApplication.shared.keyWindow?.rootViewController {
                if self.captureSession.isRunning {
                    self.captureSession.stopRunning()
                }
                rootViewController.dismiss(animated: true, completion: nil)
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

                if (launch) {
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
