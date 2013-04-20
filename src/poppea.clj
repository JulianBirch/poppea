(ns poppea)

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
      (if (keyword? k)
        (->> keys next next coffee-map-fn
             (cons (second keys))
             (cons k))
        (->> keys next coffee-map-fn (cons k) (cons (keyword k)))))))

(defmacro coffee-map [& keys]
  (cons 'hash-map (coffee-map-fn `~keys)))
