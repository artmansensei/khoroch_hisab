package com.khorochtracker

import android.content.Intent
import android.os.Bundle
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KhorochRoot() }
    }
}

@Composable
fun KhorochRoot(vm: ExpenseViewModel = viewModel()) {
    val context = LocalContext.current
    val rows by vm.transactions.collectAsState(initial = emptyList())
    var dark by remember { mutableStateOf(context.getSharedPreferences("prefs", 0).getBoolean("dark", false)) }
    var english by remember { mutableStateOf(context.getSharedPreferences("prefs", 0).getBoolean("english", false)) }
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        KhorochApp(rows, vm, dark, english, onDark = {
            dark = it
            context.getSharedPreferences("prefs", 0).edit().putBoolean("dark", it).apply()
        }, onLanguage = {
            english = it
            context.getSharedPreferences("prefs", 0).edit().putBoolean("english", it).apply()
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhorochApp(rows: List<TransactionEntity>, vm: ExpenseViewModel, dark: Boolean, english: Boolean, onDark: (Boolean) -> Unit, onLanguage: (Boolean) -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var entryType by remember { mutableStateOf("EXPENSE") }
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }
    var showEntry by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<TransactionEntity?>(null) }
    var filter by remember { mutableStateOf("ALL") }
    val added = rows.filter { it.type == "ADD" }.sumOf { it.amount }
    val spent = rows.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    Scaffold(
        topBar = { TopAppBar(title = { Text(if (english) "Expense Tracker" else "খরচের হিসাব") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text(if (english) "Home" else "হোম") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.History, null) }, label = { Text(if (english) "History" else "হিসাব") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.BarChart, null) }, label = { Text(if (english) "Reports" else "রিপোর্ট") })
                NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text(if (english) "Settings" else "সেটিংস") })
            }
        },
        floatingActionButton = {
            if (tab == 0 || tab == 1) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallFloatingActionButton(onClick = { editing = null; entryType = "ADD"; showEntry = true }) { Icon(Icons.Default.Add, "টাকা জমা") }
                    FloatingActionButton(onClick = { editing = null; entryType = "EXPENSE"; showEntry = true }) { Icon(Icons.Default.Remove, "খরচ") }
                }
            }
        }
    ) { padding ->
        when (tab) {
            0 -> Dashboard(rows, added, spent, english, Modifier.padding(padding))
            1 -> History(rows, filter, { filter = it }, onEdit = { editing = it; entryType = it.type; showEntry = true }, onDelete = { deleting = it }, english = english, modifier = Modifier.padding(padding))
            2 -> Reports(rows, english, Modifier.padding(padding))
            else -> SettingsScreen(dark, onDark, english, onLanguage, Modifier.padding(padding), vm)
        }
    }

    if (showEntry) {
        EntryDialog(entryType, editing, onDismiss = { showEntry = false }) { amount, category, note, date ->
            val old = editing
            if (old == null) vm.add(entryType, amount, category, note, date)
            else vm.update(old.copy(amount = amount, category = category, note = note, date = date))
            showEntry = false
        }
    }
    deleting?.let { item ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("হিসাব মুছবেন?") },
            text = { Text("এই লেনদেনটি স্থায়ীভাবে মুছে যাবে।") },
            confirmButton = { TextButton(onClick = { vm.delete(item); deleting = null }) { Text("মুছুন") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("বাতিল") } })
    }
}

@Composable
fun Dashboard(rows: List<TransactionEntity>, added: Double, spent: Double, english: Boolean, modifier: Modifier = Modifier) {
    val today = today()
    val todaySpent = rows.filter { it.type == "EXPENSE" && it.date == today }.sumOf { it.amount }
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("আপনার হিসাব এক জায়গায়", style = MaterialTheme.typography.bodyLarge) }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text(if (english) "Available Balance" else "বর্তমান ব্যালেন্স")
                    Text("৳${money(added - spent)}", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Summary(if (english) "Total Added" else "মোট জমা", added)
                        Summary(if (english) "Total Expense" else "মোট খরচ", spent)
                    }
                }
            }
        }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("আজকের খরচ"); Text("৳${money(todaySpent)}", style = MaterialTheme.typography.headlineMedium) } } }
        item { Text(if (english) "Recent Transactions" else "সাম্প্রতিক লেনদেন", style = MaterialTheme.typography.titleLarge) }
        items(rows.take(5), key = { it.id }) { TransactionRow(it) }
        if (rows.isEmpty()) item { Text("এখনো কোনো হিসাব নেই। + দিয়ে টাকা জমা বা − দিয়ে খরচ যোগ করুন।") }
    }
}

