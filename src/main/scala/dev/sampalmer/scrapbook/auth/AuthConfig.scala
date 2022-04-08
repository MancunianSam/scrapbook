package dev.sampalmer.scrapbook.auth

import cats.effect.Async
import dev.sampalmer.scrapbook.server.ScrapbookServer.AppConfig
import org.pac4j.core.client.Clients
import org.pac4j.core.config.{Config, ConfigFactory}
import org.pac4j.oidc.client.OidcClient
import org.pac4j.oidc.config.OidcConfiguration
import org.pac4j.http4s.{DefaultHttpActionAdapter, Http4sCacheSessionStore}

class AuthConfig[F[_] <: AnyRef: Async](clientSecret: String) extends ConfigFactory {
  override def build(parameters: AnyRef*): Config = {
    val clients = new Clients("http://localhost:8080/callback", oidcClient)
    val config = new Config(clients)
    config.setHttpActionAdapter(new DefaultHttpActionAdapter[F])  // <-- Render a nicer page
    config.setSessionStore(new Http4sCacheSessionStore[F]())
    config
  }

  def oidcClient: OidcClient = {
    val config = new OidcConfiguration
    config.setClientId("50gUUkTuT7lN3A67aM77xMVdVk8fQ6QY")
    config.setSecret(clientSecret)
    config.setDiscoveryURI("https://dev-m99urvq4.eu.auth0.com/.well-known/openid-configuration")
    new OidcClient(config)
  }

}
object AuthConfig {
  def apply[F[_] <: AnyRef: Async](clientSecret: String): F[AuthConfig[F]] = Async[F].pure(new AuthConfig[F](clientSecret))
}
