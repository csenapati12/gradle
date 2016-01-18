/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.integtests.tooling.r211

import org.gradle.api.JavaVersion
import org.gradle.integtests.tooling.fixture.TargetGradleVersion
import org.gradle.integtests.tooling.fixture.ToolingApiSpecification
import org.gradle.integtests.tooling.fixture.ToolingApiVersion
import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildController
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.UnsupportedMethodException
import org.gradle.tooling.model.idea.BasicIdeaProject
import org.gradle.tooling.model.idea.IdeaProject

@ToolingApiVersion(">=2.11")
@TargetGradleVersion(">=2.11")
class ToolingApiIdeaModelCrossVersionSpec extends ToolingApiSpecification {

    def setup(){
        settingsFile << "rootProject.name = 'root'"
    }

    @TargetGradleVersion(">=1.0-milestone-8 <2.11")
    def "older Gradle versions infer project source settings from default idea plugin language level"() {
        given:
        if (projectAppliesJavaPlugin) { buildFile << "apply plugin: 'java'"}

        when:
        def ideaProject = loadIdeaProjectModel()

        then:
        ideaProject.javaLanguageSettings.languageLevel == expectedSourceLanguageLevel
        ideaProject.javaLanguageSettings.languageLevel == toJavaVersion(ideaProject.languageLevel)

        where:
        projectAppliesJavaPlugin | expectedSourceLanguageLevel
        false                    | defaultIdeaPluginLanguageLevelForNonJavaProjects
        true                     | defaultIdeaPluginLanguageLevelForJavaProjects
    }

    @TargetGradleVersion(">=1.0-milestone-8 <2.11")
    def "older Gradle versions infer project source settings from configured idea plugin language level"() {
        given:
        buildFile << """
            apply plugin: 'idea'
            ${applyJavaPlugin ? "apply plugin: 'java'" : ""}
            idea {
                project {
                    languageLevel = '1.3'
                }
            }
        """

        when:
        def ideaProject = loadIdeaProjectModel()

        then:
        ideaProject.javaLanguageSettings.languageLevel == JavaVersion.VERSION_1_3
        toJavaVersion(ideaProject.languageLevel) == JavaVersion.VERSION_1_3

        where:
        applyJavaPlugin << [false, true]
    }

    @TargetGradleVersion(">=1.0-milestone-8 <2.11")
    def "older Gradle version throw exception when querying idea module java settings"() {
        when:
        def ideaProject = loadIdeaProjectModel()
        ideaProject.modules.find { it.name == 'root' }.getJavaLanguageSettings()

        then:
        thrown(UnsupportedMethodException)
    }

    @TargetGradleVersion(">=1.0-milestone-8 <2.11")
    def "older Gradle version throws exception when querying idea project java bytecode version or jdk"() {
        given:
        def ideaProject = loadIdeaProjectModel()

        when:
        ideaProject.javaLanguageSettings.getTargetBytecodeVersion()

        then:
        thrown(UnsupportedMethodException)

        when:
        ideaProject.javaLanguageSettings.getJdk()

        then:
        thrown(UnsupportedMethodException)
    }

    def "java source settings are null for non java modules"() {
        given:
        settingsFile << "\ninclude 'root', 'child1', 'child2'"
        buildFile << """
            apply plugin: 'idea'
            idea {
                project {
                    languageLevel = '1.5'
                }
            }
        """

        when:
        def ideaProject = loadIdeaProjectModel()

        then:

        ideaProject.javaLanguageSettings.languageLevel.isJava5()
        // modules
        ideaProject.modules.find { it.name == 'root' }.javaLanguageSettings == null
        ideaProject.modules.find { it.name == 'child1' }.javaLanguageSettings == null
        ideaProject.modules.find { it.name == 'child2' }.javaLanguageSettings == null
    }