@Composable
fun Summary(label: String, value: Double) {
    Column { Text(label); Text("৳${money(value)}", style = MaterialTheme.typography.titleLarge) }
}

@Composable
fun History(rows: List<TransactionEntity>, filter: String, onFilter: (String) -> Unit,
            onEdit: (TransactionEntity) -> Unit, onDelete: (TransactionEntity) -> Unit, english: Boolean, modifier: Modifier = Modifier) {
    val filtered = rows.filter { filter == "ALL" || it.type == filter }
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(if (english) "Transaction History" else "লেনদেনের ইতিহাস", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == "ALL", onClick = { onFilter("ALL") }, label = { Text(if (english) "All" else "সব") })
                FilterChip(selected = filter == "ADD", onClick = { onFilter("ADD") }, label = { Text(if (english) "Added" else "জমা") })
                FilterChip(selected = filter == "EXPENSE", onClick = { onFilter("EXPENSE") }, label = { Text(if (english) "Expense" else "খরচ") })
            }
        }
        items(filtered, key = { it.id }) { item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    TransactionRow(item)
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onEdit(item) }) { Text("Edit") }
                        TextButton(onClick = { onDelete(item) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
                    }
                }
            }
        }
        if (filtered.isEmpty()) item { Text("কোনো হিসাব পাওয়া যায়নি।") }
    }
}

@Composable
fun TransactionRow(item: TransactionEntity) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(item.note.ifBlank { if (item.type == "ADD") "টাকা জমা" else item.category })
            Text("${item.category} • ${item.date}", style = MaterialTheme.typography.bodySmall)
        }
        Text((if (item.type == "ADD") "+" else "−") + "৳" + money(item.amount),
            color = if (item.type == "ADD") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
    }
}

@Composable
fun Reports(rows: List<TransactionEntity>, english: Boolean, modifier: Modifier = Modifier) {
    val month = today().take(7)
    val monthly = rows.filter { it.type == "EXPENSE" && it.date.startsWith(month) }
    val total = monthly.sumOf { it.amount }
    val groups = monthly.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }.toList().sortedByDescending { it.second }
    val context = LocalContext.current
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            try { context.contentResolver.openOutputStream(uri)?.use { createExpensePdf(it, month, monthly) } }
            catch (_: Exception) { }
        }
    }
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(if (english) "Monthly Report" else "মাসিক রিপোর্ট", style = MaterialTheme.typography.titleLarge)
            Text("মাস: $month")
            Button(onClick = { pdfLauncher.launch("khoroch-report-$month.pdf") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PictureAsPdf, null); Spacer(Modifier.width(8.dp))
                Text(if (english) "Export PDF Report" else "PDF রিপোর্ট Export")
            }
        }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text(if (english) "Total Expense This Month" else "এই মাসের মোট খরচ"); Text("৳${money(total)}", style = MaterialTheme.typography.headlineLarge) } } }
        item { Text(if (english) "Expense by Category" else "ক্যাটাগরি অনুযায়ী খরচ", style = MaterialTheme.typography.titleMedium) }
        item { if (groups.isNotEmpty()) CategoryBarChart(groups) }
        items(groups) { (category, amount) ->
            Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(category); Text("৳${money(amount)}") } }
        }
        if (groups.isEmpty()) item { Text("এই মাসে কোনো খরচ নেই।") }
    }
}

