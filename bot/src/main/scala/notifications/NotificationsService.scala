package ch.xavier
package notifications

import Application.{executionContext, system}

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.*
import akka.http.scaladsl.{Http, HttpExt}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object NotificationsService {
  private val logger: Logger = LoggerFactory.getLogger("NotificationsService")
  private val http: HttpExt = Http()

  private val pushUrl: String = "https://api.pushbullet.com/v2/pushes"
  private val defaultHeaders: RawHeader = RawHeader("Access-Token", "o.DrAYDnOiBdmZlYdHwzWSqkKGbBNLQZkG")

  private val messageTemplate: String = "{\n    \"type\": \"note\",\n    \"body\": \"$BODY\",\n    \"title\": \"$TITLE\"\n}"

  def pushMessage(message: String, title: String): Unit =
    val pushNotificationResponse: Future[HttpResponse] = http.singleRequest(
      HttpRequest(
        method = HttpMethods.POST,
        uri = pushUrl,
        entity = HttpEntity(ContentTypes.`application/json`,
          messageTemplate.replace("$TITLE", title).replace("$BODY", message)
          .getBytes())
      ).withHeaders(defaultHeaders))

    pushNotificationResponse.map {
      case response@HttpResponse(StatusCodes.OK, _, _, _) =>
        response.entity.toStrict(5.seconds)
          .map(entity => entity.getData().utf8String)
          .onComplete {
            case Success(response) =>
              logger.info(s"Notification pushed with title:$title")
            case Failure(exception) =>
              logger.error(s"Failed to send notification with title:$title because of ${exception.getMessage}")
          }

      case error@_ => logger.error(s"Problem encountered when pushing title:$title with message: $message")
    }
}
