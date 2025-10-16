# Infix Clojure Library - TDD Implementation Plan

## Overview
This document outlines a Test-Driven Development (TDD) approach to implement the Infix syntax library for Clojure. The implementation will be broken down into small, testable increments following the Red-Green-Refactor cycle.

## Project Structure
```
infix/
├── deps.edn                    # Project dependencies
├── src/
│   └── infix/
│       ├── core.clj           # Main public API
│       ├── parser.clj         # Infix expression parser
│       ├── precedence.clj     # Operator precedence handling
│       ├── compiler.clj       # AST to Clojure form compiler
│       └── utils.clj          # Utility functions
├── test/
│   └── infix/
│       ├── core_test.clj      # Integration tests
│       ├── parser_test.clj    # Parser unit tests
│       ├── precedence_test.clj # Precedence tests
│       └── compiler_test.clj  # Compiler tests
└── README.md
```

## Implementation Phases

### Phase 1: Project Setup and Basic Infrastructure (v0.1 foundation)
**Goal**: Set up project structure and basic tokenization

#### Step 1.1: Project Setup
- [ ] Create `deps.edn` with minimal dependencies (clojure, test.check)
- [ ] Set up basic namespace structure
- [ ] Create basic test runner setup

#### Step 1.2: Basic Tokenization (TDD)
**Test First**: Write tests for tokenizing simple expressions
```clojure
(deftest tokenize-simple-arithmetic
  (is (= [:a :+ :b] (tokenize '(a + b))))
  (is (= [:1 :* :2] (tokenize '(1 * 2)))))
```

**Implementation**: Create `infix.parser/tokenize` function
- Handle symbols, numbers, and basic operators
- Return sequence of tokens

### Phase 2: Basic Arithmetic Parser (v0.1 core)
**Goal**: Parse and compile simple arithmetic expressions

#### Step 2.1: Precedence Table (TDD)
**Test First**: Define operator precedence tests
```clojure
(deftest operator-precedence
  (is (< (precedence '+) (precedence '*)))
  (is (= (precedence '+) (precedence '-))))
```

**Implementation**: Create `infix.precedence` namespace
- Define precedence map for `+ - * /`
- Create precedence comparison functions

#### Step 2.2: Shunting Yard Parser (TDD)
**Test First**: Parse expressions to postfix notation
```clojure
(deftest parse-to-postfix
  (is (= [:a :b :+] (parse-infix '(a + b))))
  (is (= [:a :b :c :* :+] (parse-infix '(a + b * c)))))
```

**Implementation**: Implement Shunting Yard algorithm
- Handle operator precedence
- Support parentheses for grouping

#### Step 2.3: Postfix to Clojure Form Compiler (TDD)
**Test First**: Compile postfix to Clojure expressions
```clojure
(deftest compile-postfix
  (is (= '(+ a b) (compile-postfix [:a :b :+])))
  (is (= '(+ a (* b c)) (compile-postfix [:a :b :c :* :+]))))
```

**Implementation**: Create compiler for basic arithmetic
- Convert postfix notation to Clojure forms
- Handle nested expressions

#### Step 2.4: Basic `infix` Macro (TDD)
**Test First**: Test basic infix macro
```clojure
(deftest basic-infix-macro
  (is (= 7 (infix 3 + 4)))
  (is (= 14 (infix 2 * 3 + 4 * 2)))
  (is (= '(+ (* a b) (/ c d)) (macroexpand-1 '(infix a * b + c / d)))))
```

**Implementation**: Create basic `infix` macro
- Wire together parser and compiler
- Handle macro expansion correctly

### Phase 3: Comparison and Boolean Operators (v0.1 complete)
**Goal**: Support comparison and boolean logic

#### Step 3.1: Comparison Operators (TDD)
**Test First**: Test comparison operators
```clojure
(deftest comparison-operators
  (is (= true (infix 5 > 3)))
  (is (= false (infix 2 >= 5)))
  (is (= '(< x y) (macroexpand-1 '(infix x < y)))))
```

**Implementation**: Extend parser for `< <= > >= = not=`

#### Step 3.2: Boolean Logic (TDD)
**Test First**: Test boolean operators with precedence
```clojure
(deftest boolean-logic
  (is (= true (infix true and true)))
  (is (= true (infix false or true)))
  (is (= '(and (< x y) (<= y z)) (macroexpand-1 '(infix x < y and y <= z)))))
```

