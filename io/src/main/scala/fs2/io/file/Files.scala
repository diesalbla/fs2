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

package fs2
package io
package file

import scala.concurrent.duration._

import cats.effect.kernel.{Async, Resource, Sync}
import cats.syntax.all._

import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.{Files => JFiles, _}
import java.nio.file.attribute.{BasicFileAttributes, FileAttribute, PosixFilePermission}
import java.util.stream.{Stream => JStream}

import fs2.io.CollectionCompat._

/** Provides basic capabilities related to working with files.
  *
  * Normally, the [[Files]] capability should be used instead, which extends this trait
  * with a few additional operations. `SyncFiles[F]` provides operations that only
  * require synchronous effects -- * e.g., `SyncFiles[SyncIO]` has an instance whereas
  * `Files[SyncIO]` does not.
  */
sealed trait SyncFiles[F[_]] {

  /** Copies a file from the source to the target path,
    *
    * By default, the copy fails if the target file already exists or is a symbolic link.
    */
  def copy(source: Path, target: Path, flags: Seq[CopyOption] = Seq.empty): F[Path]

  /** Creates a new directory at the given path.
    */
  def createDirectory(path: Path, flags: Seq[FileAttribute[_]] = Seq.empty): F[Path]

  /** Creates a new directory at the given path and creates all nonexistent parent directories beforehand.
    */
  def createDirectories(path: Path, flags: Seq[FileAttribute[_]] = Seq.empty): F[Path]

  /** Deletes a file.
    *
    * If the file is a directory then the directory must be empty for this action to succeed.
    * This action will fail if the path doesn't exist.
    */
  def delete(path: Path): F[Unit]

  /** Like `delete`, but will not fail when the path doesn't exist.
    */
  def deleteIfExists(path: Path): F[Boolean]

  /** Recursively delete a directory
    */
  def deleteDirectoryRecursively(path: Path, options: Set[FileVisitOption] = Set.empty): F[Unit]

  /** Creates a stream of [[Path]]s inside a directory.
    */
  def directoryStream(path: Path): Stream[F, Path]

  /** Creates a stream of [[Path]]s inside a directory, filtering the results by the given predicate.
    */
  def directoryStream(path: Path, filter: Path => Boolean): Stream[F, Path]

  /** Creates a stream of [[Path]]s inside a directory which match the given glob.
    */
  def directoryStream(path: Path, glob: String): Stream[F, Path]

  /** Checks if a file exists.
    *
    * Note that the result of this method is immediately outdated. If this
    * method indicates the file exists then there is no guarantee that a
    * subsequence access will succeed. Care should be taken when using this
    * method in security sensitive applications.
    */
  def exists(path: Path, flags: Seq[LinkOption] = Seq.empty): F[Boolean]

  /** Moves (or renames) a file from the source to the target path.
    *
    * By default, the move fails if the target file already exists or is a symbolic link.
    */
  def move(source: Path, target: Path, flags: Seq[CopyOption] = Seq.empty): F[Path]

  /** Creates a `FileHandle` for the file at the supplied `Path`. */
  def open(path: Path, flags: Seq[OpenOption]): Resource[F, FileHandle[F]]

  /** Creates a `FileHandle` for the supplied `FileChannel`. */
  def openFileChannel(channel: F[FileChannel]): Resource[F, FileHandle[F]]

  /** Get file permissions as set of [[PosixFilePermission]].
    *
    * Note: this will only work for POSIX supporting file systems.
    */
  def permissions(path: Path, flags: Seq[LinkOption] = Seq.empty): F[Set[PosixFilePermission]]

  /** Reads all data from the file at the specified `java.nio.file.Path`.
    */
  def readAll(path: Path, chunkSize: Int): Stream[F, Byte]

  /** Returns a `ReadCursor` for the specified path. The `READ` option is added to the supplied flags.
    */
  def readCursor(path: Path, flags: Seq[OpenOption] = Nil): Resource[F, ReadCursor[F]]

  /** Reads a range of data synchronously from the file at the specified `java.nio.file.Path`.
    * `start` is inclusive, `end` is exclusive, so when `start` is 0 and `end` is 2,
    * two bytes are read.
    */
  def readRange(path: Path, chunkSize: Int, start: Long, end: Long): Stream[F, Byte]

  /** Set file permissions from set of [[PosixFilePermission]].
    *
    * Note: this will only work for POSIX supporting file systems.
    */
  def setPermissions(path: Path, permissions: Set[PosixFilePermission]): F[Path]

  /** Returns the size of a file (in bytes).
    */
  def size(path: Path): F[Long]

