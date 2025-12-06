# Trove Scittle Tests

Playwright-based browser tests for Trove in Scittle/SCI.

## Setup

```bash
cd test/scittle
npm install
npm run install-playwright
```

## Run Tests

```bash
npm test
```

## What's Tested

1. **Load utils** - `taoensso.trove.utils` namespace loads
2. **const-form?** - The `:scittle` reader conditional fix works
3. **Load console** - `taoensso.trove.console` namespace loads
4. **get-log-fn** - Console backend returns a function
5. **Load trove** - Main `taoensso.trove` namespace loads
6. **\*log-fn\*** - Dynamic var is a function
7. **log! macro** - Basic logging works
8. **Structured data** - Data maps are logged
9. **All levels** - All 7 log levels work
10. **Error logging** - Errors are logged correctly
11. **Complex data** - Nested data structures work
12. **set-log-fn!** - Custom log functions work

## The Fix

The `:scittle` reader conditional in `utils.cljc`:

```clojure
(let [cons? (fn [x] (instance? #?(:clj clojure.lang.Cons, 
                                  :scittle (type (cons 1 [])), 
                                  :cljs cljs.core/Cons) x))]
  ...)
```

SCI/Scittle doesn't expose `cljs.core/Cons` as a resolvable symbol,
so we use `(type (cons 1 []))` to get the Cons type at runtime.
