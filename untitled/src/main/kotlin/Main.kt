

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sun.security.ntlm.Client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.beans.BeanDescriptor
import java.io.IOException
import java.lang.Exception
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit
import javax.xml.stream.events.Comment
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val BASE_URL ="http://127.0.0.1:9999"

private val gson = Gson()
private val client = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor(::println).apply {
        level = HttpLoggingInterceptor.Level.BODY
    })
    .connectTimeout(30,TimeUnit.SECONDS)
    .build()


fun main(args: Array<String>) {
with(CoroutineScope(EmptyCoroutineContext)) {
    launch {
        try {
            val posts = getPosts(client)
                .map {post ->
                    PostWithComment(post, getComments(client, post.id))
                    }
            println(posts)
        } catch (e: Exception){
            e.printStackTrace()
        }
    }
}
Thread.sleep(30_000L)
}



suspend fun OkHttpClient.apiCall(url: String) : Response {
    return suspendCoroutine { continuation ->
        Request.Builder()
            .url(url)
            .build()
            .let (::newCall)
            .enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }

                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }
            })
    }
}

suspend fun <T> makeRequest(url: String, client: OkHttpClient, typeToken: TypeToken<T>): T =
    withContext(Dispatchers.IO) {
        client.apiCall(url)
            .let {response ->
                if (!response.isSuccessful) {
                    response.close()
                    throw RuntimeException (response.message)
                }
                val body = response.body ?: throw  RuntimeException ("responce body is null")
                gson.fromJson(body.string(), typeToken.type)
            }
    }


suspend fun getPosts (client: OkHttpClient) : List<Post> =
    makeRequest("$BASE_URL/api/slow/posts", client, object : TypeToken<List<Post>>(){})


suspend fun getComments (client: OkHttpClient, id: Long) : List<Comment> =
    makeRequest("$BASE_URL/api/slow/comments", client, object : TypeToken<List<Comment>>(){})

suspend fun getAuthor (client: OkHttpClient, id: Long) : Author =
    makeRequest("$BASE_URL/api/authors/{id}", client, object : TypeToken<Author>() {})


data class Post (

    val id: Long,
    val authorId: Long,
    val content: String,
    val published: Long,
    val likeByMe: Boolean,
    val like: Int =0,
    var attachment: Attachment? = null,
)

data class Attachment(
    val url: String,
    val descriptor: String,
    val type: String,
)

data class Comment (
    val id: Long,
    val postId: Long,
    val authorId: Long,
    val content: String,
    val published: Long,
    val likeByMe: Boolean,
    val like: Int=0,
)

data class Author(
    val id: Long,
    val name: String,
    val avatar: String,
)

class PostWithComment(post: Post, comments: List<Comment>) {

}