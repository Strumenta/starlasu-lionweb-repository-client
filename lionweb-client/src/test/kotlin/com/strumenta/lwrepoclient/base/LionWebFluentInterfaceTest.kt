package com.strumenta.lwrepoclient.base

import com.strumenta.lwkotlin.lwLanguage
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LionWebFluentInterfaceTest {
    @Test
    fun addClass() {
        val language =
            lwLanguage(
                "fsLanguage",
                Root::class,
                Tenant::class,
                FSUser::class,
                File::class,
                Directory::class,
                TextFile::class,
                FSParsingResult::class,
                FSIssue::class,
                FSStatistics::class,
                FSStatisticsCategory::class,
                FSStatisticEntry::class,
                FSStatisticInstance::class,
                FSAttribute::class,
                FSPosition::class,
            )
        assertEquals(14, language.elements.size)

        val root = language.getConceptByName("Root")!!
        val tenant = language.getConceptByName("Tenant")!!
        val fsUser = language.getConceptByName("FSUser")!!
        val file = language.getConceptByName("File")!!
        val directory = language.getConceptByName("Directory")!!
        val textFile = language.getConceptByName("TextFile")!!
        val fsParsingResult = language.getConceptByName("FSParsingResult")

        assertEquals(false, root.isAbstract)
        assertEquals(null, root.extendedConcept)
        assertEquals(1, root.features.size)
        val rootTenants = root.getContainmentByName("tenants")!!
        assertEquals(true, rootTenants.isMultiple)
        assertEquals(tenant, rootTenants.type)

        assertEquals(false, tenant.isAbstract)
        assertEquals(null, tenant.extendedConcept)
        assertEquals(3, tenant.features.size)
        val tenantName = tenant.getPropertyByName("name")!!
        assertEquals(LionCoreBuiltins.getString(), tenantName.type)
        val tenantUsers = tenant.getContainmentByName("users")!!
        assertEquals(true, tenantUsers.isMultiple)
        assertEquals(fsUser, tenantUsers.type)
        val tenantDirectories = tenant.getContainmentByName("directories")!!
        assertEquals(true, tenantDirectories.isMultiple)
        assertEquals(directory, tenantDirectories.type)

        assertEquals(false, fsUser.isAbstract)
        assertEquals(null, fsUser.extendedConcept)
        assertEquals(2, fsUser.features.size)
        val fsUserName = fsUser.getPropertyByName("name")!!
        assertEquals(LionCoreBuiltins.getString(), fsUserName.type)
        val fsUserPassword = fsUser.getPropertyByName("password")!!
        assertEquals(LionCoreBuiltins.getString(), fsUserPassword.type)

        assertEquals(true, file.isAbstract)
        assertEquals(null, file.extendedConcept)
        assertEquals(1, file.features.size)
        val fileName = file.getPropertyByName("name")!!
        assertEquals(LionCoreBuiltins.getString(), fileName.type)

        assertEquals(false, directory.isAbstract)
        assertEquals(file, directory.extendedConcept)
        assertEquals(1, directory.features.size)
        val directoryFiles = directory.getContainmentByName("files")!!
        assertEquals(true, directoryFiles.isMultiple)
        assertEquals(file, directoryFiles.type)

        assertEquals(false, textFile.isAbstract)
        assertEquals(file, textFile.extendedConcept)
        assertEquals(2, textFile.features.size)
        val textFileParsingResult = textFile.getContainmentByName("parsingResult")!!
        assertEquals(false, textFileParsingResult.isMultiple)
        assertEquals(fsParsingResult, textFileParsingResult.type)
        val textFileContents = textFile.getPropertyByName("contents")!!
        assertEquals(LionCoreBuiltins.getString(), textFileContents.type)
    }

    @Test
    fun getConcept() {
        val language =
            lwLanguage(
                "fsLanguage",
                Root::class,
                Tenant::class,
                FSUser::class,
                File::class,
                Directory::class,
                TextFile::class,
                FSParsingResult::class,
                FSIssue::class,
                FSStatistics::class,
                FSStatisticsCategory::class,
                FSStatisticEntry::class,
                FSStatisticInstance::class,
                FSAttribute::class,
                FSPosition::class,
            )
        val root = Root()
        val rootConcept = language.getConceptByName("Root")!!

        assertEquals(rootConcept, root.concept)
    }
}
