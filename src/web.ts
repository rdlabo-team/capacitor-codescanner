import { WebPlugin } from '@capacitor/core';

import type { CodeScannerPlugin, ScannerOption } from './definitions';

export class CodeScannerWeb extends WebPlugin implements CodeScannerPlugin {
  async present(options: ScannerOption): Promise<void> {
    console.log(options);
  }
}
