#!/bin/sh
set -e

DIRNAME="$(cd "$(dirname "$0")" && pwd)"
MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-3.9.6"

if [ -f "$MAVEN_HOME/bin/mvn" ]; then
    exec "$MAVEN_HOME/bin/mvn" "$@"
fi

echo "Downloading Apache Maven 3.9.6..."
mkdir -p "$MAVEN_HOME"
TEMP_ZIP=$(mktemp)
curl -fsSL "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip" -o "$TEMP_ZIP"
unzip -q -o "$TEMP_ZIP" -d /tmp/mvn_unzip
cp -rf /tmp/mvn_unzip/apache-maven-3.9.6/* "$MAVEN_HOME/"
rm -rf "$TEMP_ZIP" /tmp/mvn_unzip
chmod +x "$MAVEN_HOME/bin/mvn"

exec "$MAVEN_HOME/bin/mvn" "$@"
