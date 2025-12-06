(ns build-scittle
  "Build a merged trove_scittle.cljs file for Scittle."
  (:require [edamame.core :as e]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [babashka.fs :as fs]))

(defn parse-file 
  "Parse a .cljc file with reader conditionals resolved for :scittle/:cljs"
  [path ns-sym]
  (e/parse-string-all 
    (slurp path) 
    {:read-cond :allow
     :features #{:scittle :cljs}
     :syntax-quote true
     :quote true
     :fn true
     :var true
     :auto-resolve {:current ns-sym}}))

(defn ns-form? [form]
  (and (list? form) (= 'ns (first form))))

(defn comment-form? [form]
  (and (list? form) (= 'comment (first form))))

(defn remove-ns-forms [forms]
  (remove ns-form? forms))

(defn remove-comment-forms [forms]
  (remove comment-form? forms))

(defn pprint-str [form]
  (with-out-str (pp/pprint form)))

(defn replace-ns-prefixes 
  "Replace utils/ and console/ prefixes since everything is in one namespace"
  [s]
  (-> s
      (str/replace #"utils/" "")
      (str/replace #"console/" "")))

(defn fix-dynamic-vars
  "Add ^:dynamic metadata to earmuffed vars that lost it during parsing"
  [s]
  (-> s
      ;; Fix *log-fn* to have ^:dynamic (pprint puts def and name on separate lines)
      (str/replace #"\(def\n \*log-fn\*" "(def ^:dynamic\n *log-fn*")))

(defn build-merged-file []
  (let [;; Parse all source files
        utils-forms   (parse-file "src/taoensso/trove/utils.cljc" 'taoensso.trove.utils)
        console-forms (parse-file "src/taoensso/trove/console.cljc" 'taoensso.trove.console)
        trove-forms   (parse-file "src/taoensso/trove.cljc" 'taoensso.trove)
        
        ;; Filter out ns and comment forms
        utils-body   (-> utils-forms remove-ns-forms remove-comment-forms)
        console-body (-> console-forms remove-ns-forms remove-comment-forms)
        trove-body   (-> trove-forms remove-ns-forms remove-comment-forms)
        
        ;; Build output
        header ";; =============================================================================
;; taoensso/trove_scittle.cljs - Auto-generated merged Trove for Scittle
;; =============================================================================
;; DO NOT EDIT - This file is auto-generated from the Trove source files.
;; Regenerate with: bb build:scittle
;;
;; Source: https://github.com/franks42/trove-scittle
;; Based on: https://github.com/taoensso/trove
;; =============================================================================

"
        ns-form "(ns taoensso.trove-scittle
  \"A minimal, modern logging facade for Clojure/Script.
  Supports both traditional and structured logging.
  
  This is a merged single-file version for Scittle/SCI.
  
  Note: This file uses namespace taoensso.trove-scittle to match the filename,
  but provides the same API as taoensso.trove.\"
  {:author \"Peter Taoussanis (@ptaoussanis)\"}
  (:require [clojure.string :as str]))

"
        utils-section "\n;; =============================================================================
;; Utils
;; =============================================================================

"
        console-section "\n;; =============================================================================
;; Console Backend  
;; =============================================================================

"
        trove-section "\n;; =============================================================================
;; Main API
;; =============================================================================

"]
    
    (-> (str header
             ns-form
             utils-section
             (str/join "\n" (map pprint-str utils-body))
             console-section
             (str/join "\n" (map pprint-str console-body))
             trove-section
             (str/join "\n" (map pprint-str trove-body)))
        replace-ns-prefixes
        fix-dynamic-vars)))

(defn -main [& _args]
  (let [output-dir "dist"
        output-file (str output-dir "/trove_scittle.cljs")]
    
    ;; Ensure output directory exists
    (fs/create-dirs output-dir)
    
    ;; Generate and write the merged file
    (println "Building merged Trove for Scittle...")
    (let [content (build-merged-file)]
      (spit output-file content)
      (println "✅ Generated:" output-file)
      (println "   Size:" (count content) "bytes"))))

(when (= *file* (System/getProperty "babashka.file"))
  (-main))
