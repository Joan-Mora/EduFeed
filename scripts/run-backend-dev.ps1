[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-24'
$env:Path = "$($env:JAVA_HOME)\bin;$env:Path"
$env:SPRING_FLYWAY_BASELINE_ON_MIGRATE = 'true'
$env:SPRING_FLYWAY_BASELINE_VERSION = '0'
& "$env:USERPROFILE\tools\maven\apache-maven-3.9.9\bin\mvn.cmd" -f "edufeed-backend\pom.xml" spring-boot:run -e
