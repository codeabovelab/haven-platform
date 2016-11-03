package com.codeabovelab.dm.common.fc;

import com.codeabovelab.dm.common.utils.OSUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 */
@Slf4j
public class FbQueueTest {
    private final String rootDir = OSUtils.getTempDir() + "/" + getClass().getName();
    private FbStorage storage;
    private final FbAdapter<String> stringAdapter = new FbAdapter<String>() {
        @Override
        public byte[] serialize(String obj) {
            return obj.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String deserialize(byte[] data, int offset, int len) {
            return new String(data, offset, len, StandardCharsets.UTF_8);
        }
    };

    @Before
    public void beforeTest() {
        storage = FbStorage.builder()
          .maxFiles(3)
          .maxFileSize(1024 * 1024 /* 1 MiB */)
          .path(rootDir)
          .build();
    }

    @After
    public void afterTest() throws IOException {
        Path path = Paths.get(rootDir);
        if(!System.getProperties().containsKey("notCleatAfterTest")) {
            FbUtils.clearDir(path);
        }
    }

    @Test
    public void testOverflow() throws Exception {
        final int queueSize = 2500;
        String id = "testOverflow";
        FbQueue<String> queue = makeQueue(id, queueSize);
        int i = 0;
        try {
            for(; i < queueSize; ++i) {
                queue.add("<" + i + ">");
            }
            queue.add("<" + i + ">");
            fail("queue silent overflow");
        } catch (IllegalStateException e) {
            //queue full as expected
            assertEquals(queueSize, i);
        } catch (Exception e) {
            System.out.println("Fail at: " + i + " iteration ");
            throw e;
        }
    }

    @Test
    public void testFileExceed() throws Exception {
        final int queueSize = 4000 /* maxFile = 3, itemsInFile = 1024, we must exceed this*/;
        String id = "testFileExceed";
        FbQueue<String> queue = makeQueue(id, queueSize);
        int i = 0;
        try {
            for(; i < queueSize; ++i) {
                queue.add("<" + i + ">");
            }
            fail("queue silent overflow");
        } catch (FbException e) {
            //queue full as expected
            assertEquals(storage.getMaxFiles() * queue.getMaxItemsInFile(), i);
        } catch (Exception e) {
            System.out.println("Fail at: " + i + " iteration ");
            throw e;
        }
    }

    private FbQueue<String> makeQueue(String id, int queueSize) {
        return FbQueue.builder(stringAdapter)
              .maxSize(queueSize)
              .id(id)
              .storage(storage)
              .build();
    }

    @Test
    public void testReadWrite() throws Exception {
        final int queueSize = 3000;
        String id = "testReadWrite";
        FbQueue<String> queue = makeQueue(id, queueSize);
        final int end = queueSize - 2;
        for(int i = 0; i < end; ++i) {
            queue.add("<" + i + ">");
        }
        final String fes = "expected str";
        queue.add(fes);
        final String ses = "second expected str";
        queue.add(ses);
        for(int i = 0; i < end; ++i) {
            String poll = queue.poll();
            Assert.assertEquals("<" + i + ">", poll);
        }
        String poll = queue.poll();
        Assert.assertEquals(fes, poll);
        poll = queue.poll();
        Assert.assertEquals(ses, poll);
        poll = queue.poll();
        Assert.assertNull(poll);
        Assert.assertEquals(0, queue.size());
    }

    @Test
    public void testPush() throws Exception {
        final int queueSize = 300;
        String id = "testPush";
        FbQueue<String> queue = makeQueue(id, queueSize);
        for(int i = 0; i < queueSize * 3; ++i) {
            queue.push("<" + i + ">");
        }
        Assert.assertEquals(queueSize, queue.size());
        for(int i = queueSize * 2; i < queueSize * 3; ++i) {
            String poll = queue.poll();
            Assert.assertEquals("<" + i + ">", poll);
        }
        String poll = queue.poll();
        Assert.assertNull(poll);
        Assert.assertEquals(0, queue.size());
    }

    @Test
    public void testPersistence() throws Exception {
        //it also test how slide queue window through files
        final int queueSize = 5000 /* we must iterate for creation more then max files */;
        final int remain = 100;
        String id = hasUtf8()? "withUTF©ёЯя™" : "testPersistence";
        FbQueue<String> queue = makeQueue(id, queueSize);
        for(int i = 0; i < queueSize; ++i) {
            queue.add("<" + i + ">");
            if(i >= remain) {
                queue.poll();
            }
        }
        final int origSize = queue.size();
        Assert.assertEquals(remain, origSize);
        queue.close();
        //another queue with same id and size
        queue = makeQueue(id, queueSize);
        int size = queue.size();
        Assert.assertEquals(origSize, size);
        for(int i = 0; i < size; ++i) {
            String poll = queue.poll();
            Assert.assertEquals("<" + (queueSize - size + i) + ">", poll);
        }
        String poll = queue.poll();
        Assert.assertNull(poll);
        Assert.assertEquals(0, queue.size());
    }

    private boolean hasUtf8() {
        try {
            // previous case when we check system variables doe not work,
            //   last way a try to create file
            File tempFile = File.createTempFile("Я™", ".tmp");
            tempFile.delete();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    public void testValidation() throws Exception {
        final int queueSize = 5;
        String id = "testValidation";
        {
            FbQueue<String> queue = makeQueue(id, queueSize + 100);
            for(int i = 0; i < queueSize; ++i) {
                queue.add("<" + i + ">");
            }
            queue.close();
        }
        //another queue with same id and different size
        {
            FbQueue<String> queue = makeQueue(id, queueSize);
            int size = queue.size();
            Assert.assertEquals(0, size);
            //test that it correct wrote
            for(int i = 0; i < queueSize; ++i) {
                queue.add("*" + i + "*");
            }
            queue.close();
        }
        // same size
        {
            FbQueue<String> queue = makeQueue(id, queueSize);
            int size = queue.size();
            Assert.assertEquals(queueSize, size);
            //test that it correct read
            for(int i = 0; i < queueSize; ++i) {
                Assert.assertEquals("*" + i +"*", queue.poll());
            }
            queue.close();
        }
    }

    @Test
    public void testConcurrency() throws Exception {
        final int queueSize = 5000;
        final int remain = 100;
        final int concurrency = 10;
        String id = "testConcurrency";
        final AtomicInteger counter = new AtomicInteger();
        Collection<String> added = Collections.synchronizedList(new ArrayList<>());
        {
            final FbQueue<String> queue = makeQueue(id, queueSize);
            List<Exception> fails = Collections.synchronizedList(new ArrayList<>());
            ExecutorService executor = Executors.newCachedThreadPool();
            Runnable task = () -> {
                try {
                    for(int i = 0; i < queueSize; ++i) {
                        String item = "<" + counter.incrementAndGet() + ">";
                        added.add(item);
                        queue.add(item);
                        if(i >=  remain/concurrency) {
                            String polled = queue.poll();
                            Assert.assertNotNull(polled);
                            boolean removed = added.remove(polled);
                            Assert.assertTrue("Can not remove " + polled + " at " + i, removed);
                        }
                    }
                } catch (Exception e) {
                    log.error("", e);
                    fails.add(e);
                }
            };
            for(int i = 0; i < concurrency; i++) {
                executor.execute(task);
            }
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            if(!fails.isEmpty()) {
                fail("Fail due errors:" + fails);
            }
            final int origSize = queue.size();
            Assert.assertEquals(added.size(), origSize);
            queue.close();
        }
        {
            //another queue with same id and size
            final FbQueue<String> queue = makeQueue(id, queueSize);
            int size = queue.size();
            Assert.assertEquals(added.size(), size);
            for(int i = 0; i < size; ++i) {
                String poll = queue.poll();
                Assert.assertTrue(added.remove(poll));
            }
            String poll = queue.poll();
            Assert.assertNull(poll);
            Assert.assertTrue("Not all element was removed: " + added, added.isEmpty());
            Assert.assertEquals(0, queue.size());
        }
    }


    @Test
    public void testIterator() throws Exception {
        final int queueSize = 3000;
        String id = "testIterator";
        FbQueue<String> queue = makeQueue(id, queueSize);
        for(int i = 0; i < queueSize; ++i) {
            queue.add("<" + i + ">");
        }
        assertIterator(queue, Integer.MAX_VALUE, 0);
        assertIterator(queue, 1000, 2000);
        assertEquals(queueSize, queue.size());
    }

    private void assertIterator(FbQueue<String> queue, int last, int first) {
        final int expected = last == Integer.MAX_VALUE? queue.size() : last;
        Iterator<String> iter = queue.iterator(last);
        int i = first;
        while(iter.hasNext()) {
            String next = iter.next();
            assertEquals("<" + i + ">", next);
            i++;
        }
        assertEquals(expected, i - first);
    }
}