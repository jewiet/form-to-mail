(ns tbb.core)

;; See unit test for example uses
(defmacro tis
  "Similar to assert but the exception message will have arguments evaluated. See unit tests for more details"
  [f & args]
  `(when-not (~f ~@args)
     (throw (AssertionError. (str "failed assertion: "
                                  (cons '~f (list ~@args)))))))

