/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.script.api.tbel;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mvel2.ConversionException;
import org.thingsboard.common.util.JacksonUtil;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@Slf4j
class TbDateTest {

    ListeningExecutorService executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * Note: This test simulated the high concurrency calls.
     * But it not always fails before as the concurrency issue happens far away from the test subject, inside the SimpleDateFormat class.
     * Few calls later after latch open, the concurrency issue is not well reproduced.
     * Depends on environment some failure may happen each 2 or 100 runs.
     * To be highly confident run this test in a ForkMode=method, repeat 100 times. This will provide about 99 failures per 100 runs.
     * The value of this test is *never* to fail when isoDateFormat.format(this) is properly synchronized
     * If this test fails time-to-time -- it is a sign that isoDateFormat.format() called concurrently and have to be fixed (synchronized)
     * The expected exception example:
     *   Caused by: java.lang.ArrayIndexOutOfBoundsException: Index 14 out of bounds for length 13
     * 	     at java.base/sun.util.calendar.BaseCalendar.getCalendarDateFromFixedDate(BaseCalendar.java:457)
     * 	     at java.base/java.util.GregorianCalendar.computeFields(GregorianCalendar.java:2394)
     * 	     at java.base/java.util.GregorianCalendar.computeFields(GregorianCalendar.java:2309)
     * 	     at java.base/java.util.Calendar.setTimeInMillis(Calendar.java:1834)
     * 	     at java.base/java.util.Calendar.setTime(Calendar.java:1800)
     * 	     at java.base/java.text.SimpleDateFormat.format(SimpleDateFormat.java:974)
     * Tests were checked with:
     *      -timezone Etc/GMT-14
     *      -timezone Etc/GMT+12
     *      -timezone America/New_York
     *      -timezone Europe/Kyiv
     */
    @Test
    void testToISOStringConcurrently() throws ExecutionException, InterruptedException, TimeoutException {
        int threads = 5;
        executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(threads));
        for (int j = 0; j < 1000; j++) {
            final int iteration = j;
            CountDownLatch readyLatch = new CountDownLatch(threads);
            CountDownLatch latch = new CountDownLatch(1);
            long now = 1709217342000L;
            List<ListenableFuture<String>> futures = new ArrayList<>(threads);
            for (int i = 0; i < threads; i++) {
                long ts = now + TimeUnit.DAYS.toMillis(i * 366) + TimeUnit.MINUTES.toMillis(iteration) + TimeUnit.SECONDS.toMillis(iteration) + iteration;
                TbDate tbDate = new TbDate(ts);
                futures.add(executor.submit(() -> {
                    readyLatch.countDown();
                    if (!latch.await(30, TimeUnit.SECONDS)) {
                        throw new RuntimeException("await timeout");
                    }
                    return tbDate.toISOString();
                }));
            }
            ListenableFuture<List<String>> future = Futures.allAsList(futures);
            Futures.addCallback(future, new FutureCallback<List<String>>() {
                @Override
                public void onSuccess(List<String> result) {

                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Failure happens on iteration {}", iteration);
                }
            }, MoreExecutors.directExecutor());
            readyLatch.await(30, TimeUnit.SECONDS);
            latch.countDown();
            future.get(30, TimeUnit.SECONDS);
        }
    }

    @Test
    void testToISOStringThreadLocalStaticFormatter() throws ExecutionException, InterruptedException, TimeoutException {
        executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
        int hrs = 14;
        int date = 29;
        int month = 2;
        String pattern = "2024-%s-%sT%s:35:42.987Z";
        String tsStr = String.format(pattern, String.format("%02d", month), date, hrs);  //Thu Feb 29 2024 14:35:42.987 GMT+0000
        TbDate tbDate = new TbDate(tsStr);
        long ts = 1709217342987L; //Thu Feb 29 2024 14:35:42.987 GMT+0000
        Assert.assertEquals(ts, tbDate.parseSecondMilli());
        int offsetLocalSecond = tbDate.getLocaleZoneOffset().getTotalSeconds(); // for example 7200 for GMT + 2

        String datePrefix = tsStr; //without time zone
        assertThat(tbDate.toISOString())
                .as("format in main thread")
                .startsWith(datePrefix);
        assertThat(executor.submit(tbDate::toISOString).get(30, TimeUnit.SECONDS))
                .as("format in executor thread")
                .startsWith(datePrefix);
        TbDateTestEntity tbDateTest = new TbDateTestEntity(tbDate.getFullYear(), month, date,hrs + offsetLocalSecond/60/60);
        String datePrefixLocal = String.format(pattern, tbDateTest.geMonthStr(), tbDateTest.geDateStr(), tbDateTest.geHoursStr());
        long offsetLocalMilli = offsetLocalSecond*1000;
        assertThat(new TbDate(ts + offsetLocalMilli).toISOString())
                .as("new instance format in main thread")
                .startsWith(datePrefixLocal);
        assertThat(executor.submit(() -> new TbDate(ts + offsetLocalMilli).toISOString()).get(30, TimeUnit.SECONDS))
                .as("new instance format in executor thread")
                .startsWith(datePrefixLocal);
    }

    @Test
    void testToLocaleDateString() {
        String s = "09:15:30 PM, Sun 10/09/2022";
        String pattern = "hh:mm:ss a, EEE M/d/uuuu";
        TbDate d = new TbDate(s, pattern, Locale.US);
        Assert.assertEquals("2022-10-09T21:15:30Z", d.toISOString());
            // tz = "-04:00"
        d = new TbDate(s, pattern, Locale.US, "-04:00");
        Assert.assertEquals("2022-10-10T01:15:30Z", d.toISOString());
        d = new TbDate(s, pattern, Locale.US, "America/New_York");
        Assert.assertEquals("2022-10-10T01:15:30Z", d.toISOString());
            // tz = "+02:00"
        /**
         * For Java 11:
         * String[2] { "vorm.", "nachm." }
         * For Java 17:
         * `{ "AM", "PM" }`
         */
        s = "09:15:30 nachm., So. 10/09/2022";
        d = new TbDate(s, pattern, Locale.GERMAN, ZoneId.of("Europe/Berlin"));
        Assert.assertEquals("2022-10-09T19:15:30Z", d.toISOString());

        s = "09:15:30 пп, середа, 4 жовтня 2023 р.";
        pattern = "hh:mm:ss a, EEEE, d MMMM y 'р.'";
        d = new TbDate(s, pattern, Locale.forLanguageTag("uk-UA"));
        Assert.assertEquals("2023-10-04T21:15:30Z", d.toISOString());

        d = new TbDate(1693962245000L);
        Assert.assertEquals("2023-09-06T01:04:05Z", d.toISOString());

        // Depends on time zone, so we just check it works;
        Assert.assertNotNull(d.toLocaleDateString());
        Assert.assertNotNull(d.toLocaleDateString("en-US"));

        Assert.assertEquals("9/5/23", d.toLocaleDateString("en-US", "America/New_York"));
        Assert.assertEquals("23. 9. 5.", d.toLocaleDateString("ko-KR",  "America/New_York"));
        Assert.assertEquals("06.09.23",  d.toLocaleDateString( "uk-UA", "Europe/Kiev"));
        Assert.assertEquals("5\u200F/9\u200F/2023",  d.toLocaleDateString( "ar-EG", "America/New_York"));

        Assert.assertEquals("Tuesday, September 5, 2023", d.toLocaleDateString("en-US", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("dateStyle", "full")
                .toString()));
        Assert.assertEquals("2023년 9월 5일 화요일", d.toLocaleDateString("ko-KR", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("dateStyle", "full")
                .toString()));
        Assert.assertEquals("середа, 6 вересня 2023 р.", d.toLocaleDateString("uk-UA", JacksonUtil.newObjectNode()
                .put("timeZone", "Europe/Kiev")
                .put("dateStyle", "full")
                .toString()));
        Assert.assertEquals("الثلاثاء، 5 سبتمبر 2023", d.toLocaleDateString("ar-EG", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("dateStyle", "full")
                .toString()));

        Assert.assertEquals("Tuesday 9/5/2023", d.toLocaleDateString("en-US", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "EEEE M/d/yyyy")
                .toString()));
        Assert.assertEquals("화요일 9/5/2023", d.toLocaleDateString("ko-KR", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "EEEE M/d/yyyy")
                .toString()));
        Assert.assertEquals("середа 9/6/2023", d.toLocaleDateString("uk-UA", JacksonUtil.newObjectNode()
                .put("timeZone", "Europe/Kiev")
                .put("pattern", "EEEE M/d/yyyy")
                .toString()));
        Assert.assertEquals("الثلاثاء 9/5/2023", d.toLocaleDateString("ar-EG", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "EEEE M/d/yyyy")
                .toString()));
    }

    @Test
    void testToLocaleTimeString() {
        TbDate d = new TbDate(1693962245000L);

        // Depends on time zone, so we just check it works;
        Assert.assertNotNull(d.toLocaleTimeString());
        Assert.assertNotNull(d.toLocaleTimeString("en-US"));

        Assert.assertEquals("9:04:05 PM", d.toLocaleTimeString("en-US", "America/New_York"));
        Assert.assertEquals("오후 9:04:05", d.toLocaleTimeString("ko-KR",  "America/New_York"));
        Assert.assertEquals("04:04:05",  d.toLocaleTimeString( "uk-UA", "Europe/Kiev"));
        Assert.assertEquals("9:04:05 م",  d.toLocaleTimeString( "ar-EG", "America/New_York"));

        Assert.assertEquals("9:04:05 PM Eastern Daylight Time", d.toLocaleTimeString("en-US", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("timeStyle", "full")
                .toString()));
        Assert.assertEquals("오후 9시 4분 5초 미 동부 하계 표준시", d.toLocaleTimeString("ko-KR", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("timeStyle", "full")
                .toString()));
        Assert.assertEquals("04:04:05 за східноєвропейським літнім часом", d.toLocaleTimeString("uk-UA", JacksonUtil.newObjectNode()
                .put("timeZone", "Europe/Kiev")
                .put("timeStyle", "full")
                .toString()));
        Assert.assertEquals("9:04:05 م التوقيت الصيفي الشرقي لأمريكا الشمالية", d.toLocaleTimeString("ar-EG", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("timeStyle", "full")
                .toString()));

        Assert.assertEquals("9:04:05 PM", d.toLocaleTimeString("en-US", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "h:mm:ss a")
                .toString()));
        Assert.assertEquals("9:04:05 오후", d.toLocaleTimeString("ko-KR", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "h:mm:ss a")
                .toString()));
        Assert.assertEquals("4:04:05 дп", d.toLocaleTimeString("uk-UA", JacksonUtil.newObjectNode()
                .put("timeZone", "Europe/Kiev")
                .put("pattern", "h:mm:ss a")
                .toString()));
        Assert.assertEquals("9:04:05 م", d.toLocaleTimeString("ar-EG", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "h:mm:ss a")
                .toString()));
    }

    @Test
    void testToLocaleString() {
        TbDate d = new TbDate(1693962245000L);

        // Depends on time zone, so we just check it works;
        Assert.assertNotNull(d.toLocaleString());
        Assert.assertNotNull(d.toLocaleString("en-US"));

        Assert.assertEquals("9/5/23, 9:04:05 PM", d.toLocaleString("en-US", "America/New_York"));
        Assert.assertEquals("23. 9. 5. 오후 9:04:05", d.toLocaleString("ko-KR",  "America/New_York"));
        Assert.assertEquals("06.09.23, 04:04:05",  d.toLocaleString( "uk-UA", "Europe/Kiev"));
        Assert.assertEquals("5\u200F/9\u200F/2023 9:04:05 م",  d.toLocaleString( "ar-EG", "America/New_York"));

        Assert.assertEquals("Tuesday, September 5, 2023 at 9:04:05 PM Eastern Daylight Time", d.toLocaleString("en-US", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("dateStyle", "full")
                .put("timeStyle", "full")
                .toString()));
        Assert.assertEquals("2023년 9월 5일 화요일 오후 9시 4분 5초 미 동부 하계 표준시", d.toLocaleString("ko-KR", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("dateStyle", "full")
                .put("timeStyle", "full")
                .toString())); 
        Assert.assertEquals("середа, 6 вересня 2023 р. о 04:04:05 за східноєвропейським літнім часом", d.toLocaleString("uk-UA", JacksonUtil.newObjectNode()
                .put("timeZone", "Europe/Kiev")
                .put("dateStyle", "full")
                .put("timeStyle", "full")
                .toString())); 
        Assert.assertEquals("الثلاثاء، 5 سبتمبر 2023 9:04:05 م التوقيت الصيفي الشرقي لأمريكا الشمالية", d.toLocaleString("ar-EG", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("dateStyle", "full")
                .put("timeStyle", "full")
                .toString())); 

        Assert.assertEquals("9/5/2023, 9:04:05 PM", d.toLocaleString("en-US", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "M/d/yyyy, h:mm:ss a")
                .toString())); 
        Assert.assertEquals("9/5/2023, 9:04:05 오후", d.toLocaleString("ko-KR", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "M/d/yyyy, h:mm:ss a")
                .toString())); 
        Assert.assertEquals("9/6/2023, 4:04:05 дп", d.toLocaleString("uk-UA", JacksonUtil.newObjectNode()
                .put("timeZone", "Europe/Kiev")
                .put("pattern", "M/d/yyyy, h:mm:ss a")
                .toString())); 
        Assert.assertEquals("9/5/2023, 9:04:05 م", d.toLocaleString("ar-EG", JacksonUtil.newObjectNode()
                .put("timeZone", "America/New_York")
                .put("pattern", "M/d/yyyy, h:mm:ss a")
                .toString())); 
    }

    @Test
    void TestFromString () {
        String stringDateUTC = "2023-09-06T01:04:05.00Z";
        TbDate d = new TbDate(stringDateUTC);
        Assert.assertEquals("2023-09-06T01:04:05Z", d.toISOString());
        String stringDateTZ = "2023-09-06T01:04:05.00+04:00:00";
        d = new TbDate(stringDateTZ);
        Assert.assertEquals("2023-09-05T21:04:05Z", d.toISOString());
        stringDateTZ = "2023-09-06T01:04:05.00+04";
        d = new TbDate(stringDateTZ);
        Assert.assertEquals("2023-09-05T21:04:05Z", d.toISOString());
        stringDateTZ = "2023-09-06T01:04:05.00-04:00";
        d = new TbDate(stringDateTZ);
        Assert.assertEquals("2023-09-06T05:04:05Z", d.toISOString());
       stringDateTZ = "2023-09-06T01:04:05.00+04:30:56";
        d = new TbDate(stringDateTZ);
        Assert.assertEquals("2023-09-05T20:33:09Z", d.toISOString());
        stringDateTZ = "2023-09-06T01:04:05.00-02:00";
        d = new TbDate(stringDateTZ);
        Assert.assertEquals("2023-09-06T03:04:05Z", d.toISOString());
        String stringDateRFC_1123  = "Sat, 3 Jun 2023 11:05:30 GMT";
        d = new TbDate(stringDateRFC_1123);
        Assert.assertEquals("2023-06-03T11:05:30Z", d.toISOString());
        stringDateRFC_1123  = "Sat, 3 Jun 2023 01:04:05 +043056";
        d = new TbDate(stringDateRFC_1123);
        Assert.assertEquals("2023-06-02T20:33:09Z", d.toISOString());
       stringDateRFC_1123  = "Sat, 3 Jun 2023 11:05:30 +0400";
        d = new TbDate(stringDateRFC_1123);
        Assert.assertEquals("2023-06-03T07:05:30Z", d.toISOString());
        stringDateRFC_1123  = "Thu, 29 Feb 2024 11:05:30 -03";
        d = new TbDate(stringDateRFC_1123);
        Assert.assertEquals("2024-02-29T14:05:30Z", d.toISOString());

        String stringDateZ_error  = "2023-09-06T01:04:05.00+045";
        Exception actual = assertThrows(ConversionException.class, () -> {
            new TbDate(stringDateZ_error);
        });
        String expectedMessage = "Cannot parse value";
        assertTrue(actual.getMessage().contains(expectedMessage));

       String stringDateRFC_1123_error  = "Tue, 3 Jun 2023 11:05:30 GMT";
       actual = assertThrows(ConversionException.class, () -> {
            new TbDate(stringDateRFC_1123_error);
        });
       assertTrue(actual.getMessage().contains(expectedMessage));
    }
    @Test
    void TestParse () {
        String stringDateUTC = "2023-09-06T01:04:05.345Z";
        TbDate d = new TbDate(stringDateUTC);
        Assert.assertEquals(1693962245345L, d.parseSecondMilli());
        Assert.assertEquals(1693962245L, d.parseSecond());
        String stringDateStart = "1970-01-01T00:00:00Z";
        d = new TbDate(stringDateStart);
        long actualMillis = TbDate.parse("1970-01-01 T00:00:00");
        Assert.assertEquals(-d.getLocaleZoneOffset().getTotalSeconds() * 1000, actualMillis);
        String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
        String stringDate = "1995-12-04 00:12:00.000";
        Assert.assertNotEquals(-1L,  TbDate.parse(stringDate, pattern));
    }

    @Test
    void TestMethodGetAsDateTimeLocal() {
        TbDate d = new TbDate(1975, 12, 31, 23,15,30, 560);
        TbDate d0 = new TbDate(1975, 12, 31, 23,15,30, 560,"UTC");
        TbDate d1 = new TbDate(1975, 12, 31, 23,15,30, 561,"+03:00");
        TbDate d2 = new TbDate(1975, 12, 31, 23,15,30, 562,"-02:00");
        TbDate dLocal = new TbDate(d.parseSecondMilli() + d.getLocaleZoneOffset().getTotalSeconds()*1000);
        TbDate dLocal0 = new TbDate(d0.parseSecondMilli() + d0.getLocaleZoneOffset().getTotalSeconds()*1000);
        TbDate dLocal1 = new TbDate(d1.parseSecondMilli() + d1.getLocaleZoneOffset().getTotalSeconds()*1000);
        TbDate dLocal2 = new TbDate(d2.parseSecondMilli() + d2.getLocaleZoneOffset().getTotalSeconds()*1000);

        Assert.assertEquals(dLocal.getUTCFullYear(), d.getFullYear());
        Assert.assertEquals(dLocal0.getUTCFullYear(), d0.getFullYear());
        Assert.assertEquals(dLocal1.getUTCFullYear(), d1.getFullYear());
        Assert.assertEquals(dLocal2.getUTCFullYear(), d2.getFullYear());

        Assert.assertEquals(dLocal.getUTCMonth(), d.getMonth());
        Assert.assertEquals(dLocal0.getUTCMonth(), d0.getMonth());
        Assert.assertEquals(dLocal1.getUTCMonth(), d1.getMonth());
        Assert.assertEquals(dLocal2.getUTCMonth(), d2.getMonth());

        Assert.assertEquals(dLocal.getUTCDate(), d.getDate());
        Assert.assertEquals(dLocal0.getUTCDate(), d0.getDate());
        Assert.assertEquals(dLocal1.getUTCDate(), d1.getDate());
        Assert.assertEquals(dLocal2.getUTCDate(), d2.getDate());

        Assert.assertEquals(dLocal.getUTCDay(), d.getDay());
        Assert.assertEquals(dLocal0.getUTCDay(), d0.getDay());
        Assert.assertEquals(dLocal1.getUTCDay(), d1.getDay());
        Assert.assertEquals(dLocal2.getUTCDay(), d2.getDay());

        Assert.assertEquals(dLocal.getUTCHours(), d.getHours());
        Assert.assertEquals(dLocal0.getUTCHours(), d0.getHours());
        Assert.assertEquals(dLocal1.getUTCHours(), d1.getHours());
        Assert.assertEquals(dLocal2.getUTCHours(), d2.getHours());

        Assert.assertEquals(dLocal.getUTCMinutes(), d.getMinutes());
        Assert.assertEquals(dLocal0.getUTCMinutes(), d0.getMinutes());
        Assert.assertEquals(dLocal1.getUTCMinutes(), d1.getMinutes());
        Assert.assertEquals(dLocal2.getUTCMinutes(), d2.getMinutes());

        Assert.assertEquals(dLocal.getUTCSeconds(), d.getSeconds());
        Assert.assertEquals(dLocal0.getUTCSeconds(), d0.getSeconds());
        Assert.assertEquals(dLocal1.getUTCSeconds(), d1.getSeconds());
        Assert.assertEquals(dLocal2.getUTCSeconds(), d2.getSeconds());

        Assert.assertEquals(dLocal.getUTCMilliseconds(), d.getMilliseconds());
        Assert.assertEquals(dLocal0.getUTCMilliseconds(), d0.getMilliseconds());
        Assert.assertEquals(dLocal1.getUTCMilliseconds(), d1.getMilliseconds());
        Assert.assertEquals(dLocal2.getUTCMilliseconds(), d2.getMilliseconds());
    }

    @Test
    void Test_Year_Moth_Date_Hours_Min_Sec_Without_TZ() {
        TbDate d = new TbDate(2023, 8, 18);
        Assert.assertEquals("2023-08-18 00:00:00", d.toLocaleString());
        d = new TbDate(2023, 9, 17, 17, 34);
        Assert.assertEquals("2023-09-17 17:34:00", d.toLocaleString());
        d = new TbDate(23, 9, 7, 8, 4);
        Assert.assertEquals("2023-09-07 08:04:00", d.toLocaleString());
        d = new TbDate(23, 9, 7, 8, 4, 5);
        Assert.assertEquals("2023-09-07 08:04:05", d.toLocaleString());
        d = new TbDate(23, 9, 7, 8, 4, 5, 567);
        Assert.assertEquals("2023-09-07 08:04:05", d.toLocaleString());
    }

    @Test
    void Test_Get_LocalDateTime_With_TZ() {
        int hrs = 8;
        int date = 7;
        int tz = 0;
        String pattern = "2023-09-%s %s:04:05";
        String tzStr = "UTC";
        TbDate d = new TbDate(23, 9, date, hrs, 4, 5);
        int localOffsetHrs = 0;
        TbDateTestEntity tbDateTest = new TbDateTestEntity(23, 9, date, hrs + localOffsetHrs - tz);
        String expected = String.format(pattern, tbDateTest.geDateStr(), tbDateTest.geHoursStr());
        Assert.assertEquals(expected, d.toLocaleString());

        d = new TbDate(23, 9, date, hrs, 4, 5, tzStr);
        localOffsetHrs = ZoneId.systemDefault().getRules().getOffset(d.getInstant()).getTotalSeconds()/60/60;
        tbDateTest = new TbDateTestEntity(23, 9, date, hrs + localOffsetHrs - tz);
        expected = String.format(pattern, tbDateTest.geDateStr(), tbDateTest.geHoursStr());
        Assert.assertEquals(expected, d.toLocaleString());

        tz = 3;
        tzStr = "+03:00";
        d = new TbDate(23, 9, date, hrs, 4, 5, tzStr);
        localOffsetHrs = ZoneId.systemDefault().getRules().getOffset(d.getInstant()).getTotalSeconds()/60/60;
        tbDateTest = new TbDateTestEntity(23, 9, date, hrs + localOffsetHrs - tz);
        expected = String.format(pattern, tbDateTest.geDateStr(), tbDateTest.geHoursStr());
        Assert.assertEquals(expected, d.toLocaleString());

        tz = -4;
        tzStr = "-04:00";
        d = new TbDate(23, 9, date, hrs, 4, 5, tzStr);
        localOffsetHrs = ZoneId.systemDefault().getRules().getOffset(d.getInstant()).getTotalSeconds()/60/60;
        tbDateTest = new TbDateTestEntity(23, 9, date, hrs + localOffsetHrs - tz);
        expected = String.format(pattern, tbDateTest.geDateStr(), tbDateTest.geHoursStr());
        Assert.assertEquals(expected, d.toLocaleString());
    }

    @Test
    public void TestToUTC() {
        Assert.assertEquals(-2209075200000L, TbDate.UTC(0));
        Assert.assertEquals("1899-12-31T00:00:00Z", new TbDate(TbDate.UTC(0)).toJSON());
        Assert.assertEquals("1996-02-02T03:04:05Z", new TbDate(TbDate.UTC(96, 2, 2, 3, 4, 5)).toJSON());
        Assert.assertEquals("2022-12-31T03:04:05.678Z", new TbDate(TbDate.UTC(22, 0, 0, 3, 4, 5, 678)).toJSON());
        Assert.assertEquals("0903-12-31T03:04:05.678Z", new TbDate(TbDate.UTC(903, 0, 0, 3, 4, 5, 678)).toJSON());
        Assert.assertEquals("1958-12-31T03:04:05.678Z", new TbDate(TbDate.UTC(1958, 0, 0, 3, 4, 5, 678)).toJSON());
        Assert.assertEquals("2032-04-05T03:04:05.678Z", new TbDate(TbDate.UTC(2032, 4, 5, 3, 4, 5, 678)).toJSON());
        Assert.assertEquals("2024-02-29T03:04:05.678Z", new TbDate(TbDate.UTC(2024, 2, 29, 3, 4, 5, 678)).toJSON());
        Exception actual = assertThrows(DateTimeException.class, () -> {
            TbDate.UTC(2023, 2, 29, 3, 4, 5, 678);
        });
        String expectedMessage = "Invalid date 'February 29' as '2023' is not a leap year";
        assertTrue(actual.getMessage().contains(expectedMessage));
    }

    @Test
    void TestMethodGetTimeUTC() {
        TbDate ddUTC = new TbDate(TbDate.UTC(1996, 1, 2, 3, 4, 5));
        Assert.assertEquals(820551845000L, ddUTC.valueOf());
        TbDate dd = new TbDate(1996, 1, 2, 3, 4, 5);
        int localOffsetMilli = ZoneId.systemDefault().getRules().getOffset(dd.getInstant()).getTotalSeconds()*1000;
        Assert.assertEquals((dd.valueOf() + localOffsetMilli), ddUTC.valueOf());

        ddUTC = new TbDate(TbDate.UTC(1969, 7, 20, 20, 17, 40));
        Assert.assertEquals(-14182940000L, ddUTC.valueOf());
        TbDate beforeStartUTC = new TbDate(1969, 7, 20, 20, 17, 40);
        localOffsetMilli = ZoneId.systemDefault().getRules().getOffset(beforeStartUTC.getInstant()).getTotalSeconds()*1000;
        Assert.assertEquals((beforeStartUTC.valueOf() + localOffsetMilli), ddUTC.valueOf());
    }

    @Test
    void TestMethodSetUTCFullYearMonthDate() {
        TbDate d1 = new TbDate(1975, 12, 31, 23,15,30, 567,"-04:00");
        TbDate d2 = new TbDate(1975, 12, 31, 23,15,30, 567,"+04:00");
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setUTCFullYear(1969);
        d2.setUTCFullYear(1969);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setUTCFullYear(1975, 5);
        d2.setUTCFullYear(2023, 11);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setUTCFullYear(2023, 2, 28);
        d2.setUTCFullYear(2023, 12, 31);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setUTCMonth(11);
        d2.setUTCMonth(2);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setUTCMonth(5, 20);
        d2.setUTCMonth(2, 8);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setUTCDate(6);
        d2.setUTCDate(15);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);
    }

    @Test
    void TestMethodSetUTCHoursMinutesSecondsMilliSec() {
        TbDate d1 = new TbDate(1975, 12, 31, 23, 15, 30, 567, "+02:00");
        TbDate d2 = new TbDate(1975, 12, 31, 23, 15, 30, 567, "-02:00");

        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setUTCHours(5);
        d2.setUTCHours(23);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setUTCHours(23, 45);
        d2.setUTCHours(1, 5);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setUTCHours(0, 12, 59);
        d2.setUTCHours(4, 45, 01);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setUTCHours(2, 32, 49, 123);
        d2.setUTCHours(8, 45, 12, 234);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setUTCMinutes(5);
        d2.setUTCMinutes(15);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setUTCMinutes(5, 34);
        d2.setUTCMinutes(25, 43);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setUTCMinutes(25, 14, 567);
        d2.setUTCMinutes(45, 3, 876);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setUTCSeconds(5);
        d2.setUTCSeconds(23);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setUTCSeconds(45, 987);
        d2.setUTCSeconds(54, 842);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setUTCMilliseconds(675);
        d1.setUTCMilliseconds(923);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);
    }
    @Test
    void TestMethodSetTime() {
        TbDate d1 = new TbDate(1975, 12, 31, 23, 15, 30, 567, "-03:00");
        long dateMilliSecond = d1.getTime();
        int fiveMinutesInMillis = 5 * 60 * 1000;
        Assert.assertEquals(15, d1.getUTCMinutes());
        Assert.assertEquals(4, d1.getUTCDay());
        d1.setTime(dateMilliSecond + fiveMinutesInMillis);
        Assert.assertEquals(20, d1.getUTCMinutes());
        Assert.assertEquals(4, d1.getUTCDay());
        d1.setTime(-378682769433L);
        Assert.assertEquals(1958, d1.getUTCFullYear());
        Assert.assertEquals(1, d1.getUTCMonth());
        Assert.assertEquals(1, d1.getUTCDate());
        Assert.assertEquals(2, d1.getUTCHours());
        Assert.assertEquals(20, d1.getUTCMinutes());
        Assert.assertEquals(30, d1.getUTCSeconds());
        Assert.assertEquals(567, d1.getUTCMilliseconds());
        Assert.assertEquals(3, d1.getUTCDay());
    }
     @Test
    void TestMethodSeFullYearMonthDate() {
         TbDate d = new TbDate(2024, 1, 1, 1, 15, 30, 567);
         testResultChangeDateTime(d);

         d = new TbDate(2023, 12, 31, 22, 15, 30, 567);
         testResultChangeDateTime(d);

         d = new TbDate(1975, 12, 31, 1, 15, 30, 567);
         testResultChangeDateTime(d);

         d.setFullYear(1969);
         testResultChangeDateTime(d);

         d.setFullYear(1975, 2);
         testResultChangeDateTime(d);

         d.setFullYear(2023, 6, 30);
         testResultChangeDateTime(d);

         d.setMonth(2);
         testResultChangeDateTime(d);

         d.setMonth(1, 24);
         testResultChangeDateTime(d);

         d.setDate(6);
         testResultChangeDateTime(d);
     }
     @Test
    void TestMethodSeHoursMinutesSecondsMilliSec() {
        TbDate d1 = new TbDate(1975, 12, 31, 23, 15, 30, 567, "+02:00");
        TbDate d2 = new TbDate(1975, 12, 31, 23, 15, 30, 567, "-02:00");
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setHours(5);
        d2.setHours(23);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setHours(23, 45);
        d2.setHours(1, 5);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setHours(0, 12, 59);
        d2.setHours(4, 45, 1);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setHours(2, 32, 49, 123);
        d2.setHours(8, 45, 12, 234);
        testResultChangeDateTime(d1);
        testResultChangeDateTime(d2);

        d1.setMinutes(5);
        d2.setMinutes(15);
        Assert.assertEquals(5, d1.getMinutes());
        Assert.assertEquals(15, d2.getMinutes());

        d1.setMinutes(5, 34);
        d2.setMinutes(25, 43);
        Assert.assertEquals(5, d1.getMinutes());
        Assert.assertEquals(34, d1.getSeconds());
        Assert.assertEquals(25, d2.getMinutes());
        Assert.assertEquals(43, d2.getSeconds());

        d1.setMinutes(25, 14, 567);
        d2.setMinutes(45, 3, 876);
        Assert.assertEquals(25, d1.getMinutes());
        Assert.assertEquals(14, d1.getSeconds());
        Assert.assertEquals(567, d1.getMilliseconds());
        Assert.assertEquals(45, d2.getMinutes());
        Assert.assertEquals(3, d2.getSeconds());
        Assert.assertEquals(876, d2.getMilliseconds());

        d1.setSeconds(5);
        d2.setSeconds(23);
        Assert.assertEquals(5, d1.getSeconds());
        Assert.assertEquals(23, d2.getSeconds());

        d1.setSeconds(45, 987);
        d2.setSeconds(54, 842);
        Assert.assertEquals(45, d1.getSeconds());
        Assert.assertEquals(987, d1.getMilliseconds());
        Assert.assertEquals(54, d2.getSeconds());
        Assert.assertEquals(842, d2.getMilliseconds());

        d1.setMilliseconds(675);
        d2.setMilliseconds(923);
        Assert.assertEquals(675, d1.getMilliseconds());
        Assert.assertEquals(923, d2.getMilliseconds());
    }
    @Test
    public void toStringAsJs() {
        TbDate d1 = new TbDate(1975, 12, 31, 23,15,30, 567,"-04:00");
        Assert.assertEquals("четвер, 1 січня 1976 р. о 06:15:30 за східноєвропейським стандартним часом", d1.toString("uk-UA", "Europe/Kyiv"));
        Assert.assertEquals("Thursday, January 1, 1976 at 6:15:30 AM Eastern European Standard Time", d1.toString("en-US", "Europe/Kyiv"));
        Assert.assertEquals("1976 Jan 1, Thu 06:15:30 Eastern European Time", d1.toString("UTC", "Europe/Kyiv"));
        Assert.assertEquals("Wednesday, December 31, 1975 at 10:15:30 PM Eastern Standard Time", d1.toString("en-US", "America/New_York"));
        Assert.assertEquals("1975 Dec 31, Wed 22:15:30 Eastern Standard Time", d1.toString("GMT", "America/New_York"));
        Assert.assertEquals("1975 Dec 31, Wed 22:15:30 Eastern Standard Time", d1.toString("UTC", "America/New_York"));

        Assert.assertEquals(d1.toUTCString("UTC"), d1.toUTCString());
        Assert.assertEquals("четвер, 1 січня 1976 р., 03:15:30", d1.toUTCString("uk-UA"));
        Assert.assertEquals("Thursday, January 1, 1976, 3:15:30 AM", d1.toUTCString("en-US"));

        Assert.assertEquals("1976-01-01T03:15:30.567Z", d1.toJSON());
        Assert.assertEquals("1976-01-01T03:15:30.567Z", d1.toISOString());

        Assert.assertEquals("1976-01-01 06:15:30", d1.toLocaleString("UTC", "Europe/Kyiv"));
        Assert.assertEquals("01.01.76, 06:15:30", d1.toLocaleString("uk-UA", "Europe/Kyiv"));
        Assert.assertEquals("1975-12-31 22:15:30", d1.toLocaleString("UTC", "America/New_York"));
        Assert.assertEquals("12/31/75, 10:15:30 PM", d1.toLocaleString("en-US", "America/New_York"));

        Assert.assertEquals("1976 Jan 1, Thu", d1.toDateString("UTC", "Europe/Kyiv"));
        Assert.assertEquals("четвер, 1 січня 1976 р.", d1.toDateString("uk-UA", "Europe/Kyiv"));
        Assert.assertEquals("1975 Dec 31, Wed", d1.toDateString("UTC", "America/New_York"));
        Assert.assertEquals("Wednesday, December 31, 1975", d1.toDateString("en-US", "America/New_York"));

        Assert.assertEquals("06:15:30", d1.toLocaleTimeString("uk-UA", "Europe/Kyiv"));
        Assert.assertEquals("06:15:30", d1.toLocaleTimeString("UTC", "Europe/Kyiv"));
        Assert.assertEquals("10:15:30 PM", d1.toLocaleTimeString("en-US", "America/New_York"));
        Assert.assertEquals("22:15:30", d1.toLocaleTimeString("UTC", "America/New_York"));

        Assert.assertEquals("06:15:30 за східноєвропейським стандартним часом", d1.toTimeString("uk-UA", "Europe/Kyiv"));
        Assert.assertEquals("06:15:30 Eastern European Time", d1.toTimeString("UTC", "Europe/Kyiv"));
        Assert.assertEquals("10:15:30 PM Eastern Standard Time", d1.toTimeString("en-US", "America/New_York"));
        Assert.assertEquals("22:15:30 Eastern Standard Time", d1.toTimeString("UTC", "America/New_York"));
    }

    @Test
    public void testNow() {
        Assertions.assertThat(TbDate.now()).isCloseTo(Instant.now().toEpochMilli(), Offset.offset(1000L));
    }

    private void testResultChangeDateTime(TbDate d) {
        int localOffset = ZoneId.systemDefault().getRules().getOffset(d.getInstant()).getTotalSeconds();
        TbDateTestEntity tbDateTestEntity = new TbDateTestEntity(d.getFullYear(), d.getMonth(), d.getDate(), (d.getHours() - (localOffset/60/60)));
        Assert.assertEquals(tbDateTestEntity.getYear(), d.getUTCFullYear());
        Assert.assertEquals(tbDateTestEntity.getMonth(), d.getUTCMonth());
        Assert.assertEquals(tbDateTestEntity.getDate(), d.getUTCDate());
        Assert.assertEquals(tbDateTestEntity.getHours(), d.getUTCHours());
        Assert.assertEquals(d.getMinutes(), d.getUTCMinutes());
        Assert.assertEquals(d.getSeconds(), d.getUTCSeconds());
        Assert.assertEquals(d.getMilliseconds(), d.getUTCMilliseconds());
    }
}