@Composable
fun EntryDialog(type: String, existing: TransactionEntity?, onDismiss: () -> Unit,
                onSave: (Double, String, String, String) -> Unit) {
    var amount by remember(existing) { mutableStateOf(existing?.amount?.toString() ?: "") }
    var category by remember(existing) { mutableStateOf(existing?.category ?: if (type == "ADD") "জমা" else "অন্যান্য") }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }
    var date by remember(existing) { mutableStateOf(existing?.date ?: today()) }
    val categories = if (type == "ADD") listOf("জমা", "অন্যান্য") else listOf("খাবার", "যাতায়াত", "কেনাকাটা", "বিল", "শিক্ষা", "স্বাস্থ্য", "বিনোদন", "অন্যান্য")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "হিসাব সম্পাদনা" else if (type == "ADD") "টাকা জমা করুন" else "খরচ যোগ করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(amount, { amount = it }, label = { Text("টাকার পরিমাণ") }, singleLine = true)
                Text("ক্যাটাগরি")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.take(4).forEach { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) }) }
                }
                if (categories.size > 4) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.drop(4).forEach { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) }) }
                }
                OutlinedTextField(note, { note = it }, label = { Text("বিবরণ (ঐচ্ছিক)") })
                OutlinedTextField(date, { date = it }, label = { Text("তারিখ (YYYY-MM-DD)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = amount.toDoubleOrNull()
                if (v != null && v > 0 && validDate(date)) onSave(v, category, note, date)
            }) { Text("সংরক্ষণ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

@Composable
fun SettingsScreen(dark: Boolean, onDark: (Boolean) -> Unit, english: Boolean, onLanguage: (Boolean) -> Unit, modifier: Modifier, vm: ExpenseViewModel) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                val data = context.getSharedPreferences("backup_temp", 0).getString("json", "[]") ?: "[]"
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(data) }
                message = "Backup সংরক্ষণ হয়েছে।"
            } catch (e: Exception) { message = "Backup করা যায়নি: ${e.message}" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: "[]"
                val type = object : TypeToken<List<TransactionEntity>>() {}.type
                val list: List<TransactionEntity> = Gson().fromJson(json, type)
                vm.replaceAll(list)
                message = "Backup Restore হয়েছে।"
            } catch (e: Exception) { message = "ফাইলটি সঠিক Backup নয়।" }
        }
    }
    // Rows are supplied to the export callback via the current database snapshot helper.
    val rows by vm.transactions.collectAsState(initial = emptyList())
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(if (english) "Settings" else "সেটিংস", style = MaterialTheme.typography.titleLarge) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Dark Mode"); Switch(checked = dark, onCheckedChange = onDark) } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (english) "English Language" else "ইংরেজি ভাষা")
            Switch(checked = english, onCheckedChange = onLanguage)
        } }
        item {
            Button(onClick = {
                val json = Gson().toJson(rows)
                context.getSharedPreferences("backup_temp", 0).edit().putString("json", json).apply()
                exportLauncher.launch("khoroch-backup-${today()}.json")
            }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Upload, null); Spacer(Modifier.width(8.dp)); Text("Backup Export") }
        }
        item {
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("Backup Restore")
            }
        }
        item { Text("সতর্কতা: Restore করলে বর্তমান হিসাবের জায়গায় Backup-এর হিসাব বসবে।") }
        if (message.isNotBlank()) item { Text(message, color = MaterialTheme.colorScheme.primary) }
        item { Text("খরচের হিসাব • Version 2.0") }
    }
}

private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
private fun validDate(s: String) = s.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
private fun money(v: Double) = "%,.2f".format(Locale.US, v)


@Composable
fun CategoryBarChart(groups: List<Pair<String, Double>>) {
    val maxValue = (groups.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ক্যাটাগরি চার্ট", style = MaterialTheme.typography.titleMedium)
        groups.take(6).forEach { (name, value) ->
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, style = MaterialTheme.typography.bodySmall)
                    Text("৳${money(value)}", style = MaterialTheme.typography.bodySmall)
                }
                Canvas(Modifier.fillMaxWidth().height(12.dp)) {
                    drawRoundRect(Color(0xFFE2E5EA), size = androidx.compose.ui.geometry.Size(size.width, size.height))
                    drawRoundRect(Color(0xFF258B68), size = androidx.compose.ui.geometry.Size(size.width * (value / maxValue).toFloat(), size.height))
                }
            }
        }
    }
}

private fun createExpensePdf(output: OutputStream, month: String, expenses: List<TransactionEntity>) {
    val document = PdfDocument()
    val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val paint = Paint().apply { color = android.graphics.Color.BLACK; textSize = 15f; isAntiAlias = true }
    var y = 55f
    page.canvas.drawText("Khoroch Tracker - Expense Report", 40f, y, paint)
    y += 30f
    paint.textSize = 12f
    page.canvas.drawText("Month: $month", 40f, y, paint)
    y += 25f
    page.canvas.drawText("Total Expense: BDT ${money(expenses.sumOf { it.amount })}", 40f, y, paint)
    y += 30f
    page.canvas.drawText("Date       Category       Amount (BDT)       Note", 40f, y, paint)
    y += 22f
    expenses.sortedBy { it.date }.take(30).forEach {
        page.canvas.drawText("${it.date}   ${it.category.take(12)}   ${money(it.amount)}   ${it.note.take(12)}", 40f, y, paint)
        y += 20f
    }
    document.finishPage(page)
    document.writeTo(output)
    document.close()
}
