cmd /C "mvn -DdepStep=1/2 versions:display-dependency-updates -Dmaven.version.rules=file:///c:/DQGitHub/common-maven/master-parent/ruleset.xml"
cmd /C "mvn -DdepStep=2/2 versions:display-plugin-updates -Dmaven.version.rules=file:///c:/DQGitHub/common-maven/master-parent/ruleset.xml"
