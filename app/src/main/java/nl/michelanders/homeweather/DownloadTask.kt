/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.michelanders.homeweather

import nl.michelanders.homeweather.DownloadCallback
import android.os.AsyncTask
import android.net.NetworkInfo
import android.net.ConnectivityManager
import android.os.Handler
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class DownloadTask(private val handler: Handler, private val runnable: Runnable, private val mCallback: DownloadCallback?) :
    AsyncTask<String?, Int?, DownloadTask.Result?>() {

    /**
     * Wrapper class that serves as a union of a result value and an exception. When the
     * download task has completed, either the result value or exception can be a non-null
     * value. This allows you to pass exceptions to the UI thread that were thrown during
     * doInBackground().
     */
    inner class Result {
        var mResultValue: String? = null
        var mException: Exception? = null

        constructor(resultValue: String?) {
            mResultValue = resultValue
        }

        constructor(exception: Exception?) {
            mException = exception
        }
    }

    /**
     * Cancel background network operation if we do not have network connectivity.
     */
    override fun onPreExecute() {
        if (mCallback != null) {
            val networkInfo = mCallback.getActiveNetworkInfo()
            if (networkInfo == null || !networkInfo.isConnected ||
                (networkInfo.type != ConnectivityManager.TYPE_WIFI
                        && networkInfo.type != ConnectivityManager.TYPE_MOBILE)
            ) {
                // If no connectivity, cancel task and update Callback with null data.
                mCallback.updateFromDownload(null)
                cancel(true)
            }
        }
    }

    /**
     * Defines work to perform on the background thread.
     */

    override fun doInBackground(vararg urls: String?): Result? {
        var result: Result? = null
        if (!isCancelled && urls != null && urls.size > 0) {
            val urlString = urls[0]
            try {
                val url = URL(urlString)
                val resultString = downloadUrl(url)
                if (resultString != null) {
                    result = Result(resultString)
                } else {
                    throw IOException("No response received.")
                }
            } catch (e: Exception) {
                result = Result(e)
            }
        }
        return result
    }

    /**
     * Send DownloadCallback a progress update.
     */
    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        if (values.size >= 2) {
            values[0]?.let { values[1]?.let { it1 -> mCallback!!.onProgressUpdate(it, it1) } }
        }
    }

    /**
     * Updates the DownloadCallback with the result.
     */
    override fun onPostExecute(result: Result?) {
        if (result != null && mCallback != null) {
            if (result.mException != null) {
                mCallback.updateFromDownload(result.mException!!.message)
            } else if (result.mResultValue != null) {
                mCallback.updateFromDownload(result.mResultValue)
            }
            mCallback.finishDownloading()
            if(mCallback.getActiveNetworkInfo().isConnected)
            {
                if(mCallback.getActiveNetworkInfo().type == ConnectivityManager.TYPE_ETHERNET ||mCallback.getActiveNetworkInfo().type == ConnectivityManager.TYPE_WIFI){
                    handler.postDelayed(runnable, 60000)
                }else{ // on a metered connection we get another set of data in 5 minutes
                    handler.postDelayed(runnable, 300000)
                }
            }else{ // if not connected we try again in a minute
                handler.postDelayed(runnable, 60000)
            }

        }
    }

    /**
     * Override to add special behavior for cancelled AsyncTask.
     */
    override fun onCancelled(result: Result?) {}

    /**
     * Given a URL, sets up a connection and gets the HTTP response body from the server.
     * If the network request is successful, it returns the response body in String form. Otherwise,
     * it will throw an IOException.
     */
    @Throws(IOException::class)
    private fun downloadUrl(url: URL): String? {
        var stream: InputStream? = null
        var connection: HttpsURLConnection? = null
        var result: String? = null
        try {
            connection = url.openConnection() as HttpsURLConnection
            // Timeout for reading InputStream arbitrarily set to 3000ms.
            connection.readTimeout = 3000
            // Timeout for connection.connect() arbitrarily set to 3000ms.
            connection!!.connectTimeout = 3000
            // For this use case, set HTTP method to GET.
            connection.requestMethod = "GET"
            // Already true by default but setting just in case; needs to be true since this request
            // is carrying an input (response) body.
            connection.doInput = true
            // Open communications link (network traffic occurs here).
            connection.connect()
            publishProgress(DownloadCallback.Progress.CONNECT_SUCCESS)
            val responseCode = connection.responseCode
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw IOException("HTTP error code: $responseCode")
            }
            // Retrieve the response body as an InputStream.
            stream = connection.inputStream
            publishProgress(DownloadCallback.Progress.GET_INPUT_STREAM_SUCCESS, 0)
            if (stream != null) {
                // Converts Stream to String with max length of 5000.
                result = readStream(stream, 5000)
                publishProgress(DownloadCallback.Progress.PROCESS_INPUT_STREAM_SUCCESS, 0)
            }
        } finally {
            // Close Stream and disconnect HTTPS connection.
            stream?.close()
            connection?.disconnect()
        }
        return result
    }

    /**
     * Converts the contents of an InputStream to a String.
     */
    @Throws(IOException::class)
    private fun readStream(stream: InputStream, maxLength: Int): String? {
        var result: String? = null
        // Read InputStream using the UTF-8 charset.
        val reader = InputStreamReader(stream, "UTF-8")
        // Create temporary buffer to hold Stream data with specified max length.
        val buffer = CharArray(maxLength)
        // Populate temporary buffer with Stream data.
        var numChars = 0
        var readSize = 0
        while (numChars < maxLength && readSize != -1) {
            numChars += readSize
            val pct = 100 * numChars / maxLength
            publishProgress(DownloadCallback.Progress.PROCESS_INPUT_STREAM_IN_PROGRESS, pct)
            readSize = reader.read(buffer, numChars, buffer.size - numChars)
        }
        if (numChars != -1) {
            // The stream was not empty.
            // Create String that is actual length of response body if actual length was less than
            // max length.
            numChars = Math.min(numChars, maxLength)
            result = String(buffer, 0, numChars)
        }
        return result
    }
}