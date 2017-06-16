(ns poppea
  (:require [clojure.string :as s]
            [clojure.tools.reader.edn :as edn]))

(defn- curry
  [[params1 params2] body]
  (cons (vec params1)
        (if (empty? params2)
          body
          (list (apply list 'fn (vec params2) body)))))

(defn- do-curried [symbol to-fn params]
  (let [result (split-with (complement vector?) params)
        [[name doc meta] [args & body]] result
        [doc meta] (if (string? doc) [doc meta] [nil doc])
        body (if meta (cons meta body) body)
        arity-for-n #(-> % inc (split-at args) (to-fn body))
        arities (->>
                 (range 0 (count args))
                 (map arity-for-n)
                 reverse)
        before (keep identity [symbol name doc])]
    (concat before arities)))

(defmacro defn-curried
  "Builds a multiple arity function similar that returns closures
          for the missing parameters, similar to ML's behaviour."
  [& params]
  (do-curried 'defn curry params))

(defmacro defn-curried-
  "Builds a multiple arity function similar that returns closures
          for the missing parameters, similar to ML's behaviour."
  [& params]
  (do-curried 'defn- curry params))

(defmacro fn-curried
  "Builds a multiple arity function similar that returns closures
          for the missing parameters, similar to ML's behaviour."
  [& params]
  (do-curried 'fn curry params))

(defn- coffee-map-fn [keys]
  (if (empty? keys)
    []
    (let [k (first keys)]
      (if (symbol? k)
        (->> keys next coffee-map-fn (cons k) (cons (keyword k)))
        (->> keys next next coffee-map-fn
             (cons (second keys))
             (cons k))))))

(defmacro coffee-map [& keys]
  (cons 'hash-map (coffee-map-fn `~keys)))

(defn extract-symbols [index lhs]
  (cond (symbol? lhs) lhs
        (vector? lhs) index
        (map? lhs) (or (:as lhs) index)
        :else index))

(defn- binding-symbols [binding-clause]
  (doall
   (map #(if (symbol? %) (keyword (name %)) %)
        (map-indexed extract-symbols binding-clause))))

(defn binding-symbols-for-var [function]
  (->> function meta :arglists
       (sort-by (comp - count))
       first binding-symbols))

(defn parameter-capture? [param]
  (and (symbol? param)
       (re-find #"^%" (name param))))

(defn bound-index [param]
  (if (parameter-capture? param)
    (let [s (.substring (name param) 1)
          a (edn/read-string s)]
      (cond (number? a) (dec a)
            (= (count s) 0) 0))))

(defn-curried include-% [params bound index]
  (if index
    (nth params index)
    bound))

(defn bound-params [this]
  (->> (::function this)
       binding-symbols-for-var
       (map #(get this % ::missing))
       (remove #(= % ::missing))))

(defn partial-invoke-% [this & params]
  (let [bound-params (bound-params this)
        indexes (apply vector (map bound-index bound-params))
        non-nil-indexes (remove nil? indexes)
        c (if (empty? non-nil-indexes)
            0
            (inc (apply max non-nil-indexes)))]
    (apply (::function this)
           (concat (map (include-% params) bound-params indexes)
                   (drop c params)))))

(defn partial-invoke [this & params]
  (apply (::function this)
         (concat (bound-params this) params)))

;;; Don't use this, it's just used to implement defrecord-get
(defn record-lookup [this & path] (get-in this path nil))

(defmacro defrecord-fn [function-symbol & definition]
  (let [invoke (symbol "invoke")
        applyTo (symbol "applyTo")]
    (concat ['defrecord] definition
            `(clojure.lang.IFn
              (~invoke [this#]
                       (~function-symbol this#))
              (~invoke [this# a#]
                       (~function-symbol this# a#))
              (~invoke [this# a# b#]
                       (~function-symbol this# a# b#))
              (~invoke [this# a# b# c#]
                       (~function-symbol this# a# b# c#))
              (~invoke [this# a# b# c# d#]
                       (~function-symbol this# a# b# c# d#))
              (~invoke [this# a# b# c# d# e#]
                       (~function-symbol this# a# b# c# d# e#))
              (~applyTo [this# args#]
                        (clojure.lang.AFn/applyToHelper this# args#))))))

(defrecord-fn partial-invoke-%
  DocumentedPartialArg [])
(defrecord-fn partial-invoke
  DocumentedPartial [])

(defn document-partial-map [symbol process params]
  (assert (not (nil? (resolve `~symbol)))
          (str "Could not resolve " symbol))
  (let [function (resolve `~symbol)]
    `(hash-map
      ::function ~function
      ~@(interleave (binding-symbols-for-var function)
                    (map process params)))))

(defn capture-% [s]
  (if (parameter-capture? s) `'~s s))

(defmacro document-partial-% [symbol & params]
  `(map->DocumentedPartialArg
    ~(document-partial-map symbol capture-% params)))

(defmacro document-partial [symbol & params]
  `(map->DocumentedPartial
    ~(document-partial-map symbol identity params)))

(defmacro defrecord-get [& definition]
  `(defrecord-fn record-lookup ~@definition))

(defn at [yield]
  (fn ([] (println "Arity 0") (yield))
    ([x] (println "Arity 1") (yield x))
    ([r x] (println "Arity 2")
       (def p0 yield)
       (def p1 r)
       (def p2 x)
       (yield r x))))
