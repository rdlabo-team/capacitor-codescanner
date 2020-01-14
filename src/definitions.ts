import { PluginListenerHandle } from '@capacitor/core';
declare module "@capacitor/core" {
  interface PluginRegistry {
    CodeScannerPlugin: CodeScannerPluginPlugin;
  }
}

export interface CodeScannerPluginPlugin {
  present(): Promise<{value: boolean}>;
  addListener(eventName: 'CodeScannerCaptchaEvent', listenerFunc: (info: any) => {
    code: string;
  }): PluginListenerHandle;
}
