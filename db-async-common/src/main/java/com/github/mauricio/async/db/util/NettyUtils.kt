package com.github.mauricio.async.db.util

import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.internal.logging.{InternalLoggerFactory, Slf4JLoggerFactory}

/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

object NettyUtils {

  InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)
  lazy val DefaultEventLoopGroup = new NioEventLoopGroup(0, DaemonThreadsFactory("db-async-netty"))

}