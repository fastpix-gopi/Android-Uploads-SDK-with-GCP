# 📦 FastPixUploader SDK (Android)

The **FastPixUploader SDK** is a robust Android library designed for chunked file uploads using
signed URLs. It simplifies large file uploads by splitting files into smaller chunks, ensuring
smooth and reliable transfers with built-in retry and progress tracking mechanisms.
  
---  

## 🚀 Features

- Chunked uploads for large files
- Resumable uploads
- Customizable chunk size and retry strategy
- Upload lifecycle callbacks
- Network state awareness
- Pause, resume and abort support

---  

## 🛠 Requirements

- Android 5.0 (API 21) or above
- Kotlin project (Java-compatible via interfaces)
- A `File` and a `signed upload URL`. [How to get a Singed Url?](https://docs.fastpix.io/docs/get-started-in-5-minutes#step-2-get-an-api-access-token-from-the-dashboard)

---  

## 📦 Installation

Add the SDK module to your project and include it as a dependency if distributed as an `.aar` or
module.

- Download the .aar file
- Place it in your project's libs folder
- Add to your app's build.gradle:

```groovy
dependencies {
    implementation("io.fastpix.upload:x.x.x")
}
```

  
---  

## ⚙️ Initialization

```kotlin  
val sdk = FastPixUploadSdk.Builder(this)
    .setFile(file)
    .setSignedUrl(_signedUrl.orEmpty())
    .setChunkSize(16 * 1024 * 1024) // Chunk Size in Byte
    .setMaxRetries(2000) // In Milliseconds
    .callback(new object : FastPixUploadCallbacks {
        override fun onProgressUpdate(progress: Double) { /* ... */
        }
        override fun onPauseUploading() {
            // Handle Pause State
        }
        override fun onResumeUploading() {
            // Handle Resume State
        }
        override fun onAbort() {
            // Handle Abort State
        }
        override fun onUploadInit() {
            // Handle Abort State
        }
        override fun onChunkHanlded(
            totalChunks: Int,
            fileSizeInBytes: Long,
            currentChunk: Int,
            currentChunkSizeInBytes: Long
        ) {
            // Update the UI according chunk data
        }
        override fun onSuccess(timiMillis: Long) {
            // Time to complete the Upload Process
        }
        override fun onChunkUploadingFailed(
            failedChunkRetries: Int,
            chunkCount: Int,
            chunkSize: Int
        ) {
            // Handle Chunk Upload Failure
        }
        override fun onError(error: String, timeMillis: Long) {
            // Handle error message
        }
        override fun onNetworkStateChanged(isOnline: Boolean) {
            // Handle Network Changes
        }

    })
    .setRetryDelay(2000) // Retry Delay  
    .build()
// Starts the Uploading Process
sdk.startUpload()
```  

  
---  

## 📁 Chunk Size Configuration

- **Default**: 16MB (16384 KB)
- **Min**: 5MB (5120 KB)
- **Max**: 500MB (512000 KB)

---  

## 🔁 Retry

Set retry delay in milliseconds if a chunk upload fails:

```kotlin  
retryDelayMs = 2000  // 2 seconds between retry attempts  
```  

  
---  

## 📡 Callbacks

Implement `FastPixUploadCallback` to handle various upload events:

| Method                                                                                                 | Description                                                          |  
|--------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------|  
| `onProgressUpdate(progress: Double)`                                                                   | Called with total upload progress (0.0 - 100.0)                      |  
| `onSuccess(timiMillis: Long)`                                                                          | Called when all chunks are successfully uploaded                     |  
| `onError(error: String, timeMillis: Long)`                                                             | Called when any fatal error occurs in uploading process              |
| `onChunkUploadingFailed(failedChunkRetries: Int, chunkCount: Int, chunkSize: Long)`                    | Called when any fatal error occurs when uploading a individual chunk |  
| `onNetworkStateChange(isOnline: Boolean)`                                                              | Called when device's connectivity status changes                     |  
| `onUploadInit()`                                                                                       | Called once upload initialization starts                             |  
| `onChunkHandled(totalChunks:Int,filSizeInBytes:Long,currentChunk:Int,  currentChunkSizeInBytes: Long)` | Provide Information About the Chunks Uploading                       |

  
---  

## ⎯ Upload Controls

The SDK supports pausing, resuming, and aborting uploads at any time.

| Method                | Description                               |  
|-----------------------|-------------------------------------------|
| `onPauseUploading()`  | Triggered when upload is paused           |
| `onResumeUploading()` | Triggered when upload resumes             |
| `onAbort()`           | Triggered when upload is manually aborted |

## ⎯ Common Errors
Following custom exceptions are build to handle the SDK exceptions. Upload Exception is the parent of all.

| Method                       | Description                                                             |  
|------------------------------|-------------------------------------------------------------------------|
| `FileNotFoundException`      | File not found or inaccessible and Check file permissions and existence |
| `ChunkSizeException`         | Chunk size should be between 5mns                                       |
| `FileNotReadableException`   | File is not readable.                                                   |
| `FileContentEmptyException`  | File content is empty                                                   |
| `SignedUrlNotFoundException` | Signed is empty or null.                                                |

### 🔧 Control Methods

```
sdk.pauseUpload()     // Pauses upload
sdk.resumeUpload()    // Resumes paused upload
sdk.abortUpload()     // Aborts the upload completely
```

----------

## 🔁 Upload Flow

```mermaid  
graph TD
    A[Initialize SDK with file & signed URL] --> B[Call INIT API with action=init, signedUrl, partitions]
    B --> C[Receive uploadId, objectName, signed URLs]
    C --> D[Start uploading chunks sequentially]
    D --> E[Upload chunk to corresponding signed URL]
    E --> F[Track progress and retry if needed]
    F --> G{All chunks uploaded?}
    G -- Yes --> H[Call COMPLETE API with action=complete, uploadId, objectName]
    G -- No --> D H --> I[Upload Completed Successfully]  
```  

  
---  

## ⚠️ Common Issues

- **High Memory Usage**:  
  Avoid allocating the entire file in memory, especially for large files (>1GB). The SDK is
  optimized to read and upload only the required chunk range.

- **Upload Failures Due to Invalid URLs**:  
  Ensure that the signed URLs provided for each chunk are valid and have not expired.

- **Interrupted Uploads on Network Change**:  
  Monitor connectivity using the `onNetworkStateChange()` callback to handle network interruptions
  gracefully and improve upload reliability.

---  

## 📫 Support

For bugs, issues or feedback, please contact [support@fastpix.io](mailto:support@fastpix.io)