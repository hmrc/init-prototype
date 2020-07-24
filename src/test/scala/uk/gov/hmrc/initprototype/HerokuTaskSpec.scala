/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.initprototype

import java.io.PrintWriter

import org.scalatest.BeforeAndAfterEach
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.JsArray
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class HerokuTaskSpec extends AnyFunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {
  implicit val ec: ExecutionContext        = ExecutionContext.global
  implicit val config: HerokuConfiguration = mock[HerokuConfiguration]
  implicit val mockManager: HerokuManager  = mock[HerokuManager]

  describe("HerokuTask") {
    val herokuTask = new HerokuTask

    describe("spinDownApps") {
      it("should set the dyno count of the given app to zero") {
        when(mockManager.spinDownApp(anyString())).thenReturn(Future.successful(JsArray(Seq.empty)))

        Await.result(herokuTask.spinDownApps(Seq("my-test-app", "my-other-app")), 1000 millis)

        verify(mockManager).spinDownApp("my-other-app")
        verify(mockManager).spinDownApp("my-test-app")
      }

      def createAppsFile(apps: Seq[String]): String = {
        import java.io.File
        val file = File.createTempFile("heroku-task-test", "txt")
        file.deleteOnExit()

        val appsFile = new PrintWriter(file)
        try {
          for (app <- apps) {
            appsFile.println(app)
          }
        } finally {
          appsFile.close()
        }

        file.getAbsolutePath
      }

      it("should read from a file") {
        val appsFile = createAppsFile(Seq("app-one", "app-two"))
        when(mockManager.spinDownApp(anyString())).thenReturn(Future.successful(JsArray(Seq.empty)))

        Await.result(herokuTask.spinDownApps(appsFile), 1000 millis)

        verify(mockManager).spinDownApp("app-one")
        verify(mockManager).spinDownApp("app-two")
      }
    }
  }
}