  /** Creates a resource containing the path of a temporary file.
    *
    * The temporary file is removed during the resource release.
    */
  def tempFile(
      dir: Path,
      prefix: String = "",
      suffix: String = ".tmp",
      attributes: Seq[FileAttribute[_]] = Seq.empty
  ): Resource[F, Path]

  /** Creates a resource containing the path of a temporary directory.
    *
    * The temporary directory is removed during the resource release.
    */
  def tempDirectory(
      dir: Path,
      prefix: String = "",
      attributes: Seq[FileAttribute[_]] = Seq.empty
  ): Resource[F, Path]

  /** Creates a stream of [[Path]]s contained in a given file tree. Depth is unlimited.
    */
  def walk(start: Path): Stream[F, Path]

  /** Creates a stream of [[Path]]s contained in a given file tree, respecting the supplied options. Depth is unlimited.
    */
  def walk(start: Path, options: Seq[FileVisitOption]): Stream[F, Path]

  /** Creates a stream of [[Path]]s contained in a given file tree down to a given depth.
    */
  def walk(start: Path, maxDepth: Int, options: Seq[FileVisitOption] = Seq.empty): Stream[F, Path]

  /** Writes all data to the file at the specified `java.nio.file.Path`.
    *
    * Adds the WRITE flag to any other `OpenOption` flags specified. By default, also adds the CREATE flag.
    */
  def writeAll(
      path: Path,
      flags: Seq[StandardOpenOption] = List(StandardOpenOption.CREATE)
  ): Pipe[F, Byte, INothing]

  /** Returns a `WriteCursor` for the specified path.
    *
    * The `WRITE` option is added to the supplied flags. If the `APPEND` option is present in `flags`,
    * the offset is initialized to the current size of the file.
    */
  def writeCursor(
      path: Path,
      flags: Seq[OpenOption] = List(StandardOpenOption.CREATE)
  ): Resource[F, WriteCursor[F]]

  /** Returns a `WriteCursor` for the specified file handle.
    *
    * If `append` is true, the offset is initialized to the current size of the file.
    */
  def writeCursorFromFileHandle(file: FileHandle[F], append: Boolean): F[WriteCursor[F]]
}

object SyncFiles {
  def apply[F[_]](implicit F: SyncFiles[F]): F.type = F

  implicit def forSync[F[_]: Sync]: SyncFiles[F] = new Impl[F]

