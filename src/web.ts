import { WebPlugin } from '@capacitor/core';
import { CodeScannerPluginPlugin } from './definitions';

export class CodeScannerPluginWeb extends WebPlugin implements CodeScannerPluginPlugin {
  constructor() {
    super({
      name: 'CodeScannerPlugin',
      platforms: ['web']
    });
  }

  async echo(options: { value: string }): Promise<{value: string}> {
    console.log('ECHO', options);
    return options;
  }
}

const CodeScannerPlugin = new CodeScannerPluginWeb();

export { CodeScannerPlugin };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(CodeScannerPlugin);
