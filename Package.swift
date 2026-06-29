// swift-tools-version:5.5
import PackageDescription

let package = Package(
    name: "OfdProtoCodec",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "OfdProtoCodec",
            targets: ["OfdProtoCodec"]
        ),
    ],
    dependencies: [],
    targets: [
        .binaryTarget(
            name: "OfdProtoCodec",
            url: "https://github.com/texport/ofd-proto-codec/releases/download/v1.1.0/OfdProtoCodec.xcframework.zip",
            checksum: "e79ac6ec34e12d414550c6aaffa811ab929b22aae1e591e500e16ed6c5698c9e"
        )
    ]
)
