import { WebPlugin } from '@capacitor/core';
import { CodeScannerPluginPlugin } from './definitions';

export class CodeScannerPluginWeb extends WebPlugin implements CodeScannerPluginPlugin {
  constructor() {
    super({
      name: 'CodeScannerPlugin',
      platforms: ['web']
    });
  }

  async present(): Promise<{value: boolean}> {
    return {
      value: true,
    }
  }
}

const CodeScannerPlugin = new CodeScannerPluginWeb();

export { CodeScannerPlugin };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(CodeScannerPlugin);
