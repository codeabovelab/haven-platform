package com.codeabovelab.dm.cluman.ui.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;

public class UiImageCatalogTest {

    @Test
    public void testSorting() {
        UiImageCatalog uiImageCatalog = new UiImageCatalog("test", "testRegistry");
        UiImageData d3 = uiImageCatalog.getOrAddId("3");
        d3.setCreated(new Date());
        d3.getNodes().add("node");
        UiImageData d2 = uiImageCatalog.getOrAddId("2");
        d2.setCreated(new Date());
        UiImageData d1 = uiImageCatalog.getOrAddId("1");
        d1.setCreated(new Date());

        List<UiImageData> ids = uiImageCatalog.getIds();
        Assert.assertThat(ids.get(0).getId(), equalTo("3"));
        Assert.assertThat(ids.get(1).getId(), equalTo("1"));
        Assert.assertThat(ids.get(2).getId(), equalTo("2"));
    }
}