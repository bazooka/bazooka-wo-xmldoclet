/*
 * This file is part of the Weborganic XMLDoclet library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.xmldoclet;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;
import com.sun.javadoc.AnnotationTypeElementDoc;
import com.sun.javadoc.AnnotationValue;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;
import com.sun.javadoc.ThrowsTag;
import com.sun.javadoc.TypeVariable;
import com.sun.tools.doclets.Taglet;

/**
 * The Doclet implementation to use with javadoc.
 *
 * <p>A Doclet to be used with JavaDoc which will output XML with all of the information from the JavaDoc.
 *
 * @author Christophe Lauret
 * @author Johan Johansson
 * @version 24 May 2013
 */
public final class XMLDoclet {
  public final static String VERSION = "1.3.1";

  /**
   * The date format matching ISO 8601, easier to parse with XSLT.
   */
  private static final String ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss";

  // Methods required by doclet specifications ====================================================

  /**
   * The method used by this method invocation.
   */
  private static Options options = null;

  public static RootDoc root;

  /**
   * Processes the JavaDoc documentation.
   *
   * <p>This method is required for all doclets.
   *
   * @see com.sun.javadoc.Doclet#start(RootDoc)
   *
   * @param root The root of the documentation tree.
   *
   * @return <code>true</code> if processing was successful.
   */
  public static boolean start(RootDoc root) {
    XMLDoclet.root = root;

    // Create the root node.
    List<XMLNode> nodes = toXMLNodes(root);

    // Save the output XML
    save(nodes);

    return true;
  }

  /**
   * Returns the version of the Java Programming Language supported by this Doclet.
   *
   * <p>This Doclet supports Java 5.
   *
   * @see com.sun.javadoc.Doclet#languageVersion()
   *
   * @return {@value LanguageVersion#JAVA_1_5}
   */
  public static LanguageVersion languageVersion() {
    return LanguageVersion.JAVA_1_5;
  }

  /**
   * Returns the number of arguments required for the given option.
   *
   * <p>This method calls {@link Options#getLength(String)}.
   *
   * @see com.sun.javadoc.Doclet#optionLength(String)
   * @see Options#getLength(String)
   *
   * @param option The name of the option.
   *
   * @return The number of arguments for that option.
   */
  public static int optionLength(String option) {
    return Options.getLength(option);
  }

  /**
   * Check that options have the correct arguments.
   *
   * <p>This method is not required, but is recommended, as every option will be considered valid
   * if this method is not present. It will default gracefully (to true) if absent.
   *
   * <p>Printing option related error messages (using the provided DocErrorReporter) is the
   * responsibility of this method.
   *
   * @see com.sun.javadoc.Doclet#validOptions(String[][], DocErrorReporter)
   *
   * @param options The two dimensional array of options.
   * @param reporter The error reporter.
   *
   * @return <code>true</code> if the options are valid.
   */
  public static boolean validOptions(String o[][], DocErrorReporter reporter) {
    options = Options.toOptions(o, reporter);
    // OK if we could set up the options.
    return options != null;
  }

  // Methods specific to this doclet implementation ===============================================

