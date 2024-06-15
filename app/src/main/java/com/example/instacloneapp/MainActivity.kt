

package com.example.instacloneapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.instacloneapp.auth.LoginScreen
import com.example.instacloneapp.auth.ProfileScreen
import com.example.instacloneapp.auth.SignupScreen
import com.example.instacloneapp.main.CommentsScreen
import com.example.instacloneapp.main.FeedScreen
import com.example.instacloneapp.main.MyPostsScreen
import com.example.instacloneapp.main.NewPostScreen
import com.example.instacloneapp.main.NotificationMessage
import com.example.instacloneapp.main.SearchScreen
import com.example.instacloneapp.main.SinglePostScreen
import com.example.instacloneapp.main.TextScreen
import com.example.instacloneapp.ui.theme.InstaCloneAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstaCloneAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InstaApp()
                }
            }
        }
    }
}

sealed class DestinationScreen(val route: String){

    object Signup: DestinationScreen("signup")
    object Login: DestinationScreen("login")
    object Feed: DestinationScreen("feed")
    object Search: DestinationScreen("search")
    object MyPosts: DestinationScreen("myposts")
    object Profile: DestinationScreen("profile")
    object NewPost: DestinationScreen("newpost/{imageUri}"){
        fun createRoute(uri: String) = "newpost/$uri"
    }
    object SinglePost: DestinationScreen("singlepost")
    object Comments: DestinationScreen("comments/{postId}"){
        fun createRoute(postId: String) = "comments/$postId"
    }
    object Text: DestinationScreen("singlepost")

}

@Composable
fun InstaApp(){
    val vm = hiltViewModel<IgViewModel>()
    val navController = rememberNavController()
    
    NotificationMessage(vm = vm)
    
    NavHost(navController = navController, startDestination = DestinationScreen.Signup.route){
        composable(DestinationScreen.Signup.route) {
            SignupScreen(navController = navController, vm = vm)
        }
        composable(DestinationScreen.Login.route) {
            LoginScreen(navController = navController, vm = vm)
        }
        composable(DestinationScreen.Feed.route) {
            FeedScreen(navController = navController, vm = vm)
        }
        composable(DestinationScreen.Search.route) {
            SearchScreen(navController = navController, vm = vm)
        }
        composable(DestinationScreen.MyPosts.route) {
            MyPostsScreen(navController = navController, vm = vm)
        }
        composable(DestinationScreen.Profile.route) {
            ProfileScreen(navController = navController, vm = vm)
        }
        composable(DestinationScreen.NewPost.route) { navBackStackEntry ->
            val imageUri = navBackStackEntry.arguments?.getString("imageUri")
            imageUri?.let {
                NewPostScreen(navController = navController, vm = vm, encodedUri = it)
            }

        }
        composable(DestinationScreen.Text.route) {
            TextScreen(navController = navController, vm = vm)
        }
        composable(
            route = DestinationScreen.SinglePost.route + "/{postId}",
            arguments = listOf(
                navArgument(name = "postId"){
                    type = NavType.StringType
                }
            )
        ) { BackStackEntry ->
            val postData = BackStackEntry.arguments?.getString("postId")
            if (postData != null) {
                SinglePostScreen(navController = navController, vm = vm, pid = postData)
                Log.d("LOL", "POSTDATA!=NULL")
            }else{
                Log.d("LOL", "POSTDATA==NULL")
                TextScreen(navController = navController, vm = vm)
            }
        }

        composable(DestinationScreen.Comments.route) { navBackStackEntry ->
            val postId = navBackStackEntry.arguments?.getString("postId")
            postId?.let {
                CommentsScreen(navController = navController, vm = vm, postId = it)
            }
        }


    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    InstaCloneAppTheme {
        InstaApp()
    }
}


