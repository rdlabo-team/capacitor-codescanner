import Foundation
import Capacitor
import AVFoundation

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitor.ionicframework.com/docs/plugins/ios
 */
@objc(CodeScannerPlugin)
public class CodeScannerPlugin: CAPPlugin, AVCaptureMetadataOutputObjectsDelegate {
    
    // セッションのインスタンス生成
    let captureSession = AVCaptureSession()
    var videoLayer: AVCaptureVideoPreviewLayer?
    
    
    var previewView: UIView!
    var detectionArea: UIView!
    var codeView: UIView!
    var code: String!
    
    var isReady = false
    
    @objc func present(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            if let rootViewController = UIApplication.shared.keyWindow?.rootViewController {
                // 検出エリア設定
                let x: CGFloat = 0.05
                let y: CGFloat = 0.3
                let width: CGFloat = 0.9
                let height: CGFloat = 0.15
                
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
                    metadataOutput.metadataObjectTypes = [
                        AVMetadataObject.ObjectType.qr,
                        AVMetadataObject.ObjectType.code39,
                        AVMetadataObject.ObjectType.ean13,
                    ]
                    metadataOutput.metadataObjectTypes = metadataOutput.availableMetadataObjectTypes
                    metadataOutput.rectOfInterest = CGRect(x: y,y: 1-x-width,width: height,height: width)
                    self.isReady = true
                }
                
                // プレビュー表示
                self.previewView = UIView()
                self.previewView.frame = rootViewController.view.bounds
                self.previewView.tag = 325973259 // rand
                rootViewController.view.addSubview(self.previewView)
                
                self.videoLayer = AVCaptureVideoPreviewLayer.init(session: self.captureSession)
                self.videoLayer?.frame = rootViewController.view.bounds
                self.videoLayer?.videoGravity = AVLayerVideoGravity.resizeAspectFill
                self.previewView.layer.addSublayer(self.videoLayer!)
                
                self.detectionArea = UIView()
                self.detectionArea.frame = CGRect(x: rootViewController.view.frame.size.width * x, y: rootViewController.view.frame.size.height * y, width: rootViewController.view.frame.size.width * width, height: rootViewController.view.frame.size.height * height)
                self.detectionArea.layer.borderColor = UIColor.red.cgColor
                self.detectionArea.layer.borderWidth = 3
                self.previewView.addSubview(self.detectionArea)
                
                // 検出ビュー
                self.codeView = UIView()
                self.codeView.layer.borderWidth = 4
                self.codeView.layer.borderColor = UIColor.red.cgColor
                self.codeView.frame = CGRect(x: 0, y: 0, width: 0, height: 0)
                
//                let tapGesture = UITapGestureRecognizer(target: self, action: #selector(self.tapGesture))
//                self.codeView.addGestureRecognizer(tapGesture)
                
                self.previewView.addSubview(self.codeView)
                
                // 閉じるボタン
                let btnClose = UIButton()
                btnClose.titleLabel?.textAlignment = .center
                btnClose.setTitle("✕", for: .normal)
                btnClose.setTitleColor(.white, for: .normal)
                btnClose.frame = CGRect(x: 20, y: 30, width: 30, height: 30)
                btnClose.layer.cornerRadius = btnClose.bounds.midY
                btnClose.backgroundColor = .black
                
                btnClose.tag = 327985328732 // rand
                self.previewView.addSubview(btnClose)
                
                let closeGesture = UITapGestureRecognizer(target: self, action: #selector(self.closeGesture))
                btnClose.addGestureRecognizer(closeGesture)
                
                
                DispatchQueue.global(qos: .userInitiated).async {
                    if !self.captureSession.isRunning {
                        self.captureSession.startRunning()
                    }
                }
                
                call.success([
                    "value": true
                ])
            }
        }
    }
    
//    @objc func tapGesture(sender:UITapGestureRecognizer) {
//        NSLog("CAP: TAP" +  self.code)
//    }
    
    @objc func closeGesture(sender:UITapGestureRecognizer) {
        DispatchQueue.main.async {
            if let rootViewController = UIApplication.shared.keyWindow?.rootViewController {
                if self.captureSession.isRunning {
                    self.captureSession.stopRunning()
                }
                if let previewView = rootViewController.view.viewWithTag(325973259) {
                    previewView.removeFromSuperview()
                }
            }
        }
    }
    
    
    public func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        // 複数のメタデータを検出できる
        for metadata in metadataObjects as! [AVMetadataMachineReadableCodeObject] {
            // コード内容の確認
            //            if Set(arrayLiteral: AVMetadataObject.ObjectType.qr, AVMetadataObject.ObjectType.code39, AVMetadataObject.ObjectType.ean13).contains(metadata.type) {
            if metadata.stringValue != nil {
                // 検出位置を取得
                let barCode = self.videoLayer?.transformedMetadataObject(for: metadata) as! AVMetadataMachineReadableCodeObject
                if barCode.bounds.height != 0 && self.code != metadata.stringValue {
                    AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
                    self.codeView!.frame = barCode.bounds
                    self.code = metadata.stringValue!
                    self.notifyListeners("CodeScannerCaptchaEvent", data: [
                        code: metadata.stringValue!
                    ])
                }
            }
        }
    }
}