    def "can retrieve project and module language level for multi project build"() {
        given:
        settingsFile << "\ninclude 'root', 'child1', 'child2', 'child3'"
        buildFile << """
            apply plugin: 'idea'

            project(':child1') {
            }

            project(':child2') {
                apply plugin: 'java'
                sourceCompatibility = '1.2'
            }

            project(':child3') {
                apply plugin: 'java'
                sourceCompatibility = '1.5'
            }

        """

        when:
        def ideaProject = loadIdeaProjectModel()

        then:
        ideaProject.javaLanguageSettings.languageLevel == JavaVersion.VERSION_1_5
        ideaProject.modules.find { it.name == 'root' }.javaLanguageSettings == null
        ideaProject.modules.find { it.name == 'child1' }.javaLanguageSettings == null
        ideaProject.modules.find { it.name == 'child2' }.javaLanguageSettings.languageLevel == JavaVersion.VERSION_1_2
        ideaProject.modules.find { it.name == 'child3' }.javaLanguageSettings.languageLevel == null // inherited
    }

    def "explicit idea project language level overrules sourceCompatiblity settings"() {
        given:
        settingsFile << "\ninclude 'root', 'child1', 'child2', 'child3'"
        buildFile << """
            apply plugin: 'idea'

            idea {
                project {
                    languageLevel = 1.7
                }
            }

            project(':child1') {
            }

            project(':child2') {
                apply plugin: 'java'
                sourceCompatibility = '1.2'
            }

            project(':child3') {
                apply plugin: 'java'
                sourceCompatibility = '1.5'
            }

        """

        when:
        def ideaProject = loadIdeaProjectModel()

        then:
        ideaProject.javaLanguageSettings.languageLevel == JavaVersion.VERSION_1_7
        ideaProject.modules.find { it.name == 'root' }.javaLanguageSettings == null
        ideaProject.modules.find { it.name == 'child1' }.javaLanguageSettings == null
        ideaProject.modules.find { it.name == 'child2' }.javaLanguageSettings.languageLevel == null
        ideaProject.modules.find { it.name == 'child3' }.javaLanguageSettings.languageLevel == null
    }

    def "can query java sdk for idea project"() {
        given:
        buildFile << """
apply plugin: 'java'

description = org.gradle.internal.jvm.Jvm.current().javaHome.toString()
"""
        when:
        def ideaProject = loadIdeaProjectModel()
        def gradleProject = ideaProject.modules.find({ it.name == 'root' }).gradleProject

        then:
        ideaProject.javaLanguageSettings.jdk != null
        ideaProject.javaLanguageSettings.jdk.javaVersion == JavaVersion.current()
        ideaProject.javaLanguageSettings.jdk.javaHome.toString() == gradleProject.description
    }

    def "module java sdk overwrite always null"() {
        given:
        settingsFile << "\ninclude 'root', 'child1', 'child2', 'child3'"
        buildFile << """
            allprojects {
                apply plugin:'java'
                apply plugin:'idea'
                targetCompatibility = "1.5"
            }

        """

        when:
        def ideaProject = loadIdeaProjectModel()

        then:
        ideaProject.javaLanguageSettings.jdk.javaVersion == JavaVersion.current()
        ideaProject.javaLanguageSettings.jdk.javaHome

        ideaProject.modules.find { it.name == 'root' }.javaLanguageSettings.jdk == null
        ideaProject.modules.find { it.name == 'child1' }.javaLanguageSettings.jdk == null
        ideaProject.modules.find { it.name == 'child2' }.javaLanguageSettings.jdk == null
        ideaProject.modules.find { it.name == 'child3' }.javaLanguageSettings.jdk == null
    }

    def "can query target bytecode version for idea project and modules"() {
        given:
        settingsFile << "\ninclude 'root', 'child1', 'child2', 'child3'"
        buildFile << """
            allprojects {
                apply plugin:'java'
                apply plugin:'idea'
                targetCompatibility = "1.5"
            }

        """

        when:
        def ideaProject = loadIdeaProjectModel()

        then:
        ideaProject.javaLanguageSettings.targetBytecodeVersion== JavaVersion.VERSION_1_5
        ideaProject.modules.find { it.name == 'root' }.javaLanguageSettings.targetBytecodeVersion == null
        ideaProject.modules.find { it.name == 'child1' }.javaLanguageSettings.targetBytecodeVersion == null
        ideaProject.modules.find { it.name == 'child2' }.javaLanguageSettings.targetBytecodeVersion == null
        ideaProject.modules.find { it.name == 'child3' }.javaLanguageSettings.targetBytecodeVersion == null
    }

