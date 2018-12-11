package com.github.jasync.sql.db

import com.github.jasync.sql.db.util.Failure
import com.github.jasync.sql.db.util.complete
import com.github.jasync.sql.db.util.failed
import com.github.jasync.sql.db.util.flatMapAsync
import com.github.jasync.sql.db.util.onCompleteAsync
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

fun <A> Connection.inTransaction(executor: Executor, f: (Connection) -> CompletableFuture<A>): CompletableFuture<A> {
    return this.sendQuery("BEGIN").flatMapAsync(executor) { _ ->
        val p = CompletableFuture<A>()
        f(this).onCompleteAsync(executor) { ty1 ->
            sendQuery(if (ty1.isFailure) "ROLLBACK" else "COMMIT").onCompleteAsync(executor) { ty2 ->
                if (ty2.isFailure && ty1.isSuccess)
                    p.failed((ty2 as Failure).exception)
                else
                    p.complete(ty1)
            }
        }
        p
    }
}