  /**
   * Save the given array of nodes.
   *
   * Will either save the files individually, or as a single file depending on the existence of the "-multiple" flag.
   *
   * @param nodes The array of nodes to be saved.
   */
  private static void save(List<XMLNode> nodes) {
    // Add admin node
    XMLNode meta = new XMLNode("meta");
    DateFormat df = new SimpleDateFormat(ISO_8601);
    meta.attribute("created", df.format(new Date()));

    XMLNode meta2 = new XMLNode("meta");
    meta2.attribute("generator", VERSION);

    // Multiple files
    if (options.useMultipleFiles()) {
      for (XMLNode node : nodes) {
        File dir = options.getDirectory();
        String name = node.getAttribute("name");
        if (options.useSubFolders()) {
          name = name.replace('.', '/');
          int x = name.lastIndexOf('/');
          if (x >= 0) {
            dir = new File(dir, name.substring(0,x));
            dir.mkdirs();
            name = name.substring(x+1);
          }
        }
        XMLNode root = new XMLNode("root");
        root.attribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
        root.child(meta);
        root.child(meta2);
        root.child(node);
        String fileName = name + ".xml";
        root.save(dir, fileName, options.getEncoding(), "");
      }
      // Index
      XMLNode root = new XMLNode("root");
      root.attribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
      root.child(meta);
      root.child(meta2);
      for (XMLNode node : nodes) {
        String name = node.getAttribute("name");
        if (options.useSubFolders()) name = name.replace('.', '/');
        XMLNode ref = new XMLNode(node.getName());
        ref.attribute("xlink:type", "simple");
        ref.attribute("xlink:href", name + ".xml");
        root.child(ref);
      }
      String fileName = "index.xml";
      root.save(options.getDirectory(), fileName, options.getEncoding(), "");

    // Single file
    } else {
      // Wrap the XML
      XMLNode root = new XMLNode("root");
      root.attribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
      root.child(meta);
      root.child(meta2);
      for (XMLNode node : nodes) {
        root.child(node);
      }
      root.save(options.getDirectory(), options.getFilename(), options.getEncoding(), "");
    }

  }

  /**
   * Returns the XML nodes for all the selected classes in the specified RootDoc.
   *
   * @param root The RootDoc from which the XML should be built.
   * @return The list of XML nodes which represents the RootDoc.
   */
  private static List<XMLNode> toXMLNodes(RootDoc root) {
    List<XMLNode> nodes = new ArrayList<XMLNode>();

    // Iterate over the classes
    for (ClassDoc doc : root.classes()) {
      if (options.filter(doc)) {
        nodes.add(toClassNode(doc));
      }
    }

    // Iterate over packages
    if (!options.hasFilter()) {
      if (root.specifiedPackages().length > 0) {
        for (PackageDoc doc : root.specifiedPackages()) {
          nodes.add(toPackageNode(doc));
        }
      } else {
        // no packages specified, iterate over classes and fetch the package that way
        List<PackageDoc> packages = new ArrayList<PackageDoc>();

        for (ClassDoc doc : root.classes()) {
          PackageDoc pkg = doc.containingPackage();

          if (pkg != null && !packages.contains(pkg)) {
            packages.add(pkg);
            nodes.add(toPackageNode(pkg));
          }
        }
      }
    }

    return nodes;
  }

  /**
   * Returns the XML node corresponding to the specified ClassDoc.
   *
   * @param doc The package to transform.
   */
  private static XMLNode toPackageNode(PackageDoc doc) {
    XMLNode node = new XMLNode("package", doc);

    // Core attributes
    node.attribute("name", doc.name());

    // Comments
    node.child(toShortComment(doc));
    node.child(toComment(doc));

    // Child nodes
    node.child(toAnnotationsNode(doc.annotations()));
    node.child(toStandardTags(doc));
    node.child(toTags(doc));
    node.child(toSeeNodes(doc.seeTags()));

    return node;
  }