  private[file] class Impl[F[_]: Sync] extends SyncFiles[F] {

    def copy(source: Path, target: Path, flags: Seq[CopyOption]): F[Path] =
      Sync[F].blocking(JFiles.copy(source, target, flags: _*))

    def createDirectory(path: Path, flags: Seq[FileAttribute[_]]): F[Path] =
      Sync[F].blocking(JFiles.createDirectory(path, flags: _*))

    def createDirectories(path: Path, flags: Seq[FileAttribute[_]]): F[Path] =
      Sync[F].blocking(JFiles.createDirectories(path, flags: _*))

    def delete(path: Path): F[Unit] =
      Sync[F].blocking(JFiles.delete(path))

    def deleteIfExists(path: Path): F[Boolean] =
      Sync[F].blocking(JFiles.deleteIfExists(path))

    def deleteDirectoryRecursively(path: Path, options: Set[FileVisitOption]): F[Unit] =
      Sync[F].blocking {
        JFiles.walkFileTree(
          path,
          options.asJava,
          Int.MaxValue,
          new SimpleFileVisitor[Path] {
            override def visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult = {
              JFiles.deleteIfExists(path)
              FileVisitResult.CONTINUE
            }
            override def postVisitDirectory(path: Path, e: IOException): FileVisitResult = {
              JFiles.deleteIfExists(path)
              FileVisitResult.CONTINUE
            }
          }
        )
        ()
      }

    def directoryStream(path: Path): Stream[F, Path] =
      _runJavaCollectionResource[DirectoryStream[Path]](
        Sync[F].blocking(JFiles.newDirectoryStream(path)),
        _.asScala.iterator
      )

    def directoryStream(path: Path, filter: Path => Boolean): Stream[F, Path] =
      _runJavaCollectionResource[DirectoryStream[Path]](
        Sync[F].blocking(JFiles.newDirectoryStream(path, (entry: Path) => filter(entry))),
        _.asScala.iterator
      )

    def directoryStream(path: Path, glob: String): Stream[F, Path] =
      _runJavaCollectionResource[DirectoryStream[Path]](
        Sync[F].blocking(JFiles.newDirectoryStream(path, glob)),
        _.asScala.iterator
      )

    private final val pathStreamChunkSize = 16
    private def _runJavaCollectionResource[C <: AutoCloseable](
        javaCollection: F[C],
        collectionIterator: C => Iterator[Path]
    ): Stream[F, Path] =
      Stream
        .resource(Resource.fromAutoCloseable(javaCollection))
        .flatMap(ds => Stream.fromBlockingIterator[F](collectionIterator(ds), pathStreamChunkSize))

    def exists(path: Path, flags: Seq[LinkOption]): F[Boolean] =
      Sync[F].blocking(JFiles.exists(path, flags: _*))

    def move(source: Path, target: Path, flags: Seq[CopyOption]): F[Path] =
      Sync[F].blocking(JFiles.move(source, target, flags: _*))

    def open(path: Path, flags: Seq[OpenOption]): Resource[F, FileHandle[F]] =
      openFileChannel(Sync[F].blocking(FileChannel.open(path, flags: _*)))

    def openFileChannel(channel: F[FileChannel]): Resource[F, FileHandle[F]] =
      Resource.make(channel)(ch => Sync[F].blocking(ch.close())).map(ch => FileHandle.make(ch))

    def permissions(path: Path, flags: Seq[LinkOption]): F[Set[PosixFilePermission]] =
      Sync[F].blocking(JFiles.getPosixFilePermissions(path, flags: _*).asScala)

    def readAll(path: Path, chunkSize: Int): Stream[F, Byte] =
      Stream.resource(readCursor(path)).flatMap { cursor =>
        cursor.readAll(chunkSize).void.stream
      }

    def readCursor(path: Path, flags: Seq[OpenOption] = Nil): Resource[F, ReadCursor[F]] =
      open(path, StandardOpenOption.READ :: flags.toList).map { fileHandle =>
        ReadCursor(fileHandle, 0L)
      }

    def readRange(path: Path, chunkSize: Int, start: Long, end: Long): Stream[F, Byte] =
      Stream.resource(readCursor(path)).flatMap { cursor =>
        cursor.seek(start).readUntil(chunkSize, end).void.stream
      }

    def setPermissions(path: Path, permissions: Set[PosixFilePermission]): F[Path] =
      Sync[F].blocking(JFiles.setPosixFilePermissions(path, permissions.asJava))

    def size(path: Path): F[Long] =
      Sync[F].blocking(JFiles.size(path))

    def tempFile(
        dir: Path,
        prefix: String,
        suffix: String,
        attributes: Seq[FileAttribute[_]]
    ): Resource[F, Path] =
      Resource.make {
        Sync[F].blocking(JFiles.createTempFile(dir, prefix, suffix, attributes: _*))
      }(deleteIfExists(_).void)

    def tempDirectory(
        dir: Path,
        prefix: String,
        attributes: Seq[FileAttribute[_]]
    ): Resource[F, Path] =
      Resource.make {
        Sync[F].blocking(JFiles.createTempDirectory(dir, prefix, attributes: _*))
      } { p =>
        deleteDirectoryRecursively(p)
          .recover { case _: NoSuchFileException => () }
      }

    def walk(start: Path): Stream[F, Path] =
      walk(start, Seq.empty)

    def walk(start: Path, options: Seq[FileVisitOption]): Stream[F, Path] =
      walk(start, Int.MaxValue, options)

    def walk(start: Path, maxDepth: Int, options: Seq[FileVisitOption]): Stream[F, Path] =
      _runJavaCollectionResource[JStream[Path]](
        Sync[F].blocking(JFiles.walk(start, maxDepth, options: _*)),
        _.iterator.asScala
      )

    def writeAll(
        path: Path,
        flags: Seq[StandardOpenOption] = List(StandardOpenOption.CREATE)
    ): Pipe[F, Byte, INothing] =
      in =>
        Stream
          .resource(writeCursor(path, flags))
          .flatMap(_.writeAll(in).void.stream)

    def writeCursor(
        path: Path,
        flags: Seq[OpenOption] = List(StandardOpenOption.CREATE)
    ): Resource[F, WriteCursor[F]] =
      open(path, StandardOpenOption.WRITE :: flags.toList).flatMap { fileHandle =>
        val size = if (flags.contains(StandardOpenOption.APPEND)) fileHandle.size else 0L.pure[F]
        val cursor = size.map(s => WriteCursor(fileHandle, s))
        Resource.liftF(cursor)
      }

    def writeCursorFromFileHandle(file: FileHandle[F], append: Boolean): F[WriteCursor[F]] =
      if (append) file.size.map(s => WriteCursor(file, s)) else WriteCursor(file, 0L).pure[F]

  }
}

