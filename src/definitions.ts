import type { PluginListenerHandle } from '@capacitor/core';

export interface CodeScannerPlugin {
  present(scannerOption: ScannerOption): Promise<void>;
  addListener(
    eventName: 'CodeScannerCatchEvent',
    listenerFunc: (event: { code: string }) => void,
  ): Promise<PluginListenerHandle>;
}

export type MetadataObjectTypes =
  | 'aztec'
  | 'code128'
  | 'code39'
  | 'code39Mod43'
  | 'code93'
  | 'dataMatrix'
  | 'ean13'
  | 'ean8'
  | 'face'
  | 'interleaved2of5'
  | 'itf14'
  | 'pdf417'
  | 'qr'
  | 'upce'
  | 'catBody'
  | 'dogBody'
  | 'humanBody'
  | 'salientObject';

export interface ScannerOption {
  detectionWidth?: number;
  detectionHeight?: number;

  /**
   * Enable close button on the top left of the scanning area (default: true)
   */
  enableCloseButton?: boolean;

  /**
   * Specify the ratio of the scanning area (sheet modal size) to the screen size.
   * Default is 0.9 for android, 1(pageSheet) for iOS.
   */
  sheetScreenRatio?: number;

  /**
   * Specify the types of codes to recognize (default: ["qr", "code39", "ean13"])
   */
  metadataObjectTypes?: MetadataObjectTypes[];

  /**
   * Enable multi scan mode (default: false)
   */
  isMulti: boolean;

  /**
   * Enable auto light when environment is dark (default: true)
   */
  enableAutoLight?: boolean;
}