  /**
   * Returns the XML node corresponding to the specified ClassDoc.
   *
   * @param classDoc The class to transform.
   */
  private static XMLNode toClassNode(ClassDoc classDoc) {
     XMLNode node = new XMLNode("class", classDoc);

    // Core attributes
    node.attribute("type",       classDoc.name());
    node.attribute("fulltype",   classDoc.qualifiedName());
    node.attribute("name",       classDoc.qualifiedName());
    node.attribute("package",    classDoc.containingPackage().name());
    node.attribute("visibility", getVisibility(classDoc));

    // generic type parameters
    node.child(toTypeParametersNode(classDoc));

    // Interfaces
    ClassDoc[] interfaces = classDoc.interfaces();
    if (interfaces.length > 0) {
      XMLNode implement = new XMLNode("implements");
      for (ClassDoc i : interfaces) {
        XMLNode interfce = new XMLNode("interface");
        interfce.attribute("type", i.name());
        interfce.attribute("fulltype", i.qualifiedName());
        implement.child(interfce);
      }
      node.child(implement);
    }

    // Superclass
    if (classDoc.superclass() != null) {
      node.attribute("superclass", classDoc.superclass().name());
      node.attribute("superclassfulltype", classDoc.superclass().qualifiedName());
    }

    // Class properties
    node.attribute("ordinaryClass", classDoc.isOrdinaryClass());
    node.attribute("interface",     classDoc.isInterface());
    node.attribute("final",         classDoc.isFinal());
    node.attribute("static",        classDoc.isStatic());
    node.attribute("abstract",      classDoc.isAbstract());
    node.attribute("serializable",  classDoc.isSerializable());
    node.attribute("enum",          classDoc.isEnum());
    node.attribute("error",         classDoc.isError());
    node.attribute("exception",     classDoc.isException());

    // Comments
    node.child(toShortComment(classDoc));
    node.child(toComment(classDoc));

    // Deprecated
    toDeprecated(classDoc, node);

     // Inheritance tree
    XMLNode inheritance = new XMLNode("inheritance");
    ClassDoc superClass = classDoc.superclass();
    if (superClass != null) {
      while (superClass != null) {
        XMLNode tmp = new XMLNode("class");
        tmp.attribute("type", superClass.name());
        tmp.attribute("fulltype", superClass.qualifiedName());
        inheritance.child(tmp);
        superClass = superClass.superclass();
      }
      node.child(inheritance);
    }

    // Other child nodes
    node.child(toAnnotationsNode(classDoc.annotations()));
    node.child(toStandardTags(classDoc));
    node.child(toTags(classDoc));
    node.child(toSeeNodes(classDoc.seeTags()));
    node.child(toFieldsNode(classDoc.fields()));
    node.child(toEnumConstantsNode(classDoc.enumConstants()));
    node.child(toConstructorsNode(classDoc.constructors()));
    node.child(toMethods(classDoc.methods()));

    // Handle inner classes
    for (ClassDoc inner : classDoc.innerClasses()) {
      node.child(toClassNode(inner));
    }

    return node;
  }

  /**
   * Generates a child node for each generic type parameter.
   * @param  doc [description]
   * @return     [description]
   */
  private static XMLNode toTypeParametersNode(ClassDoc doc) {
    TypeVariable[] typeParameters = doc.typeParameters();
    ParamTag[] typeParameterTags = doc.typeParamTags();

    XMLNode node = null;

    if (typeParameters != null && typeParameters.length > 0) {
      node = new XMLNode("typeParameters");

      for (TypeVariable i : typeParameters) {
        XMLNode paramNode = new XMLNode("parameter");
        paramNode.attribute("name", i.typeName());

        Type[] bounds = i.bounds();

        if (bounds != null && bounds.length > 0) {
          XMLNode boundsNode = new XMLNode("bounds");

          for (Type t : bounds) {
            XMLNode typeNode = new XMLNode("type");

            typeNode.attribute("fulltype", t.toString());
            typeNode.attribute("type", t.typeName());
            typeNode.attribute("name", t.qualifiedTypeName());

            boundsNode.child(typeNode);
          }

          paramNode.child(boundsNode);
        }

        if (typeParameterTags != null && typeParameterTags.length > 0) {
          ParamTag tag = null;

          for (ParamTag j : typeParameterTags) {
            // take the first one
            if (j.isTypeParameter() && j.parameterName().equals(i.typeName())) {
              tag = j;
              break;
            }
          }

          if (tag != null) {
            XMLNode commentNode = new XMLNode("comment");
            commentNode.text(toComment(tag));
            paramNode.child(commentNode);
          }
        }

        node.child(paramNode);
      }
    }

    return node;
  }