**Implementation**: Add `and or not` with correct precedence
- Boolean operators have lower precedence than comparisons
- Handle short-circuiting behavior

### Phase 4: Direct Threading Macro Support (v0.1 complete) ✅
**Goal**: Support native Clojure threading macros as infix operators

#### Step 4.1: Threading Operators Implementation (TDD) ✅
**Test First**: Test direct threading macro usage
```clojure
(deftest threading-macros
  (is (= 2 (infix {:a 1 :b 2} -> (get :b))))
  (is (= [2 3 4] (vec (infix [1 2 3] ->> (map inc)))))
  (is (= "JOHN" (infix {:user {:name "john"}} some-> :user some-> :name some-> .toUpperCase)))
  (is (= nil (infix {:user nil} some-> :user some-> :name))))
```

**Implementation**: ✅ COMPLETED
- ✅ Add `-> ->> some-> some->>` to operator precedence (0.05)
- ✅ Update parser to recognize threading macro symbols
- ✅ Update compiler to handle direct threading macro compilation
- ✅ Support multi-step threading: `data -> step1 -> step2 -> step3`
- ✅ Support mixed threading: `data -> access ->> transform`

#### Step 4.2: Comprehensive Threading Tests (TDD) ✅ 
**Test Results**: 40/40 tests passing
- ✅ Basic threading transformations for all four operators
- ✅ Nil-safe operations with proper behavior
- ✅ Mixed threading operator combinations
- ✅ Complex nested scenarios with business logic
- ✅ Integration with arithmetic, boolean, and comparison operators
- ✅ Edge cases and performance testing

### Phase 5: Function Call Syntax (v0.2 start)
**Goal**: Support `fn(args)` syntax

#### Step 5.1: Function Calls (TDD)
**Test First**: Test function call parsing
```clojure
(deftest function-calls
  (is (= '(max a b) (macroexpand-1 '(infix max(a, b)))))
  (is (= 5 (infix max(3, 5))))
  (is (= ["hello" "world"] (infix split("hello world", #" ")))))
```

**Implementation**: Parse function call syntax
- Handle comma-separated arguments
- Support nested function calls

### Phase 6: `infix-let` Macro (v0.2)
**Goal**: Implement sequential bindings with infix RHS

#### Step 6.1: Basic `infix-let` (TDD)
**Test First**: Test basic let bindings
```clojure
(deftest basic-infix-let
  (is (= 7 (infix-let [a (1 + 2) b (a + 4)] b)))
  (is (= '(let [a (+ 1 2) b (+ a 4)] b)
         (macroexpand-1 '(infix-let [a (1 + 2) b (a + 4)] b)))))
```

**Implementation**: Transform infix-let bindings
- Parse binding vector with infix RHS
- Generate proper `let` form

#### Step 6.2: Complex `infix-let` (TDD)
**Test First**: Test complex expressions in bindings
```clojure
(deftest complex-infix-let
  (testing "pipelines in let bindings"
    (is (= [2 4 6] (infix-let [nums [1 2 3]
                              doubled (nums |> map #(* 2 %))]
                     doubled))))
```

### Phase 7: Arrow Lambdas (v0.2)
**Goal**: Support `x -> expr` syntax

#### Step 7.1: Simple Arrow Lambdas (TDD)
**Test First**: Test basic arrow lambda parsing
```clojure
(deftest simple-arrow-lambdas
  (is (= [1 4 9] (map (infix x -> x * x) [1 2 3])))
  (is (= '(fn [x] (* x x)) (macroexpand-1 '(infix x -> x * x)))))
```

**Implementation**: Parse arrow lambda syntax
- Handle single parameter case
- Support infix expressions in body

#### Step 7.2: Multi-parameter Arrow Lambdas (TDD)
**Test First**: Test multiple parameters
```clojure
(deftest multi-param-arrows
  (is (= '(fn [x y] (+ x y)) (macroexpand-1 '(infix (x, y) -> x + y)))))
```

### Phase 8: `infix-defn` and Early Returns (v0.2 complete)
**Goal**: Implement functions with early returns

#### Step 8.1: Basic `infix-defn` (TDD)
**Test First**: Test function definition
```clojure
(deftest basic-infix-defn
  (infix-defn square [x] x * x)
  (is (= 9 (square 3))))
```

