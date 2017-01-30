/*
 * Copyright 2016 Code Above Lab LLC
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

package com.codeabovelab.dm.common.fc;

import com.google.common.io.BaseEncoding;
import org.springframework.util.Assert;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

/**
 */
final class FbUtils {

    private static final byte[] SIGN = new byte[]{(byte) 0xF1, 0x1E, (byte) 0xBA};
    static final int SIGN_LEN = SIGN.length;
    static final int MAX_STR_LEN = 256;
    private static final BaseEncoding HEX = BaseEncoding.base16();

    static void writeSign(DataOutput dao) throws IOException {
        dao.write(SIGN);
    }

    static void writeSign(ByteBuffer bb) {
        bb.put(SIGN);
    }

    static void readSign(DataInput dai) throws IOException {
        byte[] buf = new byte[SIGN.length];
        dai.readFully(buf);
        validateSign(buf);
    }

    static void readSign(ByteBuffer bb) {
        byte[] buf = new byte[SIGN.length];
        bb.get(buf);
        validateSign(buf);
    }

    private static void validateSign(byte[] buf) {
        if(!Arrays.equals(buf, SIGN)) {
            throw new FbException("Invalid file signature. Expected '" + HEX.encode(SIGN)+ "', but give: '" + HEX.encode(buf) + "'");
        }
    }

    static void readAndValidate(DataInput dai, byte expected) throws IOException {
        byte readed = dai.readByte();
        if(readed != expected) {
            throw new FbException(String.format("Expected %X byte, but give: %X", expected, readed));
        }
    }

    static void readAndValidate(ByteBuffer bb, byte expected) {
        byte readed = bb.get();
        if(readed != expected) {
            throw new FbException(String.format("Expected %X byte, but give: %X", expected, readed));
        }
    }

    static void clearDir(Path path) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if(attrs.isSymbolicLink()) {//do not walk over symlinks
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return super.preVisitDirectory(dir, attrs);
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return super.visitFile(file, attrs);
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if(!path.equals(dir)) {// we remain root dir
                        Files.delete(dir);
                    }
                    return super.postVisitDirectory(dir, exc);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Can not clean " + path);
        }
    }


    static void writeString(byte[] bytes, ByteBuffer to) {
        int len = bytes.length;
        Assert.isTrue(len < FbUtils.MAX_STR_LEN, "Too large byte string: " + bytes);
        to.put((byte) len);
        to.put(bytes, 0, len);
    }


    static byte[] toBytes(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        Assert.isTrue(bytes.length < FbUtils.MAX_STR_LEN, "Too large string: " + str);
        return bytes;
    }

    static String readString(byte[] tmp, ByteBuffer in) {
        int len = in.get() & 0xff;
        in.get(tmp, 0, len);
        return new String(tmp, 0, len, StandardCharsets.UTF_8);
    }

    public static void validate(String exp, String actual, String field) {
        if(exp.equals(actual)) {
            return;
        }
        differenceError(exp, actual, field);
    }

    public static void validate(long exp, long actual, String field) {
        if(exp == actual) {
            return;
        }
        differenceError(exp, actual, field);
    }

    public static void validate(int exp, int actual, String field) {
        if(exp == actual) {
            return;
        }
        differenceError(exp, actual, field);
    }

    private static void differenceError(Object exp, Object actual, String field) {
        throw new FbException("Read data has different '" + field + " expected=" + exp + " actual=" +actual);
    }
}
