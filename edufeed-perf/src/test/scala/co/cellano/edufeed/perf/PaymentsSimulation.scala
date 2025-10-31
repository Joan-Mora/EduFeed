package co.cellano.edufeed.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import java.util.UUID

class PaymentsSimulation extends Simulation {
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

  val createPago = exec(
    http("pagos_create")
      .post("/api/pagos")
      .header("Authorization", "${authHeader}")
      .body(StringBody(session => {
        val ref = UUID.randomUUID().toString
        s"""
           |{
           |  "usuarioId":"${session("usuarioId").as[String]}",
           |  "monto": 10000,
           |  "tipoPago": "DIARIO",
           |  "metodoPago": "EFECTIVO",
           |  "referenciaExterna": "$ref",
           |  "metadatos": "{\"origen\":\"perf\"}"
           |}
           |""".stripMargin
      }))
      .check(status.in(200,201))
      .check(jsonPath("$.id").saveAs("pagoId"))
  )

  val listPagos = exec(
    http("pagos_list")
      .get("/api/pagos/estado/APROBADO")
      .header("Authorization", "${authHeader}")
      .check(status.is(200))
  )

  val scn = scenario("Payments")
    .exec(login)
    .exec(createUser)
    .pause(50.millis, 200.millis)
    .exec(createPago)
    .pause(50.millis, 150.millis)
    .exec(listPagos)

  setUp(
    scn.inject(
      rampUsers(users) during (rampSeconds.seconds),
      constantUsersPerSec(users.toDouble / holdSeconds.toDouble) during (holdSeconds.seconds)
    )
  ).protocols(httpProtocol)
    .assertions(
      details("pagos_list").responseTime.percentile3.lte(1000), // p95 < 1s
      global.successfulRequests.percent.gte(99.0)
    )
}
