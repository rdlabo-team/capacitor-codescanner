// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "RdlaboCapacitorCodescanner",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "RdlaboCapacitorCodescanner",
            targets: ["CodeScannerPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
    ],
    targets: [
        .target(
            name: "CodeScannerPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/CodeScannerPlugin"),
        .testTarget(
            name: "CodeScannerPluginTests",
            dependencies: ["CodeScannerPlugin"],
            path: "ios/Tests/CodeScannerPluginTests")
    ]
)
