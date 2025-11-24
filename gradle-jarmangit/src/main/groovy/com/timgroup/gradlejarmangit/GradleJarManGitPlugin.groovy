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
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.bundling.Jar

final class GradleJarManGitPlugin implements Plugin<Project> {

    static Map<String, String> repoInfo(Project project) {
        if (project.hasProperty("jarmangit.revision")) {
            def revision = project.property("jarmangit.revision").toString()
            def branch = project.hasProperty("jarmangit.branch") ? project.property("jarmangit.branch").toString() : ""
            def origin = project.hasProperty("jarmangit.origin") ? project.property("jarmangit.origin").toString() : ""
            def dirtyState = project.hasProperty("jarmangit.dirty") ? Boolean.parseBoolean(project.property("jarmangit.dirty").toString()) : false
            [
                    "Git-Origin": origin,
                    "Git-Branch": branch,
                    "Git-Head-Rev": revision,
                    "Git-Repo-Is-Clean": Boolean.toString(!dirtyState)
            ]
        }
        else {
            FileRepositoryBuilder builder = new FileRepositoryBuilder().findGitDir(project.projectDir)

            if (builder.gitDir == null && builder.workTree == null) {
                project.logger.warn("No GIT repository found -- JarManGit information will not be included in Manifest/POM")
                return Collections.emptyMap()
            }

            Repository repository = builder.build()

            String origin = repository.getConfig().getString("remote", "origin", "url")
            if (origin == null) {
                project.logger.warn("GIT repository does not have an 'origin' upstream -- JarManGit information will not be included in Manifest/POM")
                return Collections.emptyMap()
            }
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
    }

    @Override
    void apply(Project project) {
        project.tasks.withType(Jar).configureEach { jar -> jar.doFirst {
            jar.manifest.attributes(repoInfo(project.rootProject))
        } }

        project.afterEvaluate {
            def pushJarManGitIntoPom = new Action<XmlProvider>() {
                @Override
                void execute(XmlProvider xmlProvider) {
                    project.logger.info("adding JarManGit information to Maven publication in $project")
                    def info = repoInfo(project.rootProject)
                    if (!info.isEmpty()) {
                        def scm = xmlProvider.asNode().appendNode('scm')
                        scm.appendNode('tag', info["Git-Head-Rev"])
                        scm.appendNode('url', info["Git-Origin"])
                    }
                }
            }

            project.getPluginManager().apply(PublishingPlugin.class)

            project.extensions.configure(PublishingExtension) {
                it.publications.withType(MavenPublication).configureEach {
                    pom.withXml(pushJarManGitIntoPom)
                }
            }
        }
    }
}
