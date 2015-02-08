# cuda-maven-plugin
Maven plugin for building CUDA source files into PTX files. Perfect for JCuda.

This is a very simple MOJO that you can add to your project's pom.xml like this:

    <plugin>
      <groupId>com.sybrium</groupId>
      <artifactId>cuda-maven-plugin</artifactId>
      <version>0.0.1-SNAPSHOT</version>
      <executions>
        <execution>
          <phase>compile</phase>
          <goals>
            <goal>compile-cuda</goal>
          </goals>
        </execution>
      </executions>
    </plugin>

It will scan src/main/cuda recursively for *.cu files, compile them to ptx files and puts them on your classpath for you.

Pairs nicely with https://github.com/MysterionRise/mavenized-jcuda

NOTE: This was hacked together in a couple hours and this is my first Maven plugin. It's only been tested on Windows. And I'm a bit worried about the path manipulation for translating relative paths. Basically, please don't use this in production. And if you like it, contribute!
