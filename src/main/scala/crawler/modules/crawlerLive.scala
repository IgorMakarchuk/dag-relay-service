package crawler

import java.net.URLEncoder

import domain.Project
import common.createHeaders
import sttp.client.{HttpURLConnectionBackend, Identity, NothingT, Response, SttpBackend, quickRequest}
import zio.{Task, ZIO}
import sttp.client._
import io.circe.parser.decode
import sttp.model.StatusCode


package object modules {

  class CrawlerLive extends crawler.Service {

    implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

    case class ProjectNameDag (project: Option[String], nameDag: Option[String])

    override def fetch(project: Project): Task[Map[String, String]] = for {
      dagsStr   <- getDagsFromUrl(project)
      found      = dagsStr.code != StatusCode.NotFound
      dags       = if (found) getDagsMap(dagsStr.body) else Map[String, String]()
    } yield (dags)

    def getDagsFromUrl(project: Project): Task[Response[String]] = Task {
      quickRequest
        .headers(createHeaders(project.git))
        .contentType("application/json")
        .get(uri"${project.fetchEndpoint}/${project.name}")
        .send[Identity]()
    }

    def getDagsMap(string: String): Map[String, String] = {
      decode[Map[String, String]](string) match {
        case Right(x) => x
        case _        => Map()
  }
}


  }

}
