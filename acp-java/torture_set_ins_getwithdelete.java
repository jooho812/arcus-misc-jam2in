/* -*- Mode: Java; tab-width: 2; c-basic-offset: 2; indent-tabs-mode: nil -*- */
/*
 * acp-java : Arcus Java Client Performance benchmark program
 * Copyright 2013-2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.Set;

import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.CollectionOverflowAction;
import net.spy.memcached.collection.ElementValueType;
import net.spy.memcached.internal.CollectionFuture;

public class torture_set_ins_getwithdelete implements client_profile {
  public boolean do_test(client cli) {
    try {
      if (!do_set_test(cli))
        return false;
    } catch (Exception e) {
      System.out.printf("client_profile exception. id=%d exception=%s\n", 
                        cli.id, e.toString());
      e.printStackTrace();
    }
    return true;
  }
  
  // create a set and insert elements and get(withDelete=true)

  public boolean do_set_test(client cli) throws Exception {
    // Pick a key
    String key = cli.ks.get_key();

    String[] temp = key.split("-");
    long base = Long.parseLong(temp[1]);
    base = base * 64*1024;

    // Create a set item
    if (!cli.before_request(false))
      return false;
    ElementValueType vtype = ElementValueType.BYTEARRAY;
    CollectionAttributes attr = 
      new CollectionAttributes(cli.conf.client_exptime,
                               new Long(cli.conf.ins_element_size),
                               CollectionOverflowAction.error);
    CollectionFuture<Boolean> fb = cli.next_ac.asyncSopCreate(key, vtype, attr);
    boolean ok = fb.get(1000L, TimeUnit.MILLISECONDS);
    if (!ok) {
      System.out.printf("sop create failed. id=%d key=%s: %s\n", cli.id,
                        key, fb.getOperationStatus().getResponse());
    }
    if (!cli.after_request(ok, false))
      return false;

    // Insert elements
    for (long skey = base; skey < base + cli.conf.ins_element_size; skey++) {
      if (!cli.before_request(false))
        return false;
      byte[] val = new byte[10];

      long n = skey;
      int i = 0;
      while (n != 0 && i < val.length) {
        val[i] = (byte)(n % 10);
        n = n / 10;
        i++;
      }
      fb = cli.next_ac.asyncSopInsert(key, val,
                                      null /* Do not auto-create item */);
      ok = fb.get(1000L, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("sop insert failed. id=%d key=%s skey=%d: %s\n",
                          cli.id, key, skey,
                          fb.getOperationStatus().getResponse());
      }
      if (!cli.after_request(ok, false))
        return false;
    }

    // Delete elements
    if (!cli.before_request())
      return false;
    CollectionFuture<Set<Object>> f =
      cli.next_ac.asyncSopGet(key, cli.conf.act_element_size, true /* withDelete */,
                              true /* dropIfEmpty */);
    Set<Object> val = f.get(5000L, TimeUnit.MILLISECONDS);
    if (val == null || val.size() <= 0) {
      System.out.printf("Collection_Set: SopGet failed." +
                        " id=%d key=%s val.size=%d\n",
                        cli.id, key,
                        val == null ? -1 : val.size());
    }
    if (!cli.after_request(true))
      return false;

    return true;
  }
}