#### Step 8.2: Early Return Mechanism (TDD)
**Test First**: Test return statement
```clojure
(deftest early-return
  (infix-defn safe-div [x y]
    when y = 0 then (return nil)
    x / y)
  (is (nil? (safe-div 5 0)))
  (is (= 2.5 (safe-div 5 2))))
```

**Implementation**: Create Return exception mechanism
- Define `Return` type for non-local exits
- Wrap function body in try-catch

### Phase 9: Advanced Features (v0.3)
**Goal**: Implement membership tests, sorting helpers

#### Step 9.1: Membership Tests (TDD)
```clojure
(deftest membership-tests
  (is (= true (infix :a in #{:a :b :c})))
  (is (= false (infix :d not-in #{:a :b :c}))))
```

#### Step 9.2: Sorting Helpers (TDD)
```clojure
(deftest sorting-helpers
  (is (= "longest" (infix max-by(count, ["a" "bb" "longest"])))))
```

### Phase 10: Error Handling and Polish (v0.4)
**Goal**: Improve error messages and debugging

#### Step 10.1: Parser Error Messages (TDD)
```clojure
(deftest parser-errors
  (is (thrown-with-msg? Exception #"Mismatched parentheses"
        (macroexpand-1 '(infix (a + b]))))
```

#### Step 10.2: Macro Expansion Debugging
- Add source metadata preservation
- Improve error locations

## Testing Strategy

### Unit Testing
- Each parser component tested in isolation
- Property-based testing with `test.check` for expression generation
- Edge cases: empty expressions, malformed syntax

### Integration Testing  
- Full macro expansion tests
- Performance benchmarks vs hand-written Clojure
- Real-world usage examples

### Regression Testing
- Maintain comprehensive test suite
- Test backward compatibility with each version

## Development Workflow

### Red-Green-Refactor Cycle
1. **Red**: Write failing test for next small feature
2. **Green**: Write minimal code to make test pass
3. **Refactor**: Clean up code while keeping tests green
4. **Commit**: Small, focused commits with descriptive messages

### Continuous Integration
- Run full test suite on every commit
- Measure test coverage (aim for >90%)
- Performance regression detection

### Documentation
- Docstrings for all public functions
- README examples as executable tests
- API documentation generation

## Success Criteria

### Functional Requirements
- [ ] All README examples compile and execute correctly
- [ ] Full operator precedence compliance
- [ ] Proper error handling and messages
- [ ] Performance within 2x of hand-written Clojure

### Quality Requirements  
- [ ] >90% test coverage
- [ ] Clean, readable code structure
- [ ] Comprehensive documentation
- [ ] Zero breaking changes in patch versions

## Implementation Summary (Current Status)

### Completed Phases

#### ✅ Phase 1: Project Setup and Basic Infrastructure
- Project structure with `deps.edn`, proper namespaces
- Basic tokenization and infrastructure setup
- Foundation for TDD approach

#### ✅ Phase 2: Basic Arithmetic Parser  
- Shunting Yard algorithm implementation for operator precedence
- Support for `+ - * /` with correct precedence (1, 1, 2, 2)
- Postfix compilation to Clojure forms
- Basic `infix` macro functionality

#### ✅ Phase 3: Comparison and Boolean Operators
- Extended parser for `< <= > >= = not=` (precedence 0.5)
- Added `and or not` boolean operators (precedence 0.1, 0.2, 0.8)
- Unary operator support (`not`) with proper compilation
- Comprehensive test coverage with real-world scenarios

#### ✅ Phase 4: Direct Threading Macro Support
- Native Clojure threading macros as infix operators: `-> ->> some-> some->>`
- Lowest precedence (0.05) for proper left-to-right evaluation
- Support for multi-step threading: `data -> step1 -> step2 -> step3`
- Mixed threading support: `data -> access ->> transform ->> reduce`
- Nil-safe threading with proper `some->` and `some->>` behavior
- Smart function call detection vs infix grouping

### Current Status (v0.1 Complete)
- **Test Coverage**: 151/151 tests passing (100% success)
- **Operator Support**: Full arithmetic, comparison, boolean, and threading
- **Parser**: Robust Shunting Yard with smart function call detection  
- **Compiler**: Direct threading macro compilation with unary operator support
- **Integration**: Seamless function call, variable, and complex expression support
- **Edge Cases**: Extensive testing with nested expressions, extreme scenarios, and real-world business logic

This plan provides a systematic, test-driven approach to implementing the Infix library while maintaining high quality and ensuring all requirements are met.