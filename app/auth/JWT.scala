package auth

import models.User
import org.jose4j.jwk.{RsaJsonWebKey, RsaJwkGenerator}
import org.jose4j.jws.{AlgorithmIdentifiers, JsonWebSignature}
import org.jose4j.jwt.{JwtClaims, NumericDate}
import org.jose4j.jwt.consumer.{JwtConsumer, JwtConsumerBuilder}
import play.api.Logger

import scala.util.{Failure, Success, Try}

/**
  * Created by simfischer on 3/10/17.
  */

object JWT {

  val JwtLifetimeMins = 15
  val JwtLifetimeSecs = JwtLifetimeMins * 60

  val rsaJsonWebKey: RsaJsonWebKey = RsaJwkGenerator.generateJwk(2048)
  rsaJsonWebKey.setKeyId("themis-key")

  def makeJWT(claims: JwtClaims) = {
    // A JWT is a JWS and/or a JWE with JSON claims as the payload.
    // In this example it is a JWS so we create a JsonWebSignature object.
    val jws = new JsonWebSignature()

    // The payload of the JWS is JSON content of the JWT Claims
    jws.setPayload(claims.toJson());

    // The JWT is signed using the private key
    jws.setKey(rsaJsonWebKey.getPrivateKey());

    // Set the Key ID (kid) header because it's just the polite thing to do.
    // We only have one key in this example but a using a Key ID helps
    // facilitate a smooth key rollover process
    jws.setKeyIdHeaderValue(rsaJsonWebKey.getKeyId());

    // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
    jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

    // Sign the JWS and produce the compact serialization or the complete JWT/JWS
    // representation, which is a string consisting of three dot ('.') separated
    // base64url-encoded parts in the form Header.Payload.Signature
    // If you wanted to encrypt it, you can simply set this jwt as the payload
    // of a JsonWebEncryption object and set the cty (Content Type) header to "jwt".
    jws.getCompactSerialization();
  }

  def makeClaims(user: User): JwtClaims = {
    // Create the Claims, which will be the content of the JWT
    val claims: JwtClaims = new JwtClaims()
    claims.setIssuer("themis")               // who creates the token and signs it
    claims.setAudience("themis-frontend")    // to whom the token is intended to be sent
    claims.setExpirationTimeMinutesInTheFuture(JwtLifetimeMins);
    claims.setGeneratedJwtId()
    claims.setIssuedAtToNow()
    claims.setNotBeforeMinutesInThePast(2)   // time before which the token is not yet valid (2 minutes ago)
    claims.setSubject(user.username)         // the subject/principal is whom the token is about
    claims.setClaim("email", user.email)
    claims.setClaim("url", "todo") //TODO
    claims
  }

  def validateJWT(jwt: String): Option[String] = {
    // Use JwtConsumerBuilder to construct an appropriate JwtConsumer, which will
    // be used to validate and process the JWT.
    // The specific validation requirements for a JWT are context dependent, however,
    // it typically advisable to require a expiration time, a trusted issuer, and
    // and audience that identifies your system as the intended recipient.
    // If the JWT is encrypted too, you need only provide a decryption key or
    // decryption key resolver to the builder.
    val jwtConsumer: JwtConsumer = new JwtConsumerBuilder()
      .setRequireExpirationTime() // the JWT must have an expiration time
      .setAllowedClockSkewInSeconds(30) // allow some leeway in validating time based claims to account for clock skew
      .setRequireSubject() // the JWT must have a subject claim
      .setExpectedIssuer("themis") // whom the JWT needs to have been issued by
      .setExpectedAudience("themis-frontend") // to whom the JWT is intended for
      .setEvaluationTime(NumericDate.now())
      .setVerificationKey(rsaJsonWebKey.getKey()) // verify the signature with the public key
      .build(); // create the JwtConsumer instance

    val t: Try[JwtClaims] = Try(jwtConsumer.processToClaims(jwt))
    t match {
      case Success(claims: JwtClaims) => Some(claims.getSubject())
      case Failure(e) => {
        Logger.debug("JWT validation failed: "+e)
        None
      }
    }
    /*
    catch (InvalidJwtException | MalformedClaimException e) {
      // InvalidJwtException will be thrown, if the JWT failed processing or validation in anyway.
      // Hopefully with meaningful explanations(s) about what went wrong.
      None
    }
    */
  }
}