import { PluginListenerHandle } from '@capacitor/core';

export interface CodeScannerPlugin {
  present(scannerOption: ScannerOption): Promise<void>;
  addListener(eventName: 'CodeScannerCatchEvent', listenerFunc: (event: {
    code: string;
  }) => void): Promise<PluginListenerHandle>;
}

export interface ScannerOption {
  detectionX?: number;
  detectionY?: number;
  detectionWidth?: number;
  detectionHeight?: number;
  metadataObjectTypes?: Record<'aztec' | 'code128' | 'code39' | 'code39Mod43' | 'code93' | 'dataMatrix'
    | 'ean13' | 'ean8' | 'face' | 'interleaved2of5' | 'itf14' | 'pdf417'
    | 'qr' | 'upce' | 'catBody' | 'dogBody' | 'humanBody' | 'salientObject'
    , []>
}
