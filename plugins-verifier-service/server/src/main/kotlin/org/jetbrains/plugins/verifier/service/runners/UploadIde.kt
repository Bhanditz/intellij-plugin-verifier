package org.jetbrains.plugins.verifier.service.runners

import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.repository.IdeRepository
import org.jetbrains.plugins.verifier.service.core.Progress
import org.jetbrains.plugins.verifier.service.core.Task
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Function

/**
 * @author Sergey Patrikeev
 */
class UploadIdeRunner(val ideVersion: IdeVersion, val fromSnapshots: Boolean = false) : Task<Boolean>() {

  private val LOG: Logger = LoggerFactory.getLogger(UploadIdeRunner::class.java)

  override fun presentableName(): String = "UploadIde #$ideVersion"

  override fun computeResult(progress: Progress): Boolean {
    val artifact = IdeRepository.fetchIndex(fromSnapshots)
        .find { it.version == ideVersion } ?: throw IllegalArgumentException("Unable to find the IDE #$ideVersion in snapshots = $fromSnapshots")

    val ideFile = FileManager.createTempFile(".zip")

    try {
      try {
        IdeRepository.downloadIde(artifact, ideFile, Function<Double, Unit>() { progress.setProgress(it) })
      } catch(e: Exception) {
        LOG.error("Unable to download IDE ${artifact.version} community=${artifact.isCommunity} from snapshots=${artifact.isSnapshot}", e)
        throw e
      }

      val success = IdeFilesManager.addIde(ideFile)
      LOG.info("IDE #$ideVersion ${if (fromSnapshots) "from snapshots repo" else ""} has been added")
      return success
    } finally {
      ideFile.deleteLogged()
    }
  }

}