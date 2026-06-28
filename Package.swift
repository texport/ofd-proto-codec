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
            checksum: "274cc92e63b4209e38725f6d2105947fa692c5084f6de6cca0e4121b8855a4c6"
        )
    ]
)
