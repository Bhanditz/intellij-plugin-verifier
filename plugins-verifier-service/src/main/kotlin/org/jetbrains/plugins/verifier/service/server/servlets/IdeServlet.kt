package org.jetbrains.plugins.verifier.service.server.servlets

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jetbrains.plugins.verifier.service.service.ide.DeleteIdeTask
import org.jetbrains.plugins.verifier.service.service.ide.DownloadIdeTask
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Servlet responsible for IDE downloading and removing.
 */
class IdeServlet : BaseServlet() {

  override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    val path = getPath(req, resp) ?: return
    when {
      path.endsWith("upload") -> processUploadIde(req, resp)
      path.endsWith("deleteIde") -> processDeleteIde(req, resp)
      else -> sendJson(resp, serverContext.ideFilesBank.getAvailableIdeVersions())
    }
  }

  private fun parseIdeVersionParameter(req: HttpServletRequest, resp: HttpServletResponse): IdeVersion? {
    val ideVersionParam = req.getParameter("ideVersion") ?: return null
    val ideVersion = IdeVersion.createIdeVersionIfValid(ideVersionParam)
    if (ideVersion == null) {
      sendNotFound(resp, "Invalid IDE version: $ideVersionParam")
    }
    return ideVersion
  }

  private fun processUploadIde(req: HttpServletRequest, resp: HttpServletResponse) {
    val ideVersionParam = parseIdeVersionParameter(req, resp) ?: return
    val availableIde = serverContext.ideRepository.fetchIndex().find {
      it.version.asStringWithoutProductCode() == ideVersionParam.asStringWithoutProductCode()
    }
    if (availableIde == null) {
      sendNotFound(resp, "IDE with version $ideVersionParam is not found in the ${serverContext.ideRepository}")
      return
    }
    val ideVersion = availableIde.version
    val ideRunner = DownloadIdeTask(serverContext.ideFilesBank, ideVersion)
    val taskDescriptor = serverContext.taskManager.enqueue(ideRunner)
    serverContext.serviceDAO.addManuallyDownloadedIde(ideVersion)
    sendOk(resp, "Downloading $ideVersion (#${taskDescriptor.taskId})")
  }

  private fun processDeleteIde(req: HttpServletRequest, resp: HttpServletResponse) {
    val ideVersion = parseIdeVersionParameter(req, resp) ?: return
    if (serverContext.ideFilesBank.isAvailable(ideVersion)) {
      val deleteIdeRunner = DeleteIdeTask(serverContext.ideFilesBank, ideVersion)
      val taskDescriptor = serverContext.taskManager.enqueue(deleteIdeRunner)
      serverContext.serviceDAO.removeManuallyDownloadedIde(ideVersion)
      sendOk(resp, "Deleting $ideVersion (#${taskDescriptor.taskId})")
    }
  }

}
