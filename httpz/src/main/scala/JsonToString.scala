package httpz

import argonaut.EncodeJson

abstract class JsonToString[A <: JsonToString[A]: EncodeJson] { self: A =>

  protected[this] def prettyParam = argonaut.PrettyParams.spaces2

  override final def toString =
    implicitly[EncodeJson[A]].apply(self).pretty(prettyParam)

}

