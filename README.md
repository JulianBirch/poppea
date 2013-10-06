# poppea

A couple of macros that do useful, general things.

## Usage

The easiest way to use Poppea in your project is via
[Clojars](http://clojars.org/net.colourcoding/poppea).

Leiningen:

```clj
[net.colourcoding/poppea "0.1.0"]
```

Namespace:

```clj
(:require
  [poppea :refer :all])
```

## document-partial

`document-partial` does the same thing as `partial`, except that
rather than returning a function, it returns a record that does
the same thing and (importantly) is serializable.

```clj
(defn f [a b c d] (+ a b c d))

(document-partial f 1 2)
=> #poppea.DocumentedPartial{:-symbol user/f, :b 2, :a 1}

((document-partial f 1 2) 3 4)
=> 10
```

It's a proper data structure, so you can do things like this:

```clj
((assoc (document-partial f 1 2) :a 5) 3 4)
=> 14
```

Note that this provides a fairly elegant way of serializing
anonymous functions: rewrite to a var function and then use
document-partial.

### Easter eggs

`defrecord-get` is the same as `defrecord` but implements acts as
a function the same way a `hash-map` does.  `defrecord-fn` is a
helper function for records that implement `IFn`.

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

This is a valid alternative to over-long anonymous functions, and
in my opinion vastly preferable to functions that just return
other functions.

### Performance

`defn-curried` is marginally faster than `partial`.  `document-partial` is significantly slower.

### Road Map

I'd like to implement a version that does eager evaluation of the code that only depends on the curried arguments, but it's much more complex than what has currently been implemented.

It'd also be nice to have a generator macro, which rewrites imperative style code as a reducer.  Turning a reducer into a lazy sequence shoudn't be problematic.

## quick-map

The other macro is coffee-map, which is inspired by Coffeescript's quick map syntax.

```clj
(let [x 3
      y 5]
  (coffee-map x y "a" 78))
```

is the same as

```clj
(hash-map :x 3 :y 5 "a" 78)
```

Basically, this is structuring, the complement of Clojures {:keys [x y]} destructuring.

## Why is it called Poppea?

[Listen](http://www.youtube.com/watch?v=ijDi-2RADX0)

## License

Copyright © 2013 Julian Birch

Distributed under the Eclipse Public License, the same as Clojure.
