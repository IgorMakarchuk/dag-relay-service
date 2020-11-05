import domain.GitRepoSettings
import sttp.client.{Identity, Response, quickRequest}
import zio.Task

package object common {
    def createHeaders(gitRepoSettings: GitRepoSettings): Map[String, String] = Map("PRIVATE-TOKEN" -> gitRepoSettings.privateToken)

}
