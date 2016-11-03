package com.codeabovelab.dm.common.utils;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 */
public class ContainerDetectorTest {

    @Test
    public void test() {
        List<String> data = Arrays.asList("9:cpuset:/docker/2e1f9a845e40cd2445cf54ae0aa37a317b809dc7d73baa77354297c6c9abbd5a",
          "8:blkio:/docker/2e1f9a845e40cd2445cf54ae0aa37a317b809dc7d73baa77354297c6c9abbd5a",
          "7:perf_event:/docker/2e1f9a845e40cd2445cf54ae0aa37a317b809dc7d73baa77354297c6c9abbd5a",
          "6:freezer:/docker/2e1f9a845e40cd2445cf54ae0aa37a317b809dc7d73baa77354297c6c9abbd5a",
          "5:cpu,cpuacct:/docker/2e1f9a845e40cd2445cf54ae0aa37a317b809dc7d73baa77354297c6c9abbd5a",
          "4:net_cls,net_prio:/docker/2e1f9a845e40cd2445cf54ae0aa37a317b809dc7d73baa77354297c6c9abbd5a",
          "3:devices:/docker/2e1f9a845e40cd2445cf54ae0aa37a317b809dc7d73baa77354297c6c9abbd5a",
          "2:pids:/docker/2e1f9a845e40cd2445cf54ae0aa37a317b809dc7d73baa77354297c6c9abbd5a",
          "1:name=systemd:/docker/2e1f9a845e40cd2445cf54ae0aa37a317b809dc7d73baa77354297c6c9abbd5a");
        Optional<String> id = ContainerDetector.findId(data.stream());
        assertEquals("2e1f9a845e40cd2445cf54ae0aa37a317b809dc7d73baa77354297c6c9abbd5a", id.get());

        data = Arrays.asList("9:hugetlb:/",
          "8:perf_event:/",
          "7:blkio:/",
          "6:freezer:/",
          "5:devices:/",
          "4:memory:/",
          "3:cpuacct:/",
          "1:cpuset:/",
          "2:cpu:/docker/25ef774c390558ad8c4e9a8590b6a1956231aae404d6a7aba4dde320ff569b8b");
        id = ContainerDetector.findId(data.stream());
        assertEquals("25ef774c390558ad8c4e9a8590b6a1956231aae404d6a7aba4dde320ff569b8b", id.get());
    }
}