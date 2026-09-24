#!/usr/bin/env bash
set -euo pipefail

kok="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$kok/frontend"
npm ci
npm test
npm run build

cd "$kok"
mkdir -p src/main/resources/static
find src/main/resources/static -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +
cp -R frontend/dist/. src/main/resources/static/
bash ./mvnw -DskipTests package
echo "Jar uretildi: $kok/target/seyyah-0.0.1-SNAPSHOT.jar"
