# Changelog

All notable changes to the infix library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0-rc1] - 2024-10-16

### Added
- **Core Infix Operations**: Complete arithmetic, comparison, and boolean operators
  - Arithmetic: `+ - * /` with proper precedence
  - Comparison: `= not= < <= > >=`
  - Boolean: `and or not` with short-circuiting
  - Threading: `-> ->> some-> some->>` as infix operators

- **Arrow Lambda Syntax**: Clean anonymous function syntax
  - Single parameter: `x => x * 2`
  - Multi-parameter: `(x y) => x + y`
  - Complex expressions: `x => x * x + 1`
  - Smart disambiguation from threading operators

- **Function Definitions**: `infix-defn` macro with full feature support
  - Support for docstrings: `(infix-defn name "doc" [params] body)`
  - Early return mechanism with `return` statement
  - Multiple return points and guard clause patterns
  - Complex expressions with all operators

- **Function Call Syntax**: Familiar `fn(args)` transformation
  - Function calls: `max(3, 5)` → `(max 3 5)`
  - Method calls: `.method(obj, arg)` → `(.method obj arg)`
  - Integration with infix operators
  - Threading support: `obj -> .method() -> .other()`

- **Complete OOP Interop**: Full Java object support
  - Method calls: `obj.method(args)` → `(.method obj args)`
  - Method chaining: `obj -> .method1() -> .method2()`
  - Object creation: `ClassName(args)` → `(new ClassName args)`
  - Alternative syntax: `ClassName.new(args)`
  - Constructor chaining: `StringBuilder("hi") -> .append("!")`

### Technical Features
- **Robust Parser**: Shunting Yard algorithm with smart function detection
- **Proper Precedence**: Mathematical precedence with threading macros as lowest
- **Comprehensive Testing**: 100+ tests covering all features and edge cases
- **Full Integration**: All features work together seamlessly
- **Zero Learning Curve**: Optional enhancement that doesn't change Clojure semantics

### Project Setup
- **Multi-format Support**: deps.edn, project.clj, and pom.xml configurations
- **Maven Coordinates**: `com.github.jiriknesl/infix "1.0-rc1"`
- **License**: Eclipse Public License 2.0 (same as Clojure)
- **Documentation**: Comprehensive README with examples and usage patterns

### Release Candidate Status
This release candidate includes all planned features for version 1.0. The library is feature-complete and ready for production use. The API is stable and no breaking changes are expected for the 1.0 release.

## [Unreleased] - Future Features

### Planned for v1.1+
- Collection operators: `in`, `not-in`
- Enhanced comma handling
- Nested function call improvements
- Advanced error messages
- Performance optimizations