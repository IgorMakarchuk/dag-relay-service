package crawler

import java.net.URLEncoder

import domain.{Dag, GitRepoSettings, Project}
import common.createHeaders
import io.circe.Json
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import org.http4s.blaze.http.http2.Http2Settings.Setting
import sttp.client.{HttpURLConnectionBackend, Identity, NothingT, Response, SttpBackend, quickRequest}
import zio.{Task, ZIO}
import sttp.client._
import sttp.model.StatusCode

import scala.util.Try

package object modules {

  class CrawlerLive extends crawler.Service {

    implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

    case class ProjectNameDag (project: Option[String], nameDag: Option[String])

    override def fetch(project: Project): Task[Map[String, String]] = for {
      files       <- getFiles(project.git)
      names       = getFileNames(files.body)
      raws        <- ZIO.collectAll(names.map(fileName => getRawFile(project.git, fileName)))
      raws_files  = raws.zipWithIndex.map(x => names(x._2) -> x._1.body).toMap
      mapDags     = names.map(fileName => {
                            fileName -> raws_files(fileName)
                          }).toMap
    } yield (mapDags)


    def getFiles(settings: GitRepoSettings): Task[Response[String]] = Task {
      quickRequest
        .headers(createHeaders(settings))
        .contentType("application/json")
        .get(uri"${settings.repository}/tree?recursive=true&path=${settings.path}&ref=${settings.branch}")
        .send[Identity]()
    }
    def getRawFile(settings: GitRepoSettings, fileName: String): Task[Response[String]] = Task {
      val url = s"${settings.repository}/files/${createPath(settings.path, fileName)}/raw?ref=${settings.branch}"
      quickRequest
        .headers(createHeaders(settings))
        .contentType("application/json")
        .get(uri"${url}")
        .send[Identity]()
    }

    def getFileNames(string: String) = {
      val json = parse(string).getOrElse(Json.Null)
      val jsonList = json.hcursor.focus
        .flatMap(_.asArray)
        .getOrElse(Vector.empty)

      jsonList
        .flatMap(_.asJson.hcursor.get[String]("name").toOption)
        .map(".*\\.yaml".r.findFirstIn(_))
        .flatMap {
          case Some(x) => List(x)
          case None => Nil
        }
    }
//    def separateName(string: String): ProjectNameDag = {
//      val parseName = Try("([\\w]*?)_([\\w]*)\\.yaml".r.unapplySeq(string).get)
//      if (parseName.isSuccess) ProjectNameDag(Some(parseName.get(0)), Some(parseName.get(1)))
//      else ProjectNameDag(None, None)
//
//    }
    def createPath(path: String, fileName: String): String = {
      URLEncoder.encode(s"${path}/${fileName}", "UTF-8")
    }


  }

}
