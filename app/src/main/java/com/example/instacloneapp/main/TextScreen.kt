package com.example.instacloneapp.main

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.instacloneapp.IgViewModel

//val messagesList = mutableStateOf<List<String>>(listOf())
//val rtDb = Firebase.database.getReference("messages")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextScreen(navController: NavController, vm: IgViewModel) {

    val messagesList = vm.messages.value

    val focusManager = LocalFocusManager.current
    val msgText = rememberSaveable {
        mutableStateOf("")
    }





    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        if (messagesList.isEmpty()){
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "No message")
            }
        }else{
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items = messagesList){msg ->

                        ChatRow(msg)


                }

            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()

        ) {

            TextField(
                value = msgText.value,
                onValueChange = { msgText.value = it },
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color.LightGray),
                colors = TextFieldDefaults.textFieldColors(
                    textColor = Color.Black,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            Button(
                onClick = {
                          vm.sendMessage(msgText.value)
                    msgText.value = ""
                    focusManager.clearFocus()
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(text = "Send")
            }
        }
    }


}


@Composable
fun ChatRow(msg: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {

        msg?.let { Text(text = it, modifier = Modifier.padding(8.dp)) }

    }
}

fun formatTime(timeStamp: Long?): String {
    val date = java.util.Date(timeStamp!!)
    val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
    return format.format(date)
}
