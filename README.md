bazooka-wo-xmldoclet
====================

An improved clone of the [wo-xmldoclet](https://code.google.com/p/wo-xmldoclet/) Java doclet that generates XML data from annotated Java (5/6) source code.

Based on version 0.8.11 of [wo-xmldoclet](https://code.google.com/p/wo-xmldoclet/).

Maintained by Johan Johansson of [Bazooka](http://bazooka.se). To contact us, please e-mail info@bazooka.se.

This file is part of version **1.3.0 (05 March 2014)**. For the latest version, please go to the official github page: https://github.com/bazooka/bazooka-wo-xmldoclet.

What it does
------------
This is a doclet that can be specified when using the `javadoc` utility to generate documentation data from the source code to a single structured XML file instead of static HTML files.
This data can then be parsed and used in whatever way you want (e.g. a PHP parser).

How to build
------------
The process is to compile the Java files and then bundle them in a jar that can then be used in conjunction with `javadoc`.

You need to compile with the Java 6 compiler, so please install that version of the JDK/JRE first.

The following libraries must be referenced in the Java classpath variable (or direct to `javac`):

* jtidy-r938.jar (included in /lib)
* tools.jar (from the JDK lib folder)

Here's how to compile the files standing in the project root (make sure build folder exists):

    javac -d ./build src/org/weborganic/xmldoclet/*.java

Then use the following syntax to compile the class files into a jar standing in the build folder:

    jar -cf bazooka-wo-xmldoclet.jar org

How to use
----------
Use the following `javadoc` syntax to generate documentation data from a set of source files (the above libraries must be present in the classpath, the option -classpath doesn't seem to work quite right here):

    # generate documentation file from the folder "src" into folder "out" with packages "com.something.mypackage1" and "com.something.mypackage2"
    javadoc -docletpath ./bazooka-wo-xmldoclet.jar -doclet org.weborganic.xmldoclet.XMLDoclet -d ./out -sourcepath src com.something.mypackage1 com.something.mypackage2

For more instructions on how to use the `javadoc` utility, see the official docs here: http://docs.oracle.com/javase/1.5.0/docs/tooldocs/windows/javadoc.html.

List of modifications of the original doclet
--------------------------------------------
* Encodes HTML comments.
* Adds information about packages from the containing classes if no packages are specified at generate-time.
* Adds short comments (first sentence).
* Adds more class attributes (is ordinaryclass, is static, is error, is exception).
* Adds a flat inheritance tree tag for easier inheritance parsing.
* Adds enum constants information.
* Adds type dimension information.
* Adds method attributes (is varargs).
* Adds comments on @returns tag.
* Adds support for a number of tags such as @value, @deprecated, @since (and more).
* Generalizes inline @link and @see tags to share a common format, with more information about the link.
* ..And a lot more!

Changelog
---------
1.3.0
* Fixed a few bugs with the @see and @link tag URL building (generate fully qualified type names in the URL).
* Added a few missing tags such as @since and @value.
* Added standard block tags for constructors.
* Added information about a method's overridden parent.
* Disabled the JTidy word wrap (screwed up pre tags among other things).

1.2.1
* Added support for the deprecated tag (renders a shortDeprecated and a deprecated tag for relevant elements).