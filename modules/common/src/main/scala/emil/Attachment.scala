package emil

import java.nio.charset.StandardCharsets

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import fs2.Chunk
import fs2.Stream
import scodec.bits.ByteVector

final case class Attachment[F[_]](
    filename: Option[String],
    mimeType: MimeType,
    content: Stream[F, Byte],
    length: F[Long]
) {

  def withMimeType(mt: MimeType): Attachment[F] =
    copy(mimeType = mt)

  def withFilename(name: String): Attachment[F] =
    copy(filename = Some(name))

  def withLength(flen: F[Long]): Attachment[F] =
    copy(length = flen)

  def withLength(len: Long)(implicit ev: Applicative[F]): Attachment[F] =
    withLength(len.pure[F])
}

object Attachment {

  def apply[F[_]: Sync](
      filename: Option[String],
      mimeType: MimeType,
      content: Stream[F, Byte]
  ): Attachment[F] = {
    val len: F[Long] = content.compile.foldChunks(0L)((n, ch) => n + ch.size)
    Attachment(filename, mimeType, content, len)
  }

  def text[F[_]: Applicative](cnt: String, mimeType: MimeType): Attachment[F] = {
    val bytes = cnt.getBytes(StandardCharsets.UTF_8)
    Attachment(
      None,
      mimeType,
      Stream.chunk(Chunk.byteVector(ByteVector.view(bytes))),
      bytes.length.toLong.pure[F]
    )
  }
  def textPlain[F[_]: Applicative](cnt: String): Attachment[F] =
    text(cnt, MimeType.textPlain)

  def textHtml[F[_]: Applicative](cnt: String): Attachment[F] =
    text(cnt, MimeType.textHtml)

  def content[F[_]: Applicative](cnt: BodyContent, mimeType: MimeType): Attachment[F] =
    text(cnt.asString, mimeType)
}
