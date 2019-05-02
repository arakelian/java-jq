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

import static java.util.logging.Level.CONFIG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class JqTest {
    private static final Logger LOGGER = Logger.getLogger(JqTest.class.getName());

    private static final JqLibrary library = ImmutableJqLibrary.of();

    @Before
    public void logging() throws SecurityException, IOException {
        LogManager manager = LogManager.getLogManager();
        manager.readConfiguration(getClass().getResourceAsStream("/logging.properties"));
    }

    @Test
    public void testAllCommitsWithLimitedFields() throws IOException {
        // see: https://stedolan.github.io/jq/tutorial/
        checkJq("/all_commits_limited");
    }

    @Test
    public void testAllCommitsWithLimitedFieldsAsArray() throws IOException {
        // see: https://stedolan.github.io/jq/tutorial/
        checkJq("/all_commits_limited_array");
    }

    @Test
    public void testAllCommitsWithParents() throws IOException {
        // see: https://stedolan.github.io/jq/tutorial/
        checkJq("/all_commits_with_parents");
    }

    @Test
    public void testArgJson() {
        final JqRequest request = ImmutableJqRequest.builder() //
                .lib(library) //
                .putArgJson("index", "0") //
                .input("{\n" + //
                        "\"data\": [\n" + //
                        "    {\n" + //
                        "        \"id\": 1, \n" + //
                        "        \"name\": \"John\"\n" + //
                        "    }, \n" + //
                        "    {\n" + //
                        "        \"id\": 2, \n" + //
                        "        \"name\": \"Doe\"\n" + //
                        "    }" + //
                        "]}")
                .filter(".data[$index].id") //
                .build();

        final JqResponse response = request.execute();
        assertEquals("1", response.getOutput());
    }

    @Test
    public void testFirstCommit() throws IOException {
        // see: https://stedolan.github.io/jq/tutorial/
        checkJq("/first_commit");
    }

    @Test
    public void testFirstCommitWithLimitedFields() throws IOException {
        // see: https://stedolan.github.io/jq/tutorial/
        checkJq("/first_commit_limited");
    }

    @Test
    public void testFree() {
        final JqRequest request = ImmutableJqRequest.builder() //
                .lib(library) //
                .input("{\"a\":[1,2,3,4,5],\"b\":\"hello\"}") //
                .filter(".") //
                .build();

        // iterate multiple times to ensure no native "free" memory issues
        for (int i = 0; i < 10; i++) {
            LOGGER.log(CONFIG, "Iteration {0}", new Object[] { i });
            executeJq(request);
        }
    }

    @Test
    public void testIdentity() {
        final JqRequest request = ImmutableJqRequest.builder() //
                .lib(library) //
                .input("{\"a\":[1,2,3,4,5],\"b\":\"hello\"}") //
                .filter(".") //
                .build();

        final String expected = "{\n" + //
                "    \"a\": [\n" + //
                "        1,\n" + //
                "        2,\n" + //
                "        3,\n" + //
                "        4,\n" + //
                "        5\n" + //
                "    ],\n" + //
                "    \"b\": \"hello\"\n" + //
                "}";
        final String response = executeJq(request);
        Assert.assertEquals(expected, response);
    }

    @Test
    public void testIdentityWithSortedKeys() {
        // keys are out of order deliberately
        final JqRequest request = ImmutableJqRequest.builder() //
                .lib(library) //
                .input("{\"b\":[1,2,3,4,5],\"a\":\"hello\"}") //
                .filter(".") //
                .sortKeys(true) //
                .build();

        assertEquals("{\n" + //
                "    \"a\": \"hello\",\n" + //
                "    \"b\": [\n" + //
                "        1,\n" + //
                "        2,\n" + //
                "        3,\n" + //
                "        4,\n" + //
                "        5\n" + //
                "    ]\n" + //
                "}", //
                executeJq(request));
    }

    @Test
    public void testInvalidInput() {
        final JqRequest request = ImmutableJqRequest.builder().lib(library).input("{{").filter(".").build();

        final JqResponse response = request.execute();
        assertTrue(response.hasErrors());
    }

    @Test
    public void testOniguruma() {
        // keys are out of order deliberately
        final JqRequest request = ImmutableJqRequest.builder() //
                .lib(library) //
                .input("\"abbbc\"") //
                .filter("match(\"(b+)\")") //
                .build();

        assertEquals("{\n" + //
                "    \"offset\": 1,\n" + //
                "    \"length\": 3,\n" + //
                "    \"string\": \"bbb\",\n" + //
                "    \"captures\": [\n" + //
                "        {\n" + //
                "            \"offset\": 1,\n" + //
                "            \"length\": 3,\n" + //
                "            \"string\": \"bbb\",\n" + //
                "            \"name\": null\n" + //
                "        }\n" + //
                "    ]\n" + //
                "}", //
                executeJq(request));
    }

    @Test
    public void testJq16Feature() {
        // make sure a JQ 1.6 feature works
        final JqRequest request = ImmutableJqRequest.builder() //
                .lib(library) //
                .input("123") //
                .filter("strflocaltime(\"%Y-%m-%dT%H:%M:%S %Z\")") //
                .build();

        assertEquals(
                "\"1969-12-31T19:02:03 EST\"", //
                executeJq(request));
    }

    private void checkJq(final String path) throws IOException {
        final String input = path + "/input.json";
        final String jq = path + "/jq.json";
        final JqRequest request = ImmutableJqRequest.builder() //
                .lib(library) //
                .input(readResource(input)) //
                .filter(readResource(jq)) //
                .build();

        final String output = path + "/output.json";
        Assert.assertEquals(readResource(output), executeJq(request));
    }

    private String executeJq(final JqRequest request) {
        final JqResponse response = request.execute();
        assertTrue(response.getErrors().toString(), !response.hasErrors());
        final String out = response.getOutput();
        return out;
    }

    private String readResource(final String resource) throws IOException {
        final URL url = this.getClass().getResource(resource);
        Assert.assertTrue("Resource does not exist: " + resource, resource != null);
        return Resources.toString(url, Charsets.UTF_8);
    }
}
