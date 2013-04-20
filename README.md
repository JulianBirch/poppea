# poppea

A couple of macros that do useful, general things.

## Usage

The easiest way to use Clostache in your project is via
[Clojars](http://clojars.org/net.colourcoding/poppea).

Leiningen:

```clj
[net.colourcoding/poppea "0.0.1"]
```

Namespace:

```clj
(:require
  [poppea :refer :all])
```

## Currying

Poppea implements very basic ML-style currying in three closely related macros.  defn-curried, defn-curried- and fn-curried


```clj
(defn-curried wrap-handler [handler request]
  (assoc (handler request) :x 3))
```

is the same as


```clj
(defn wrap-handler
  ([handler request]
   (assoc (handler request) :x 3))
  ([handler]
   (fn [request] (assoc (handler request) :x 3))))
```

It should be pointed out that there are a couple of practical issues of which one should be aware when using defn-curried.

 * Since it's not returning a var, reloading on the repl won't change a curried function.
 * Be careful using currying with -> and ->>.  The argument order may not be what you expect.  Clojure doesn't have ML's rich operators.

### Road Map

I'd like to implement a version that does eager evaluation of the code that only depends on the curried arguments, but it's much more complex than what has currently been implemented.

It'd also be nice to have a generator macro, which rewrites imperative style code as a reducer.  Turning a reducer into a lazy sequence shoudn't be problematic.

## quick-map

The other macro is coffee-map, which is inspired by Coffeescript's quick map syntax.


```clj
(let [x 3
      y 5]
  (coffee-map x y :z 1))
```

is the same as


```clj
(hash-map :x 3 :y 5 :z 1)
```

## Why is it called Poppea?

[Listen](http://www.youtube.com/watch?v=ijDi-2RADX0)

## License

Copyright © 2013 Julian Birch

Distributed under the Eclipse Public License, the same as Clojure.
