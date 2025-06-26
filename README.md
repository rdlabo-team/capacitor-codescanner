# @rdlabo/capacitor-codescanner

Capacitor Plugin for Code Scanner

## Install

```bash
npm install @rdlabo/capacitor-codescanner
npx cap sync
```


## Usage

```typescript
import { CodeScanner } from '@rdlabo/capacitor-codescanner';

// 基本的なQRコードスキャン
const scanQRCode = async () => {
  await CodeScanner.addListener('CodeScannerCatchEvent', (event) => {
    console.log('スキャンされたコード:', event.code);
  });

  await CodeScanner.present({
    detectionX: 0.2,
    detectionY: 0.35,
    detectionWidth: 0.6,
    detectionHeight: 0.15,
    isMulti: false,
    CodeTypes: ['qr']
  });
};

// 複数のバーコードタイプを連続スキャン
const scanMultipleCodes = async () => {
  await CodeScanner.addListener('CodeScannerCatchEvent', (event) => {
    console.log('スキャンされたコード:', event.code);
  });

  await CodeScanner.present({
    detectionX: 0.1,
    detectionY: 0.3,
    detectionWidth: 0.8,
    detectionHeight: 0.2,
    isMulti: true,
    CodeTypes: ['qr', 'code39', 'ean13', 'code128']
  });
};
```

## Functions

- **自動ライト制御**: スキャナー起動時に自動的にフラッシュライトが点灯します
- **バイブレーション**: コード検出時にバイブレーションでフィードバックを提供します
- **検出エリア表示**: 赤い枠で検出エリアを視覚的に表示します
- **検出コードハイライト**: 検出されたコードを赤い枠でハイライト表示します
- **閉じるボタン**: 右上の「✕」ボタンでスキャナーを閉じることができます
- **複数スキャンモード**: `isMulti: true`で複数のコードを連続してスキャンできます


## API

<docgen-index>

* [`present(...)`](#present)
* [`addListener('CodeScannerCatchEvent', ...)`](#addlistenercodescannercatchevent-)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### present(...)

```typescript
present(scannerOption: ScannerOption) => Promise<void>
```

| Param               | Type                                                    |
| ------------------- | ------------------------------------------------------- |
| **`scannerOption`** | <code><a href="#scanneroption">ScannerOption</a></code> |

--------------------


### addListener('CodeScannerCatchEvent', ...)

```typescript
addListener(eventName: 'CodeScannerCatchEvent', listenerFunc: (event: { code: string; }) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                               |
| ------------------ | -------------------------------------------------- |
| **`eventName`**    | <code>'CodeScannerCatchEvent'</code>               |
| **`listenerFunc`** | <code>(event: { code: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### Interfaces


#### ScannerOption

| Prop                      | Type                               |
| ------------------------- | ---------------------------------- |
| **`detectionX`**          | <code>number</code>                |
| **`detectionY`**          | <code>number</code>                |
| **`detectionWidth`**      | <code>number</code>                |
| **`detectionHeight`**     | <code>number</code>                |
| **`isMulti`**             | <code>boolean</code>               |
| **`metadataObjectTypes`** | <code>MetadataObjectTypes[]</code> |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


### Type Aliases


#### MetadataObjectTypes

<code>'aztec' | 'code128' | 'code39' | 'code39Mod43' | 'code93' | 'dataMatrix' | 'ean13' | 'ean8' | 'face' | 'interleaved2of5' | 'itf14' | 'pdf417' | 'qr' | 'upce' | 'catBody' | 'dogBody' | 'humanBody' | 'salientObject'</code>

</docgen-api>
