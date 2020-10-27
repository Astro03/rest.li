package com.linkedin.restli.tools.clientgen;


import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.pegasus.generator.GeneratorResult;
import com.linkedin.restli.internal.common.RestliVersion;
import com.linkedin.restli.tools.ExporterTestUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

/**
 * @author Keren Jin
 */
public class TestRestRequestBuilderGenerator
{
  @BeforeClass
  public void setUp() throws IOException
  {
    System.out.println("THIS IS BEFORE CLASS");
    outdir = ExporterTestUtils.createTmpDir();
    outdir2 = ExporterTestUtils.createTmpDir();
    moduleDir = System.getProperty("user.dir");
    System.out.println("outdir  : " + outdir);
    System.out.println("outdir2 : " + outdir2);
  }

  @AfterClass
  public void tearDown()
  {
    //ExporterTestUtils.rmdir(outdir);
    //ExporterTestUtils.rmdir(outdir2);
  }

  /**
   * <p>Verifies that REST method source code is emitted in natural order (the order in which the
   * {@link com.linkedin.restli.common.ResourceMethod} enum constants are declared).
   * <p>Natural enum order is deterministic.
   */
  @Test(dataProvider = "restliVersionsDataProvider")
  public void testDeterministicMethodOrder(RestliVersion version) throws Exception
  {
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    RestRequestBuilderGenerator.run(pegasusDir,
            null,
            moduleDir,
            true,
            false,
            version,
            null,
            outPath,
            new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "testSimple.restspec.json" }, true);

    final File builderFile = new File(outPath + FS + "com" + FS + "linkedin" + FS + "restli" + FS + "swift" + FS + "integration" + FS + "TestSimpleBuilders.java");
    Assert.assertTrue(builderFile.exists());

    final String builderFileContent = IOUtils.toString(new FileInputStream(builderFile));
    Assert.assertTrue(builderFileContent.contains("Generated from " + RESOURCES_DIR + FS + "idls" + FS + "testSimple.restspec.json"));

