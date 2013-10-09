(ns poppea
  (:require [spyscope.core]))

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

(defn binding-symbols-for-symbol [symbol]
  (->> symbol find-var meta :arglists
       (sort-by (comp - count))
       first binding-symbols))

(defn partial-invoke [{:keys [-symbol] :as this} & params]
  (apply (find-var -symbol)
         (concat
          (->> (binding-symbols-for-symbol -symbol)
               (map #(get this % ::missing))
               (remove #(= % ::missing)))
          params)))

(defn lookup [this & path] (get-in this path nil))

(defn qualify [sym]
  (let [{:keys [ns name]} (meta (resolve sym))]
    (symbol (str (ns-name ns) "/" name))))

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

(defrecord-fn partial-invoke DocumentedPartial [-symbol])

(defmacro document-partial [symbol & params]
  (let [symbol (qualify symbol)]
    (merge (->DocumentedPartial symbol)
           (zipmap (binding-symbols-for-symbol symbol)
                   params))))

(defmacro defrecord-get [& definition]
  `(defrecord-fn lookup ~@definition))
