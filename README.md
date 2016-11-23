Build tool plugins that will do two things:

1) include the following tags in the Jar META-INF/MANIFEST.MF
  Git-Origin: git@git.net.local:coolproject
  Git-Repo-Is-Clean: false
  Git-Branch: master
  Git-Head-Rev: b178b10c259bb1442bcf27012c305a50c26796a2

2) include the following section in the pom.xml
  <scm>
    <url>git@git.net.local:coolproject</url>
    <tag>b178b10c259bb1442bcf27012c305a50c26796a2</tag>
  </scm>
