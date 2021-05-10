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
* [`addListener(...)`](#addlistener)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### present(...)

```typescript
present(scannerOption: ScannerOption) => any
```

| Param               | Type                                                    |
| ------------------- | ------------------------------------------------------- |
| **`scannerOption`** | <code><a href="#scanneroption">ScannerOption</a></code> |

**Returns:** <code>any</code>

--------------------


### addListener(...)

```typescript
addListener(eventName: 'CodeScannerCatchEvent', listenerFunc: (event: { code: string; }) => void) => PluginListenerHandle
```

| Param              | Type                                               |
| ------------------ | -------------------------------------------------- |
| **`eventName`**    | <code>"CodeScannerCatchEvent"</code>               |
| **`listenerFunc`** | <code>(event: { code: string; }) =&gt; void</code> |

**Returns:** <code><a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### Interfaces


#### ScannerOption

| Prop                      | Type                |
| ------------------------- | ------------------- |
| **`detectionX`**          | <code>number</code> |
| **`detectionY`**          | <code>number</code> |
| **`detectionWidth`**      | <code>number</code> |
| **`detectionHeight`**     | <code>number</code> |
| **`metadataObjectTypes`** | <code>any</code>    |


#### PluginListenerHandle

| Prop         | Type                      |
| ------------ | ------------------------- |
| **`remove`** | <code>() =&gt; any</code> |

</docgen-api>
