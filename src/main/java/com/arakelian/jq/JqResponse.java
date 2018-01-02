package com.arakelian.jq;

import java.util.List;

import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;

@Value.Immutable
public interface JqResponse {
    @Value.Default
    public default List<String> getErrors() {
        return ImmutableList.of();
    }

    @Value.Derived
    @Value.Auxiliary
    public default boolean hasErrors() {
        return getErrors().size() != 0;
    }

    @Value.Default
    public default String getOutput() {
        return "";
    }
}
