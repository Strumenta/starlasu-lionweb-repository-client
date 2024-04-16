package com.strumenta.lwrepoclient.base

import com.strumenta.lwkotlin.BaseNode
import com.strumenta.lwkotlin.Implementation

class Root : BaseNode() {
    val tenants = multipleContainment<Tenant>("tenants")
    override fun calculateID(): String? = "the-code-insight-studio-root"
}

interface Named {
    val name: String?
}

class Tenant : BaseNode(), Named {
    override var name: String? by property("name")
    val users = multipleContainment<FSUser>("users")
    val directories= multipleContainment<Directory>("directories")
}

class FSUser : BaseNode(), Named {
    // Note that this means users should be unique across all tenants
    override fun calculateID(): String? = "user-${name!!}"

    override var name: String? by property("name")

    var password: String? by property("password")
}


abstract class File: BaseNode(), Named {

    override var name: String? by property("name")

    override fun calculateID(): String {
        return "${parent!!.id!!}___${name!!.replace('.', '_')}"
    }

    @Implementation
    val path: String
        get() {
            return if (this.parent is File) {
                "${(parent as File).path}/$name!!"
            } else {
                name!!
            }
        }
}

class Directory: File() {
    val files = multipleContainment<File>("files")
}

class TextFile: File() {

    var parsingResult: FSParsingResult? by singleContainment("parsingResult")

    var contents: String? by property("contents")

    @Implementation
    val isParsed: Boolean
        get() = parsingResult != null
}

class FSParsingResult(): BaseNode() {
    val issues = multipleContainment<FSIssue>("issues")
    var statistics: FSStatistics? by singleContainment("statistics")
}

class FSIssue(): BaseNode() {
    var message: String? by property("message")
    var severity: String? by property("severity")
    var fsPosition: FSPosition? by singleContainment("fsPosition")

}

class FSStatistics(): BaseNode() {
    val categories = multipleContainment<FSStatisticsCategory>("categories")
}

class FSStatisticsCategory(): BaseNode(), Named {
    override var name: String? by property("name")
    val entries = multipleContainment<FSStatisticEntry>("entries")
}

class FSStatisticEntry: BaseNode(), Named {
    override var name: String? by property("name")
    val instances = multipleContainment<FSStatisticInstance>("instances")
}

class FSStatisticInstance(): BaseNode() {
    val fsPosition: FSPosition? by singleContainment("fsPosition")
    val attributes = multipleContainment<FSAttribute>("attributes")
}

class FSAttribute(): BaseNode(), Named {
    override var name: String? by property("name")

    val value: String?  by property("value")
}

class FSPosition: BaseNode() {
    val startLine: Int? by property("startLine")
    val startColumn: Int? by property("startColumn")
    val endLine: Int? by property("endLine")
    val endColumn: Int? by property("endColumn")
    val fsSource: String? by property("fsSource")
}