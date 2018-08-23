/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
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

package com.github.mauricio.async.db.postgresql.column

import org.specs2.mutable.Specification

class ByteArrayDecoderSpec extends Specification {

  val escapeTestData =
    """\000\001\002\003\004\005\006\007\010\011\012\013\014\015\016\017\020\021\022\023\024\025\026\027""" +
    """\030\031\032\033\034\035\036\037 !"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^""" +
    """_`abcdefghijklmnopqrstuvwxyz{|}~\177\200\201\202\203\204\205\206\207\210\211\212\213\214\215\216""" +
    """\217\220\221\222\223\224\225\226\227\230\231\232\233\234\235\236\237\240\241\242\243\244\245\246""" +
    """\247\250\251\252\253\254\255\256\257\260\261\262\263\264\265\266\267\270\271\272\273\274\275\276""" +
    """\277\300\301\302\303\304\305\306\307\310\311\312\313\314\315\316\317\320\321\322\323\324\325\326""" +
    """\327\330\331\332\333\334\335\336\337\340\341\342\343\344\345\346\347\350\351\352\353\354\355\356""" +
    """\357\360\361\362\363\364\365\366\367\370\371\372\373\374\375\376\377\377\376\375\374\373\372\371""" +
    """\370\367\366\365\364\363\362\361\360\357\356\355\354\353\352\351\350\347\346\345\344\343\342\341""" +
    """\340\337\336\335\334\333\332\331\330\327\326\325\324\323\322\321\320\317\316\315\314\313\312\311""" +
    """\310\307\306\305\304\303\302\301\300\277\276\275\274\273\272\271\270\267\266\265\264\263\262\261""" +
    """\260\257\256\255\254\253\252\251\250\247\246\245\244\243\242\241\240\237\236\235\234\233\232\231""" +
    """\230\227\226\225\224\223\222\221\220\217\216\215\214\213\212\211\210\207\206\205\204\203\202\201""" +
    """\200\177~}|{zyxwvutsrqponmlkjihgfedcba`_^]\\[ZYXWVUTSRQPONMLKJIHGFEDCBA@?>=<;:9876543210/.-,+*)(""" +
    """'&%$#"! \037\036\035\034\033\032\031\030\027\026\025\024\023\022\021\020\017\016\015\014\013\012""" +
    """\011\010\007\006\005\004\003\002\001\000"""
    
  val hexTestData =
    """\x000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e""" +
    """2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e""" +
    """5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e""" +
    """8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbe""" +
    """bfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedee""" +
    """eff0f1f2f3f4f5f6f7f8f9fafbfcfdfefffffefdfcfbfaf9f8f7f6f5f4f3f2f1f0efeeedecebeae9e8e7e6e5e4e3e2e1""" +
    """e0dfdedddcdbdad9d8d7d6d5d4d3d2d1d0cfcecdcccbcac9c8c7c6c5c4c3c2c1c0bfbebdbcbbbab9b8b7b6b5b4b3b2b1""" +
    """b0afaeadacabaaa9a8a7a6a5a4a3a2a1a09f9e9d9c9b9a999897969594939291908f8e8d8c8b8a898887868584838281""" +
    """807f7e7d7c7b7a797877767574737271706f6e6d6c6b6a696867666564636261605f5e5d5c5b5a595857565554535251""" +
    """504f4e4d4c4b4a494847464544434241403f3e3d3c3b3a393837363534333231302f2e2d2c2b2a292827262524232221""" +
    """201f1e1d1c1b1a191817161514131211100f0e0d0c0b0a09080706050403020100"""
    
  val originalData = ((0 to 255) ++ ((0 to 255).reverse)).map(_.toByte).toArray

  "decoder" should {

    "parse escape data" in {
      ByteArrayEncoderDecoder.decode(escapeTestData) === originalData
    }
    
    "parse hex data" in {
      ByteArrayEncoderDecoder.decode(hexTestData) === originalData
    }
  }

}
