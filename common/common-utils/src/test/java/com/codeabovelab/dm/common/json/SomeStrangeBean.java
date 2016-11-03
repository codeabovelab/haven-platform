package com.codeabovelab.dm.common.json;

import com.codeabovelab.dm.common.utils.Keeper;
import lombok.Data;
import org.springframework.util.MimeType;

/**
 */
@Data
public class SomeStrangeBean {
    private MimeType mimeType;
    private final Keeper<String> keeperProp = new Keeper<>("default val");
}
