package com.strumenta.lwrepoclient.base

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.BufferedSink
import okio.GzipSink
import okio.buffer
import java.io.IOException

val JSON: MediaType = "application/json".toMediaType()

fun String.compress() : RequestBody = this.toRequestBody(JSON).compress()

fun RequestBody.compress() : RequestBody = forceContentLength(gzip(this))

private fun gzip(body: RequestBody): RequestBody {
    return object : RequestBody() {
        override fun contentType(): MediaType? {
            return body.contentType()
        }

        override fun contentLength(): Long {
            return -1 // We don't know the compressed length in advance!
        }

        @Throws(IOException::class)
        override fun writeTo(sink: BufferedSink) {
            val gzipSink: BufferedSink = GzipSink(sink).buffer()
            body.writeTo(gzipSink)
            gzipSink.close()
        }
    }
}

@Throws(IOException::class)
private fun forceContentLength(requestBody: RequestBody): RequestBody {
    val buffer: Buffer = Buffer()
    requestBody.writeTo(buffer)
    return object : RequestBody() {
        override fun contentType(): MediaType? {
            return requestBody.contentType()
        }

        override fun contentLength(): Long {
            return buffer.size
        }

        @Throws(IOException::class)
        override fun writeTo(sink: BufferedSink) {
            sink.write(buffer.snapshot())
        }
    }
}