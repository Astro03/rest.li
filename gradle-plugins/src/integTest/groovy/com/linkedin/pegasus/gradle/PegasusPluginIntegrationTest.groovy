package com.linkedin.pegasus.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class PegasusPluginIntegrationTest extends Specification {
  @Rule
  TemporaryFolder tempDir = new TemporaryFolder()

  def 'apply pegasus plugin'() {
    setup:
    def buildFile = tempDir.newFile('build.gradle')
    buildFile.text = "plugins { id 'pegasus' }"

    when:
    def result = GradleRunner.create()
        .withProjectDir(tempDir.root)
        .withPluginClasspath()
        .withArguments("mainDataTemplateJar")
        .forwardOutput()
        .build()

    then:
    result.task(':mainDataTemplateJar').outcome == SUCCESS
  }

  def 'mainCopySchema will remove stale pdsc'() {
    setup:
    def buildFile = tempDir.newFile('build.gradle')
    buildFile.text = "plugins { id 'pegasus' }"
    def schemasDir = tempDir.newFolder('src', 'main', 'pegasus')
    def mainSchemasDir = tempDir.newFolder('build', 'mainSchemas')
    def pdscFilename1 = "A.pdsc"
    def pdscFilename2 = "B.pdsc"
    def pdscFile1 = new File("$schemasDir.path$File.separator$pdscFilename1")
    pdscFile1.createNewFile()
    def pdscFile2 = new File("$schemasDir.path$File.separator$pdscFilename2")
    pdscFile2.createNewFile()
    GradleRunner runner = GradleRunner.create()
        .withProjectDir(tempDir.root)
        .withArguments("mainCopySchemas")
        .withPluginClasspath()
        .forwardOutput()

    when:
    def result = runner.build()

    then:
    result.task(':mainCopySchemas').getOutcome() == SUCCESS
    new File("$mainSchemasDir$File.separator$pdscFilename1").exists()
    new File("$mainSchemasDir$File.separator$pdscFilename2").exists()

    when:
    pdscFile1.delete()
    result = runner.build()

    then:
    result.task(':mainCopySchemas').getOutcome() == SUCCESS
    !new File("$mainSchemasDir$File.separator$pdscFilename1").exists()
    new File("$mainSchemasDir$File.separator$pdscFilename2").exists()
  }
}
