package co.cellano.edufeed.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import java.util.UUID

class AccessCheckSimulation extends Simulation {
  import BaseSimulation._

  val users = Integer.getInteger("users", 500).toInt
  val rampSeconds = Integer.getInteger("rampSeconds", 30).toInt
  val holdSeconds = Integer.getInteger("holdSeconds", 60).toInt

  val createUser = exec(
    http("usuarios_create")
      .post("/api/usuarios")
      .header("Authorization", "${authHeader}")
      .body(StringBody(session => {
        val documento = Math.abs(scala.util.Random.nextInt()).toString
        s"""
          |{
          |  "documento":"$documento",
          |  "nombreCompleto":"Usuario $documento",
          |  "tipoUsuario":"ESTUDIANTE",
          |  "email":"user$documento@example.com",
          |  "telefono":"300$documento",
          |  "activo":true
          |}
          |""".stripMargin
      }))
      .check(status.in(200,201))
      .check(jsonPath("$.id").saveAs("usuarioId"))
  )

  val checkAccess = exec(
    http("accesos_verificar")
      .post("/api/accesos/verificar")
      .header("Authorization", "${authHeader}")
      .body(StringBody(session => s"""{"usuarioId":"${session("usuarioId").as[String]}","modalidad":"HUELLA"}"""))
      .check(status.is(200))
  )

  val scn = scenario("AccessCheck")
    .exec(login)
    .exec(createUser)
    .pause(100.millis, 300.millis)
    .exec(checkAccess)

  setUp(
    scn.inject(
      rampUsers(users) during (rampSeconds.seconds),
      constantUsersPerSec(users.toDouble / holdSeconds.toDouble) during (holdSeconds.seconds)
    )
  ).protocols(httpProtocol)
    .assertions(
      details("accesos_verificar").responseTime.percentile3.lte(2000), // p95 < 2s
      global.successfulRequests.percent.gte(99.0)
    )
}