  /**
   * Returns the specified field as an XML node.
   *
   * @param field A field.
   * @return The corresponding node.
   */
  private static XMLNode toFieldNode(FieldDoc field) {
    // Create the <field> node and populate it.
    XMLNode node = new XMLNode("field");
    node.attribute("name", field.name());
    node.attribute("type", field.type().typeName());
    node.attribute("fulltype", field.type().toString());
    node.attribute("dimension", field.type().dimension());

    if (field.constantValue() != null && field.constantValue().toString().length() > 0)
      node.attribute("const", field.constantValue().toString());

    if (field.constantValueExpression() != null && field.constantValueExpression().length() > 0)
      node.attribute("constexpr", field.constantValueExpression());

    node.attribute("static", field.isStatic());
    node.attribute("final", field.isFinal());
    node.attribute("transient", field.isTransient());
    node.attribute("volatile", field.isVolatile());
    node.attribute("visibility", getVisibility(field));

    // Comment
    node.child(toShortComment(field));
    node.child(toComment(field));

    // Deprecated
    toDeprecated(field, node);

    // Other child nodes
    node.child(toStandardTags(field));
    node.child(toTags(field));
    node.child(toSeeNodes(field.seeTags()));

    return node;
  }

  /**
   * Returns the .
   *
   * @param constructors The constructors.
   * @param node The node to add the XML to.
   */
  private static XMLNode toConstructorsNode(ConstructorDoc[] constructors) {
    if (constructors.length < 1) return null;

    // Create the <constructors> node
    XMLNode node = new XMLNode("constructors");

    // Add the <constructor> nodes
    for (ConstructorDoc  constructor : constructors) {
      XMLNode c = new XMLNode("constructor");
      updateExecutableMemberNode(constructor, c);
      // standard block tags
      c.child(toStandardTags(constructor));
      // deprecated
      toDeprecated(constructor, c);
      node.child(c);
    }

    return node;
  }

  /**
   * Transforms an array of methods and an array of constructor methods into XML and adds those to the host node.
   *
   * @param methods The methods.
   * @param constructors The constructors.
   * @param node The node to add the XML to.
   */
  private static XMLNode toMethods(MethodDoc[] methods) {
    if (methods.length < 1) return null;

    // Create the <methods> node
    XMLNode node = new XMLNode("methods");

    // Add the <method> nodes
    for (MethodDoc method : methods) {
      XMLNode methodNode = new XMLNode("method");

      updateExecutableMemberNode(method, methodNode);

      methodNode.attribute("type",     method.returnType().typeName());
      methodNode.attribute("fulltype", method.returnType().toString());
      methodNode.attribute("dimension", method.returnType().dimension());
      methodNode.attribute("abstract", method.isAbstract());
      methodNode.attribute("varargs", method.isVarArgs());

      MethodDoc overrides = method.overriddenMethod();
      if (overrides != null) {
        methodNode.attribute("overrides", overrides.toString());
      }

      Tag[] returnTags = method.tags("@return");
      if (returnTags.length > 0) {
        XMLNode returnNode = new XMLNode("returns");
        returnNode.text(toComment(returnTags[0]));
        methodNode.child(returnNode);
      }

      // standard block tags
      methodNode.child(toStandardTags(method));

      // Deprecated
      toDeprecated(method, methodNode);

      node.child(methodNode);
    }

    return node;
  }

  /**
   * Returns the fields node.
   *
   * @param fields The set of fields.
   * @return the fields or <code>null</code> if none.
   */
  private static XMLNode toFieldsNode(FieldDoc[] fields) {
    if (fields.length < 1) return null;
    // Iterate over the fields
    XMLNode node = new XMLNode("fields");
    for (FieldDoc field : fields) {
      node.child(toFieldNode(field));
    }
    return node;
  }