    def "can have different target bytecode version among modules"() {
        given:
        settingsFile << "\ninclude 'root', 'child1', ':child2:child3', 'child4'"
        buildFile << """
            apply plugin:'java'
            targetCompatibility = "1.5"

            project(':child1') {
                apply plugin:'java'
                targetCompatibility = "1.5"
            }

            project(':child2') {
                apply plugin:'java'
                targetCompatibility = '1.6'
            }

            project(':child2:child3') {
                apply plugin:'java'
                targetCompatibility = '1.7'
            }
            project(':child4') {
            }
        """

        when:
        def ideaProject = loadIdeaProjectModel()

        then:
        ideaProject.javaLanguageSettings.targetBytecodeVersion == JavaVersion.VERSION_1_7
        ideaProject.modules.find { it.name == 'root' }.javaLanguageSettings.targetBytecodeVersion == JavaVersion.VERSION_1_5
        ideaProject.modules.find { it.name == 'child1' }.javaLanguageSettings.targetBytecodeVersion == JavaVersion.VERSION_1_5
        ideaProject.modules.find { it.name == 'child2' }.javaLanguageSettings.targetBytecodeVersion == JavaVersion.VERSION_1_6
        ideaProject.modules.find { it.name == 'child3' }.javaLanguageSettings.targetBytecodeVersion == null
        ideaProject.modules.find { it.name == 'child4' }.javaLanguageSettings == null
    }

    def "Project with a single Software Model component can be imported into the IDE"() {
        given:
        buildFile << """
            plugins {
                id 'jvm-component'
                id 'java-lang'
            }
            model {
                components {
                    main(JvmLibrarySpec)
                }
            }
        """.stripIndent().trim()

        when:
        def ideaProject = loadIdeaProjectModel()
        def mainModule = ideaProject.modules.find { it.name == 'main' }

        then:
        mainModule.contentRoots[0].rootDirectory == projectDir

        def sourceDirectory = mainModule.contentRoots[0].sourceDirectories.first()
        sourceDirectory.generated == false
        sourceDirectory.directory == new File(projectDir, "src/main/java")
    }

    private IdeaProject loadIdeaProjectModel() {
        withConnection { connection ->
            executeIdeaBuildAction(connection)
            connection.getModel(IdeaProject)
        }
    }

    static class IdeaModuleCountBuildAction implements BuildAction<String>, Serializable {
        @Override
        String execute(BuildController controller) {
            assert System.getProperty('build-action') == 'true'
            def project = controller.getModel(BasicIdeaProject)
            println "${project.modules.size()} found"
            "ok"
        }
    }

    private void executeIdeaBuildAction(ProjectConnection connection) {
        def stdout = new ByteArrayOutputStream()
        def action = connection.action(new IdeaModuleCountBuildAction())
        action
            .withArguments("-Dbuild-action=true")
            .setStandardOutput(stdout)
            .run()
        println(new String(stdout.toByteArray()))
    }

    private JavaVersion toJavaVersion(ideaLanguageLevel) {
        JavaVersion.valueOf(ideaLanguageLevel.level.replaceFirst("JDK", "VERSION"));
    }

    private JavaVersion getDefaultIdeaPluginLanguageLevelForNonJavaProjects() {
        JavaVersion.VERSION_1_6 // see IdeaPlugin#configureIdeaProject(Project)
    }

    private JavaVersion getDefaultIdeaPluginLanguageLevelForJavaProjects() {
        // see IdeaPlugin#configureIdeaProjectForJava(Project)
        println "GRADLE_VERSION " + getTargetDist().getVersion().toString()
        getTargetDist().getVersion().version.startsWith("1.0-milestone-8") ? JavaVersion.VERSION_1_5 : JavaVersion.current()
    }
}
