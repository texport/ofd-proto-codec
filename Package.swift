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
            url: "https://github.com/texport/ofd-proto-codec/releases/download/v1.2.0/OfdProtoCodec.xcframework.zip",
            checksum: "e4f523c4b4e5b5392b3e5086dc233907558d47d3febf0a6d2c651f0012adf82b"
        )
    ]
)
