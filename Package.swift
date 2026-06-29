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
            url: "https://github.com/texport/ofd-proto-codec/releases/download/v1.1.1/OfdProtoCodec.xcframework.zip",
            checksum: "686e75c1c9c292b6f6b5e27a4f0b89ba5446556706aa603e70243d603199f83f"
        )
    ]
)
