/*
 * Copyright 2016 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.padlockpresenter;

import com.pyamsoft.padlockmodel.sql.PadLockEntry;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import static com.pyamsoft.padlock.TestUtils.expected;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class) @Config(sdk = 23, constants = BuildConfig.class)
public class PadLockDBTest {

  private PadLockDBImpl db;
  private Scheduler subscribeScheduler;
  private Scheduler observeScheduler;

  @Before public void setup() {
    db = new PadLockDBImpl(RuntimeEnvironment.application, Schedulers.immediate());
    subscribeScheduler = Schedulers.immediate();
    observeScheduler = Schedulers.immediate();
  }

  /**
   * Inserts entry into DB when we dont care about the result
   */
  private void insertDummy(int number) {
    insertDummy("TEST", String.valueOf(number));
  }

  /**
   * Inserts entry into DB when we dont care about the result
   */
  private void insertDummy(String className) {
    insertDummy("TEST", className);
  }

  /**
   * Inserts entry into DB when we dont care about the result
   */
  private void insertDummy(String packageName, String className) {
    db.insert(packageName, className, null, 0, 0, false, false)
        .subscribeOn(subscribeScheduler)
        .observeOn(observeScheduler)
        .subscribe();
  }

  /**
   * Normal insert
   *
   * @throws Exception
   */
  @Test public void testInsert() throws Exception {
    // Normal inserts
    TestSubscriber<Long> testSubscriber = new TestSubscriber<>();
    db.insert("TEST", "TEST", null, 0, 0, false, false)
        .subscribeOn(subscribeScheduler)
        .observeOn(observeScheduler)
        .subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValueCount(1);
    testSubscriber.assertValue(1L);
  }

  /**
   * Null inserts should fail
   *
   * @throws Exception
   */
  @Test public void testInsertNull() throws Exception {
    try {
      //noinspection ConstantConditions
      db.insert(null, null, null, 0, 0, false, false)
          .subscribeOn(subscribeScheduler)
          .observeOn(observeScheduler)
          .subscribe();
      fail("Should have thrown an NullPointerException");
    } catch (NullPointerException e) {
      expected("NullPointerException in testInsert with NULL information");
    }
  }

  /**
   * Empty inserts should fail
   *
   * @throws Exception
   */
  @Test public void testInsertEmpty() throws Exception {
    try {
      final PadLockEntry empty = PadLockEntry.empty();
      db.insert(empty.packageName(), empty.activityName(), empty.lockCode(), empty.lockUntilTime(),
          empty.ignoreUntilTime(), empty.systemApplication(), empty.whitelist())
          .subscribeOn(subscribeScheduler)
          .observeOn(observeScheduler)
          .subscribe();
      fail("Should have thrown RuntimeException");
    } catch (RuntimeException e) {
      expected("RuntimeException in testInsert with NULL information");
    }
  }

  /**
   * Duplicate inserts should always delete the entry if it exists before inserting again
   *
   * @throws Exception
   */
  @Test public void testInsertDuplicates() throws Exception {
    TestSubscriber<Long> testSubscriber;
    for (int i = 0; i < 50; ++i) {
      testSubscriber = new TestSubscriber<>();
      db.insert("TEST", "TEST", null, 0, 0, false, false)
          .subscribeOn(subscribeScheduler)
          .observeOn(observeScheduler)
          .subscribe(testSubscriber);
      testSubscriber.assertNoErrors();
      testSubscriber.assertValueCount(1);
      testSubscriber.assertValue(1L);
    }
  }

  /**
   * A delete even when entry does not exist should not fail
   *
   * @throws Exception
   */
  @Test public void testDeleteNothing() throws Exception {
    // Normal deletes do not fail when nothing is deleted
    TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();
    db.deleteWithPackageActivityName("TEST", "1")
        .subscribeOn(subscribeScheduler)
        .observeOn(observeScheduler)
        .subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValueCount(1);
    testSubscriber.assertValue(0);
  }

  /**
   * Normal delete
   *
   * @throws Exception
   */
  @Test public void testDelete() throws Exception {
    insertDummy(1);
    // Test actual delete
    TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();
    db.deleteWithPackageActivityName("TEST", "1")
        .subscribeOn(subscribeScheduler)
        .observeOn(observeScheduler)
        .subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValueCount(1);
    testSubscriber.assertValue(1);
  }

  /**
   * Unguarded deletes must have someone else call open and close for them
   *
   * @throws Exception
   */
  @Test public void testDeleteUnguarded() throws Exception {
    insertDummy(1);
    // Deletes will not fail, but they are only meant to be used internally
    TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();

    // db instance is not initialized in unguarded

    try {
      db.deleteWithPackageActivityNameUnguarded("TEST", "1")
          .subscribeOn(subscribeScheduler)
          .observeOn(observeScheduler)
          .subscribe(testSubscriber);
    } catch (NullPointerException e) {
      expected("Database is NULL when unguarded", e);
    }
  }

  /**
   * Make sure delete all actually deletes, all
   *
   * @throws Exception
   */
  @Test public void testDeleteAll() throws Exception {
    for (int i = 0; i < 50; ++i) {
      insertDummy(i);
    }

    // Deletes will not fail, but they are only meant to be used internally
    TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();
    db.deleteAll()
        .subscribeOn(subscribeScheduler)
        .observeOn(observeScheduler)
        .subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValueCount(1);
  }

  /**
   * Test that query all returns a list of items with all items in it
   *
   * @throws Exception
   */
  @Test public void testQueryAll() throws Exception {
    for (int i = 0; i < 50; ++i) {
      insertDummy(i);
    }

    TestSubscriber<List<PadLockEntry.AllEntries>> testSubscriber = new TestSubscriber<>();
    db.queryAll().first().
        subscribeOn(subscribeScheduler).observeOn(observeScheduler).subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValueCount(1);

    TestSubscriber<PadLockEntry.AllEntries> flatTestSubscriber = new TestSubscriber<>();
    db.queryAll().first().flatMap(Observable::from).
        subscribeOn(subscribeScheduler).observeOn(observeScheduler).subscribe(flatTestSubscriber);
    flatTestSubscriber.assertNoErrors();
    flatTestSubscriber.assertValueCount(50);
  }

  /**
   * Test that query returns all items of a given package name and only for said package name
   *
   * @throws Exception
   */
  @Test public void testQueryWithPackageName() throws Exception {
    for (int i = 0; i < 50; ++i) {
      insertDummy(i);
    }

    TestSubscriber<List<PadLockEntry.WithPackageName>> testSubscriber = new TestSubscriber<>();
    db.queryWithPackageName("TEST").first().
        subscribeOn(subscribeScheduler).observeOn(observeScheduler).subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValueCount(1);

    TestSubscriber<PadLockEntry.WithPackageName> flatTestSubscriber = new TestSubscriber<>();
    db.queryWithPackageName("TEST").first().flatMap(Observable::from).
        subscribeOn(subscribeScheduler).observeOn(observeScheduler).subscribe(flatTestSubscriber);
    flatTestSubscriber.assertNoErrors();
    flatTestSubscriber.assertValueCount(50);

    testSubscriber = new TestSubscriber<>();
    db.queryWithPackageName("NOT HERE").first().
        subscribeOn(subscribeScheduler).observeOn(observeScheduler).subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValueCount(1);
    assertTrue(testSubscriber.getOnNextEvents().get(0).isEmpty());

    flatTestSubscriber = new TestSubscriber<>();
    db.queryWithPackageName("NOT HERE").first().flatMap(Observable::from).
        subscribeOn(subscribeScheduler).observeOn(observeScheduler).subscribe(flatTestSubscriber);
    flatTestSubscriber.assertNoErrors();
    flatTestSubscriber.assertValueCount(0);
  }

  /**
   * Test that query returns an item for every query, and the item is EMPTY if the entry did not
   * exist
   *
   * @throws Exception
   */
  @Test public void testQueryWithPackageActivityName() throws Exception {
    for (int i = 0; i < 50; ++i) {
      insertDummy(i);
    }

    TestSubscriber<PadLockEntry.WithPackageActivityName> testSubscriber = new TestSubscriber<>();
    db.queryWithPackageActivityName("TEST", "1").first().
        subscribeOn(subscribeScheduler).observeOn(observeScheduler).subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValueCount(1);
    assertFalse(
        PadLockEntry.WithPackageActivityName.isEmpty(testSubscriber.getOnNextEvents().get(0)));

    testSubscriber = new TestSubscriber<>();
    db.queryWithPackageActivityName("TEST", "10").first().
        subscribeOn(subscribeScheduler).observeOn(observeScheduler).subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValueCount(1);
    assertFalse(
        PadLockEntry.WithPackageActivityName.isEmpty(testSubscriber.getOnNextEvents().get(0)));

    testSubscriber = new TestSubscriber<>();
    db.queryWithPackageActivityName("TEST", "25").first().
        subscribeOn(subscribeScheduler).observeOn(observeScheduler).subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValueCount(1);
    assertFalse(
        PadLockEntry.WithPackageActivityName.isEmpty(testSubscriber.getOnNextEvents().get(0)));

    testSubscriber = new TestSubscriber<>();
    db.queryWithPackageActivityName("NOT HERE", "0").first().
        subscribeOn(subscribeScheduler).observeOn(observeScheduler).subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValueCount(1);
    assertTrue(
        PadLockEntry.WithPackageActivityName.isEmpty(testSubscriber.getOnNextEvents().get(0)));
  }

  /**
   * Test that the default behavior will return the PACKAGE entry when the specifically named
   * activity entry does not exist, and EMPTY when no entries exist
   *
   * @throws Exception
   */
  @Test public void testQueryWithPackageActivityNameDefault() throws Exception {
    TestSubscriber<PadLockEntry> testSubscriber = new TestSubscriber<>();
    db.queryWithPackageActivityNameDefault("TEST", "1").first().
        subscribeOn(subscribeScheduler).observeOn(observeScheduler).subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValueCount(1);
    assertTrue(PadLockEntry.isEmpty(testSubscriber.getOnNextEvents().get(0)));

    insertDummy(PadLockEntry.PACKAGE_ACTIVITY_NAME);
    insertDummy(1);
    insertDummy(2);

    testSubscriber = new TestSubscriber<>();
    db.queryWithPackageActivityNameDefault("TEST", "1").first().
        subscribeOn(subscribeScheduler).observeOn(observeScheduler).subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValueCount(1);
    assertFalse(PadLockEntry.isEmpty(testSubscriber.getOnNextEvents().get(0)));
    assertEquals("1", testSubscriber.getOnNextEvents().get(0).activityName());

    testSubscriber = new TestSubscriber<>();
    db.queryWithPackageActivityNameDefault("TEST", "2").first().
        subscribeOn(subscribeScheduler).observeOn(observeScheduler).subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValueCount(1);
    assertFalse(PadLockEntry.isEmpty(testSubscriber.getOnNextEvents().get(0)));
    assertEquals("2", testSubscriber.getOnNextEvents().get(0).activityName());

    testSubscriber = new TestSubscriber<>();
    db.queryWithPackageActivityNameDefault("TEST", "3").first().
        subscribeOn(subscribeScheduler).observeOn(observeScheduler).subscribe(testSubscriber);
    testSubscriber.assertNoErrors();
    testSubscriber.assertValueCount(1);
    assertFalse(PadLockEntry.isEmpty(testSubscriber.getOnNextEvents().get(0)));
    assertEquals(PadLockEntry.PACKAGE_ACTIVITY_NAME,
        testSubscriber.getOnNextEvents().get(0).activityName());
  }

  // TODO Add test for updates, clearing and then accessing again afterwards
}