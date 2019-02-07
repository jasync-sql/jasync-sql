package com.github.jasync.sql.db.pool

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ActorBasedAsyncObjectPoolSpec : AbstractAsyncObjectPoolSpec<ActorBasedObjectPool<Widget>>() {


    override fun pool(factory: ObjectFactory<Widget>, conf: ObjectPoolConfiguration): ActorBasedObjectPool<Widget> =
        ActorBasedObjectPool(factory, conf, true)

    @Test
    fun `SingleThreadedAsyncObjectPool should successfully record a closed state`() {
        val p = pool()
        assertThat(p.close().get()).isEqualTo(p)
        assertThat(p.closed).isTrue()
    }

}
