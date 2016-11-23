package com.timgroup.gradlejarmangit

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.IdentityFileResolver
import org.gradle.api.java.archives.internal.DefaultManifest
import org.gradle.api.tasks.bundling.Jar

public final class GradleJarManGitPlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    project.tasks.withType(Jar) {
      manifest = new DefaultManifest(new IdentityFileResolver()) {
          @Override
          DefaultManifest attributes(Map<String, ?> attributes) {
              return super.attributes(attributes)
          }
//        attributes(
//            (java.util.jar.Attributes.Name.IMPLEMENTATION_TITLE.toString()): (project.hasProperty('BF_PROJECTNAME') ? project.getProperty('BF_PROJECTNAME') : "${ -> project.someCustomExtensionObject.fieldInCustomExtensionObject}"),
//            (java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION.toString()): project.hasProperty('BF_TAG') ? project.getProperty('BF_TAG') : 'UNCONTROLLED ARTIFACT',
//            (java.util.jar.Attributes.Name.IMPLEMENTATION_VENDOR.toString()): 'MyCompany',
//            'Created-By': System.getProperty('java.version') + ' (' + System.getProperty('java.vendor') + ')',
//            'Built-With': "gradle-${project.getGradle().getGradleVersion()}, groovy-${GroovySystem.getVersion()}",
//            'Build-Time': project.hasProperty('Current_Date') ? project.getProperty('Current_Date') : 'NOT FOR PRODUCTION USE',
//            'Built-By': project.hasProperty('BF_USER') ? project.getProperty('BF_USER') : System.getProperty('user.name'),
//            'Built-On': "${InetAddress.localHost.hostName}/${InetAddress.localHost.hostAddress}"
//            )
      }
    }
  }
}
