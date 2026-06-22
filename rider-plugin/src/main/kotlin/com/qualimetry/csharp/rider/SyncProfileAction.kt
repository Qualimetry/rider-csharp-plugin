package com.qualimetry.csharp.rider

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.time.Duration
import java.util.Base64

class SyncProfileAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val settings = QualimetrySettings.getInstance()

        val url = settings.sonarQubeUrl.trim().trimEnd('/')
        val token = settings.token.trim()
        val profileName = settings.profileName.trim()

        if (url.isEmpty() || token.isEmpty()) {
            Messages.showErrorDialog(
                project,
                "Set the SonarQube URL and token in Settings > Tools > Qualimetry C# first.",
                "Qualimetry C#"
            )
            return
        }

        val solutionRoot = project.basePath
        if (solutionRoot == null) {
            Messages.showErrorDialog(project, "No solution directory is available.", "Qualimetry C#")
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Syncing Qualimetry rules", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(30))
                        .build()
                    val authHeader = basicAuthHeader(token)

                    indicator.text = "Resolving quality profile"
                    val profileKey = resolveProfileKey(client, authHeader, url, profileName)
                        ?: throw IllegalStateException("Quality profile \"$profileName\" was not found for language cs.")

                    indicator.text = "Reading active rules"
                    val severities = fetchActiveRuleSeverities(client, authHeader, url, profileKey, indicator)
                    if (severities.isEmpty()) {
                        throw IllegalStateException("The profile reported no active qualimetry-csharp rules.")
                    }

                    val content = renderGlobalConfig(profileName, severities)
                    writeGlobalConfig(project, solutionRoot, content)

                    ApplicationManager.getApplication().invokeLater {
                        Messages.showInfoMessage(
                            project,
                            "Wrote .globalconfig with ${severities.size} active rule(s) from \"$profileName\".",
                            "Qualimetry C#"
                        )
                    }
                } catch (t: Throwable) {
                    LOG.warn("Qualimetry rule sync failed", t)
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "Rule sync failed: ${t.message}",
                            "Qualimetry C#"
                        )
                    }
                }
            }
        })
    }

    private fun basicAuthHeader(token: String): String {
        // SonarQube user tokens authenticate as the Basic username with an empty password.
        val raw = "$token:".toByteArray(StandardCharsets.UTF_8)
        return "Basic " + Base64.getEncoder().encodeToString(raw)
    }

    private fun get(client: HttpClient, authHeader: String, uri: String): JsonObject {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .timeout(Duration.ofSeconds(60))
            .header("Authorization", authHeader)
            .header("Accept", "application/json")
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("SonarQube returned HTTP ${response.statusCode()} for $uri")
        }
        return JsonParser.parseString(response.body()).asJsonObject
    }

    private fun resolveProfileKey(
        client: HttpClient,
        authHeader: String,
        baseUrl: String,
        profileName: String
    ): String? {
        val uri = "$baseUrl/api/qualityprofiles/search?language=cs"
        val json = get(client, authHeader, uri)
        val profiles = json.getAsJsonArray("profiles") ?: return null
        for (element in profiles) {
            val profile = element.asJsonObject
            val name = profile.get("name")?.asString ?: continue
            if (name == profileName) {
                return profile.get("key")?.asString
            }
        }
        return null
    }

    private fun fetchActiveRuleSeverities(
        client: HttpClient,
        authHeader: String,
        baseUrl: String,
        profileKey: String,
        indicator: ProgressIndicator
    ): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        val pageSize = 500
        var page = 1
        while (true) {
            indicator.checkCanceled()
            val encodedKey = URLEncoder.encode(profileKey, StandardCharsets.UTF_8)
            val uri = "$baseUrl/api/rules/search?activation=true" +
                "&qprofile=$encodedKey" +
                "&languages=cs" +
                "&repositories=roslyn.qualimetry-csharp" +
                "&ps=$pageSize" +
                "&p=$page"
            val json = get(client, authHeader, uri)

            val actives = json.getAsJsonObject("actives")
            val rules = json.getAsJsonArray("rules") ?: break
            if (rules.size() == 0) break

            for (element in rules) {
                val rule = element.asJsonObject
                val fullKey = rule.get("key")?.asString ?: continue
                val diagnosticId = extractDiagnosticId(fullKey) ?: continue

                val severity = activeSeverity(actives, fullKey) ?: rule.get("severity")?.asString
                result[diagnosticId] = mapSeverity(severity)
            }

            val total = json.get("total")?.asInt ?: result.size
            if (page * pageSize >= total) break
            page++
        }
        return result
    }

    private fun activeSeverity(actives: JsonObject?, fullKey: String): String? {
        val array = actives?.getAsJsonArray(fullKey) ?: return null
        if (array.size() == 0) return null
        return array[0].asJsonObject.get("severity")?.asString
    }

    private fun extractDiagnosticId(fullKey: String): String? {
        val candidate = if (fullKey.contains(':')) fullKey.substringAfterLast(':') else fullKey
        return if (DIAGNOSTIC_ID.matches(candidate)) candidate else null
    }

    private fun mapSeverity(sonarSeverity: String?): String = when (sonarSeverity?.uppercase()) {
        "BLOCKER", "CRITICAL", "MAJOR" -> "warning"
        "MINOR" -> "suggestion"
        "INFO" -> "silent"
        else -> "warning"
    }

    private fun renderGlobalConfig(profileName: String, severities: Map<String, String>): String {
        val builder = StringBuilder()
        builder.append("is_global = true\n")
        builder.append("\n")
        builder.append("# Active rule set synced from SonarQube quality profile \"$profileName\".\n")
        builder.append("# Managed by the Qualimetry C# Rider plugin. Re-run the sync action to refresh.\n")
        builder.append("\n")
        for (id in severities.keys.sorted()) {
            builder.append("dotnet_diagnostic.$id.severity = ${severities[id]}\n")
        }
        return builder.toString()
    }

    private fun writeGlobalConfig(project: Project, solutionRoot: String, content: String) {
        val targetPath = Paths.get(solutionRoot, ".globalconfig")
        ApplicationManager.getApplication().invokeAndWait {
            ApplicationManager.getApplication().runWriteAction {
                val dir = VfsUtil.createDirectoryIfMissing(solutionRoot)
                    ?: LocalFileSystem.getInstance().refreshAndFindFileByPath(solutionRoot)
                    ?: throw IllegalStateException("Cannot access solution directory: $solutionRoot")
                val existing = dir.findChild(".globalconfig")
                val file = existing ?: dir.createChildData(this, ".globalconfig")
                VfsUtil.saveText(file, content)
            }
        }
        LOG.info("Qualimetry .globalconfig written to $targetPath")
    }

    companion object {
        private val LOG = Logger.getInstance(SyncProfileAction::class.java)
        private val DIAGNOSTIC_ID = Regex("^qa_[a-z0-9_]+$")
    }
}
