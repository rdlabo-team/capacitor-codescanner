import { PluginListenerHandle } from '@capacitor/core';
declare module "@capacitor/core" {
  interface PluginRegistry {
    CodeScanner: CodeScannerPlugin;
  }
}

export interface CodeScannerPlugin {
  present(): Promise<{value: boolean}>;
  addListener(eventName: 'CodeScannerCaptchaEvent', listenerFunc: (info: any) => {
    code: string;
  }): PluginListenerHandle;
}
