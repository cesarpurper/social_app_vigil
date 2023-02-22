package vigil.cesar.socialApp.util

import authentikat.jwt.{JsonWebToken, JwtClaimsSet, JwtClaimsSetMap, JwtHeader}

import java.util.concurrent.TimeUnit

object JWTUtils {


  final val accessTokenHeaderName = "X-Access-Token"

  private val tokenExpiryPeriodInDays = 1
  val header = JwtHeader("HS256")
  val secretKey = "cesar_vigil"


  def setClaims(username: String): JwtClaimsSetMap =
    JwtClaimsSet(
      Map("user" -> username,
        "expiredAt" -> (System.currentTimeMillis() + TimeUnit.DAYS
          .toMillis(tokenExpiryPeriodInDays)))
    )

  def getClaims(jwt: String): Map[String, String] = jwt match {
    case JsonWebToken(_, claims, _) => claims.asSimpleMap.getOrElse(Map.empty[String, String])
  }

  def isTokenExpired(jwt: String): Boolean =
    getClaims(jwt).get("expiredAt").exists(_.toLong < System.currentTimeMillis())

}
