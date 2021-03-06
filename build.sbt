name := "xmppz"

organization := "xmppz.org"

version := ".1-SNAPSHOT"

scalaVersion := "2.10.0"

resolvers ++= Seq("Sonatype Nexus releases" at "https://oss.sonatype.org/content/repositories/releases",
                  "respository.jboss.org" at "http://repository.jboss.org/nexus/content/groups/public",
                "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/")

libraryDependencies +=  "org.scalaz" % "scalaz-effect_2.10" % "7.0.0"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test"

libraryDependencies += "org.jboss.netty" % "netty" % "3.2.7.Final" % "compile"

libraryDependencies +=  "org.slf4j" % "slf4j-api" % "1.6.1" % "compile"

libraryDependencies += "joda-time" % "joda-time" % "2.1"

libraryDependencies += "org.joda" % "joda-convert" % "1.2"

scalariformSettings
