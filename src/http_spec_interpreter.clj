(ns http-spec-interpreter)

(println "{ \"type\": \"InterpreterState\", \"ready\": true }")
;; TODO: Read all lines
(doseq [line (line-seq (java.io.BufferedReader. *in*))]
    (println "{ \"type\": \"Success\"}"))
