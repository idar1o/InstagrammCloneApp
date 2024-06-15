package com.example.instacloneapp.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.instacloneapp.DestinationScreen
import com.example.instacloneapp.IgViewModel
import com.example.instacloneapp.R
import com.example.instacloneapp.data.PostData

@Composable
fun SinglePostScreen(navController: NavController, vm: IgViewModel, pid: String) {
    val comments = vm.comments.value

    LaunchedEffect(key1 = Unit) {
        vm.getComments(postId = pid)

    }

    vm.getPostData(pid)
    val post = vm.postData.value


    post?.let { post ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(text = "Back", modifier = Modifier.clickable { navController.popBackStack() })

            CommonDivider()

            SinglePostDisplay(navController = navController, vm = vm, post = post, comments.size)
        }
    }

}


@Composable
fun SinglePostDisplay(
    navController: NavController?,
    vm: IgViewModel,
    post: PostData,
    nbComments: Int
) {

    val userData = vm.userData?.value
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    )
    {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = CircleShape, modifier = Modifier
                    .padding(8.dp)
                    .size(32.dp)
            ) {
                Image(
                    painter = rememberImagePainter(data = post.userImage),
                    contentDescription = null
                )
            }
            Text(text = post.username ?: "")
            Text(text = ".", modifier = Modifier.padding(8.dp))

            if (userData?.userId == post.userId) {
                // Current user's post. Dont show Follow btn
            } else if (userData?.following?.contains(post.userId) == true) {
                Text(text = "Following", color = Color.Gray, modifier = Modifier.clickable {
                    vm.onFollowClick(post.userId!!)
                })
            } else {
                Text(text = "Follow", color = Color.Blue, modifier = Modifier.clickable {
                    vm.onFollowClick(post.userId!!)
                })
            }
        }


    }
    Box {
        val modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 150.dp)
        CommonImage(
            data = post?.postImage,
            modifier = modifier,
            contentScale = ContentScale.FillWidth
        )
    }

    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.ic_like),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(Color.Red)
        )
    }

    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = post?.username ?: "", fontWeight = FontWeight.Bold)
        Text(text = post?.postDescription ?: "", modifier = Modifier.padding(start = 8.dp))
    }

    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$nbComments comments",
            color = Color.Gray,
            modifier = Modifier
                .padding(start = 8.dp)
                .clickable {
                    post.postId?.let {
                        navController?.navigate(DestinationScreen.Comments.createRoute(it))
                    }
                })
    }

}
