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
          {"",                       new ImageName("", "", "", null)},
          {"img",                    new ImageName("", "img", "", null)},
          {"img:latest",             new ImageName("", "img", "latest", null)},
          {"hubrepo/img:latest",     new ImageName("", "hubrepo/img", "latest", null)},
          {"repo.org/img:latest",    new ImageName("repo.org", "img", "latest", null)},
          {"repo.org:80/img:latest", new ImageName("repo.org:80", "img", "latest", null)},
          {"repo.org:80/ns/img:1.0", new ImageName("repo.org:80", "ns/img", "1.0", null)},
          {"repo.org:80/img",        new ImageName("repo.org:80", "img", "", null)},
          {"debian@sha256:913c8c1da94e7d9d7fe21a45b4677770ecbb59087556e98fae5c45959f008351",
            new ImageName("", "debian", "", "sha256:913c8c1da94e7d9d7fe21a45b4677770ecbb59087556e98fae5c45959f008351")},
          {"sha256:913c8c1da94e7d9d7fe21a45b4677770ecbb59087556e98fae5c45959f008351",
            new ImageName("",       "", "", "sha256:913c8c1da94e7d9d7fe21a45b4677770ecbb59087556e98fae5c45959f008351")},
        };

        for(Object[] src: srcs) {
            String str = (String) src[0];
            ImageName res = ImageName.parse(str);
            Assert.assertEquals("On parse: "  + str, src[1], res);
        }
    }

    @Test
    public void testSetTag() {
        assertEquals("nginx:latest", ImageName.setTag("nginx@sha256:00000000", "latest"));
        assertEquals("nginx:latest", ImageName.setTag("nginx", "latest"));
        assertEquals("nginx:latest", ImageName.setTag("nginx:0.00.1", "latest"));
    }
}