;; =============================================================================
;; taoensso/trove_scittle.cljs - Auto-generated merged Trove for Scittle
;; =============================================================================
;; DO NOT EDIT - This file is auto-generated from the Trove source files.
;; Regenerate with: bb build:scittle
;;
;; Source: https://github.com/franks42/trove-scittle
;; Based on: https://github.com/taoensso/trove
;; =============================================================================

(ns taoensso.trove-scittle
  "A minimal, modern logging facade for Clojure/Script.
  Supports both traditional and structured logging.
  
  This is a merged single-file version for Scittle/SCI.
  
  Note: This file uses namespace taoensso.trove-scittle to match the filename,
  but provides the same API as taoensso.trove."
  {:author "Peter Taoussanis (@ptaoussanis)"}
  (:require [clojure.string :as str]))

;; =============================================================================
;; Utils
;; =============================================================================

(def nl "System line separator" "\n")

(let
 [cons? (fn [x] (instance? (type (cons 1 [])) x))]
  (defn
    const-form?
    [form]
    (cond
      (list? form)
      false
      (cons? form)
      false
      (map? form)
      (every? const-form? (vals form))
      (coll? form)
      (every? const-form? form)
      :else
      true)))

(defn
  callsite-coords
  "Returns [line column] from meta on given macro `&form`."
  [macro-form]
  (when-let
   [{:keys [line column]} (meta macro-form)]
    (when line (if column [line column] [line]))))

(defn
  assoc-some
  "Assocs each kv to given ?map iff its value is not nil."
  ([m k v] (if-not (nil? v) (assoc m k v) m))
  ([m m-kvs] (reduce-kv assoc-some m m-kvs)))

(defn
  format-id
  "`:foo.bar/baz` -> \"::baz\", etc."
  [ns x]
  (if
   (keyword? x)
    (if (= (namespace x) ns) (str "::" (name x)) (str x))
    (str x)))

;; =============================================================================
;; Console Backend  
;; =============================================================================

(defn-
  level->int
  [x]
  (case
   x
    :trace
    10
    :debug
    20
    :info
    50
    :warn
    60
    :error
    70
    :fatal
    80
    :report
    90
    -1))

(defn- timestamp [] (.toISOString (js/Date.)))

(defn
  get-log-fn
  "Returns a simple log-fn that:\n    - Clj:  logs to `*out*` using `println`.\n    - Cljs: logs to JavaScript console.\n\n  Options:\n    `:min-level` - ∈ #{nil :trace :debug :info :warn :error :fatal :report},\n                   log calls with a lower level will noop."
  ([] (get-log-fn nil))
  ([{:keys [min-level], :or {min-level nil}}]
   (fn
     log-fn:console
     [ns coords level id lazy_]
     (when
      (exists? js/console)
       (when
        (or
         (not min-level)
         (>= (level->int level) (level->int min-level)))
         (let
          [{:keys [msg data error]}
           (force lazy_)
           combo-msg
           (str/join
            " "
            (into
             []
             (filter some?)
             [(timestamp)
              level
              ns
              coords
              (when id (format-id ns id))
              msg
              (when-not (empty? data) (str nl "  data: " data))
              (when error (str nl " error: " error))]))]
           (case
            level
             :trace
             (.debug js/console combo-msg)
             :debug
             (.debug js/console combo-msg)
             :info
             (.info js/console combo-msg)
             :warn
             (.warn js/console combo-msg)
             :error
             (.error js/console combo-msg)
             :fatal
             (.error js/console combo-msg)
             :report
             (.info js/console combo-msg))))))))

;; =============================================================================
;; Main API
;; =============================================================================

(def ^:dynamic
  *log-fn*
  "The value of this var determines the Trove backend,\n  i.e. what happens on `trove/log!` calls.\n\n  When `nil`, all `trove/log!` calls will noop.\n  Otherwise value should be a (fn [ns coords level id lazy_]) with:\n\n    `ns` ------- String namespace  of   `log!` callsite, e.g. \"my-app.utils\"\n    `coords` --- ?[line column]    of   `log!` callsite, may be lost (nil) for macros wrapping `log!`\n\n    `level` ----  Keyword `:level` from `log!` call ∈ #{:trace :debug :info :warn :error :fatal :report}\n    `id` ------- ?Keyword `:id`    from `log!` call, e.g. `:auth/login`, `::order-complete`, etc.\n\n    `lazy_` ---- {:keys [msg data error kvs]}, MAY be wrapped with `delay` so access with `force`:\n      `:msg` --- ?String `:msg`        from `log!` call\n      `:data` -- ?Map    `:data`       from `log!` call, e.g. {:user-id 1234}\n      `:error` - ?Error  `:error`      from `log!` call, (`java.lang.Throwable`, `js/Error`, or nil)\n      `:kvs` --- ?Map of any other kvs from `log!` call, handy for custom `log-fn` opts, etc.\n\n  The configured `log-fn` may filter (conditionally noop), or produce the\n  relevant logging side effects (printing, etc.).\n\n  The configured `log-fn` will be called SYNCHRONOUSLY so:\n    - It has access to the `trove/log!` calling thread/context (can be handy).\n    - It should implement appropriate async/threading/backpressure for\n      expensive work.\n\n  Config:\n\n    Change dynamic value with `binding`.\n    Change root    value with `set-log-fn!`.\n\n    Basic fns are provided for some common backends, see `taoensso.trove.x/get-log-fn`\n    with x ∈ #{console telemere timbre mulog tools-logging slf4j} (default console)."
  (get-log-fn))

