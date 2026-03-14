(ns tbb-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [tbb]
   [clojure.string :as str]))

(deftest tis-test
  (testing "Simple expressions"
    (is (nil?
         (tbb/tis = 2 2))
        "Returns nil if assertion is correct"))

  (testing "Simple failing assertion"
    (is (thrown-with-msg? AssertionError #"failed assertion: \(= 1 0\)"
                          (tbb/tis = 1 0))
        "Throws an AssertionError with appropriate message"))

  (testing "complex expression"
    (is (nil?
         (tbb/tis < 10 (* 3 (+ 2 2))))
        "Still nil")

    (is (thrown-with-msg? AssertionError #"failed assertion: \(< 20 12\)"
                          (tbb/tis < 20 (* 3 (+ 2 2))))
        "In the exception message the arguments will be evaluated"))

  (testing "inside a let expression"
    (is (nil?
         (let [heystack "Babashka"
               needle "Baba"]
           (tbb/tis str/starts-with? heystack needle)))
        "Still works inside a let expression")

    (is (thrown-with-msg? AssertionError #"failed assertion: \(str/starts-with\? \"Babashka\" \"Ruska\"\)"
                          (let [heystack "Babashka"
                                needle "Ruska"]
                            (tbb/tis str/starts-with? heystack needle)))
        "Exception is thrown from the let expression too")))

(let [{:keys [fail error]} (clojure.test/run-tests)]
  (when (pos? (+ fail error))
    (System/exit 1)))
