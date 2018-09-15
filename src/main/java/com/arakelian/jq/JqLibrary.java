/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arakelian.jq;

import static java.util.logging.Level.INFO;

import java.util.List;
import java.util.logging.Logger;

import org.immutables.value.Value;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.sun.jna.Callback;
import com.sun.jna.Function;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.ByReference;
import com.sun.jna.Structure.ByValue;
import com.sun.jna.Union;
import com.sun.jna.ptr.PointerByReference;

@Value.Immutable(singleton = true)
public abstract class JqLibrary {
    public interface ErrorCallback extends Callback {
        public void callback(final Pointer data, final Jv jv);
    }

    public static class Jv extends Structure implements ByValue {
        public static class U extends Union {
            public JvRefCount ptr;
            public double number;
        }

        public byte kind_flags;
        public byte pad_;
        public short offset;
        public int size;
        public U u;

        @Override
        protected List<String> getFieldOrder() {
            return ImmutableList.of("kind_flags", "pad_", "offset", "size", "u");
        }
    }

    public static class JvRefCount extends Structure implements ByReference {
        public int count;

        @Override
        protected List<String> getFieldOrder() {
            return ImmutableList.of("count");
        }
    }

    private static final Logger LOGGER = Logger.getLogger(JqLibrary.class.getName());

    public static final int JV_KIND_INVALID = 0;
    public static final int JV_KIND_NULL = 1;
    public static final int JV_KIND_FALSE = 2;
    public static final int JV_KIND_TRUE = 3;
    public static final int JV_KIND_NUMBER = 4;
    public static final int JV_KIND_STRING = 5;
    public static final int JV_KIND_ARRAY = 6;
    public static final int JV_KIND_OBJECT = 7;

    public static final int JV_PARSE_SEQ = 1;
    public static final int JV_PARSE_STREAMING = 2;
    public static final int JV_PARSE_STREAM_ERRORS = 4;

    public static final int JV_PRINT_PRETTY = 1;
    public static final int JV_PRINT_ASCII = 2;
    public static final int JV_PRINT_COLOR = 4;
    public static final int JV_PRINT_SORTED = 8;
    public static final int JV_PRINT_INVALID = 16;
    public static final int JV_PRINT_REFCOUNT = 32;
    public static final int JV_PRINT_TAB = 64;
    public static final int JV_PRINT_ISATTY = 128;
    public static final int JV_PRINT_SPACE0 = 256;
    public static final int JV_PRINT_SPACE1 = 512;
    public static final int JV_PRINT_SPACE2 = 1024;

    /** No arguments **/
    public static final Object[] NO_ARGS = new Object[0];

    @Value.Auxiliary
    public Function getJqCompile() {
        return getLoader().getNativeLibrary().getFunction("jq_compile");
    }

    @Value.Auxiliary
    public Function getJqCompileArgs() {
        return getLoader().getNativeLibrary().getFunction("jq_compile_args");
    }

    @Value.Auxiliary
    public Function getJqInit() {
        return getLoader().getNativeLibrary().getFunction("jq_init");
    }

    @Value.Auxiliary
    public Function getJqNext() {
        return getLoader().getNativeLibrary().getFunction("jq_next");
    }

    @Value.Auxiliary
    public Function getJqSetErrorCb() {
        return getLoader().getNativeLibrary().getFunction("jq_set_error_cb");
    }

    @Value.Auxiliary
    public Function getJqStart() {
        return getLoader().getNativeLibrary().getFunction("jq_start");
    }

    @Value.Auxiliary
    public Function getJqTeardown() {
        return getLoader().getNativeLibrary().getFunction("jq_teardown");
    }

    @Value.Auxiliary
    public Function getJvArray() {
        return getLoader().getNativeLibrary().getFunction("jv_array");
    }

    @Value.Auxiliary
    public Function getJvArrayAppend() {
        return getLoader().getNativeLibrary().getFunction("jv_array_append");
    }

    @Value.Auxiliary
    public Function getJvArrayConcat() {
        return getLoader().getNativeLibrary().getFunction("jv_array_concat");
    }

    @Value.Auxiliary
    public Function getJvCopy() {
        return getLoader().getNativeLibrary().getFunction("jv_copy");
    }

    @Value.Auxiliary
    public Function getJvDumpString() {
        return getLoader().getNativeLibrary().getFunction("jv_dump_string");
    }

    @Value.Auxiliary
    public Function getJvFree() {
        return getLoader().getNativeLibrary().getFunction("jv_free");
    }

    @Value.Auxiliary
    public Function getJvGetKind() {
        return getLoader().getNativeLibrary().getFunction("jv_get_kind");
    }

    @Value.Auxiliary
    public Function getJvInvalidGetMsg() {
        return getLoader().getNativeLibrary().getFunction("jv_invalid_get_msg");
    }

    @Value.Auxiliary
    public Function getJvInvalidHasMsg() {
        return getLoader().getNativeLibrary().getFunction("jv_invalid_has_msg");
    }

    @Value.Auxiliary
    public Function getJvObject() {
        return getLoader().getNativeLibrary().getFunction("jv_object");
    }

    @Value.Auxiliary
    public Function getJvObjectHas() {
        return getLoader().getNativeLibrary().getFunction("jv_object_has");
    }

    @Value.Auxiliary
    public Function getJvObjectSet() {
        return getLoader().getNativeLibrary().getFunction("jv_object_set");
    }

    @Value.Auxiliary
    public Function getJvParse() {
        return getLoader().getNativeLibrary().getFunction("jv_parse");
    }

    @Value.Auxiliary
    public Function getJvParserFree() {
        return getLoader().getNativeLibrary().getFunction("jv_parser_free");
    }

