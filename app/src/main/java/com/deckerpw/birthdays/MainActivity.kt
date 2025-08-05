@file:OptIn(ExperimentalMaterial3Api::class)

package com.deckerpw.birthdays

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.deckerpw.birthdays.api.Birthday
import com.deckerpw.birthdays.api.Database
import com.deckerpw.birthdays.api.DatabaseException
import com.deckerpw.birthdays.ui.theme.BirthdaysTheme
import java.time.LocalDateTime

val bebasNeue = FontFamily(
    Font(R.font.bebas_neue, FontWeight.Normal)
)

val db = Database("http://play.deckerpw.com:52280")
var birthdays by mutableStateOf(mutableListOf<Birthday>())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            scheduleDailyNotifications(LocalContext.current, this)
            var creatorVisible by remember { mutableStateOf(false) }
            BirthdaysTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.app_name)) }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { creatorVisible = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(80.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.cake_add),
                                contentDescription = "Add",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    },
                    floatingActionButtonPosition = FabPosition.Center,
                    modifier = Modifier
                        .fillMaxSize()
                ) { innerPadding ->
                    Thread {
                        birthdays = db.getBirthdays().toMutableList().apply {
                            sortBy { it.daysUntilNextBirthday() }
                        }
                    }.start()
                    var isLoading by remember { mutableStateOf(false) }
                    PullToRefreshBox(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        isRefreshing = isLoading,
                        onRefresh = {
                            Thread {
                                isLoading = true
                                birthdays = db.getBirthdays().toMutableList()
                                isLoading = false
                            }.start()
                        }
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(birthdays) { birthday ->
                                BirthdayItem(birthday, modifier = Modifier.padding(8.dp), bebasNeue)
                            }
                        }
                    }
                    BirthdayCreator(
                        visible = creatorVisible,
                        onDismiss = {
                            creatorVisible = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BirthdayItem(
    birthday: Birthday,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily? = null
) {
    val context = LocalContext.current
    var showPasswordDialog by remember { mutableStateOf(false) }
    Card(
        onClick = {
            showPasswordDialog = true
        },
        shape = RoundedCornerShape(20),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp)
    ) {
        Column {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp, 8.dp, 16.dp, 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = birthday.name,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = "${birthday.date.dayOfMonth}. ${
                            birthday.date.month.getDisplayName(
                                java.time.format.TextStyle.FULL,
                                java.util.Locale.GERMAN
                            )
                        }",
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.txt_days_until),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    var style = MaterialTheme.typography.displayLarge
                    if (fontFamily != null) {
                        style = style.copy(
                            fontFamily = fontFamily
                        )
                    }
                    Text(
                        text = birthday.daysUntilNextBirthday().toString(),
                        style = style,
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (birthday.isBirthday) {
                TextButton(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp, 0.dp, 8.dp, 8.dp),
                    onClick = {
                        Thread {
                            val db = Database("http://192.168.178.81:52280")
                            val link = db.getBirthdayVideo(birthday.id ?: 1)
                            // Open the video link in a web browser or video player
                            Intent(Intent.ACTION_VIEW).apply {
                                data = link.toUri()
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }.also { intent ->
                                ContextCompat.startActivity(
                                    context,
                                    intent,
                                    null
                                )
                            }
                        }.start()
                    }
                ) {
                    Text(stringResource(R.string.btn_celebrate, birthday.name))
                }
            }

        }
    }
    var fullBirthday by remember { mutableStateOf<Birthday?>(null) }
    var showCustomizationDialog by remember { mutableStateOf(false) }
    var video by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
            },
            title = { Text(stringResource(R.string.txt_pass_dialog_title)) },
            text = {
                OutlinedTextField(
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.txt_pass_dialog_label)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    Thread {
                        try {
                            fullBirthday = db.getFullBirthday(birthday.id ?: 1, password)
                            video = fullBirthday?.video ?: ""
                            showCustomizationDialog = true
                        } catch (e: DatabaseException) {
                            e.printStackTrace()
                            return@Thread
                        }
                    }.start()
                }) {
                    Text(stringResource(R.string.btn_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
    if (showCustomizationDialog) {
        AlertDialog(
            onDismissRequest = {
                showCustomizationDialog = false
            },
            title = { Text(stringResource(R.string.title_change_video)) },
            text = {
                OutlinedTextField(
                    value = video,
                    onValueChange = { video = it },
                    label = { Text(stringResource(R.string.txt_video)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCustomizationDialog = false
                    Thread {
                        db.changeBirthdayVideo(birthday.id ?: 1, video, password)
                    }.start()
                }) {
                    Text(stringResource(R.string.btn_apply))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showCustomizationDialog = false
                        Thread {
                            db.deleteBirthday(birthday.id ?: 1, password)
                            birthdays = db.getBirthdays().toMutableList().apply {
                                sortBy { it.daysUntilNextBirthday() }
                            }
                        }.start()
                    }) {
                        Text(stringResource(R.string.btn_delete))
                    }
                    TextButton(onClick = { showCustomizationDialog = false }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            }
        )
    }
}


@Preview(showBackground = true)
@Composable
fun BirthdayItemPreview() {
    BirthdaysTheme {
        BirthdayItem(
            Birthday(
                id = 1,
                name = "John Doe",
                date = LocalDateTime.now(),
                video = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
            ),
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
fun BirthdayCreator(
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    var birthdayName by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var video by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    if (visible) {
        AlertDialog(
            onDismissRequest = {
                onDismiss()
            },
            title = { Text(stringResource(R.string.title_create_birthday)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = birthdayName,
                        onValueChange = { birthdayName = it },
                        label = { Text(stringResource(R.string.txt_name)) }
                    )
                    OutlinedTextField(
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.txt_pass_dialog_label)) }
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        placeholder = { Text("TT.MM") },
                        label = { Text(stringResource(R.string.txt_date)) }
                    )
                    OutlinedTextField(
                        value = video,
                        onValueChange = { video = it },
                        label = { Text(stringResource(R.string.txt_video)) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onDismiss()
                    Thread {
                        val datePieces = date.split(".")
                        val day = datePieces.getOrNull(0)?.toIntOrNull()
                        val month = datePieces.getOrNull(1)?.toIntOrNull()
                        val birthday = Birthday(
                            name = birthdayName,
                            date = LocalDateTime.of(
                                LocalDateTime.now().year,
                                month ?: 1,
                                day ?: 1,
                                0,
                                0
                            ),
                            video = video
                        )
                        Thread {
                            db.addBirthday(birthday, password)
                            birthdays = db.getBirthdays().toMutableList().apply {
                                sortBy { it.daysUntilNextBirthday() }
                            }
                        }.start()
                        birthdayName = ""
                        date = ""
                        video = ""
                        password = ""
                    }.start()
                }) {
                    Text(stringResource(R.string.btn_apply))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { onDismiss() }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            }
        )
    }
}


fun clear() {
    val db = Database("http://192.168.178.81:52280")
    val birthdays = db.getBirthdays()
    for (birthday in birthdays) {
        db.deleteBirthday(birthday.id ?: 1, "password")
    }
}