  /**
   * Returns the enumConstants node.
   *
   * @param fields The set of fields.
   * @return the fields or <code>null</code> if none.
   */
  private static XMLNode toEnumConstantsNode(FieldDoc[] fields) {
    if (fields.length < 1) return null;
    // Iterate over the fields
    XMLNode node = new XMLNode("enumConstants");
    for (FieldDoc field : fields) {
      node.child(toFieldNode(field));
    }
    return node;
  }

  /**
   * Set the commons attribute and child nodes for method and constructor nodes.
   *
   * @param member The executable member documentation.
   * @param node The node to update
   */
  private static void updateExecutableMemberNode(ExecutableMemberDoc member, XMLNode node) {
    // Add the basic attribute values
    node.attribute("name",         member.name());
    node.attribute("static",       member.isStatic());
    node.attribute("interface",    member.isInterface());
    node.attribute("final",        member.isFinal());
    node.attribute("visibility",   getVisibility(member));
    node.attribute("synchronized", member.isSynchronized());
    node.attribute("synthetic",    member.isSynthetic());

    // Comments
    node.child(toShortComment(member));
    node.child(toComment(member));

    // Other objects attached to the method/constructor.
    node.child(toTags(member));
    node.child(toSeeNodes(member.seeTags()));
    node.child(toParametersNode(member.parameters(),       member.paramTags(), member.isVarArgs()));
    node.child(toExceptionsNode(member.thrownExceptionTypes(), member.throwsTags()));
  }

  /**
   * Transforms common tags on the Doc object into XML.
   *
   * @param doc The Doc object.
   * @return The corresponding list of nodes.
   */
  private static List<XMLNode> toStandardTags(Doc doc) {
    // Create the comment node
    List<XMLNode> nodes = new ArrayList<XMLNode>();

    // Handle the tags
    for (Tag tag : doc.tags()) {
      Taglet taglet = options.getTagletForName(tag.name().length() > 1? tag.name().substring(1) : "");
      if (taglet instanceof BlockTag) {
        nodes.add(((BlockTag) taglet).toXMLNode(tag));
      }
    }

    // Add the node to the host
    return nodes;
  }

  /**
   * Transforms comments on the Doc object into XML.
   *
   * @param doc The Doc object.
   * @param node The node to add the comment nodes to.
   */
  private static XMLNode toTags(Doc doc) {
    // Create the comment node
    XMLNode node = new XMLNode("tags");

    boolean hasTags = false;

    // Handle the tags
    for (Tag tag : doc.tags()) {
      Taglet taglet = options.getTagletForName(tag.name().length() > 1? tag.name().substring(1) : "");
      if (taglet != null && !(taglet instanceof BlockTag)) {
        XMLNode tNode = new XMLNode("tag");
        tNode.attribute("name", tag.name());
        tNode.text(taglet.toString(tag));
        node.child(tNode);
        hasTags = true;
      }
    }

    // Add the node to the host
    return hasTags? node : null;
  }

  // Aggregate XML methods ========================================================================

  /**
   * Returns the XML for the specified parameters using the param tags for additional description.
   *
   * @param parameters parameters instances to process
   * @param tags       corresponding parameter tags (not necessarily in the same order)
   *
   * @return the XML for the specified parameters using the param tags for additional description.
   */
  private static XMLNode toParametersNode(Parameter[] parameters, ParamTag[] tags, boolean isVarArgs) {
    if (parameters.length == 0) return null;

    // Iterate over the parameters
    XMLNode node = new XMLNode("parameters");
    int pos = 0;
    for (Parameter parameter : parameters) {
      XMLNode p = toParameterNode(parameter, find(tags, parameter.name()), isVarArgs && pos++ == (parameters.length - 1));
      node.child(p);
    }

    return node;
  }

