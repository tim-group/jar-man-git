package com.timgroup.gradlejarmangit

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.maven.PomFilterContainer
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.bundling.Jar

import java.lang.reflect.Method

final class GradleJarManGitPlugin implements Plugin<Project> {

    static Map<String, String> repoInfo() {
        FileRepositoryBuilder builder = new FileRepositoryBuilder().readEnvironment().findGitDir()

        if (builder.getGitDir() == null && builder.getWorkTree() == null) {
            return new HashMap<String, String>()
        }

        Repository repository = builder.build()

        String origin = repository.getConfig().getString("remote", "origin", "url")
        String head = ObjectId.toString(repository.findRef(Constants.HEAD).getObjectId())
        String branch = repository.getBranch()
        String isClean = new Git(repository).status().call().isClean().toString()

        [
          "Git-Origin": origin,
          "Git-Branch": branch,
          "Git-Head-Rev": head,
          "Git-Repo-Is-Clean": isClean
        ]
    }

    @Override
    void apply(Project project) {
        project.tasks.withType(Jar) { jar -> jar.manifest.attributes(repoInfo()) }

        project.afterEvaluate {
            // "Legacy" (but still used) artifacts publication with 'maven' plugin
            Upload uploadArchives = (Upload)project.getTasks().withType(Upload.class).findByName("uploadArchives")
            if (uploadArchives != null) {
                def info = repoInfo()
                if (!info.isEmpty()) {
                    uploadArchives.repositories.mavenDeployer { PomFilterContainer deployer ->
                        def model = deployer.getPom().getModel()

                        Method setScmMethod = model.getClass().getMethods().find { method -> (method.name == "setScm") }
                        Class<?> scmType = setScmMethod.parameterTypes[0]

                        def scm = scmType.newInstance()
                        scmType.getMethod("setTag", String.class).invoke(scm, info.get("Git-Head-Rev"))
                        scmType.getMethod("setUrl", String.class).invoke(scm, info.get("Git-Origin"))
                        setScmMethod.invoke(model, scm)
                    }
                }
            }

            // "Modern" (but still incubating after several years) artifacts publication with 'maven-publish' plugin
            // API has changed radically since Gradle 1, don't even try to run this there
            if (!project.gradle.gradleVersion.startsWith("1.")) {
                def pushJarManGitIntoPom = new Action<XmlProvider>() {
                    @Override
                    void execute(XmlProvider xmlProvider) {
                        def info = repoInfo()
                        if (!info.isEmpty()) {
                            def scm = xmlProvider.asNode().appendNode('scm')
                            scm.appendNode('tag', info["Git-Head-Rev"])
                            scm.appendNode('url', info["Git-Origin"])
                        }
                    }
                }
                project.extensions.configure(org.gradle.api.publish.PublishingExtension) {
                    it.publications.withType(org.gradle.api.publish.maven.MavenPublication) {
                        project.logger.info("adding JarManGit information to Maven publication $it.groupId:$it.artifactId:$it.version in $project")
                        pom.withXml(pushJarManGitIntoPom)
                    }
                }
            }
        }
    }
}
