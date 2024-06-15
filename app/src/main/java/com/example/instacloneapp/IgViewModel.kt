package com.example.instacloneapp

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.instacloneapp.data.CommentData
import com.example.instacloneapp.data.Event
import com.example.instacloneapp.data.PostData
import com.example.instacloneapp.data.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject

const val USERS = "users"
const val POSTS = "posts"
const val COMMENTS = "comments"
const val MESSAGES = "messages"

@HiltViewModel
class IgViewModel @Inject constructor(
    val auth: FirebaseAuth,
    val db: FirebaseFirestore,
    val storage: FirebaseStorage,
    val rtdb: DatabaseReference
) : ViewModel() {
    val signedIn = mutableStateOf(false)
    val inProgress = mutableStateOf(false)
    val userData = mutableStateOf<UserData?>(null)
    val postData = mutableStateOf<PostData?>(null)
    val popupNotification = mutableStateOf<Event<String>?>(null)

    val refreshPostsProgress = mutableStateOf(false)
    val posts = mutableStateOf<List<PostData>>(listOf())

    val searchedPosts = mutableStateOf<List<PostData>>(listOf())
    val searchedPostProgress = mutableStateOf(false)

    val postsFeed = mutableStateOf<List<PostData>>(listOf())
    val postsFeedProgress = mutableStateOf(false)

    val comments = mutableStateOf<List<CommentData>>(listOf())
    val commentsProgress = mutableStateOf(false)

    val followers = mutableStateOf(0)

    val messages = mutableStateOf<List<String>>(listOf())

    init {
//        auth.signOut()
        val currentUser = auth.currentUser
        signedIn.value = currentUser != null
        currentUser?.uid?.let { uid ->
            getUserData(uid)
        }

        rtdb.child(MESSAGES).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newMessages = snapshot.children.mapNotNull { it.value.toString()}

                messages.value = newMessages
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }

    fun sendMessage(text: String) {

        rtdb.child(MESSAGES).push().setValue(userData.value?.username + "\n" +text )
    }
    fun onSignUp(
        username: String,
        email: String,
        password: String
        ){
        if ( username.isEmpty() or email.isEmpty() or password.isEmpty()){
            handleException(customMessage = "Please fill in all fields")
            return
        }
        popupNotification.value = Event("Ты входишь")

        inProgress.value = true
        db.collection(USERS).whereEqualTo("username", username).get()
            .addOnSuccessListener { documents ->
                Log.d("LOL", "username checking")
                if (documents.size() > 0){
                    handleException(customMessage = "Username already exists")
                    inProgress.value = false
                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener{ task->
                            if(task.isSuccessful){
                                signedIn.value = true

                                createOrUpdateProfile(username = username)
                            } else{
                                handleException(task.exception, "Signup failed")
                            }
                            inProgress.value = false
                        }
                }

            }.addOnFailureListener() {  }
    }

    fun onLogin(email: String, pass : String){
        if (email.isEmpty() or pass.isEmpty()){
            handleException(customMessage = "Please fill in all fields")
            return
        }
        inProgress.value = true
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener{ task ->
                if (task.isSuccessful){
                    signedIn.value = true
                    inProgress.value = false
                    auth.currentUser?.uid?.let{uid ->
                        getUserData(uid)
                        handleException(customMessage = "Login success")
                    }
                }else{
                    handleException(customMessage = "Please fill in all fields")
                    inProgress.value = false
                }

            }
            .addOnFailureListener {exc ->
                handleException(exc, customMessage = "Login failed")
                inProgress.value = false

            }
    }


    private fun createOrUpdateProfile(
        name: String? = null,
        username: String? = null,
        bio: String? = null,
        imageUrl: String? = null
    ){
        val uid = auth.currentUser?.uid
        val userData = UserData(
            userId = uid,
            name = name ?: userData.value?.name,
            username = username ?: userData.value?.username,
            bio = bio ?: userData.value?.bio,
            imageUrl = imageUrl ?: userData.value?.imageUrl,
            following = userData.value?.following
        )
        uid?.let { uid ->
            inProgress.value = true
            db.collection(USERS).document(uid).get().addOnSuccessListener {
                if (it.exists()){
                    it.reference.update(userData.toMap())
                        .addOnSuccessListener {
                            this.userData.value = userData
                            inProgress.value = false
                        }
                        .addOnFailureListener{
                            handleException(it, "Cannot update user")
                            inProgress.value = false
                        }

                }else{
                    db.collection(USERS).document(uid).set(userData)
                    getUserData(uid)
                    inProgress.value = false
                }
            }
                .addOnFailureListener{
                    exc ->
                    handleException(exc, "Cannot create user")
                    inProgress.value = false
                }
        }
    }

    private fun getUserData( uid: String){
        inProgress.value = true
        db.collection(USERS).document(uid).get()
            .addOnSuccessListener {
                val user = it.toObject<UserData>()
                userData.value = user
                inProgress.value = false
                refreshPosts()
                getPersonalizedFeed()
                getFollowers(user?.userId)
            }
            .addOnFailureListener{exc ->
                handleException(exc, "Cannot retrieve user data")
                inProgress.value = false
            }
    }
     fun getPostData( pid: String){
        inProgress.value = true
        db.collection(POSTS).document(pid).get()
            .addOnSuccessListener {
                val post = it.toObject<PostData>()
                postData.value = post
                inProgress.value = false

            }
            .addOnFailureListener{exc ->
                handleException(exc, "Cannot retrieve post data")
                inProgress.value = false
            }
    }

    private fun handleException(exception : Exception? = null, customMessage: String = "") {
        exception?.printStackTrace()
        val errorMsg = exception?.localizedMessage ?: ""
        val message = if (customMessage.isEmpty()) errorMsg else "$customMessage: $errorMsg"
        popupNotification.value = Event(message)
    }

    fun updateProfileData( name: String, username: String, bio: String) {
        createOrUpdateProfile(name, username, bio)
    }

    private fun uploadImage( uri: Uri, onSuccess: (Uri) -> Unit){
        inProgress.value = true

        val storageRef = storage.reference
        val uuid = UUID.randomUUID()
        val imageRef = storageRef.child("image/$uuid")
        val uploadTask = imageRef.putFile(uri)

        uploadTask.addOnSuccessListener {
            val result = it.metadata?.reference?.downloadUrl
            result?.addOnSuccessListener(onSuccess)
            inProgress.value = false
        }
            .addOnFailureListener{exc ->
                handleException(exc)
                inProgress.value = false
            }
    }

    fun uploadProfileImage(uri: Uri){
        uploadImage(uri){
            createOrUpdateProfile(imageUrl = it.toString())
            uploadPostUserImageData(it.toString())
        }

    }

    private fun uploadPostUserImageData( imageUrl: String){

        val currentuUid = auth.currentUser?.uid
        db.collection(POSTS).whereEqualTo("userId", currentuUid).get()
            .addOnSuccessListener {
                val posts = mutableStateOf<List<PostData>>(arrayListOf())
                convertPosts(it, posts)
                val refs = arrayListOf<DocumentReference>()
                for (post in posts.value){
                    post.postId?.let { id ->
                        refs.add(db.collection(POSTS).document(id))
                    }
                }
                if (refs.isNotEmpty()){
                    db.runBatch{batch ->
                        for (ref in refs){
                            batch.update(ref, "userImage", imageUrl)
                        }
                    }
                        .addOnSuccessListener {
                            refreshPosts()
                        }
                }
            }
    }
    fun onLogout(){
        auth.signOut()
        signedIn.value = false
        userData.value = null
        popupNotification.value = Event("Logged out")
        searchedPosts.value = listOf()
        postsFeed.value = listOf()
        comments.value = listOf()
    }

    fun onNewPost(uri: Uri, description: String, onPostSuccess: () -> Unit){
        uploadImage(uri){
            onCreatePost(it, description, onPostSuccess)
        }
    }

    private fun onCreatePost(imageUri: Uri, description: String, onPostSuccess: () -> Unit) {
        inProgress.value = true
        val currentUid = auth.currentUser?.uid
        val currentUsername = userData.value?.username
        val currentUserImage = userData.value?.imageUrl

        if (currentUid != null){
            val postUuid = UUID.randomUUID().toString()

            val fillerWords = listOf("is", "the", "to", "be", "of", "and", "or", "a", "in", "it")
            val searchTerms  = description
                .split(" ", ".","," , "?", "!", "#")
                .map{it.lowercase()}
                .filter { it.isNotEmpty() and !fillerWords.contains(it) }


            val post = PostData(
                postId = postUuid,
                userId = currentUid,
                username = currentUsername,
                userImage = currentUserImage,
                postImage = imageUri.toString(),
                postDescription = description,
                time = System.currentTimeMillis(),
                likes = listOf<String>(),
                searchTerms = searchTerms
            )

            db.collection(POSTS).document(postUuid).set(post)
                .addOnSuccessListener {
                    popupNotification.value = Event("Post successfully created")
                    inProgress.value = false
                    refreshPosts()
                    onPostSuccess.invoke()

                }
                .addOnFailureListener { exc ->
                    handleException(exc, "Unable to create post")
                    inProgress.value = false
                }
        }else{
            handleException(customMessage = "Error: username unavailable. Unable to create post")
            onLogout()
            inProgress.value = false
        }

    }


    private fun refreshPosts(){

        val currentUid = auth.currentUser?.uid
        if(currentUid != null){
            refreshPostsProgress.value = true
            db.collection(POSTS).whereEqualTo("userId", currentUid).get()
                .addOnSuccessListener {documents ->
                    convertPosts(documents, posts)
                    refreshPostsProgress.value = false
                }
                .addOnFailureListener {
                    handleException(it, "Cannot fetch posts")
                    refreshPostsProgress.value = false
                }
        }else{
            handleException(customMessage = "Error: username unavailable. Unable to refresh posts")
            onLogout()
        }
    }

    private fun convertPosts(documents: QuerySnapshot, outState: MutableState<List<PostData>>) {
        val newPosts = mutableListOf<PostData>()
        documents.forEach{doc->
            val post = doc.toObject<PostData>()
            newPosts.add(post)
        }
        val sortedPosts = newPosts.sortedByDescending { it.time }
        outState.value = sortedPosts
    }
    fun searchPosts( searchTerm: String){
        if ( searchTerm.isNotEmpty()){
            searchedPostProgress.value = true
            db.collection(POSTS)
                .whereArrayContains("searchTerms", searchTerm)
                .get()
                .addOnSuccessListener {
                    convertPosts(it, searchedPosts)
                    searchedPostProgress.value = false
                }
                .addOnFailureListener {exc ->
                    handleException(exc, "Cannot search posts")
                    searchedPostProgress.value = false
                }
        }
    }
    fun onFollowClick(userId: String){
        auth.currentUser?.uid?.let {currentUser ->
            val following = arrayListOf<String>()
            userData.value?.following?.let {
                following.addAll(it)
            }
            if (following.contains(userId)){
                following.remove(userId)
            }else{
                following.add(userId)
            }
            db.collection(USERS).document(currentUser).update("following", following)
                .addOnSuccessListener {
                    getUserData(currentUser)
                }
        }
    }

    private fun getPersonalizedFeed(){
        postsFeedProgress.value = true
        val following = userData.value?.following
        if(!following.isNullOrEmpty()){
            db.collection(POSTS).whereIn("userId", following)
                .get()
                .addOnSuccessListener {
                    convertPosts(it, postsFeed)
                    if(postsFeed.value.isEmpty()){
                        getGeneralFeed()
                    }else {
                        postsFeedProgress.value = false
                    }
                }
                .addOnFailureListener {exc ->
                    handleException(exc, "Cannot get personalized feed")
                    postsFeedProgress.value = false
                }
        }else{
            getGeneralFeed()
        }
    }

    private fun getGeneralFeed() {
        postsFeedProgress.value = false
        val currentTime = System.currentTimeMillis()
        val difference = 24 * 60 * 60 * 1000
        db.collection(POSTS)
            .whereGreaterThan("time", currentTime - difference)
            .get()
            .addOnSuccessListener {
                convertPosts(documents = it, outState = postsFeed)
                postsFeedProgress.value = false

            }
            .addOnFailureListener { exc ->
                handleException(exc, "Cannot get general feed")
            }
    }

    fun onLikePost( postData: PostData){
        auth.currentUser?.uid?.let {userId ->
            postData.likes?.let { likes ->
                val newLikes = arrayListOf<String>()
                if (likes.contains(userId)){
                    newLikes.addAll(likes.filter { userId != it })
                }else{
                    newLikes.addAll(likes)
                    newLikes.add(userId)
                }
                postData.postId?.let {postId ->
                    db.collection(POSTS).document(postId).update("likes", newLikes)
                        .addOnSuccessListener {
                            postData.likes = newLikes
                        }
                        .addOnFailureListener { exc ->
                            handleException(exc, "Unable to like or unlike")
                        }

                }
            }

        }
    }
    fun createComment(postId: String, text: String){
        userData.value?.username?.let {username ->
            val commentid = UUID.randomUUID().toString()
            val comment = CommentData(
                commentId = commentid,
                postId = postId,
                username = username,
                text = text,
                timeStamp = System.currentTimeMillis()
            )
            db.collection(COMMENTS).document(commentid).set(comment)
                .addOnSuccessListener {
                    getComments(postId)
                }
                .addOnFailureListener { exc ->
                    handleException(exc, "Cannot to create comment.")
                }
        }
    }
    fun getComments(postId: String){
        commentsProgress.value = true

        db.collection(COMMENTS).whereEqualTo("postId", postId).get()
            .addOnSuccessListener {document ->
                val newcommentsList = arrayListOf<CommentData>()
                document.forEach{doc ->
                    val comment = doc.toObject<CommentData>()
                    newcommentsList.add(comment)
                }
                val sortedComments = newcommentsList.sortedByDescending { it.timeStamp  }
                comments.value = sortedComments
                commentsProgress.value = false
            }
            .addOnFailureListener {exc ->
                handleException(exc, "Cannot to load comments.")
                commentsProgress.value = false

            }

    }

    fun getFollowers(uid: String?){
        db.collection(USERS).whereArrayContains("following", uid ?: "").get()
            .addOnSuccessListener { documents ->
                followers.value = documents.size()
            }
            .addOnFailureListener {  }
    }



}