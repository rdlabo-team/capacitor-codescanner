import { WebPlugin } from '@capacitor/core';
import { CodeScannerPlugin, ScannerOption } from './definitions';

export class CodeScannerWeb extends WebPlugin implements CodeScannerPlugin {
  constructor() {
    super({
      name: 'CodeScanner',
      platforms: ['web']
    });
  }

  async present(): Promise<{value: boolean}> {
    return {
      value: true,
    }
  }
}

const CodeScanner = new CodeScannerWeb();

export { CodeScanner, ScannerOption };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(CodeScanner);
