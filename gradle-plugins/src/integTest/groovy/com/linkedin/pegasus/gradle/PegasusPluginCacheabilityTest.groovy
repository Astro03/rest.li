package com.linkedin.pegasus.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.*;

class PegasusPluginCacheabilityTest extends Specification {
  @Rule
  TemporaryFolder tempDir = new TemporaryFolder()

  def 'mainCopySchemas tasks are up-to-date'() {
    setup:
    def buildFile = tempDir.newFile('build.gradle')
    buildFile.text = "plugins { id 'pegasus' }"
    def pegasusDir = tempDir.newFolder('src', 'main', 'pegasus')
    new File("$pegasusDir.path$File.separator"+"A.pdsc").createNewFile()

    when:
    def runner = GradleRunner.create()
        .withProjectDir(tempDir.root)
        .withPluginClasspath()
        .withArguments("mainCopySchemas")
        .forwardOutput()
    def result = runner.build()

    then:
    result.task(':mainDestroyStaleFiles').outcome == SKIPPED
    result.task(':mainCopyPdscSchemas').outcome == SKIPPED
    result.task(':mainCopySchemas').outcome == SUCCESS

    when:
    result = runner.build()

    then:
    result.task(':mainDestroyStaleFiles').outcome == SKIPPED
    result.task(':mainCopyPdscSchemas').outcome == SKIPPED
    result.task(':mainCopySchemas').outcome == UP_TO_DATE
  }
}