/** Provides operations related to working with files in the effect `F`.
  *
  * An instance is available for any effect `F` which has an `Async[F]` instance.
  */
trait Files[F[_]] extends SyncFiles[F] {

  /** Returns an infinite stream of data from the file at the specified path.
    * Starts reading from the specified offset and upon reaching the end of the file,
    * polls every `pollDuration` for additional updates to the file.
    *
    * Read operations are limited to emitting chunks of the specified chunk size
    * but smaller chunks may occur.
    *
    * If an error occurs while reading from the file, the overall stream fails.
    */
  def tail(
      path: Path,
      chunkSize: Int,
      offset: Long = 0L,
      pollDelay: FiniteDuration = 1.second
  ): Stream[F, Byte]

  /** Creates a [[Watcher]] for the default file system.
    *
    * The watcher is returned as a resource. To use the watcher, lift the resource to a stream,
    * watch or register 1 or more paths, and then return `watcher.events()`.
    */
  def watcher: Resource[F, Watcher[F]]

  /** Watches a single path.
    *
    * Alias for creating a watcher and watching the supplied path, releasing the watcher when the resulting stream is finalized.
    */
  def watch(
      path: Path,
      types: Seq[Watcher.EventType] = Nil,
      modifiers: Seq[WatchEvent.Modifier] = Nil,
      pollTimeout: FiniteDuration = 1.second
  ): Stream[F, Watcher.Event]

  /** Writes all data to a sequence of files, each limited in size to `limit`.
    *
    * The `computePath` operation is used to compute the path of the first file
    * and every subsequent file. Typically, the next file should be determined
    * by analyzing the current state of the filesystem -- e.g., by looking at all
    * files in a directory and generating a unique name.
    */
  def writeRotate(
      computePath: F[Path],
      limit: Long,
      flags: Seq[StandardOpenOption] = List(StandardOpenOption.CREATE)
  ): Pipe[F, Byte, INothing]
}

object Files {
  def apply[F[_]](implicit F: Files[F]): F.type = F

  implicit def forAsync[F[_]: Async]: Files[F] = new AsyncFiles[F]

  private final class AsyncFiles[F[_]: Async] extends SyncFiles.Impl[F] with Files[F] {

    def tail(path: Path, chunkSize: Int, offset: Long, pollDelay: FiniteDuration): Stream[F, Byte] =
      Stream.resource(readCursor(path)).flatMap { cursor =>
        cursor.seek(offset).tail(chunkSize, pollDelay).void.stream
      }

    def watcher: Resource[F, Watcher[F]] = Watcher.default

    def watch(
        path: Path,
        types: Seq[Watcher.EventType],
        modifiers: Seq[WatchEvent.Modifier],
        pollTimeout: FiniteDuration
    ): Stream[F, Watcher.Event] =
      Stream
        .resource(Watcher.default)
        .evalTap(_.watch(path, types, modifiers))
        .flatMap(_.events(pollTimeout))

    def writeRotate(
        computePath: F[Path],
        limit: Long,
        flags: Seq[StandardOpenOption]
    ): Pipe[F, Byte, INothing] = {
      def openNewFile: Resource[F, FileHandle[F]] =
        Resource
          .liftF(computePath)
          .flatMap(p => open(p, StandardOpenOption.WRITE :: flags.toList))

      def newCursor(file: FileHandle[F]): F[WriteCursor[F]] =
        writeCursorFromFileHandle(file, flags.contains(StandardOpenOption.APPEND))

      def go(
          fileHotswap: Hotswap[F, FileHandle[F]],
          cursor: WriteCursor[F],
          acc: Long,
          s: Stream[F, Byte]
      ): Pull[F, Unit, Unit] = {
        val toWrite = (limit - acc).min(Int.MaxValue.toLong).toInt
        s.pull.unconsLimit(toWrite).flatMap {
          case Some((hd, tl)) =>
            val newAcc = acc + hd.size
            cursor.writePull(hd).flatMap { nc =>
              if (newAcc >= limit)
                Pull
                  .eval {
                    fileHotswap
                      .swap(openNewFile)
                      .flatMap(newCursor)
                  }
                  .flatMap(nc => go(fileHotswap, nc, 0L, tl))
              else
                go(fileHotswap, nc, newAcc, tl)
            }
          case None => Pull.done
        }
      }

      in =>
        Stream
          .resource(Hotswap(openNewFile))
          .flatMap { case (fileHotswap, fileHandle) =>
            Stream.eval(newCursor(fileHandle)).flatMap { cursor =>
              go(fileHotswap, cursor, 0L, in).stream.drain
            }
          }
    }
  }

}
