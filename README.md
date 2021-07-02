# java-jq

java-jq is not a re-implementation of [jq](http://stedolan.github.io/jq/) in Java; instead, 
it embeds the necessary jq and Oniguruma native libraries in a jar file, and then uses 
[Java Native Access](https://github.com/java-native-access/jna) (JNA) to call the embedded 
libraries in a Java-friendly way.

The distribution of java-jq includes native JQ 1.6 libraries for all major platforms (Mac, Windows and Linux), 
and includes a statically linked version of Oniguruma 5.9.6 to avoid any runtime compatibility issues.

java-jq was heavily inspired by [jjq](https://github.com/bskaggs/jjq).


## Usage

Using Java-JQ is very easy.


First, let's get a reference to the Native JQ library. This class is a thread-safe singleton.

```java
JqLibrary library = ImmutableJqLibrary.of();
```

Now, let's create a JQ request. A "request" is an immutable bean that contains three basic elements: a reference
to the JQ library we created above, the JSON input you want to transform, and the JQ filter expression that you
want to execute.

```java
final JqRequest request = ImmutableJqRequest.builder() //
        .lib(library) //
        .input("{\"a\":[1,2,3,4,5],\"b\":\"hello\"}") //
        .filter(".") //
        .build();
```

As a final step, let's execute the request.

```java
final JqResponse response = request.execute();
if( response.hasErrors() ) {
   // display errors in response.getErrors()
} else {
   System.out.println( "JQ output: " + response.getOutput());
}
```

## Compatibility

As of version 1.1.0, java-jq successfully executes the complete [jq](http://stedolan.github.io/jq/) 
test suite, including all tests in jq.test, onig.test, base64.test, and optional.test.

java-jq supports modules as well. To use modules, include the directory paths where your modules
can be found with your JqRequest as follows: 

```java
final JqRequest request = ImmutableJqRequest.builder() //
        .lib(library) //
        .input("your json goes here") //
        .filter(".") //
        .addModulePath(new File("/some/modules/can/be/found/here")) //
        .addModulePath(new File("/other/modules/can/be/found/here")) //
        .build();
```
  

## Installation

The library is available on [Maven Central](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.arakelian%22%20AND%20a%3A%22java-jq%22).

### Maven

Add the following to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>central</id>
        <name>Central Repository</name>
        <url>http://repo.maven.apache.org/maven2</url>
        <releases>
            <enabled>true</enabled>
        </releases>
    </repository>
</repositories>

...

<dependency>
    <groupId>com.arakelian</groupId>
    <artifactId>java-jq</artifactId>
    <version>1.1.0</version>
    <scope>test</scope>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

```groovy
repositories {
  mavenCentral()
}

dependencies {
  testCompile 'com.arakelian:java-jq:1.1.0'
}
```

## Licence

Apache Version 2.0
