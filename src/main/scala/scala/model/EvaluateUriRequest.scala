package scala.model

case class EvaluateUriRequest(uri: String, threatTypes: List[String], allowScan: Boolean = true)
