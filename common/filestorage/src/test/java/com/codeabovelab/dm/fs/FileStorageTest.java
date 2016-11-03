package com.codeabovelab.dm.fs;

import com.codeabovelab.dm.fs.configuration.FileStorageModuleConfig;
import com.codeabovelab.dm.fs.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * TODO: check test running under the Windows
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
@SpringBootTest(classes = {FileStorageTest.TestConfig.class, FileStorageModuleConfig.class})
//@Ignore("Test fails on windows")
public class FileStorageTest {

    @Configuration
    @EnableAutoConfiguration
    public static class TestConfig {

    }

    @Autowired
    FileStorage storage;

    final FileHandle fileHandle = SimpleFileHandle.builder()
      .id("bf32591c-1da6-45b6-b6ff-acc0d0e67f05")
      .putAttribute(FileHandle.ATTR_NAME, "blah")
      .putAttribute(FileHandle.ATTR_DATE_CREATION, String.valueOf(System.currentTimeMillis()))
      .streamFactory(FileStorageUtils.streamFactory("bla bla bla bla".getBytes()))
      .build();

    @Before
    public void before() throws IOException {
        storage.delete(DeleteOptions.builder().id(fileHandle.getId()).build());
        storage.write(WriteOptions.builder().fileHandle(fileHandle).failIfExists(true).build());
    }

    @After
    public void after() throws IOException {
        storage.delete(DeleteOptions.builder().id(fileHandle.getId()).build());
    }


    @Test
    public void testRead() throws IOException {
        assertRead(fileHandle);
    }

    @Test
    public void testDelete() throws IOException {
        storage.delete(DeleteOptions.builder().id(fileHandle.getId()).build());
        try {
            FileHandle read = storage.read(ReadOptions.builder().id(fileHandle.getId()).failIfAbsent(true).build());
            toString(read);
            fail("We expect that file has been deleted");
        } catch (FileStorageAbsentException e) {
            log.error("", e);
        }
    }

    @Test
    public void testReplace() throws IOException {
        // try to replace
        try {
            storage.write(WriteOptions.builder().fileHandle(SimpleFileHandle.builder()
              .id(fileHandle.getId())
              .putAttribute(FileHandle.ATTR_NAME, "bad name")
              .putAttribute(FileHandle.ATTR_MIME_TYPE, "bad mime")
              .streamFactory(FileStorageUtils.streamFactory("bad".getBytes()))
              .build()).failIfExists(true).build());
            fail("Write mut be failed");
        } catch (FileStorageExistsException e) {
            log.error("", e);
        }
        assertRead(fileHandle);
        // replace
        SimpleFileHandle.Builder builder = SimpleFileHandle.builder()
          .id(fileHandle.getId())
          .putAttribute(FileHandle.ATTR_NAME, "new name") // this attr will be replaced
          .putAttribute(FileHandle.ATTR_MIME_TYPE, "new mime") // this attr will be added
          .streamFactory(FileStorageUtils.streamFactory("new data".getBytes()));
        storage.write(WriteOptions.builder().fileHandle(builder.build()).failIfAbsent(true).build());
        SimpleFileHandle newFile = builder
          // this attr is must remain from original handle
          .putAttribute(FileHandle.ATTR_DATE_CREATION, fileHandle.getAttributes().get(FileHandle.ATTR_DATE_CREATION))
          .build();
        assertRead(newFile);
    }

    @Test
    public void testUpdateAttributes() throws IOException {
        assertRead(fileHandle);
        // replace
        SimpleFileHandle.Builder builder = SimpleFileHandle.builder()
          .id(fileHandle.getId())
          .putAttribute(FileHandle.ATTR_NAME, "new name") // this attr will be replaced
          .putAttribute(FileHandle.ATTR_MIME_TYPE, "new mime"); // this attr will be added
        storage.write(WriteOptions.builder().fileHandle(builder.build()).failIfAbsent(true).build());
        SimpleFileHandle newFile = builder
          // this attr is must remain from original handle
          .putAttribute(FileHandle.ATTR_DATE_CREATION, fileHandle.getAttributes().get(FileHandle.ATTR_DATE_CREATION))
          .build();

        FileHandle readedHandle = storage.read(ReadOptions.builder().id(fileHandle.getId()).failIfAbsent(true).build());
        String readedData = toString(readedHandle);
        assertEquals(toString(fileHandle), readedData);
        assertEquals(newFile.getAttributes(), readedHandle.getAttributes());
    }

    private void assertRead(FileHandle fileHandle) throws IOException {
        FileHandle readedHandle = storage.read(ReadOptions.builder().id(fileHandle.getId()).failIfAbsent(true).build());
        String readedData = toString(readedHandle);
        assertEquals(toString(fileHandle), readedData);
        assertEquals(fileHandle.getAttributes(), readedHandle.getAttributes());
    }

    private static String toString(FileHandle fileHandle) throws IOException {
        return IOUtils.toString(fileHandle.getData(), StandardCharsets.UTF_8);
    }
}
