package dev.adamko.githubassetpublish.lib.internal

import java.io.OutputStream.nullOutputStream
import java.math.BigInteger
import java.nio.file.Path
import java.security.DigestOutputStream
import java.security.MessageDigest
import kotlin.io.path.inputStream

internal fun Path.computeChecksum(algorithm: String): String {
  val md = MessageDigest.getInstance(algorithm)
  DigestOutputStream(nullOutputStream(), md).use { os ->
    inputStream().use { it.transferTo(os) }
  }
  return BigInteger(1, md.digest()).toString(16)
    .padStart(md.digestLength * 2, '0')
}
