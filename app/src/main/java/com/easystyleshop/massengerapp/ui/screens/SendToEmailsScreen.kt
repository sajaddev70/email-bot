package com.easystyleshop.massengerapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.easystyleshop.massengerapp.data.model.Email
import com.easystyleshop.massengerapp.util.createExcelFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.WorkbookFactory

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SendToEmailsContent(
    onSend: (email: String, subject: String, content: String) -> Unit,
    isLoading: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf(false) }
    var contentError by remember { mutableStateOf(false) }

    val emailList = remember { mutableStateListOf<String>() }
    var sentCount by remember { mutableStateOf(0) }

    val fromEmail = "devstest90@gmail.com"
    val excelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                scope.launch {
                    val imported = readEmailsFromExcel(context, uri)
                    emailList.clear()
                    emailList.addAll(imported)
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError) emailError = false
                },
                label = {
                    val label = if (emailList.isNotEmpty()) "ایمیل‌ها (${emailList.size})" else "ایمیل"
                    Text(label)
                },
                isError = emailError,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        excelLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
//                        val imported = listOf(
//                            "neverforgetthesixmillion@outlook.com",
//                            "5715551@gmail.com",
//                            "superjerrii@live.com",
//                            "nirmillerftw1234@gmail.com",
//                            "ibrahimbadarin@yahoo.com",
//                            "matanru1@gmail.com",
//                            "ms.gayka@gmail.com",
//                            "jesseadi@windowslive.com",
//                            "lielac2@gmail.com",
//                            "hila.gae.in@gmail.com",
//                            "bogdan.k.ezoter@gmail.com",
//                            "avixaichka@yahoo.ie",
//                            "amitibr19@gmail.com",
//                            "chenlanda@gmail.com",
//                            "tallanda96@gmail.com",
//                            "eden.nahari1@gmail.com",
//                            "hellolo18@gmail.com",
//                            "shelly324@walla.com",
//                            "14mor@walla.com",
//                            "starata2010@yahoo.fr",
//                            "sap_151@walla.com",
//                            "yettaellenbogen@yahoo.com",
//                            "hilamlll@yahoo.com",
//                            "rsm14857@gmail.com",
//                            "shahaf_h55@walla.com",
//                            "hodi_chan@walla.com",
//                            "isacsimantov@gmail.com",
//                            "lgboim@gmail.com",
//                            "johnsmithanybody@gmail.com",
//                            "rika185@gmail.com",
//                            "jouel43@hotmail.com",
//                            "kfirbep@gmail.com",
//                            "skittleskit13@gmail.com",
//                            "dastien007@gmail.com",
//                            "rogatka@riseup.net",
//                            "amsalem.nathaniel@gmail.com",
//                            "arram.attoun@gmail.com",
//                            "kerenluna@gmail.com",
//                            "dotanbd@outlook.com",
//                            "neronov2000@gmail.com",
//                            "alliexoxo2014@gmail.com",
//                            "yankaleb@gmail.com",
//                            "bahaa.zahika@gmail.com",
//                            "itayste@gmail.com",
//                            "roni.sweetkona@gmail.com",
//                            "shahar547@walla.com",
//                            "dan210993@hotmail.com",
//                            "boss_for_ever18@hotmail.com",
//                            "didosha94@gmail.com",
//                            "yulym1@walla.com",
//                            "bilosh15@gmail.com",
//                            "tanyski@gmail.com",
//                            "cassandra03@walla.com",
//                            "ikuto3@walla.com",
//                            "y1962.eng@gmail.com",
//                            "netwarm@gmail.com",
//                            "rachely2020@gmail.com",
//                            "rina.rainbow@gmail.com",
//                            "fatima.al.sahin@gmx.de",
//                            "vitaly.kroivets@gmail.com",
//                            "jenun2009@hotmail.com",
//                            "rosedrops@live.com",
//                            "sammergenim@yahoo.com",
//                            "kellynewluv@yahoo.com",
//                            "shmuel9613@gmail.com",
//                            "antigonish36@hotmail.com",
//                            "dodfogo@walla.com",
//                            "is.92@hotmail.com",
//                            "ukl.der@mail.ru",
//                            "loladadon@gmail.com",
//                            "julio_nicodeme@hotmail.com",
//                            "sunflowerpolina@gmail.com",
//                            "shirlley1993@gmail.com",
//                            "katana7@walla.co.il",
//                            "adi5696@gmail.com",
//                            "gaydoong@aol.com",
//                            "red1hawk@gmail.com",
//                            "yanivmelamed.others@gmail.com",
//                            "danigotts@yahoo.com",
//                            "danielpj100@gmail.com",
//                            "tuvtuv56@gmail.com",
//                            "j1@hotmail.co.il",
//                            "tamar_yo@walla.com",
//                            "ajr.tupe007@gmail.com",
//                            "aviyabaran@gmail.com",
//                            "ben.cornelius@hotmail.com",
//                            "drorkau@gmail.com",
//                            "saray.cohen1@mail.huji.ac.il",
//                            "tokaor@yandex.ru",
//                            "wajd33@hotmail.com",
//                            "n_zed92@hotmail.com",
//                            "yoelch732@gmail.com",
//                            "elenakalmikov@gmail.com",
//                            "damianomeehan@hotmail.com",
//                            "lily_1224@walla.com",
//                            "adun26@gmail.com",
//                            "rapdaddyd2@breakthru.com",
//                            "cerberus.kai.leng@gmail.com",
//                            "mentodasheep@gmail.com",
//                            "bukimi.shojo@gmail.com",
//                            "kacyy@walla.co.il",
//                            "samorg1@gmail.com",
//                            "hadasbmi@walla.com",
//                            "palham.sahib@gmail.com",
//                            "chavatzellet@yahoo.com",
//                            "lior35@gmail.com",
//                            "amit_hananel5@walla.com",
//                            "priestprince@hotmail.com",
//                            "chen017@walla.co.il",
//                            "mikizaltzman@yahoo.com",
//                            "trotsky@walla.co.il",
//                            "elisha05@walla.com",
//                            "alaa.aldin.88@hotmail.com",
//                            "marynov@yandex.ru",
//                            "kingmalik94@aol.com",
//                            "panpalfreind@yahoo.com",
//                            "lyona_chumakova@mail.ru",
//                            "come-1900@hotmail.com",
//                            "samy4you_pal@yahoo.com",
//                            "peterjacobus5691@gmail.com",
//                            "shaked.orenshtein@gmail.com",
//                            "shaulreznik@gmail.com",
//                            "esilver@walla.co.il",
//                            "dovenet008@gmail.com",
//                            "asaflevi91@gmail.com",
//                            "rccc85@gmail.com",
//                            "moh_q@yahoo.com",
//                            "eladgreenberg111@gmail.com",
//                            "simchagittel@gmail.com",
//                            "vaughanbrown3@gmail.com",
//                            "karin1813@walla.com",
//                            "romankh@inbox.ru",
//                            "edelman8@walla.com",
//                            "soric_isabella@yahoo.de",
//                            "nuress6655@hotmail.com",
//                            "sval2k@gmail.com",
//                            "freeguy70@yahoo.com",
//                            "avital4848@gmail.com",
//                            "miryampeleg@gmail.com",
//                            "itaimagician@gmail.com",
//                            "shoshiit@gmail.com",
//                            "singh.015.sunny@gmail.com",
//                            "george.habeeb@yahoo.com",
//                            "k.green.light.k@gmail.com",
//                            "edenbalon@hotmail.com",
//                            "matinikolai@gmail.com",
//                            "reina073@gmail.com",
//                            "amsams@gmial.com",
//                            "darkangelx1bird@gmail.com",
//                            "misterygirl@netvision.net.il",
//                            "dr.7amas@hotmail.com",
//                            "ulito4ka@gmail.com",
//                            "adir101@gmail.com",
//                            "daisy20052005@mail.ru",
//                            "sharlott-olga@yandex.ru",
//                            "gabira2003@gmail.com",
//                            "akiva.breuer92@gmail.com",
//                            "tanii91@gmail.com",
//                            "osher4566@gmail.com",
//                            "hannachan88@gmail.com",
//                            "ile_b@windowslive.com",
//                            "yuvali1016@walla.com",
//                            "hadasbmi6@walla.co.il",
//                            "esilver@nana.co.il",
//                            "tamar527@gmail.com",
//                            "eilaty25@gmail.com",
//                            "nadibs@gmail.com",
//                            "payizibahar@yahoo.com",
//                            "oribt23@gmail.com",
//                            "anolick@netvision.net.il",
//                            "mayabaya@walla.co.il",
//                            "nzonibe@hotmail.fr",
//                            "dgpsycho@gmail.com",
//                            "yoni_maist@yahoo.com",
//                            "manu_ben2@hotmail.com",
//                            "atheer.ismael@gmail.com",
//                            "lev.tigrich@gmail.com",
//                            "tal10160@walla.com",
//                            "cute_gurl_91@hotmail.com",
//                            "angi93s@gmail.com",
//                            "shaharr23@walla.co.il",
//                            "x3iloveyouu@gmail.com",
//                            "mrdeathknight@gmail.com",
//                            "morabut@gmail.com",
//                            "avichai.gontjarov@gmail.com",
//                            "oomadisonoo@gmail.com",
//                            "lrki169@gmail.com",
//                            "omerde12@gmail.com",
//                            "eladg22@gmail.com",
//                            "esilver17@gmail.com",
//                            "anita_gozzi09@hotmail.com",
//                            "lieltal1234@walla.com",
//                            "sirafa@t2.technion.ac.il",
//                            "tasneem_k@homail.com",
//                            "lisamarie_hdn@hotmail.com",
//                            "trb_6rb@hotmail.com",
//                            "adayark8@hotmail.com",
//                            "rajaee_99@hotmail.com",
//                            "sytrus@nana.co.il",
//                            "salimullah.zadran@gmail.com",
//                            "vitorio7654@walla.co.il",
//                            "koji.gabriel218@gmail.com",
//                            "autonomic@bk.ru",
//                            "amax150@walla.com",
//                            "nir.pincas@gmail.com",
//                            "alina.imas@mail.huji.ac.il",
//                            "tariq_in_black@hotmail.com",
//                            "yehudit.dahari@gmail.com",
//                            "ravivsta@gmail.com",
//                            "sexaphoria@yahoo.com",
//                            "sabata00@gmail.com",
//                            "agdabah46@gmail.com",
//                            "shmichaver@walla.co.il",
//                            "batel_ch2@walla.co.il",
//                            "danielasraf1995@walla.com",
//                            "lotem_n7@walla.com",
//                            "joao7leibo@walla.co.il",
//                            "bogdanovaliubov1309@gmail.com",
//                            "mshanhav@gmail.com",
//                            "avishagharir@gmail.com",
//                            "benescribe@gmail.com",
//                            "ntf3rd@hotmail.com",
//                            "dandanbuc@operamail.com",
//                            "individual_combinations@usa.com",
//                            "haker_alt@hotmail.com",
//                            "kb57il@yahoo.com",
//                            "buzymcbuzy@gmail.com",
//                            "facebookfp46@hotmail.com",
//                            "efratraube@gmail.com",
//                            "tovag13@walla.com",
//                            "daniell.122@hotmail.com",
//                            "str_568@yahoo.com",
//                            "straw_knight@yahoo.com",
//                            "daniellet17@yahoo.com",
//                            "shohat_adi@walla.co.il",
//                            "franque.langat@yahoo.com",
//                            "i_am_rania@hotmail.com",
//                            "lyalyakot3@gmail.com",
//                            "yunoavailablename@gmail.com"
//                        )
//                        emailList.clear()
//                        emailList.addAll(imported)
                    }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Import Excel")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            if (emailError) {
                Text(
                    text = "ایمیل معتبر نیست",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = subject,
                onValueChange = {
                    subject = it
                },
                label = { Text("موضوع") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = content,
                onValueChange = {
                    content = it
                    if (contentError) contentError = false
                },
                label = { Text("محتوا") },
                isError = contentError,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                keyboardOptions = KeyboardOptions.Default,
                singleLine = false
            )
            if (contentError) {
                Text(
                    text = "محتوا نباید خالی باشد",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            if (sentCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "تعداد ایمیل‌های ارسال‌شده: $sentCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .combinedClickable(
                    onClick = {
                        val firstHalf = emailList.subList(0, emailList.size)
                        val emailLogs: ArrayList<Email> = ArrayList()
                        scope.launch {
                            sentCount = 0
                            for (email in firstHalf) {
                                val trimmedEmail = email.trim()
                                emailLogs.add(
                                    Email(fromEmail, trimmedEmail, content.trim())
                                )

                                onSend(trimmedEmail, subject.trim(), content.trim())
                                sentCount++
                                Log.d("EmailSending", "Sent: $sentCount/${firstHalf.size}")

                                // ✅ دیلی ایمن برای جلوگیری از بلاک توسط Gmail
                                delay(5_000) // ۱۵ ثانیه بین هر ایمیل
                            }

                            Toast.makeText(context, "ارسال $sentCount ایمیل با موفقیت انجام شد", Toast.LENGTH_LONG).show()
                            createExcelFile(context, emailLogs)
                        }

                    },
                    onLongClick = {
                        if (emailList.isNotEmpty()) {
                            val half = emailList.size / 2
                            val secondHalf = emailList.subList(half, emailList.size)
                            scope.launch {
                                sentCount = 0
                                for (email in secondHalf) {
                                    onSend(email.trim(), subject.trim(), content.trim())
                                    sentCount++
                                    delay(5000)
                                }
                                Toast.makeText(context, "نیمی از ایمیل‌ها ($sentCount) ارسال شد", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
                .let { if (!isLoading) it else Modifier },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("ارسال")
            }
        }
    }
}

fun readEmailsFromExcel(context: Context, uri: Uri): List<String> {
    val emails = mutableListOf<String>()
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        val workbook = WorkbookFactory.create(inputStream)
        val sheet = workbook.getSheetAt(0)
        for (row in sheet) {
            val cell = row.getCell(0)
            val email = cell?.toString()?.trim()
            if (!email.isNullOrBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emails.add(email)
            }
        }
        workbook.close()
    }
    return emails
}
