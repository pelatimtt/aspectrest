package com.dataheaps.aspectrest;

import lombok.AllArgsConstructor;

/**
 * Created by matteopelati on 27/11/15.
 */

@AllArgsConstructor
public class RestRequest {
    RestServiceDescriptor descriptor;
    Object[] args;
}
