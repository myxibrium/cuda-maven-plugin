package com.sybrium.cudamavenplugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Compiles CUDA source files using nvcc, then puts the resulting *.ptx files on
 * the classpath.
 */
@Mojo(name = "compile-cuda")
public class CudaCompileMojo extends AbstractMojo {

    /**
     * The source folder where CUDA source files are located.
     */
    @Parameter(property = "baseDirectory", defaultValue = "${project.basedir}/src/main/cuda")
    private File baseDirectory;

    /**
     * The extensions of the files to search for. Should leave this default.
     */
    @Parameter(property = "extensions", defaultValue = "cu")
    private String[] extensions;

    /**
     * The directory where the resulting ptx files go.
     */
    @Parameter(property = "outputDirectory", defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    /**
     * Determines whether the path of the source file relative to the
     * baseDirectory should be preserved in the ooutputDirectory. Should leave
     * this default.
     */
    @Parameter(property = "preservePath", defaultValue = "true")
    private Boolean preservePath;

    @Component
    private BuildContext buildContext;

    public void execute() throws MojoExecutionException {
        Collection<File> inputFiles = FileUtils.listFiles(baseDirectory, extensions, true);

        getLog().info("Compiling " + inputFiles.size() + " cuda source file(s) to " + outputDirectory + ",");

        for (File inputFile : inputFiles) {
            if (!buildContext.hasDelta(inputFile)) {
                continue;
            }
            buildContext.removeMessages(inputFile);
            String relativePath = getRelativePath(inputFile);
            String outputFileName;
            File outputFile;
            try {
                if (preservePath) {
                    outputFileName = stripExtension(outputDirectory.getCanonicalPath() + relativePath) + ".ptx";
                } else {
                    outputFileName = stripExtension(outputDirectory.getCanonicalPath() + File.separator
                            + inputFile.getName())
                            + ".ptx";
                }
                outputFile = new File(outputFileName);
                outputFile.getParentFile().mkdirs();
            } catch (IOException e) {
                buildContext.addMessage(inputFile, 0, 0, "Could not create output directories.",
                        BuildContext.SEVERITY_ERROR, e);
                continue;
            }
            try {
                String command = "nvcc -ptx " + inputFile.getCanonicalPath() + " -o " + outputFileName;
                Process process = Runtime.getRuntime().exec(command);
                int exitValue = 0;
                exitValue = process.waitFor();
                if (exitValue != 0) {
                    interpretErrors(inputFile, process);
                }
                getLog().info("Compiled " + outputFileName);
            } catch (Exception e) {
                buildContext.addMessage(inputFile, 0, 0, "Could not compile.", BuildContext.SEVERITY_ERROR, e);
                continue;
            }
            buildContext.refresh(outputFile);
        }
    }

    private void interpretErrors(File inputFile, Process process) {
        Scanner scan = new Scanner(process.getErrorStream());
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            Pattern errorPattern = Pattern.compile(".*?[.]cu[(]([0-9]+)[)]: error: (.*)");
            Matcher matcher = errorPattern.matcher(line);
            if (matcher.matches()) {
                Integer lineNumber = Integer.parseInt(matcher.group(1));
                String message = matcher.group(2);
                buildContext.addMessage(inputFile, lineNumber, 1, message, BuildContext.SEVERITY_ERROR, null);
            }
        }
        scan.close();
    }

    private String getRelativePath(File inputFile) throws MojoExecutionException {
        try {
            return inputFile.getCanonicalPath().substring(baseDirectory.getCanonicalPath().length());
        } catch (IOException e) {
            throw new MojoExecutionException("Could not get relative path of resource.", e);
        }
    }

    private String stripExtension(String str) {
        int lastIndexOfPeriod = str.lastIndexOf('.');
        if (lastIndexOfPeriod == -1) {
            return str;
        } else {
            return str.substring(0, lastIndexOfPeriod);
        }
    }
}
