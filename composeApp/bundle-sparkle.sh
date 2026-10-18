#!/bin/sh
# Copies Sparkle into the app and signs it, so the notarized DMG can update itself.
# Usage: bundle-sparkle.sh <Gridline.app> <Sparkle.framework> <signing identity> <entitlements>
set -eu

app="$1"
identity="$3"
framework="$app/Contents/Frameworks/Sparkle.framework"

sign() { codesign --force --timestamp --options runtime --sign "$identity" "$@"; }

rm -rf "$framework"
mkdir -p "$app/Contents/Frameworks"
ditto "$2" "$framework"
# The XPC services are only needed by sandboxed apps
rm -rf "$framework/Versions/B/XPCServices" "$framework/XPCServices"

# Inside out, as codesign --deep isn't recommended
sign "$framework/Versions/B/Autoupdate"
sign "$framework/Versions/B/Updater.app"
sign "$framework"
# Re-seal the app, its other contents keep their signatures
sign --entitlements "$4" "$app"