    List<String> actualMethodNames = StaticJavaParser.parse(builderFileContent)
            .findAll(MethodDeclaration.class).stream()
            .map(MethodDeclaration::getNameAsString)
            .collect(Collectors.toList());
    List<String> expectedMethodNames = Lists.newArrayList(
            "getBaseUriTemplate",
            "getRequestOptions",
            "getPathComponents",
            "assignRequestOptions",
            "getPrimaryResource",
            "options",
            "get",
            "update",
            "delete");
    Assert.assertEquals(actualMethodNames, expectedMethodNames, "Expected method names to be generated in explicit order.");
  }

  @Test(dataProvider = "arrayDuplicateDataProvider")
  public void testGeneration(RestliVersion version, String ABuildersName, String BBuildersName) throws Exception
  {
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    RestRequestBuilderGenerator.run(pegasusDir,
                                    null,
                                    moduleDir,
                                    true,
                                    false,
                                    version,
                                    null,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "arrayDuplicateA.restspec.json" });
    RestRequestBuilderGenerator.run(pegasusDir,
                                    null,
                                    moduleDir,
                                    true,
                                    false,
                                    version,
                                    null,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "arrayDuplicateB.restspec.json" });

    final File aBuilderFile = new File(outPath + FS + ABuildersName);
    final File bBuilderFile = new File(outPath + FS + BBuildersName);
    Assert.assertTrue(aBuilderFile.exists());
    Assert.assertTrue(bBuilderFile.exists());

    final String aBuilderFileContent = IOUtils.toString(new FileInputStream(aBuilderFile));
    Assert.assertTrue(aBuilderFileContent.contains("Generated from " + RESOURCES_DIR + FS + "idls" + FS + "arrayDuplicateA.restspec.json"));
    final String bBuilderFileContent = IOUtils.toString(new FileInputStream(bBuilderFile));
    Assert.assertTrue(bBuilderFileContent.contains("Generated from " + RESOURCES_DIR + FS + "idls" + FS + "arrayDuplicateB.restspec.json"));
  }

  @Test(dataProvider = "arrayDuplicateDataProvider2")
  public void testGeneration2(String restspec1, String restspec2, boolean shouldLower) throws Exception
  {
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    File tmpDir = ExporterTestUtils.createTmpDir();
    final String outPath = tmpDir.getPath();
    final String file1 = moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + restspec1;
    final String file2 = moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + restspec2;
    GeneratorResult r = RestRequestBuilderGenerator.run(pegasusDir,
            null,
            moduleDir,
            true,
            false,
            RestliVersion.RESTLI_2_0_0,
            null,
            outPath,
            new String[] { file1 },
            shouldLower);
    GeneratorResult r2 = RestRequestBuilderGenerator.run(pegasusDir,
            null,
            moduleDir,
            true,
            false,
            RestliVersion.RESTLI_2_0_0,
            null,
            outPath,
            new String[] { file2 },
            shouldLower);

    final JacksonDataCodec _dataCodec = new JacksonDataCodec();
    final DataMap data1 = _dataCodec.readMap(new FileInputStream(file1));
    final DataMap data2 = _dataCodec.readMap(new FileInputStream(file2));
    String path1 = data1.getString("namespace").replace('.', '/');
    String path2 = data2.getString("namespace").replace('.', '/');
    if (shouldLower) {
      path1 = path1.toLowerCase();
      path2 = path2.toLowerCase();
//      path1 = path1.toUpperCase();
//      path2 = path2.toUpperCase();
    }

    new File("a").createNewFile();
    boolean isFileSystemCaseSensitive = !new File( "A" ).exists();
    //boolean isFileSystemCaseSensitive = !new File( "a" ).getCanonicalPath().equals( new File( "A" ).getCanonicalPath() );
    //isFileSystemCaseSensitive = false;
    System.out.println("isFileSystemCaseSensitive: " + isFileSystemCaseSensitive);
    System.out.println("** path1 : " + path1);

    //isFileSystemCaseSensitive = false;  // false is mac/windows ; true is linux
    for (File f : r.getModifiedFiles()) {
      System.out.println("can   : " + f.getCanonicalPath());
      Assert.assertTrue(f.exists());
      if (!f.getPath().contains("Builder")) {
        System.out.println("   skipped ");
        continue;
      }
      if (!isFileSystemCaseSensitive && shouldLower) {
        Assert.assertTrue(f.getCanonicalPath().endsWith(f.getAbsolutePath()));
      } else if (!isFileSystemCaseSensitive && !shouldLower) {
        // could be anything
      } else if (isFileSystemCaseSensitive) {
        Assert.assertTrue(f.getCanonicalPath().contains(path1));
      }
    }

    System.out.println("** path2 : " + path2);
    for (File f : r2.getModifiedFiles()){
      System.out.println("can   : " + f.getCanonicalPath());
      Assert.assertTrue(f.exists());
      if (!f.getPath().contains("Builder")) {
        System.out.println("   skipped ");
        continue;
      }
      if (!isFileSystemCaseSensitive && shouldLower) {
        Assert.assertTrue(f.getCanonicalPath().endsWith(f.getAbsolutePath()));
      } else if (!isFileSystemCaseSensitive && !shouldLower) {
        // could be anything
      } else if (isFileSystemCaseSensitive) {
        Assert.assertTrue(f.getCanonicalPath().contains(path2));
      }
    }
    ExporterTestUtils.rmdir(tmpDir);
    new File("a").delete();
//    Assert.fail();
  }

  @Test(dataProvider = "deprecatedByVersionDataProvider")
  public void testDeprecatedByVersion(String idlName, String buildersName, String substituteClassName) throws Exception
  {
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    RestRequestBuilderGenerator.run(pegasusDir,
                                    null,
                                    moduleDir,
                                    true,
                                    false,
                                    RestliVersion.RESTLI_1_0_0,
                                    RestliVersion.RESTLI_2_0_0,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + idlName });

    final File builderFile = new File(outPath + FS + buildersName);
    Assert.assertTrue(builderFile.exists());

    final String fileContent = IOUtils.toString(new FileInputStream(builderFile));
    final Pattern regex = Pattern.compile(".*@deprecated$.*\\{@link " + substituteClassName + "\\}.*^@Deprecated$\n^public class .*", Pattern.MULTILINE | Pattern.DOTALL);
    Assert.assertTrue(regex.matcher(fileContent).matches());
    Assert.assertTrue(fileContent.contains("Generated from " + RESOURCES_DIR + FS + "idls" + FS + idlName));
  }

  @Test(dataProvider = "oldNewStyleDataProvider")
  public void testOldStylePathIDL(RestliVersion version, String AssocKeysPathBuildersName, String SubBuildersName, String SubGetBuilderName) throws Exception
  {
    final String pegasusDir = moduleDir + FS + RESOURCES_DIR + FS + "pegasus";
    final String outPath = outdir.getPath();
    final String outPath2 = outdir2.getPath();
    RestRequestBuilderGenerator.run(pegasusDir,
                                    null,
                                    true,
                                    false,
                                    version,
                                    null,
                                    outPath,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "oldStyleAssocKeysPath.restspec.json" });
    RestRequestBuilderGenerator.run(pegasusDir,
                                    null,
                                    true,
                                    false,
                                    version,
                                    null,
                                    outPath2,
                                    new String[] { moduleDir + FS + RESOURCES_DIR + FS + "idls" + FS + "newStyleAssocKeysPath.restspec.json" });

    final File oldStyleSuperBuilderFile = new File(outPath + FS + AssocKeysPathBuildersName);
    final File oldStyleSubBuilderFile = new File(outPath + FS + SubBuildersName);
    final File oldStyleSubGetBuilderFile = new File(outPath + FS + SubGetBuilderName);
    Assert.assertTrue(oldStyleSuperBuilderFile.exists());
    Assert.assertTrue(oldStyleSubBuilderFile.exists());
    Assert.assertTrue(oldStyleSubGetBuilderFile.exists());

    final File newStyleSubGetBuilderFile = new File(outPath2 + FS + SubGetBuilderName);
    Assert.assertTrue(newStyleSubGetBuilderFile.exists());

    BufferedReader oldStyleReader = new BufferedReader(new FileReader(oldStyleSubGetBuilderFile));
    BufferedReader newStyleReader = new BufferedReader(new FileReader(newStyleSubGetBuilderFile));

    String oldLine = oldStyleReader.readLine();
    String newLine = newStyleReader.readLine();

    while(!(oldLine == null || newLine == null))
    {
      if (!oldLine.startsWith("@Generated")) // the Generated line contains a time stamp, which could differ between the two files.
      {
        Assert.assertEquals(oldLine, newLine);
      }
      oldLine = oldStyleReader.readLine();
      newLine = newStyleReader.readLine();
    }

    Assert.assertTrue(oldLine == null && newLine == null);

    oldStyleReader.close();
    newStyleReader.close();
  }

  @DataProvider
  private static RestliVersion[] restliVersionsDataProvider()
  {
    return new RestliVersion[] { RestliVersion.RESTLI_1_0_0, RestliVersion.RESTLI_2_0_0  };
  }

  @DataProvider
  private static Object[][] arrayDuplicateDataProvider()
  {
    return new Object[][] {
      { RestliVersion.RESTLI_1_0_0, "ArrayDuplicateABuilders.java", "ArrayDuplicateBBuilders.java" },
      { RestliVersion.RESTLI_2_0_0, "ArrayDuplicateARequestBuilders.java", "ArrayDuplicateBRequestBuilders.java" }
    };
  }

  @DataProvider
  private static Object[][] arrayDuplicateDataProvider2() {
    return new Object[][]{
//            {"testSimple.namespace.restspec.json", "testSimple.restspec.json", true},    // testSimpleSub has it's own builder - need to fix
//            {"testSimple.namespace.restspec.json", "testSimple.restspec.json", false},
            {"arrayDuplicateA.namespace.restspec.json", "arrayDuplicateB.namespace.restspec.json", true},
            {"arrayDuplicateA.namespace.restspec.json", "arrayDuplicateB.namespace.restspec.json", false},
            {"arrayDuplicateB.namespace.restspec.json", "arrayDuplicateA.namespace.restspec.json", true},
            {"arrayDuplicateB.namespace.restspec.json", "arrayDuplicateA.namespace.restspec.json", false},
    };
  }

  @DataProvider
  private static Object[][] deprecatedByVersionDataProvider()
  {
    return new Object[][] {
        { "arrayDuplicateA.restspec.json", "ArrayDuplicateABuilders.java", "ArrayDuplicateARequestBuilders" },
        { "arrayDuplicateB.restspec.json", "ArrayDuplicateBBuilders.java", "ArrayDuplicateBRequestBuilders" }
    };
  }

  @DataProvider
  private static Object[][] oldNewStyleDataProvider()
  {
    return new Object[][] {
      { RestliVersion.RESTLI_1_0_0, "AssocKeysPathBuilders.java", "SubBuilders.java", "SubGetBuilder.java" },
      { RestliVersion.RESTLI_2_0_0, "AssocKeysPathRequestBuilders.java", "SubRequestBuilders.java", "SubGetRequestBuilder.java", }
    };
  }

  private static final String FS = File.separator;
  private static final String RESOURCES_DIR = "src" + FS + "test" + FS + "resources";

  private File outdir;
  private File outdir2;
  private String moduleDir;
}
