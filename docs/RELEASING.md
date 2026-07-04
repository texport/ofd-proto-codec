# Release Checklist / Чеклист релиза

This project is published to Maven Central and exposes a Swift Package Manager binary artifact. Do not publish from a local agent run unless the maintainer explicitly confirms the release and provides credentials.
/ Проект публикуется в Maven Central и отдает binary artifact для Swift Package Manager. Не публикуйте релиз из локального запуска агента без явного подтверждения владельца и настроенных credentials.

## Version / Версия

1. Update `version` in `build.gradle.kts`.
2. Update visible README install snippets and badges.
3. Keep dependency and plugin versions in `gradle/libs.versions.toml`.

For Android target support added in `1.2.0`, use a minor release, not a patch release.
/ Для добавления Android target в `1.2.0` используется minor release, а не patch release.

## Verification / Проверка

Run the full local gate:

```bash
./gradlew jvmTest jacocoTestReport detekt assemble --warning-mode all
```

For Swift Package Manager artifacts, generate the XCFramework manifest after the version is final:

```bash
./gradlew generateSpmManifest
```

`Package.swift` contains the checksum of the release zip and must be generated from the actual `OfdProtoCodec.xcframework.zip`; do not edit the checksum manually.
/ `Package.swift` содержит checksum релизного zip-файла и должен генерироваться из реального `OfdProtoCodec.xcframework.zip`; checksum нельзя редактировать вручную.

## Publish / Публикация

1. Confirm `OSSRH_USERNAME`, `OSSRH_PASSWORD`, `SIGNING_KEY`, and `SIGNING_PASSWORD`.
2. Publish Maven publications with the configured `nmcp` task.
3. Create Git tag `v<version>`.
4. Create GitHub Release for the same tag.
5. Upload `OfdProtoCodec.xcframework.zip` to that GitHub Release.
6. Commit the generated `Package.swift` that references the uploaded asset and checksum.

Current known Gradle deprecation warnings may originate inside Gradle plugins. If `--warning-mode all --stacktrace` points to plugin internals rather than project build scripts, record them in the release notes instead of hiding them with suppressions.
/ Текущие Gradle deprecation warnings могут приходить из внутренних частей Gradle-плагинов. Если `--warning-mode all --stacktrace` указывает на код плагина, а не на build script проекта, фиксируйте это в release notes вместо скрытия suppressions.
