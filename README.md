This is a Kotlin Multiplatform project targeting Android, Desktop.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…


When you have problems building for Desktop ("jpackage" not found), make sure you're using jbr-17 to build (jbr included in android studio doesn't have jpackage integrated, so you might have to download it seperately)

https://kotlinlang.slack.com/archives/C01D6HTPATV/p1700567342600669?thread_ts=1700493566.103599&cid=C01D6HTPATV

Desktop: Currently works with `packageDmg` build, but not release builds. Android works with release build.