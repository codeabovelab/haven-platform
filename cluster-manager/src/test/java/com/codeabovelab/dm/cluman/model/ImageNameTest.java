package com.codeabovelab.dm.cluman.model;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 */
public class ImageNameTest {

    @Test
    public void test() {
        Object[][] srcs = new Object[][] {
          {"",                       new ImageName("", "", "")},
          {"img",                    new ImageName("", "img", "")},
          {"img:latest",             new ImageName("", "img", "latest")},
          {"hubrepo/img:latest",     new ImageName("", "hubrepo/img", "latest")},
          {"repo.org/img:latest",    new ImageName("repo.org", "img", "latest")},
          {"repo.org:80/img:latest", new ImageName("repo.org:80", "img", "latest")},
          {"repo.org:80/ns/img:1.0", new ImageName("repo.org:80", "ns/img", "1.0")},
          {"repo.org:80/img",        new ImageName("repo.org:80", "img", "")},
        };

        for(Object[] src: srcs) {
            ImageName res = ImageName.parse((String) src[0]);
            Assert.assertEquals(src[1], res);
        }
    }
}