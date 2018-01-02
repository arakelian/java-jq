package com.arakelian.jq;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class JqTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(JqTest.class);

    private final JqLibrary library = ImmutableJqLibrary.builder().build();

    @Test
    public void testFree() {
        final JqRequest request = ImmutableJqRequest.builder() //
                .lib(library) //
                .input("{\"a\":[1,2,3,4,5],\"b\":\"hello\"}") //
                .filter(".") //
                .build();

        // iterate multiple times to ensure no native "free" memory issues
        for (int i = 0; i < 10; i++) {
            LOGGER.debug("Iteration {}", i);
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

        Assert.assertEquals("{\n" + //
                "    \"a\": \"hello\",\n" + //
                "    \"b\": [\n" + //
                "        1,\n" + //
                "        2,\n" + //
                "        3,\n" + //
                "        4,\n" + //
                "        5\n" + //
                "    ]\n" + //
                "}", executeJq(request));
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

    private String readResource(String resource) throws IOException {
        final URL url = this.getClass().getResource(resource);
        Assert.assertTrue("Resource does not exist: " + resource, resource != null);
        return Resources.toString(url, Charsets.UTF_8);
    }

    private String executeJq(final JqRequest request) {
        final JqResponse response = request.execute();
        assertTrue(response.getErrors().toString(), !response.hasErrors());
        final String out = response.getOutput();
        return out;
    }
}
