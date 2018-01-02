# java-jq

java-jq is not a re-implementation of [jq](http://stedolan.github.io/jq/) in Java; instead, it embeds 
the necessary jq and Oniguruma native libraries in a jar file, and then uses 
[Java Native Access](https://github.com/java-native-access/jna) (JNA) to call the 
embedded libraries in a Java-friendly way.

The Maven Central distribution of java-jq includes native JQ 1.5 libraries for all major platforms (Mac, Windows and Linux).

java-jq is heavily inspired by [jjq](https://github.com/bskaggs/jjq).


## Usage

Using Java-JQ is very easy.


First, let's get a reference to the Native JQ library. This class is thread-safe and should be shared globally.

```
JqLibrary library = ImmutableJqLibrary.builder().build();
```

Now, let's create a JQ request. A "request" is an immutable bean that contains three basic elements: a reference
to the JQ library we created above, the JSON input you want to transform, and the JQ filter expression that you
want to execute.

```
final JqRequest request = ImmutableJqRequest.builder() //
        .lib(library) //
        .input("{\"a\":[1,2,3,4,5],\"b\":\"hello\"}") //
        .filter(".") //
        .build();
```

As a final step, let's execute the request.

```
final JqResponse response = request.execute();
if( response.hasError ) {
   // display errors in response.getErrors()
} else {
   System.out.println( "JQ output: " + response.getOutput);
}
```

## Installation

The library is available on Maven Central

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
    <version>0.9.0</version>
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
  testCompile 'com.arakelian:java-jq:0.9.0'
}
```

## Licence

Apache Version 2.0
