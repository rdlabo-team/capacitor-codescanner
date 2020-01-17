import { PluginListenerHandle } from '@capacitor/core';
declare module "@capacitor/core" {
  interface PluginRegistry {
    CodeScanner: CodeScannerPlugin;
  }
}

export interface CodeScannerPlugin {
  present(): Promise<{value: boolean}>;
  addListener(eventName: 'CodeScannerCatchEvent', listenerFunc: (info: any) => {
    code: string;
  }): PluginListenerHandle;
}

export interface ScannerOption {
  detectionX?: number;
  detectionY?: number;
  detectionWidth?: number;
  detectionHeight?: number;
  // metadataObjectTypes: Record<
  //   'aztec' | 'code128' | 'code39' | 'code39Mod43' | 'code93' | 'detaMatrix'
  //   | 'ean13' | 'ean8' | 'face' | 'interleaved2of5' | 'itf14' | 'pdf417'
  //   | 'qr' | 'upce' | 'catBody' | 'dogBody' | 'humanBody' | 'salientObject'
  //   , []>
}