    @Value.Auxiliary
    public Function getJvParserNew() {
        return getLoader().getNativeLibrary().getFunction("jv_parser_new");
    }

    @Value.Auxiliary
    public Function getJvParserNext() {
        return getLoader().getNativeLibrary().getFunction("jv_parser_next");
    }

    @Value.Auxiliary
    public Function getJvParserSetBuf() {
        return getLoader().getNativeLibrary().getFunction("jv_parser_set_buf");
    }

    @Value.Auxiliary
    public Function getJvString() {
        return getLoader().getNativeLibrary().getFunction("jv_string");
    }

    @Value.Auxiliary
    public Function getJvStringValue() {
        return getLoader().getNativeLibrary().getFunction("jv_string_value");
    }

    @Value.Lazy
    @Value.Auxiliary
    public NativeLib getLoader() {
        final ImmutableNativeLib jq = ImmutableNativeLib.builder() //
                .name("jq") //
                .build();
        Preconditions.checkState(jq.getNativeLibrary() != null, "Cannot load JQ library");
        LOGGER.log(INFO, "Loaded {0}", new Object[] { jq.getLocalCopy() });
        return jq;
    }

    public boolean jq_compile(final Pointer state, final String filter) {
        return getJqCompile().invokeInt(new Object[] { state, filter }) != 0;
    }

    public boolean jq_compile_args(final Pointer state, final String filter, final Jv args) {
        return getJqCompileArgs().invokeInt(new Object[] { state, filter, args }) != 0;
    }

    public Pointer jq_init() {
        return (Pointer) getJqInit().invoke(Pointer.class, NO_ARGS);
    }

    public Jv jq_next(final Pointer state) {
        return (Jv) getJqNext().invoke(Jv.class, new Object[] { state });
    }

    public void jq_set_error_cb(final Pointer state, final ErrorCallback callback, final Pointer data) {
        getJqSetErrorCb().invoke(new Object[] { state, callback, data });
    }

    public void jq_start(final Pointer state, final Jv jv) {
        getJqStart().invoke(new Object[] { state, jv, 0 });
    }

    public void jq_teardown(final Pointer state) {
        final PointerByReference ref = new PointerByReference(state);
        getJqTeardown().invoke(new Object[] { ref });
    }

    public Jv jv_array() {
        return (Jv) getJvArray().invoke(Jv.class, new Object[] {});
    }

    public Jv jv_array_append(final Jv array, final Jv value) {
        return (Jv) getJvArrayAppend().invoke(Jv.class, new Object[] { array, value });
    }

    public Jv jv_array_concat(final Jv array, final Jv anotherArray) {
        return (Jv) getJvArrayConcat().invoke(Jv.class, new Object[] { array, anotherArray });
    }

    public Jv jv_copy(final Jv jv) {
        return (Jv) getJvCopy().invoke(Jv.class, new Object[] { jv });
    }

    public String jv_dump_string(final Jv next, final int flags) {
        final Jv dumped = (Jv) getJvDumpString().invoke(Jv.class, new Object[] { next, flags });
        try {
            return jv_string_value(dumped);
        } finally {
            jv_free(dumped);
        }
    }

    public void jv_free(final Jv jv) {
        getJvFree().invoke(new Object[] { jv });
    }

    public int jv_get_kind(final Jv jv) {
        return getJvGetKind().invokeInt(new Object[] { jv });
    }

    public Jv jv_invalid_get_msg(final Jv jv) {
        return (Jv) getJvInvalidGetMsg().invoke(Jv.class, new Object[] { jv });
    }

    public boolean jv_invalid_has_msg(final Jv jv) {
        return getJvInvalidHasMsg().invokeInt(new Object[] { jv }) != 0;
    }

    public final boolean jv_is_valid(final Jv jv) {
        final int kind = getJvGetKind().invokeInt(new Object[] { jv });
        return kind != JqLibrary.JV_KIND_INVALID;
    }

    public Jv jv_object() {
        return (Jv) getJvObject().invoke(Jv.class, new Object[] {});
    }

    public boolean jv_object_has(final Jv object, final Jv key) {
        final int has = getJvObjectHas().invokeInt(new Object[] { object, key });
        return has != 0;
    }

    public Jv jv_object_set(final Jv object, final Jv key, final Jv value) {
        return (Jv) getJvObjectSet().invoke(Jv.class, new Object[] { object, key, value });
    }

    public Jv jv_parse(final String json) {
        return (Jv) getJvParse().invoke(Jv.class, new Object[] { json });
    }

    public void jv_parser_free(final Pointer parser) {
        getJvParserFree().invoke(new Object[] { parser });
    }

    public Pointer jv_parser_new(final int flags) {
        return (Pointer) getJvParserNew().invoke(Pointer.class, new Object[] { Integer.valueOf(flags) });
    }

    public Jv jv_parser_next(final Pointer parser) {
        return (Jv) getJvParserNext().invoke(Jv.class, new Object[] { parser });
    }

    public void jv_parser_set_buf(
            final Pointer parser,
            final Pointer pointer,
            final int length,
            final boolean finished) {
        getJvParserSetBuf().invoke(new Object[] { parser, pointer, length, finished ? 0 : 1 });
    }

    public Jv jv_string(final String value) {
        return (Jv) getJvString().invoke(Jv.class, new Object[] { value });
    }

    public String jv_string_value(final Jv jv) {
        final Pointer result = (Pointer) getJvStringValue().invoke(Pointer.class, new Object[] { jv });
        final String error = result.getString(0, Charsets.UTF_8.name());
        return error;
    }
}
