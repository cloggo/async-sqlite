(ns async.sqlite.core
  #?(:clj
     (:require
      [async.async.core])))


#?(:clj
   (defmacro transaction
     "predicate? if fail return true"
     [db params & body]
     (let [[commit-handler rollback-handler predicate?] params
           rollback-handler (or rollback-handler
                                `(fn [err#]
                                   {:status :INTERNAL_SERVER_ERROR
                                             :error (async.async.core/append-error-message
                                                     err# " [Change not committed.]")}))
           predicate? (or predicate? `async.async.core/error?)]
       `(async.async.core/go-try
         (-> (async.sqlite.core/begin-transaction ~db)
             (async.async.core/<?_ ~@body)
             (async.async.core/<!)
             (#(if (~predicate? %)
                 (do (async.sqlite.core/rollback-transaction ~db)
                     (~rollback-handler %))
                 (do (async.sqlite.core/commit-transaction ~db)
                     (~commit-handler %)))))))))

