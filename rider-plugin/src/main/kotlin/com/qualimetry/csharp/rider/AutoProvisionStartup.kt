package com.qualimetry.csharp.rider

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

class AutoProvisionStartup : ProjectActivity {

    override suspend fun execute(project: Project) {
        if (!QualimetrySettings.getInstance().autoProvision) return

        val basePath = project.basePath ?: return
        val baseDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath) ?: return

        val csprojFiles = ArrayList<VirtualFile>()
        runReadAction { collect(baseDir, csprojFiles, 0) }
        if (csprojFiles.isEmpty()) return

        val alreadyReferenced = runReadAction {
            csprojFiles.any { PACKAGE_PRESENT.containsMatchIn(VfsUtil.loadText(it)) }
        }
        if (alreadyReferenced) return

        offer(project, csprojFiles)
    }

    private fun collect(dir: VirtualFile, acc: MutableList<VirtualFile>, depth: Int) {
        if (depth > MAX_DEPTH) return
        for (child in dir.children) {
            if (child.isDirectory) {
                if (child.name.lowercase() in SKIP_DIRS) continue
                collect(child, acc, depth + 1)
            } else if (child.extension.equals("csproj", ignoreCase = true)) {
                acc.add(child)
            }
        }
    }

    private fun offer(project: Project, csprojFiles: List<VirtualFile>) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(
                "Qualimetry C#",
                "Add the Qualimetry analyzer to this solution so findings appear as you type.",
                NotificationType.INFORMATION,
            )
        notification.addAction(NotificationAction.createSimple("Add analyzer") {
            provisionAll(csprojFiles)
            notification.expire()
        })
        notification.addAction(NotificationAction.createSimple("Don't ask again") {
            QualimetrySettings.getInstance().autoProvision = false
            notification.expire()
        })
        notification.notify(project)
    }

    private fun provisionAll(csprojFiles: List<VirtualFile>) {
        val version = QualimetrySettings.getInstance().analyzerVersion.trim()
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                for (file in csprojFiles) {
                    val text = VfsUtil.loadText(file)
                    if (PACKAGE_PRESENT.containsMatchIn(text)) continue
                    val updated = insertPackageReference(text, version) ?: continue
                    try {
                        VfsUtil.saveText(file, updated)
                    } catch (t: Throwable) {
                        LOG.warn("Failed to add analyzer reference to ${file.path}", t)
                    }
                }
            }
        }
    }

    private fun insertPackageReference(content: String, version: String): String? {
        val closing = "</Project>"
        val index = content.lastIndexOf(closing)
        if (index < 0) return null
        val reference =
            "  <ItemGroup>\n" +
            "    <PackageReference Include=\"$PACKAGE_ID\" Version=\"$version\" PrivateAssets=\"all\" />\n" +
            "  </ItemGroup>\n"
        return content.substring(0, index) + reference + content.substring(index)
    }

    companion object {
        private val LOG = Logger.getInstance(AutoProvisionStartup::class.java)
        private const val GROUP_ID = "Qualimetry C#"
        private const val PACKAGE_ID = "Qualimetry.CSharp.Analyzer"
        private const val MAX_DEPTH = 12
        private val SKIP_DIRS = setOf("bin", "obj", ".git", ".idea", ".vs", "node_modules")
        private val PACKAGE_PRESENT =
            Regex("Include\\s*=\\s*\"Qualimetry\\.CSharp\\.Analyzer\"", RegexOption.IGNORE_CASE)
    }
}
