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

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.logging.LogManager;

import org.junit.jupiter.api.BeforeAll;
import org.opentest4j.AssertionFailedError;

import com.arakelian.jq.JqRequest.Indent;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import net.javacrumbs.jsonunit.JsonAssert;

public abstract class AbstractJqTest {
    public static class JqTestParser {
        private final String resource;

        public JqTestParser(final String resource) {
            this.resource = resource;
        }

        public Collection<Object[]> data() throws IOException {
            final URL url = JqTest.class.getResource(resource);
            assertTrue(url != null, "Resource does not exist: " + resource);
            final String content = Resources.toString(url, Charsets.UTF_8);

            final List<Object[]> data = Lists.newArrayList();

            try (BufferedReader r = new BufferedReader(new StringReader(content))) {
                int lineNo = 0;
                for (;;) {
                    lineNo++;
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    if (skip(line)) {
                        continue;
                    }

                    final int lineNumber = lineNo;
                    final Type type;
                    final String program;
                    final String input;

                    if (mustFail(line)) {
                        type = Type.MUST_FAIL;
                        lineNo++;
                        program = readLine(r);
                        input = "";
                    } else if (mustFailIgnoreMessage(line)) {
                        type = Type.MUST_FAIL_IGNORE_MESSAGE;
                        lineNo++;
                        program = readLine(r);
                        input = "";
                    } else {
                        type = Type.MUST_PASS;
                        lineNo++;
                        program = line;
                        input = readLine(r);
                    }

                    final StringBuilder expected = new StringBuilder();
                    for (;;) {
                        lineNo++;
                        line = r.readLine();
                        if (skip(line)) {
                            break;
                        }
                        if (expected.length() != 0) {
                            expected.append('\n');
                        }
                        expected.append(line);
                    }
                    final String testName = "Line " + lineNumber + ": " + program;
                    data.add(new Object[] { testName, type, program, input, expected.toString() });
                }
            }
            return data;
        }

        private boolean mustFail(final String line) {
            return "%%FAIL".equalsIgnoreCase(line);
        }

        private boolean mustFailIgnoreMessage(final String line) {
            return "%%FAIL IGNORE MSG".equalsIgnoreCase(line);
        }

        private String readLine(final BufferedReader r) throws IOException {
            final String line = r.readLine();
            Preconditions.checkState(line != null, "unexpected end of file");
            return line;
        }

        private boolean skip(final String line) {
            if (line == null) {
                return true;
            }
            final int length = line.length();
            if (length == 0) {
                return true;
            }
            for (int i = 0; i < length; i++) {
                final char ch = line.charAt(i);
                if (ch == ' ' || ch == '\t') {
                    continue;
                }
                if (ch == '#') {
                    return true;
                }
                break;
            }
            return false;
        }
    }

    public enum Type {
        MUST_PASS, MUST_FAIL, MUST_FAIL_IGNORE_MESSAGE;
    }

    private static final JqLibrary library = ImmutableJqLibrary.of();

    @BeforeAll
    public static void logging() throws SecurityException, IOException {
        final LogManager manager = LogManager.getLogManager();
        manager.readConfiguration(AbstractJqTest.class.getResourceAsStream("/logging.properties"));
        JsonAssert.setTolerance(0.01);
    }

    protected String testName;
    protected Type type;
    protected String input;
    protected String[] expected;
    protected String program;

    private void assertFail(final JqResponse response) {
        final List<String> errors = response.getErrors();
        assertTrue(errors.size() > 0, "Expected at least one error");

        String error = errors.get(0);
        final int pos = error.indexOf('\n');
        if (pos != -1) {
            error = error.substring(0, pos);
        }
        assertEquals(1, expected.length);
        assertEquals(error, expected[0]);
    }

    private void assertPass(final JqResponse response) {
        assertTrue(!response.hasErrors(), response.getErrors().toString());
        final String[] actual = response.getOutput().split("\n");

        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            try {
                assertJsonEquals(expected[i], actual[i]);
            } catch (final AssertionFailedError e) {
                assertEquals(expected[i], actual[i]);
            }
        }
    }

    protected void test(
            final String testName,
            final Type type,
            final String program,
            final String input,
            final String expected) {
        this.testName = testName;
        this.type = type;
        this.input = input;
        this.expected = expected.split("\\n");
        this.program = program;

        final URL a_jq = getClass().getClassLoader().getResource("modules/a.jq");
        assertNotNull(a_jq != null, "Cannot find path of a.jq");
        final File modulePath = new File(a_jq.getFile()).getParentFile();
        assertTrue(modulePath.exists());
        assertTrue(modulePath.isDirectory());

        final JqRequest request = ImmutableJqRequest.builder() //
                .lib(library) //
                .input(input) //
                .filter(program) //
                .pretty(false) //
                .indent(Indent.SPACE) //
                .addModulePath(modulePath) //
                .build();

        final JqResponse response = request.execute();
        switch (type) {
        case MUST_FAIL:
            // fall through
            assertFail(response);
            break;
        case MUST_FAIL_IGNORE_MESSAGE:
            assertTrue(response.hasErrors(), "Expected failure");
            break;
        case MUST_PASS:
            assertPass(response);
            break;
        default:
            throw new IllegalStateException("Unknown test type");
        }
    }
}
