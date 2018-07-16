package com.dataheaps.aspectrest;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by admin on 17/3/18.
 */
@AllArgsConstructor @Data
public class RestResponse {
    int status;
    Object payload;
}
