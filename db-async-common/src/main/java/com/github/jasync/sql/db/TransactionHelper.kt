package com.github.jasync.sql.db

import com.github.jasync.sql.db.util.Failure
import com.github.jasync.sql.db.util.complete
import com.github.jasync.sql.db.util.failure
import com.github.jasync.sql.db.util.flatMap
import com.github.jasync.sql.db.util.onComplete
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

fun <A> Connection.inTransaction(executor: Executor, f: (Connection) -> CompletableFuture<A>): CompletableFuture<A> {
  return this.sendQuery("BEGIN").flatMap(executor) { _ ->
    val p = CompletableFuture<A>()
    f(this).onComplete(executor) { ty1 ->
      sendQuery(if (ty1.isFailure) "ROLLBACK" else "COMMIT").onComplete(executor) { ty2 ->
        if (ty2.isFailure && ty1.isSuccess)
          p.failure((ty2 as Failure).exception)
        else
          p.complete(ty1)
      }
    }
  }
}
