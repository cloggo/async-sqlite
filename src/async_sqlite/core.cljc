(ns async-sqlite.core
  #?(:clj
     (:require
      [cljs-async.core])))


#?(:clj
   (defmacro transaction
     "predicate? if fail return true"
     [db params & body]
     (let [[commit-handler rollback-handler predicate?] params
           rollback-handler (or rollback-handler
                                `(fn [err#]
                                   {:status :INTERNAL_SERVER_ERROR
                                             :error (cljs-async.core/append-error-message
                                                     err# " [Change not committed.]")}))
           predicate? (or predicate? `cljs-async.core/error?)]
       `(cljs-async.core/go-try
         (-> (async.sqlite.core/begin-transaction ~db)
             (cljs-async.core/<?_ ~@body)
             (cljs-async.core/<!)
             (#(if (~predicate? %)
                 (do (async.sqlite.core/rollback-transaction ~db)
                     (~rollback-handler %))
                 (do (async.sqlite.core/commit-transaction ~db)
                     (~commit-handler %)))))))))

