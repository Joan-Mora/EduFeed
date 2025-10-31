package co.cellano.edufeed.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class ReportsSimulation extends Simulation {
  import BaseSimulation._

  val users = Integer.getInteger("users", 500).toInt
  val rampSeconds = Integer.getInteger("rampSeconds", 30).toInt
  val holdSeconds = Integer.getInteger("holdSeconds", 60).toInt

  val getIngresos = exec(
    http("reportes_ingresos")
      .get("/api/reportes/ingresos")
      .header("Authorization", "${authHeader}")
      .check(status.is(200))
  )

  val scn = scenario("Reports")
    .exec(login)
    .pause(100.millis, 200.millis)
    .exec(getIngresos)

  setUp(
    scn.inject(
      rampUsers(users) during (rampSeconds.seconds),
      constantUsersPerSec(users.toDouble / holdSeconds.toDouble) during (holdSeconds.seconds)
    )
  ).protocols(httpProtocol)
    .assertions(
      details("reportes_ingresos").responseTime.percentile3.lte(3000), // p95 < 3s
      global.successfulRequests.percent.gte(99.0)
    )
}
