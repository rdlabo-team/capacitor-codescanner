import { registerPlugin } from '@capacitor/core';

import type { CodeScannerPlugin } from './definitions';

const CodeScanner = registerPlugin<CodeScannerPlugin>('CodeScanner', {
  web: () => import('./web').then((m) => new m.CodeScannerWeb()),
});

export * from './definitions';
export { CodeScanner };
