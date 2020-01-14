declare module "@capacitor/core" {
  interface PluginRegistry {
    CodeScannerPlugin: CodeScannerPluginPlugin;
  }
}

export interface CodeScannerPluginPlugin {
  echo(options: { value: string }): Promise<{value: string}>;
}
