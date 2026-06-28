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
            checksum: "0f993532b0e89e96859f27f4ec8d607bc35c385bb3f85896ba76065c3b992753"
        )
    ]
)
