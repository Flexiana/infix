#!/bin/bash
set -e

VERSION="1.0-rc1"
echo "Preparing release for infix library v$VERSION"

# Verify all tests pass
echo "Running tests..."
clojure -M:test -e "(require 'infix.oop-final-test) (require 'infix.object-creation-simple-test) (clojure.test/run-all-tests #\"infix\..*\")"

# Build JAR with tools.build (if available)
if command -v clojure &> /dev/null && clojure -T:build 2>/dev/null; then
    echo "Building JAR..."
    clojure -T:build jar
fi

# Create git tag if in git repo
if [ -d ".git" ]; then
    echo "Creating git tag v$VERSION..."
    git tag -a "v$VERSION" -m "Release candidate v$VERSION - Feature complete infix library"
    echo "Tag created. Push with: git push origin v$VERSION"
fi

echo "Release preparation complete!"
echo "To deploy to Clojars:"
echo "  With Leiningen: lein deploy clojars"
echo "  With deps.edn:  clojure -T:build deploy"
echo ""
echo "Maven coordinates:"
echo "  [com.github.jiriknesl/infix \"$VERSION\"]"
echo "  com.github.jiriknesl/infix {:mvn/version \"$VERSION\"}"