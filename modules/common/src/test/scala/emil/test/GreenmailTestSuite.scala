package emil.test

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ExecutorService, Executors, TimeUnit}

import emil.test.GreenmailTestSuite.Context
import emil.{MailAddress, MailConfig}
import munit._

abstract class GreenmailTestSuite extends FunSuite {

  var context: Context = _

  def users: List[MailAddress]

  override def beforeEach(ctx: BeforeEach): Unit = {
    context = GreenmailTestSuite.createContext(users)
    context.server.start()
  }

  override def afterEach(ctx: AfterEach): Unit =
    if (context != null) {
      context.server.stop()
      context.executor.shutdown()
      context.executor.awaitTermination(15, TimeUnit.SECONDS)
      context = null
    }

  def server: GreenmailServer =
    context.server

  def smtpConf(user: MailAddress): MailConfig =
    server.smtpConfig(user)

  def smtpConfNoUser: MailConfig =
    server.smtpConfigNoUser

  def imapConf(user: MailAddress): MailConfig =
    server.imapConfig(user)

}

object GreenmailTestSuite {
  private[this] val counter = new AtomicLong(0)

  case class Context(server: GreenmailServer, executor: ExecutorService)

  def createContext(users: List[MailAddress]): Context =
    Context(
      GreenmailServer.randomPorts(users: _*),
      Executors.newCachedThreadPool { (r: Runnable) =>
        val t = Executors.defaultThreadFactory().newThread(r)
        t.setName(s"test-blocker-${counter.getAndIncrement()}")
        t
      }
    )
}
