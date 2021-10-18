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

import static java.util.logging.Level.FINE;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.immutables.value.Value;

import com.arakelian.jq.JqLibrary.Jv;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

@Value.Immutable
public abstract class JqRequest {
    public enum Indent {
        NONE, //
        TAB, //
        SPACE, //
        TWO_SPACES;
    }

    private static final Logger LOGGER = Logger.getLogger(JqRequest.class.getName());

    /**
     * JQ is not thread-safe - https://github.com/stedolan/jq/issues/120
     */
    private static final ReentrantLock SYNC = new ReentrantLock();

    public final JqResponse execute() {
        SYNC.lock();
        try {
            return jq();
        } finally {
            SYNC.unlock();
        }
    }

    @Value.Default
    public Map<String, String> getArgJson() {
        return ImmutableMap.of();
    }

    @Value.Derived
    @Value.Auxiliary
    public int getDumpFlags() {
        int flags = 0;

        if (isPretty()) {
            flags |= JqLibrary.JV_PRINT_PRETTY;
        }
        switch (getIndent()) {
        case TAB:
            flags = JqLibrary.JV_PRINT_TAB;
            break;
        case SPACE:
            flags = JqLibrary.JV_PRINT_SPACE1;
            break;
        case TWO_SPACES:
            flags = JqLibrary.JV_PRINT_SPACE2;
            break;
        case NONE:
        default:
            break;
        }

        if (isSortKeys()) {
            flags |= JqLibrary.JV_PRINT_SORTED;
        }
        return flags;
    }

    @Value.Default
    public String getFilter() {
        return ".";
    }

    @Value.Default
    public Indent getIndent() {
        return Indent.TWO_SPACES;
    }

    public abstract String getInput();

    public abstract JqLibrary getLib();

    public abstract List<File> getModulePaths();

    @Value.Default
    public String getStreamSeparator() {
        return "\n";
    }

    @Value.Default
    public boolean isPretty() {
        return true;
    }

    @Value.Default
    public boolean isSortKeys() {
        return false;
    }

    /**
     * Adds any messages produced by jq native code it to the error store, with the provided prefix.
     *
     * @param value
     *            value reference
     */
    private String getInvalidMessage(final Jv value) {
        final Jv copy = getLib().jv_copy(value);
        if (getLib().jv_invalid_has_msg(copy)) {
            final Jv message = getLib().jv_invalid_get_msg(value);
            return getLib().jv_string_value(message);
        } else {
            getLib().jv_free(value);
            return null;
        }
    }

    private boolean isValid(final ImmutableJqResponse.Builder response, final Jv value) {
        if (getLib().jv_is_valid(value)) {
            return true;
        }

        // success finishes will return "invalid" value without a message
        final String message = getInvalidMessage(value);
        if (message != null) {
            response.addError(message);
        }
        return false;
    }

    private JqResponse jq() {
        LOGGER.log(FINE, "Initializing JQ");
        final JqLibrary lib = getLib();
        final Pointer jq = lib.jq_init();
        Preconditions.checkState(jq != null, "jq must be non-null");

        Jv moduleDirs = lib.jv_array();
        for (final File file : getModulePaths()) {
            try {
                final String dir = file.getCanonicalPath();
                LOGGER.log(FINE, "Using module path: " + dir);
                moduleDirs = lib.jv_array_append(moduleDirs, lib.jv_string(dir));
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        lib.jq_set_attr(jq, lib.jv_string("JQ_LIBRARY_PATH"), moduleDirs);

        try {
            final JqResponse response = parse(jq);
            LOGGER.log(FINE, "Response ready");
            return response;
        } finally {
            LOGGER.log(FINE, "Releasing JQ");
            lib.jq_teardown(jq);
            LOGGER.log(FINE, "JQ released successfully");
        }
    }

    private JqResponse parse(final Pointer jq) {
        final ImmutableJqResponse.Builder response = ImmutableJqResponse.builder();

        LOGGER.log(FINE, "Configuring callback");
        final JqLibrary lib = getLib();
        lib.jq_set_error_cb(jq, (data, jv) -> {
            LOGGER.log(FINE, "Error callback");
            final int kind = lib.jv_get_kind(jv);
            if (kind == JqLibrary.JV_KIND_STRING) {
                final String error = lib.jv_string_value(jv).replaceAll("\\s++$", "");
                response.addError(error);
            }
        }, new Pointer(0));

        // for JQ 1.5, arguments is an array; this changes with JQ 1.6+
        Jv args = lib.jv_object();

        final Map<String, String> argJson = getArgJson();
        for (final String varname : argJson.keySet()) {
            final String text = argJson.get(varname);

            final Jv json = lib.jv_parse(text);
            if (!lib.jv_is_valid(json)) {
                response.addError("Invalid JSON text passed to --argjson (name: " + varname + ")");
                return response.build();
            }

            args = lib.jv_object_set(args, lib.jv_string(varname), json);
        }

        try {
            // compile JQ program
            LOGGER.log(FINE, "Compiling filter");
            final String filter = getFilter();
            if (!lib.jq_compile_args(jq, filter, lib.jv_copy(args))) {
                // compile errors are captured by callback
                LOGGER.log(FINE, "Compilation failed");
                return response.build();
            }

            // create JQ parser
            LOGGER.log(FINE, "Creating parse");
            final int parserFlags = 0;
            final Pointer parser = lib.jv_parser_new(parserFlags);
            try {
                parse(jq, parser, getInput(), response);
                return response.build();
            } finally {
                LOGGER.log(FINE, "Releasing parser");
                lib.jv_parser_free(parser);
            }
        } finally {
            LOGGER.log(FINE, "Releasing callback");
            lib.jq_set_error_cb(jq, null, null);
        }
    }

    /**
     * Add the contents of a native memory array as text to the next chunk of input of the jq
     * program.
     *
     * @param jq
     *            JQ instance
     * @param parser
     *            JQ parser
     * @param text
     *            input JSON
     * @param response
     *            response that we are building
     */
    private void parse(
            final Pointer jq,
            final Pointer parser,
            final String text,
            final ImmutableJqResponse.Builder response) {
        final byte[] input = text.getBytes(Charsets.UTF_8);
        final Memory memory = new Memory(input.length);
        memory.write(0, input, 0, input.length);

        // give text to JQ parser
        LOGGER.log(FINE, "Sending text to parser");
        getLib().jv_parser_set_buf(parser, memory, input.length, true);

        final int flags = getDumpFlags();
        final StringBuilder buf = new StringBuilder();
        for (;;) {
            // iterate until JQ consumes all inputs
            LOGGER.log(FINE, "Parsing text");
            final Jv parsed = getLib().jv_parser_next(parser);
            if (!isValid(response, parsed)) {
                break;
            }

            // iterate until we consume all JQ streams
            // see: https://stedolan.github.io/jq/tutorial/
            LOGGER.log(FINE, "Consuming JQ response");
            getLib().jq_start(jq, parsed);
            for (;;) {
                final Jv next = getLib().jq_next(jq);
                if (!isValid(response, next)) {
                    break;
                }

                LOGGER.log(FINE, "Dumping response");
                final String out = getLib().jv_dump_string(next, flags);
                if (buf.length() != 0) {
                    buf.append(getStreamSeparator());
                }
                buf.append(out);
            }
        }

        // tell parser we are finished
        LOGGER.log(FINE, "Finishing with parser");

        // finalize output
        final String output = buf.toString();
        response.output(output);
    }
}
