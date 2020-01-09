//import play.core.PlayVersion
//import play.sbt.PlayImport._
//import sbt._
//
//object MicroServiceBuild extends Build with MicroService {
//
//  val appName = "ras-api"
//
//  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()
//
//  val compile = Seq(
//   ws,
//    "uk.gov.hmrc" %% "auth-client" % "2.32.1-play-25", //TODO 2.19.0-play-25 -> 2.32.1-play-25 err // Matchers
//    "uk.gov.hmrc" %% "domain" % "5.6.0-play-25",
//    "uk.gov.hmrc" %% "http-caching-client" % "9.0.0-play-25", //TODO 8.0.0 -> 9.0.0-play-25 err // MAJOR // Matchers
//    "uk.gov.hmrc" %% "json-encryption" % "4.4.0-play-25",
//    "uk.gov.hmrc" %% "microservice-bootstrap" % "10.9.0",
//    "uk.gov.hmrc" %% "mongo-caching" % "5.4.0", //TODO 5.4.0 -> 6.7.0-play-25 // MAJOR
//    "uk.gov.hmrc" %% "play-hmrc-api" % "3.6.0-play-25",
//    "uk.gov.hmrc" %% "play-reactivemongo" % "6.7.0",
//
//    "com.typesafe.akka" % "akka-actor_2.11" % "2.5.18", //TODO?
//    "com.typesafe.akka" % "akka-testkit_2.11" % "2.5.18", //TODO?
//    "joda-time" % "joda-time" % "2.7.0"
//  )
//
//  def test(scope: String = "test,it") = Seq(
//    "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-25",
//    "uk.gov.hmrc" %% "reactivemongo-test" % "3.1.0", //TODO 3.1.0 -> 4.16.0-play-25 // MAJOR
//
//    "org.scalatest" %% "scalatest" % "3.0.8",
//    "org.pegdown" % "pegdown" % "1.6.0",
//    "com.typesafe.play" %% "play-test" % PlayVersion.current,
//    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1",
//    "org.mockito" % "mockito-core" % "3.2.4",
//    "com.typesafe.play" %% "play-specs2" % PlayVersion.current,
//    "com.typesafe.akka" % "akka-testkit_2.11" % "2.5.18", //TODO?
//    "de.leanovate.play-mockws" %% "play-mockws" % "2.7.1"
//  )
//
//}
