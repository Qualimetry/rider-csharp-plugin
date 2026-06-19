package com.qualimetry.csharp.rider

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

class ProvisionAnalyzerAction : AnAction() {

    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible = file != null && isCsproj(file)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        if (file == null || !isCsproj(file)) {
            Messages.showErrorDialog(project, "Select a .csproj file first.", "Qualimetry C#")
            return
        }

        val version = QualimetrySettings.getInstance().analyzerVersion.trim()
        val original = VfsUtil.loadText(file)

        if (PACKAGE_PRESENT.containsMatchIn(original)) {
            Messages.showInfoMessage(
                project,
                "${file.name} already references $PACKAGE_ID.",
                "Qualimetry C#"
            )
            return
        }

        val updated = insertPackageReference(original, version)
        if (updated == null) {
            Messages.showErrorDialog(
                project,
                "Could not find a </Project> element in ${file.name}.",
                "Qualimetry C#"
            )
            return
        }

        try {
            ApplicationManager.getApplication().runWriteAction {
                VfsUtil.saveText(file, updated)
            }
            Messages.showInfoMessage(
                project,
                "Added $PACKAGE_ID $version to ${file.name}.",
                "Qualimetry C#"
            )
        } catch (t: Throwable) {
            LOG.warn("Failed to update ${file.path}", t)
            Messages.showErrorDialog(project, "Could not update ${file.name}: ${t.message}", "Qualimetry C#")
        }
    }

    private fun isCsproj(file: VirtualFile): Boolean =
        !file.isDirectory && file.extension.equals("csproj", ignoreCase = true)

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
        private val LOG = Logger.getInstance(ProvisionAnalyzerAction::class.java)
        private const val PACKAGE_ID = "Qualimetry.CSharp.Analyzer"
        private val PACKAGE_PRESENT =
            Regex("Include\\s*=\\s*\"Qualimetry\\.CSharp\\.Analyzer\"", RegexOption.IGNORE_CASE)
    }
}
