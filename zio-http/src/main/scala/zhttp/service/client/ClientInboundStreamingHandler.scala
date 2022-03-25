package zhttp.service.client
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http._
import zhttp.http.{HttpData, Request, Response}
import zhttp.service.server.content.handlers.{ClientRequestHandler, ClientResponseHandler}
import zhttp.service.{CLIENT_INBOUND_HANDLER, HTTP_CLIENT_CONTENT_HANDLER, HttpRuntime}
import zio.Promise

final class ClientInboundStreamingHandler[R](
  val zExec: HttpRuntime[R],
  req: Request,
  promise: Promise[Throwable, Response],
) extends SimpleChannelInboundHandler[HttpObject](false)
    with ClientRequestHandler[R] {

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    writeRequest(req)(ctx)
    ()
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: HttpObject): Unit = {
    ctx.channel().config().setAutoRead(false): Unit
    msg match {
      case response: HttpResponse =>
        zExec.unsafeRun(ctx) {
          promise
            .succeed(
              Response.unsafeFromJResponse(
                response,
                HttpData.UnsafeAsync(callback =>
                  ctx
                    .pipeline()
                    .addAfter(
                      CLIENT_INBOUND_HANDLER,
                      HTTP_CLIENT_CONTENT_HANDLER,
                      new ClientResponseHandler(callback),
                    ): Unit,
                ),
              ),
            )
            .uninterruptible

        }
      case content: HttpContent   =>
        ctx.fireChannelRead(content): Unit

      case err => throw new IllegalStateException(s"Client unexpected message type: ${err}")
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, error: Throwable): Unit = {
    zExec.unsafeRun(ctx)(promise.fail(error).uninterruptible)
  }

}
