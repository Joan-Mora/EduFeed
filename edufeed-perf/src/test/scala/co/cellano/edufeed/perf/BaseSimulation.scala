package co.cellano.edufeed.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object BaseSimulation {
  val baseUrl: String = System.getProperty("baseUrl", "http://localhost:8080")
  val adminUser: String = System.getProperty("perf.username", sys.env.getOrElse("SEED_OPERATOR_USERNAME", "admin"))
  val adminPass: String = System.getProperty("perf.password", sys.env.getOrElse("SEED_OPERATOR_PASSWORD", "Admin123$"))

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("EduFeed-Perf/1.0")

  val login = exec(
    http("auth_login")
      .post("/api/auth/login")
      .body(StringBody(session => s"""{"username":"${adminUser}","password":"${adminPass}"}"""))
      .check(status.is(200))
      .check(jsonPath("$.accessToken").saveAs("accessToken"))
  ).exec(session => session.set("authHeader", s"Bearer ${session("accessToken").as[String]}"))
}
