import crawler.modules.CrawlerLive
import domain._

import zio.{Task, _}

package object crawler {
  type Crawler = Has[Service]

  trait Service {
    def fetch(project:Project):Task[Map[String, Dag]]
  }

  val live: ZLayer[Any, Nothing, Crawler] = ZLayer.succeed(new CrawlerLive)

//  // helpers
//  def fetch: URIO[Config, AppConf] = ZIO.accessM[Config](_.get.app)
}
