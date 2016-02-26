========================
BUILD OUTPUT DESCRIPTION
========================

When you build an Java application project that has a main class, the IDE
automatically copies all of the JAR
files on the projects classpath to your projects dist/lib folder. The IDE
also adds each of the JAR files to the Class-Path element in the application
JAR files manifest file (MANIFEST.MF).

To run the project from the command line, go to the dist folder and
type the following:

java -jar "Fuzzer.jar" 

Fuzzer must be run with additional arguments. There are 3 commands:

	Discover
	Test
	OPTIONS
	
Discover must also be run with a given URL. The user must specify the given URL that would liked fuzzed.
Discover does page guessing as well as input guessing.

Test is not yet implemented

OPTIONS pulls up the options panel. Although right now, the user must input the given URL, we are working on changing
that so the user only needs to input the OPTIONS command. 