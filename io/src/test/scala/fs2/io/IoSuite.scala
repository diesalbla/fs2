/*
 * Copyright (c) 2013 Functional Streams for Scala
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package fs2.io

import java.io.{ByteArrayInputStream, InputStream, OutputStream}
import java.util.concurrent.Executors
import cats.effect.{Blocker, ContextShift, IO, Resource}
import fs2.Fs2Suite
import scala.concurrent.ExecutionContext
import org.scalacheck.effect.PropF.forAllF

class IoSuite extends Fs2Suite {
  group("readInputStream") {
    test("non-buffered") {
      forAllF { (bytes: Array[Byte], chunkSize0: Int) =>
        val chunkSize = (chunkSize0 % 20).abs + 1
        val is: InputStream = new ByteArrayInputStream(bytes)
        Blocker[IO].use { blocker =>
          val stream = readInputStream(IO(is), chunkSize, blocker)
          stream.compile.toVector.map(it => assertEquals(it, bytes.toVector))
        }
      }
    }

    test("buffered") {
      forAllF { (bytes: Array[Byte], chunkSize0: Int) =>
        val chunkSize = (chunkSize0 % 20).abs + 1
        val is: InputStream = new ByteArrayInputStream(bytes)
        Blocker[IO].use { blocker =>
          val stream = readInputStream(IO(is), chunkSize, blocker)
          stream
            .buffer(chunkSize * 2)
            .compile
            .toVector
            .map(it => assertEquals(it, bytes.toVector))
        }
      }
    }
  }

  group("readOutputStream") {
    test("writes data and terminates when `f` returns") {
      forAllF { (bytes: Array[Byte], chunkSize0: Int) =>
        val chunkSize = (chunkSize0 % 20).abs + 1
        Blocker[IO].use { blocker =>
          readOutputStream[IO](blocker, chunkSize)((os: OutputStream) =>
            blocker.delay[IO, Unit](os.write(bytes))
          ).compile
            .to(Vector)
            .map(it => assertEquals(it, bytes.toVector))
        }
      }
    }

    test("can be manually closed from inside `f`") {
      forAllF { (chunkSize0: Int) =>
        val chunkSize = (chunkSize0 % 20).abs + 1
        Blocker[IO].use { blocker =>
          readOutputStream[IO](blocker, chunkSize)((os: OutputStream) =>
            IO(os.close()) *> IO.never
          ).compile.toVector
            .map(it => assert(it == Vector.empty))
        }
      }
    }

    test("fails when `f` fails") {
      forAllF { (chunkSize0: Int) =>
        val chunkSize = (chunkSize0 % 20).abs + 1
        val e = new Exception("boom")
        Blocker[IO].use { blocker =>
          readOutputStream[IO](blocker, chunkSize)((_: OutputStream) =>
            IO.raiseError(e)
          ).compile.toVector.attempt
            .map(it => assert(it == Left(e)))
        }
      }
    }

    test("Doesn't deadlock with size-1 ContextShift thread pool") {
      val pool = Resource
        .make(IO(Executors.newFixedThreadPool(1)))(ec => IO(ec.shutdown()))
        .map(ExecutionContext.fromExecutor)
        .map(IO.contextShift)
      def write(os: OutputStream): IO[Unit] =
        IO {
          os.write(1)
          os.write(1)
          os.write(1)
          os.write(1)
          os.write(1)
          os.write(1)
        }
      Blocker[IO].use { blocker =>
        // Note: name `munitContextShift` is important because it shadows the outer implicit, preventing ambiguity
        pool
          .use { implicit munitContextShift: ContextShift[IO] =>
            readOutputStream[IO](blocker, chunkSize = 1)(write)
              .take(5)
              .compile
              .toVector
          }
          .map(it => assert(it.size == 5))
      }
    }
  }

  group("unsafeReadInputStream") {
    test("non-buffered") {
      forAllF { (bytes: Array[Byte], chunkSize0: Int) =>
        val chunkSize = (chunkSize0 % 20).abs + 1
        val is: InputStream = new ByteArrayInputStream(bytes)
        Blocker[IO].use { blocker =>
          val stream = unsafeReadInputStream(IO(is), chunkSize, blocker)
          stream.compile.toVector.map(it => assertEquals(it, bytes.toVector))
        }
      }
    }
  }
}
