
(ns semi-sweet-simple.core-test
  (:use clojure.test)
  (:use midje.semi-sweet)
)

(defn note-expected [] (println "^^^^ The previous failure was expected ^^^^"))

                       ;;; 

;; This is an example of the Midje version of a clojure.test test that would 
;; look like this:
;;      (is (= (+ 1 1) 2))
;;
;; Midje uses the clojure.test reporting mechanism, so that you can continue to
;; use tools that assume clojure.test.
(deftest example-of-a-simple-equality-test
  (expect (+ 1 1) => 2))

;; Failing tests should look familiar:
;;     FAIL at (core_test.clj:19)
;;     expected: 3
;;       actual: 4
(deftest example-of-a-simple-equality-test-failure
  (expect ( #(+ 1 %) 3) => 3)                                          (note-expected))

;; You can also use functions on the right-hand side. In that case,
;; the actual result is passed as the function's single argument.
(deftest example-of-a-function-as-right-hand-side
  (expect ( #(+ 1 %) 3) => odd?)                                       (note-expected))
;; The failing test will look slighly different:
;;     FAIL at (core_test.clj:28)
;;     Actual result did not pass expected function.
;;     expected function: odd?
;;         actual result: 4

;; If you're testing something that produces a function, use
;; (exactly):
(deftest example-of-an-exact-function-as-right-hand-side
  (expect (first [even? odd?]) => (exactly odd?))                      (note-expected))
;;     Actual result did not pass expected function.
;;     expected function: (exactly odd?)
;;         actual result: #<core$even_QMARK___4680 clojure.core$even_QMARK___4680@494b6bed>

;; There are a number of matching functions available. You can find them all with 
;;            (ns-publics (40 'midje.checkers))
;; They have doc strings.
;; Here's one of them:
(deftest example-of-a-predefined-checker
  (expect '[3 1 2] => (just [1 2 3] :in-any-order))                         ;; succeeds
  (expect '[3 3 1 2] => (just [1 2 3] :in-any-order))                       (note-expected))
;; Actual result did not agree with the checking function.
;;         Actual result: [3 3 1 2]
;;     Checking function: (just [1 2 3] :in-any-order)
;;     The checker said this about the reason:
;;        Expected three elements. There were four.

;; In the semi-sweet version of Midje, functions can be faked out as follows.
;; As with normal functions, faked functions have to be declared before use.
(declare first-fake another-fake)

;; Let's suppose we want to test this function but we haven't gotten around to
;; writing first-fake yet.
(defn function-under-test-1 [& rest]
  (apply first-fake rest))

;; The following test fakes the first-fake so that it returns a
;; predefined value when it's called. After that, the result of
;; function-under-test is checked in the normal way. 
(deftest example-of-a-simple-fake
  (expect (function-under-test-1 3) => 5
      (fake (first-fake 3) => 5)))

;; Here's the failure when a fake is never called
(defn function-under-test-2 [_] 5)
(deftest example-of-a-simple-fake-failure
  (expect (function-under-test-2 3) => 5
      (fake (first-fake 3) => 5))                                      (note-expected))
;;     FAIL for (core_test.clj:80)
;;     This expectation was never satisfied:
;;     (first-fake 3) should be called at least once.

;; If you rerun this test, you'll find that the line number in the 
;; actual error will point to the line containing the (fake)
;; call. I go to some trouble to get line numbers right. For example,
;; they should be correct even if the expect and fake are generated by
;; a macro. Let me know of cases where line numbers are wrong.


;; You can describe more than one call to the faked function, and you 
;; can fake more than one function.
(defn function-under-test-3 []
   (+ (first-fake 1) (first-fake 2 2) (another-fake)))
(deftest example-of-multiple-faked-functions
  (expect (function-under-test-3) => 111
     (fake (first-fake 1) => 1)
     (fake (first-fake 2 2) => 10)
     (fake (another-fake) => 100)))

;; When looking for a matching fake, Midje uses the same rules as when
;; checking a function-under-test's actual result. That means you can 
;; use single-argument matching functions and you must use (exactly) to 
;; match a functional argument.
(defn function-under-test-4 []
  (+ (first-fake 1 2 '(a blue cow))
     (another-fake inc)))

(deftest example-of-interesting-functional-args
  (expect (function-under-test-4) => 11
     (fake (first-fake odd? even? anything) => 1)
     (fake (another-fake (exactly inc)) => 10)))

;; The return values of a fake don't follow the rules for fake
;; arguments. I suppose I could be convinced that a "returning" a
;; function should be interpreted as returning the value that function
;; produces when given the actual arguments. I've never seen a need
;; for that. If you do, let me know.

;; You can insist that a function not be called:

(defn not-caller [n] (+ 1 n))
(defn some-function-never-called [])
(deftest example-of-not-called
  (expect (not-caller 3) => 4
     (not-called some-function-never-called)))

;; You can fake a function outside the current namespace. Suppose we 
;; have a function that operates on two sets. We want to override 
;; clojure.set/intersection so that our tests can only talk about properties
;; of tests, rather than have to laboriously construct actual sets with those
;; properties. So we pass in descriptive strings and fake out intersection.
;;
;; There is (will be) more support for this style in midje.sweet.

(use 'clojure.set)
(defn set-handler [set1 set2]
  (if (empty? (intersection set1 set2))
    set1
    (intersection set1 set2)))

(deftest example-of-faking-function-from-another-namespace
  "For disjoint sets, return the first."
  (expect (set-handler "some set" "some disjoint set") => "some set"
	  (fake (intersection "some set" "some disjoint set") => #{}))
  "For overlapping sets, return the intersection"
  (expect (set-handler "set" "overlapping set") => #{"intersection"}
	  (fake (intersection "set" "overlapping set") => #{"intersection"}))
)


;; My development style is "programming by wishful thinking"
;; (Sussman). Suppose I'm writing function (quux). If I hit anything
;; hard, I say "I really really believe there's already a function
;; (frozzle) that does just what I need". I then fake (frozzle) until
;; I'm done writing (quux). Later I'll get around to writing
;; (frozzle). In the meantime I need to define it somehow. Preferably
;; I'll define it in a way that will make it really obvious what
;; happens if I try to run the whole program before I've got a real
;; (frozzle). The way (declare) fails is not revealing enough. So I
;; use this:

(only-mocked frozzle another-function)

;; If I call (frozzle), I get this:
;;      Exception in thread "main" java.lang.Error: frozzle has no
;;      implementation. It's used in mock tests. (core_test.clj:1)





(defn test-ns-hook []
  "This calls the functions in order."
  (example-of-a-simple-equality-test)
  (example-of-a-simple-equality-test-failure)
  (example-of-a-function-as-right-hand-side)
  (example-of-an-exact-function-as-right-hand-side)
  (example-of-a-predefined-checker)
  (example-of-a-simple-fake)
  (example-of-a-simple-fake-failure)
  (example-of-multiple-faked-functions)
  (example-of-interesting-functional-args)
  (example-of-not-called)
  (example-of-faking-function-from-another-namespace)
)

