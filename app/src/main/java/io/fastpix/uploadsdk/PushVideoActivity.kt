package io.fastpix.uploadsdk

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import io.fastpix.upload.UploadExceptions
import io.fastpix.upload.internal.FastPixUploadSdk
import io.fastpix.upload.model.FastPixUploadCallbacks
import io.fastpix.uploadsdk.databinding.ActivityPushVideoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

class PushVideoActivity : AppCompatActivity(), FastPixUploadCallbacks {

    private lateinit var _binding: ActivityPushVideoBinding
    private var LOGGER = Logger.getLogger("com.fastpix.uploader")
    private val galleryContract =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                _binding.textProgress.text = "Please wait!\nReading File.."
                handleVideoUri(it)
            }

        }

    private val mainDispatcher = Dispatchers.Main
    private lateinit var sdk: FastPixUploadSdk
    var chunkSize: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityPushVideoBinding.inflate(layoutInflater)
        setContentView(_binding.root)
        handleIntent()
        getSignedUrl()

        _binding.uploadVideo.setOnClickListener {
            if (_binding.uploadVideo.text == "Upload Again") {
                getSignedUrl()
            }
            galleryContract.launch("video/*")
        }

        _binding.pause.setOnClickListener {

            sdk.pauseUploading()
            _binding.resume.isEnabled = true
            _binding.pause.isEnabled = false
        }

        _binding.abort.setOnClickListener {
            sdk.abort()
            _binding.pause.isEnabled = false
            _binding.resume.isEnabled = false
            finish()

        }

        _binding.resume.setOnClickListener {
            sdk.resumeUploading()
            _binding.pause.isEnabled = true
            _binding.resume.isEnabled = false

        }
    }

    private fun handleVideoUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val file = getFileFromUri(uri)
            withContext(Dispatchers.Main) {
                callToFastPixSDK(file)
            }
        }
    }

    private fun callToFastPixSDK(file: File?) {
        try {
            _binding.chunkSize.text = "Chunk Size: $chunkSize"
            sdk = FastPixUploadSdk.Builder(this)
                .setFile(file)
                .setSignedUrl(_signedUrl.orEmpty())
                .setChunkSize(chunkSize * 1024 * 1024) // Chunk Size in Byte
                .callback(this) // Callback for handling the sdk events
                .setRetryDelay(2000) // Retry Delay
                .build()
            sdk.startUpload()
        } catch (ex: UploadExceptions) {
            _binding.textProgress.text = ex.message
        }

    }

    private fun handleIntent() {
        chunkSize = intent?.getLongExtra("chunks", 16) ?: 0L
    }

    private fun getSignedUrl() {
        val requestBody = getRequestForSignedUrl()
        val credentials = "${Constants.TOKEN_ID}:${Constants.SECRET_KEY}"
        val auth = "Basic ".plus(Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP))
        val headers = mapOf(Pair("Authorization", auth), Pair("Content-Type", "application/json"))
        OkHttpHelper.post(
            "https://venus-v1.fastpix.dev/on-demand/uploads/v2",
            headers = headers,
            body = requestBody,
            callback = object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    LOGGER.log(Level.SEVERE, e.message)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val jsonObject = JSONObject(responseBody)
                        val jsonData = jsonObject.getJSONObject("data")
                        _signedUrl = jsonData.getString("url")
                        lifecycleScope.launch(mainDispatcher) {
                            _binding.uploadVideo.isVisible = true
                            _binding.textProgress.text = "You can see\nprogress here"
                        }
                    } else {
                        _binding.textProgress.text = "Something went to wrong! try again later"

                    }
                }

            })
    }

    private var _signedUrl: String? = null
    private var _uploadId: String? = null

    private fun getRequestForSignedUrl(): RequestBody {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val metadata = JSONObject().apply {
            put("key1", "value1")
        }

        val pushMediaSettings = JSONObject().apply {
            put("metadata", metadata)
            put("accessPolicy", "public")
            put("maxResolution", "2160p")
        }

        val jsonObject = JSONObject().apply {
            put("corsOrigin", "*")
            put("pushMediaSettings", pushMediaSettings)
        }

        return jsonObject.toString().toRequestBody(mediaType)
    }


    fun getFileFromUri(uri: Uri): File? {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = getFileName(this, uri)
            val tempFile = File(cacheDir, fileName)
            tempFile.outputStream().use { fileOut ->
                inputStream.copyTo(fileOut)
            }
            return tempFile
        } catch (e: FileNotFoundException) {
            LOGGER.log(Level.SEVERE, e.message)
            return null
        }
    }

    fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1 && cut != null) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "temp_file"
    }


    override fun onProgressUpdate(progress: Double) {
        lifecycleScope.launch(mainDispatcher) {
            _binding.progressIndicator.progress = progress.toInt()
            _binding.textProgress.text = "${progress.toInt()}%"
        }
    }

    override fun onSuccess(timiMillis: Long) {
        lifecycleScope.launch(mainDispatcher) {
            _binding.textProgress.text = "Uploading\nSuccessful"
            delay(1000)
            _binding.textProgress.text = "Total time:\n${timiMillis / 1000}s"
            _binding.pauseResumeLayout.isVisible = false
            _binding.uploadVideo.isVisible = true
            _binding.uploadVideo.text = "Upload Again"
        }
    }

    override fun onError(error: String, timiMillis: Long) {
        lifecycleScope.launch(mainDispatcher) {
            _binding.textProgress.text = "$error $timiMillis"
        }
    }

    override fun onNetworkStateChange(isOnline: Boolean) {
        // Handle Network State
        if (isOnline) {
            _binding.pause.isEnabled = true
            _binding.resume.isEnabled = false
        }
    }

    override fun onUploadInit() {
        lifecycleScope.launch(mainDispatcher) {
            _binding.textProgress.text = "SDK Initialized\nSuccessfully"
            _binding.pauseResumeLayout.isVisible = true
            _binding.uploadVideo.isVisible = false

        }
        LOGGER.info("SDK initialized")
    }

    override fun onAbort() {
        lifecycleScope.launch(mainDispatcher) {
            _binding.progressIndicator.progress = 0
            _binding.textProgress.text = "Process\nAbort"
        }
    }

    override fun onChunkHandled(
        totalChunks: Int,
        filSizeInBytes: Long,
        currentChunk: Int,
        currentChunkSizeInBytes: Long
    ) {
        lifecycleScope.launch(mainDispatcher) {
            _binding.totalChunks.text = "Total Chunks: $totalChunks"
            _binding.fileSizeInBytes.text = "File Size in MBs: ${filSizeInBytes / 1024 / 1024}"
            _binding.chunkCurrentlyUploading.text = "Count Of Chunk Uploading: $currentChunk"
            _binding.currentChunkSizeInBytes.text =
                "Current Chunk Size in MBs: ${currentChunkSizeInBytes / 1024 / 1024}"
        }
    }

    override fun onChunkUploadingFailed(
        failedChunkRetries: Int,
        chunkCount: Int,
        currentChunkSize: Long
    ) {
        lifecycleScope.launch(mainDispatcher) {
            _binding.textProgress.text =
                "Retried Exhausted: $failedChunkRetries\n Chunk Count: $chunkCount\n Chunk Size: $currentChunkSize"
        }
    }

    override fun onPauseUploading() {
        lifecycleScope.launch(mainDispatcher) {
            delay(1000)
            _binding.textProgress.text = "Paused"
        }
    }

    override fun onResumeUploading() {
        lifecycleScope.launch(mainDispatcher) {
            delay(1000)
            _binding.textProgress.text = "Resume"
        }
    }
}


