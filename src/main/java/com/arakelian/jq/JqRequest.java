package com.arakelian.jq;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arakelian.jq.JqLibrary.Jv;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(JqRequest.class);

    public final JqResponse execute() {
        LOGGER.trace("Initializing JQ");
        final Pointer state = getLib().jq_init();
        Preconditions.checkState(state != null, "state must be non-null");
        try {
            final JqResponse response = parse(state);
            LOGGER.trace("Response ready");
            return response;
        } finally {
            LOGGER.trace("Releasing JQ");
            getLib().jq_teardown(state);
            LOGGER.trace("JQ released successfully");
        }
    }

    @Value.Default
    public String getFilter() {
        return ".";
    }

    @Value.Derived
    @Value.Auxiliary
    public int getDumpFlags() {
        int flags = 0;

        switch (getIndent()) {
        case TAB:
            flags = JqLibrary.JV_PRINT_PRETTY | JqLibrary.JV_PRINT_TAB;
            break;
        case SPACE:
            flags = JqLibrary.JV_PRINT_PRETTY | JqLibrary.JV_PRINT_SPACE1;
            break;
        case TWO_SPACES:
            flags = JqLibrary.JV_PRINT_PRETTY | JqLibrary.JV_PRINT_SPACE2;
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
    public Indent getIndent() {
        return Indent.TWO_SPACES;
    }

    @Value.Default
    public String getStreamSeparator() {
        return "\n";
    }

    public abstract String getInput();

    /**
     * Adds any messages produced by jq native code it to the error store, with the provided prefix.
     *
     * @param value
     * @param prefix
     */
    private String getInvalidMessage(final Jv value) {
        final Jv copy = getLib().jv_copy(value);
        try {
            if (getLib().jv_invalid_has_msg(copy)) {
                final Jv message = getLib().jv_invalid_get_msg(value);
                return getLib().jv_string_value(message);
            }
            return null;
        } finally {
            getLib().jv_free(value);
        }
    }

    public abstract JqLibrary getLib();

    private boolean isFinished(final ImmutableJqResponse.Builder response, final Jv value) {
        if (getLib().jv_is_valid(value)) {
            return false;
        }

        // success finishes will return "invalid" value without a message
        final String message = getInvalidMessage(value);
        if (message != null) {
            response.addError(message);
        }
        return true;
    }

    @Value.Default
    public boolean isSortKeys() {
        return false;
    }

    private JqResponse parse(final Pointer state) {
        final ImmutableJqResponse.Builder response = ImmutableJqResponse.builder();

        LOGGER.trace("Configuring callback");
        getLib().jq_set_error_cb(state, (data, jv) -> {
            LOGGER.trace("Error callback");
            final int kind = getLib().jv_get_kind(jv);
            if (kind == JqLibrary.JV_KIND_STRING) {
                final String error = getLib().jv_string_value(jv);
                response.addError(error);
            }
        }, new Pointer(0));

        try {
            // compile JQ program
            LOGGER.trace("Compiling filter");
            final String filter = getFilter();
            if (!getLib().jq_compile(state, filter)) {
                // compile errors are captured by callback
                return response.build();
            }

            // create JQ parser
            LOGGER.trace("Creating parse");
            final int parserFlags = 0;
            final Pointer parser = getLib().jv_parser_new(parserFlags);
            try {
                parse(state, parser, getInput(), response);
                return response.build();
            } finally {
                LOGGER.trace("Releasing parser");
                getLib().jv_parser_free(parser);
            }
        } finally {
            LOGGER.trace("Releasing callback");
            getLib().jq_set_error_cb(state, null, null);
        }
    }

    /**
     * Add the contents of a native memory array as text to the next chunk of input of the jq
     * program.
     *
     * @param state
     *            JQ instance
     * @param parser
     *            JQ parser
     * @param text
     *            input JSON
     * @param response
     *            response that we are building
     */
    private void parse(
            final Pointer state,
            final Pointer parser,
            final String text,
            final ImmutableJqResponse.Builder response) {
        final byte[] input = text.getBytes(Charsets.UTF_8);
        final Memory memory = new Memory(input.length);
        memory.write(0, input, 0, input.length);

        // give text to JQ parser
        LOGGER.trace("Sending text to parser");
        getLib().jv_parser_set_buf(parser, memory, input.length, false);

        final StringBuilder buf = new StringBuilder();
        for (;;) {
            // iterate until JQ consumes all inputs
            LOGGER.trace("Parsing text");
            final Jv parsed = getLib().jv_parser_next(parser);
            if (isFinished(response, parsed)) {
                break;
            }

            // iterate until we consume all JQ streams
            // see: https://stedolan.github.io/jq/tutorial/
            LOGGER.trace("Consuming JQ response");
            getLib().jq_start(state, parsed);
            for (;;) {
                final Jv next = getLib().jq_next(state);
                if (isFinished(response, next)) {
                    break;
                }

                LOGGER.trace("Dumping response");
                final int flags = getDumpFlags();
                final String out = getLib().jv_dump_string(next, flags);
                if (buf.length() != 0) {
                    buf.append(getStreamSeparator());
                }
                buf.append(out);
            }
        }

        // tell parser we are finished
        LOGGER.trace("Finishing with parser");
        getLib().jv_parser_set_buf(parser, new Pointer(-1), 0, true);
        final Jv parsed = getLib().jv_parser_next(parser);
        isFinished(response, parsed);

        // finalize output
        final String output = buf.toString();
        response.output(output);
    }
}
