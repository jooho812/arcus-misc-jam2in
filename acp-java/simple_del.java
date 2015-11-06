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
import java.util.Random;

public class simple_del implements client_profile {

  String DEFAULT_PREFIX = "arcustest-";
  int KeyLen = 20;
  char[] dummystring = 
    ("1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
     "abcdefghijlmnopqrstuvwxyz").toCharArray();
  Random random = new Random(); // repeatable is okay

  public boolean do_test(client cli) {
    try {
      if (!do_simple_test(cli))
		return false;
	} catch (Exception e) {
      cli.after_request(false);
	}
	return true;
  }

  public boolean do_simple_test(client cli) throws Exception {

	String key = cli.ks.get_key();
	byte[] val = cli.vset.get_value();

	// SET
	Future<Boolean> fb = 
	  cli.next_ac.set(key, cli.conf.client_exptime, val);
	boolean ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
	if (!cli.after_request(ok))
	  return false;

	// DELETE
	fb = cli.next_ac.delete(key);
	ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
	if (!cli.after_request(ok))
	  return false;


	return true;

  }

}