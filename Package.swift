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
            checksum: "50132342b0b5175205c4bb60599cf7d23e28542da03f6f51458f230487b456ce"
        )
    ]
)