(defmacro
  set-log-fn!
  "Sets the root value of `*log-fn*` (see its docstring for more info)."
  [f]
  (if
   (:ns &env)
    (clojure.core/sequence
     (clojure.core/seq
      (clojure.core/concat
       (clojure.core/list 'set!)
       (clojure.core/list '*log-fn*)
       (clojure.core/list f))))
    (clojure.core/sequence
     (clojure.core/seq
      (clojure.core/concat
       (clojure.core/list 'alter-var-root)
       (clojure.core/list
        (clojure.core/sequence
         (clojure.core/seq
          (clojure.core/concat
           (clojure.core/list 'var)
           (clojure.core/list '*log-fn*)))))
       (clojure.core/list
        (clojure.core/sequence
         (clojure.core/seq
          (clojure.core/concat
           (clojure.core/list 'fn)
           (clojure.core/list
            (clojure.core/vec
             (clojure.core/sequence
              (clojure.core/seq
               (clojure.core/concat
                (clojure.core/list '___198__auto__))))))
           (clojure.core/list f))))))))))

(defmacro
  log!
  "Logs the given info to the currently configured backend (see `*log-fn*`)\n  and returns nil.\n\n  Common options:\n    `:level` -- ∈ #{:trace :debug :info :warn :error :fatal :report} (default `:info`)\n    `:id` ----- Optional keyword used to identify event, e.g. `:auth/login`, `::order-complete`, etc.\n    `:msg` ---- Optional message string describing event (use `str`, `format`, etc. as needed)\n    `:data` --- Optional arb map of structured data associated with event, e.g. {:user-id 1234}\n    `:error` -- Optional platform error (`java.lang.Throwable`, `js/Error`)\n\n  Advanced options:\n    `:let` ---- Bindings shared by lazy args: {:keys [msg data error kvs]}\n    `:ns` ----- Custom namespace string to override default\n    `:coords` - Custom [line column]    to override default\n    `:log-fn` - Custom `log-fn`         to override default (`*log-fn*`)\n    <kvs> ----- Any other kvs will also be provided to `log-fn`, handy for\n                custom `log-fn` opts, etc.\n\n  Traditional logs typically include at least {:keys [level msg ...]}.\n  Structured  logs typically include at least {:keys [level id data ...]}."
  {:arglists '([{:keys [level id msg data error]}])}
  [opts]
  (when-not
   (map? opts)
    (throw
     (ex-info
      "Trove opts must be a compile-time map"
      {:opts {:value opts, :type (type opts)}})))
  (let
   [{:keys [ns coords level id msg data error log-fn],
     letf :let,
     :or
     {ns (str *ns*),
      level :info,
      coords (callsite-coords &form),
      log-fn '*log-fn*}}
    opts
    lfn
    (gensym "lfn__")
    kvs
    (not-empty
     (dissoc
      opts
      :ns
      :coords
      :level
      :id
      :error
      :let
      :msg
      :data
      :log-fn))
    lazy-form
    (when-let
     [opts
      (assoc-some
       nil
       {:error error, :msg msg, :data data, :kvs kvs})]
      (if
       (every? const-form? [opts letf])
        (if
         letf
          (clojure.core/sequence
           (clojure.core/seq
            (clojure.core/concat
             (clojure.core/list 'let)
             (clojure.core/list letf)
             (clojure.core/list opts))))
          opts)
        (if
         letf
          (clojure.core/sequence
           (clojure.core/seq
            (clojure.core/concat
             (clojure.core/list 'delay)
             (clojure.core/list
              (clojure.core/sequence
               (clojure.core/seq
                (clojure.core/concat
                 (clojure.core/list 'let)
                 (clojure.core/list letf)
                 (clojure.core/list opts))))))))
          (clojure.core/sequence
           (clojure.core/seq
            (clojure.core/concat
             (clojure.core/list 'delay)
             (clojure.core/list opts)))))))]
    (clojure.core/sequence
     (clojure.core/seq
      (clojure.core/concat
       (clojure.core/list 'let)
       (clojure.core/list
        (clojure.core/vec
         (clojure.core/sequence
          (clojure.core/seq
           (clojure.core/concat
            (clojure.core/list lfn)
            (clojure.core/list log-fn))))))
       (clojure.core/list
        (clojure.core/sequence
         (clojure.core/seq
          (clojure.core/concat
           (clojure.core/list 'when)
           (clojure.core/list lfn)
           (clojure.core/list
            (clojure.core/sequence
             (clojure.core/seq
              (clojure.core/concat
               (clojure.core/list lfn)
               (clojure.core/list ns)
               (clojure.core/list coords)
               (clojure.core/list level)
               (clojure.core/list id)
               (clojure.core/list lazy-form)))))))))
       (clojure.core/list nil))))))
