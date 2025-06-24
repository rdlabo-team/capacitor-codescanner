# @rdlabo/capacitor-codescanner

Capacitor Plugin for Code Scanner

## Install

```bash
npm install @rdlabo/capacitor-codescanner
npx cap sync
```

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

| Prop                      | Type                                                                                                                                                                                                                                                                                         |
| ------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`detectionX`**          | <code>number</code>                                                                                                                                                                                                                                                                          |
| **`detectionY`**          | <code>number</code>                                                                                                                                                                                                                                                                          |
| **`detectionWidth`**      | <code>number</code>                                                                                                                                                                                                                                                                          |
| **`detectionHeight`**     | <code>number</code>                                                                                                                                                                                                                                                                          |
| **`metadataObjectTypes`** | <code><a href="#record">Record</a>&lt;'aztec' \| 'code128' \| 'code39' \| 'code39Mod43' \| 'code93' \| 'dataMatrix' \| 'ean13' \| 'ean8' \| 'face' \| 'interleaved2of5' \| 'itf14' \| 'pdf417' \| 'qr' \| 'upce' \| 'catBody' \| 'dogBody' \| 'humanBody' \| 'salientObject' , []&gt;</code> |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


### Type Aliases


#### Record

Construct a type with a set of properties K of type T

<code>{ [P in K]: T; }</code>

</docgen-api>