  /**
   * Returns the XML for the specified exceptions using the throws tags for additional description.
   *
   * @param exceptions exceptions instances to process
   * @param tags       corresponding throws tags (not necessarily in the same order)
   *
   * @return the XML for the specified parameters using the param tags for additional description.
   */
  private static XMLNode toExceptionsNode(Type[] exceptions, ThrowsTag[] tags) {
    if (exceptions.length == 0) return null;

    // Iterate over the exceptions
    XMLNode node = new XMLNode("exceptions");
    for (Type exception : exceptions) {
      XMLNode n = toExceptionNode(exception, find(tags, exception.typeName()));
      node.child(n);
    }

    return node;
  }

  /**
   * Transforms comments on the Doc object into XML.
   *
   * @param doc The Doc object.
   * @param node The node to add the comment nodes to.
   */
  private static List<XMLNode> toSeeNodes(SeeTag[] tags) {
    if (tags == null || tags.length == 0) return Collections.emptyList();

    List<XMLNode> nodes = new ArrayList<XMLNode>(tags.length);
    for (SeeTag tag : tags) {
      XMLNode n = toSeeNode(tag);
      if (n != null) nodes.add(n);
    }

    // Add the node to the host
    return nodes;
  }

  /**
   * Returns the XML node corresponding to the specified ClassDoc.
   *
   * @param classDoc The class to transform.
   */
  private static XMLNode toAnnotationsNode(AnnotationDesc[] annotations) {
    if (annotations.length < 1) return null;

    XMLNode node = new XMLNode("annotations");
    for (AnnotationDesc annotation : annotations) {
      node.child(toAnnotationNode(annotation));
    }

    return node;
  }

  // Atomic XML methods ===========================================================================

  /**
   * Returns the XML for a see tag.
   *
   * @param tag The See tag to process.
   */
  private static XMLNode toSeeNode(SeeTag tag) {
    if (tag == null) return null;
    XMLNode see = new XMLNode("see");

    boolean multiple = options.useMultipleFiles();

    // A link
    if (tag.text().startsWith("<a")) {
      String text = tag.text();
      Matcher href = Pattern.compile("href=\"(.+)\"").matcher(text);
      if (href.find()) {
        see.attribute("type", "custom");
        see.attribute("href", href.group(1));
      }
      Matcher title = Pattern.compile("\\>(.+)\\<\\/").matcher(text);
      if (title.find()) {
        see.attribute("title", title.group(1));
      }

    } else {
    // A referenced Package, class and/or member

      String pkg = null;
      String cls = null;
      String mbr = null;
      String type = tag.referencedMember() != null ? "member" : tag.referencedClass() != null ? "class" : tag.referencedPackage() != null ? "package" : "unknown";

      if (tag.referencedPackage() != null) {
        pkg = tag.referencedPackage().name();
      } else if (tag.referencedClass() != null && tag.referencedClass().containingPackage() != null) {
        pkg = tag.referencedClass().containingPackage().name();
      }

      cls = tag.referencedClass() != null ? tag.referencedClass().name() : null;
      mbr = tag.referencedMemberName();

      see.attribute("type", type);

      if (type == "unknown") {
        see.attribute("text", tag.text());
      } else {
        see.attribute("package", pkg);

        if (cls != null && cls.length() > 0) {
          see.attribute("class", cls);
        }

        String label = tag.label();

        if (tag.referencedMember() != null && tag.referencedMember() instanceof ExecutableMemberDoc) {
          ExecutableMemberDoc memberDoc = (ExecutableMemberDoc)tag.referencedMember();
          mbr = memberDoc.name() + memberDoc.signature();

          if (label == null || label.length() == 0) {
            label = memberDoc.name() + memberDoc.flatSignature();
          }
        }

        if (mbr != null && mbr.length() > 0) {
          see.attribute("member", mbr);
        }

        see.attribute("title", label);
      }
    }

    return see;
  }

