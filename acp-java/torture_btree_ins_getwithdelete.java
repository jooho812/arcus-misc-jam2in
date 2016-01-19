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
import java.util.Map;

import net.spy.memcached.collection.ByteArrayBKey;
import net.spy.memcached.collection.ElementFlagFilter;
import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.CollectionOverflowAction;
import net.spy.memcached.collection.Element;
import net.spy.memcached.collection.ElementValueType;
import net.spy.memcached.internal.CollectionFuture;

public class torture_btree_ins_getwithdelete implements client_profile {
  public boolean do_test(client cli) {
    try {
      if (!do_btree_test(cli))
        return false;
    } catch (Exception e) {
      System.out.printf("client_profile exception. id=%d exception=%s\n", 
                        cli.id, e.toString());
      e.printStackTrace();
    }
    return true;
  }
  
  // create a btree and insert elements, get(withDelete=true)

  public boolean do_btree_test(client cli) throws Exception {
    // Pick a key
    String key = cli.ks.get_key();

    String[] temp = key.split("-");
    long base = Long.parseLong(temp[1]);
    base = base * 64*1024;

    // Create a btree item
    if (!cli.before_request(false))
      return false;
    byte[] eflag = ("EFLAG").getBytes();
    ElementFlagFilter filter =
      new ElementFlagFilter(ElementFlagFilter.CompOperands.Equal,
                            ("EFLAG").getBytes());
    ElementValueType vtype = ElementValueType.BYTEARRAY;
    CollectionAttributes attr = 
      new CollectionAttributes(cli.conf.client_exptime,
                               new Long(cli.conf.ins_element_size),
                               CollectionOverflowAction.smallest_trim);
    CollectionFuture<Boolean> fb = cli.next_ac.asyncBopCreate(key, vtype, attr);
    boolean ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    if (!ok) {
      System.out.printf("bop create failed. id=%d key=%s: %s\n", cli.id,
                        key, fb.getOperationStatus().getResponse());
    }
    if (!cli.after_request(ok, false))
      return false;
    if (!ok)
      return true;

    // Insert elements
    for (long bkey = base; bkey < base + cli.conf.ins_element_size; bkey++) {
      if (!cli.before_request(false))
        return false;
      byte[] val = cli.vset.get_value();
      assert(val.length <= 4096);
      fb = cli.next_ac.asyncBopInsert(key, bkey, eflag /* eflag */,
                                      val,
                                      null /* Do not auto-create item */);
      ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("bop insert failed. id=%d key=%s bkey=%d: %s\n",
                          cli.id, key, bkey,
                          fb.getOperationStatus().getResponse());
      }
      if (!cli.after_request(ok, false))
        return false;
      if (!ok)
        return true;
    }

    // Delete elements
    {
        if (!cli.before_request())
          return false;

        long bkey = base + 1;
        long bkey_to = base + cli.conf.act_element_size;
        CollectionFuture<Map<Long, Element<Object>>> f =
          cli.next_ac.asyncBopGet(key, bkey, bkey_to, filter,
                                  0, cli.conf.act_element_size, true, true);
        Map<Long, Element<Object>> val =
          f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
        if (val == null || val.size() <= 0) {
          System.out.printf("Collection_Btree: BopGet failed." +
                            " id=%d key=%s val.size=%d\n", cli.id,
                            key, val == null ? -1 : 0);
        }
        if (!cli.after_request(true))
          return false;
    }
   
    return true;
  }
}
