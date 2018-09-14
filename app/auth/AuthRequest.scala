package auth

import play.api.mvc.{Request, WrappedRequest}

/** Request holding a user object if successfully authorized. */
class AuthorizedRequest[A](val username: String, request: Request[A]) extends WrappedRequest[A](request)