  /**
   * Returns the XML for a parameter and its corresponding param tag.
   *
   * @return The corresponding XML.
   */
  private static XMLNode toParameterNode(Parameter parameter, ParamTag tag, boolean isVarArgs) {
    if (parameter == null) return null;
    XMLNode node = new XMLNode("parameter");
    node.attribute("name", parameter.name());
    node.attribute("type", parameter.type().typeName());
    node.attribute("fulltype", parameter.type().toString());

    String dimension = parameter.type().dimension();

    if (isVarArgs) {
      dimension = dimension.replaceAll("\\[\\]$", "") + "...";
    }

    node.attribute("dimension", dimension);
    node.attribute("varargs", isVarArgs);

    if (tag!= null) {
      node.text(toComment(tag));
    }

    return node;
  }

  /**
   * Returns the XML for an exception and its corresponding throws tag.
   *
   * @return The corresponding XML.
   */
  private static XMLNode toExceptionNode(Type exception, ThrowsTag tag) {
    if (exception == null) return null;
    XMLNode node = new XMLNode("exception");
    node.attribute("type", exception.typeName());
    node.attribute("fulltype", exception.qualifiedTypeName());
    if (tag != null) {
      node.attribute("comment", tag.exceptionComment());
      node.text(toComment(tag));
    }
    return node;
  }

  /**
   * Transforms comments on the Doc object into XML.
   *
   * @param doc The Doc object.
   * @param node The node to add the comment nodes to.
   */
  private static XMLNode toComment(Doc doc) {
    if (doc.commentText() == null || doc.commentText().length() == 0) return null;
    XMLNode node = new XMLNode("comment", doc, doc.position().line());
    StringBuilder comment = new StringBuilder();

    // Analyse each token and produce comment node
    for (Tag t : doc.inlineTags()) {
      Taglet taglet = options.getTagletForName(t.name());
      if (taglet != null) comment.append(taglet.toString(t));
      else comment.append(t.text());
    }

    return node.text(comment.toString());
  }

  /**
   * Transforms short comments on the Doc object into XML.
   *
   * @param doc The Doc object.
   * @param node The node to add the comment nodes to.
   */
  private static XMLNode toShortComment(Doc doc) {
    if (doc.commentText() == null || doc.commentText().length() == 0) return null;
    XMLNode node = new XMLNode("shortComment", doc, doc.position().line());
    StringBuilder comment = new StringBuilder();

    // Analyse each token and produce comment node
    for (Tag t : doc.firstSentenceTags()) {
      Taglet taglet = options.getTagletForName(t.name());
      if (taglet != null) comment.append(taglet.toString(t));
      else comment.append(t.text());
    }

    return node.text(comment.toString());
  }

  /**
   * Transforms comments on the Doc object into XML.
   *
   * @param doc The Doc object.
   * @param node The node to add the comment nodes to.
   */
  private static String toComment(Tag tag) {
    if (tag.text() == null || tag.text().length() == 0) return null;
    StringBuilder comment = new StringBuilder();

    // Analyse each token and produce comment node
    for (Tag t : tag.inlineTags()) {
      Taglet taglet = options.getTagletForName(t.name());
      if (taglet != null) comment.append(taglet.toString(t));
      else comment.append(t.text());
    }

    return comment.toString();
  }

  /**
   * Transforms deprecated tags on the Doc object into XML.
   *
   * @param doc The Doc object.
   * @param node The node to add the deprecated nodes to.
   */
  private static XMLNode toDeprecated(Doc doc, XMLNode node) {
      Tag[] deprecatedTags = doc.tags("@deprecated");

      if (deprecatedTags.length > 0) {
        StringBuilder shortText = new StringBuilder();

        // Analyse each token and produce text node
        for (Tag t : deprecatedTags[0].firstSentenceTags()) {
          Taglet taglet = options.getTagletForName(t.name());
          if (taglet != null) shortText.append(taglet.toString(t));
          else shortText.append(t.text());
        }

        if (shortText.length() == 0) {
          shortText.append("Deprecated.");
        }

        String fullText = toComment(deprecatedTags[0]);
        fullText = fullText != null ? fullText.substring(shortText.length()).trim() : "";

        XMLNode shortNode = new XMLNode("shortDeprecated", doc, doc.position().line());
        XMLNode fullNode = new XMLNode("deprecated", doc, doc.position().line());

        if (shortText.toString().length() > 0) {
          shortNode.text(shortText.toString());
          node.child(shortNode);
        }

        if (fullText.length() > 0) {
          fullNode.text(fullText);
          node.child(fullNode);
        }
      }

      return null;
  }

