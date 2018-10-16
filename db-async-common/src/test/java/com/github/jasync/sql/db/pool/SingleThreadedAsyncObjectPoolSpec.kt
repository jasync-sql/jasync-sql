package com.github.jasync.sql.db.pool

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SingleThreadedAsyncObjectPoolSpec : AbstractAsyncObjectPoolSpec<SingleThreadedAsyncObjectPool<Widget>>() {


    override fun pool(factory: ObjectFactory<Widget>, conf: PoolConfiguration, testItemsPeriodically: Boolean): SingleThreadedAsyncObjectPool<Widget> =
            SingleThreadedAsyncObjectPool(factory, conf)

    @Test
    fun `SingleThreadedAsyncObjectPool should successfully record a closed state`() {
        val p = pool()
        assertThat(p.close().get()).isEqualTo(p)
        assertThat(p.isClosed()).isTrue()
    }

}
