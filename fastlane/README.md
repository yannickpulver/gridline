fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android increment_build

```sh
[bundle exec] fastlane android increment_build
```



### android build

```sh
[bundle exec] fastlane android build
```

Build the Android app

### android deploy

```sh
[bundle exec] fastlane android deploy
```

Deploy a new version to the Google Play Store

----


## iOS

### ios increment_build

```sh
[bundle exec] fastlane ios increment_build
```



### ios build

```sh
[bundle exec] fastlane ios build
```

Build the iOS app

### ios deliver_store

```sh
[bundle exec] fastlane ios deliver_store
```



### ios deploy

```sh
[bundle exec] fastlane ios deploy
```

Build and deploy the iOS app

----


## macos

### macos buildDmg

```sh
[bundle exec] fastlane macos buildDmg
```

Build Macos app

### macos buildPkg

```sh
[bundle exec] fastlane macos buildPkg
```

Build Macos package

### macos deploy_dmg

```sh
[bundle exec] fastlane macos deploy_dmg
```

Deploy

### macos deploy_store

```sh
[bundle exec] fastlane macos deploy_store
```



----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