  /**
   *
   * @return an "annotation" XML node for the annotation.
   */
  private static XMLNode toAnnotationNode(AnnotationDesc annotation) {
    if (annotation == null) return null;
    XMLNode node = new XMLNode("annotation");
    node.attribute("name", annotation.annotationType().name());
    for (ElementValuePair pair : annotation.elementValues()) {
      node.child(toPairNode(pair));
    }
    return node;
  }

  /**
   *
   * @return an "element" XML node for the element value pair.
   */
  private static XMLNode toPairNode(ElementValuePair pair) {
    if (pair == null) return null;
    XMLNode node = new XMLNode("element");
    AnnotationTypeElementDoc element = pair.element();
    node.attribute("name", element.name());
    node.child(toShortComment(element));
    node.child(toComment(element));
    node.child(toAnnotationValueNode(pair.value()));
    return node;
  }

  /**
   *
   * @return an "value" or "array" XML node for the annotation value.
   */
  private static XMLNode toAnnotationValueNode(AnnotationValue value) {
    if (value == null) return null;
    XMLNode node = null;
    Object o = value.value();
    Class<?> c = o.getClass();
    if (c.isArray()) {
      node = new XMLNode("array");
      Object[]a = (Object[])o;
      for (Object i : a) {
        if (i instanceof AnnotationValue) {
          node.child(toAnnotationValueNode((AnnotationValue)i));
        } else {
          System.err.println("Unexpected annotation value type"+i);
        }
      }
    } else {
      node = new XMLNode("value");
      node.attribute("type", getAnnotationValueType(o));
      node.attribute("value", o.toString());
    }

    return node;
  }

  // Utilities ====================================================================================

  /**
   * Sets the visibility for the class, method or field.
   *
   * @param member The member for which the visibility needs to be set (class, method, or field).
   * @param node The node to which the visibility should be set.
   */
  private static String getVisibility(ProgramElementDoc member) {
    if (member.isPrivate()) return "private";
    if (member.isProtected()) return "protected";
    if (member.isPublic()) return "public";
    if (member.isPackagePrivate()) return "package-private";
    // Should never happen
    return null;
  }

  /**
   * Find the corresponding throws tag
   *
   * @return
   */
  private static ThrowsTag find(ThrowsTag[] tags, String name){
    for (ThrowsTag tag : tags) {
      if (tag.exceptionName().equalsIgnoreCase(name)) {
        return tag;
      }
    }
    return null;
  }

  /**
   * Find the corresponding parameter tag.
   *
   * @return
   */
  private static ParamTag find(ParamTag[] tags, String name){
    for (ParamTag tag : tags) {
      if (tag.parameterName().equalsIgnoreCase(name)) {
        return tag;
      }
    }
    return null;
  }

  /**
   * Returns the value type of the annotation depending on the specified object's class.
   *
   * @param o the object representing the type of annotation value.
   * @return the primitive if any of full class name.
   */
  private static String getAnnotationValueType(Object o) {
    if (o instanceof String)  return "String";
    if (o instanceof Integer) return "int";
    if (o instanceof Boolean) return "boolean";
    if (o instanceof Long)    return "long";
    if (o instanceof Short)   return "short";
    if (o instanceof Float)   return "float";
    if (o instanceof Double)  return "double";
    if (o instanceof FieldDoc) {
      return ((FieldDoc)o).containingClass().qualifiedName();
    }
    return o.getClass().getName();
  }

}
