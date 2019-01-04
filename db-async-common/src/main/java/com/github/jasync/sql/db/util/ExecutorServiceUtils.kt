package com.github.jasync.sql.db.util

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool

@Suppress("unused")
object ExecutorServiceUtils {

    val CommonPool: ExecutorService = ForkJoinPool.commonPool()

    fun newFixedPool(count: Int, name: String): ExecutorService {
        return Executors.newFixedThreadPool(count, DaemonThreadsFactory(name))
    }

}
