package com.goimonan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private const val STUDENT = "K24DTCN008 - Đào Xuân Dũng"

private class Range(val label: String, val from: Long?, val to: Long?)

private val priceRanges = listOf(
    Range("Tất cả giá", null, null),
    Range("Dưới 100k", null, 99_999),
    Range("100k–200k", 100_000, 200_000),
    Range("Trên 200k", 200_001, null)
)

private val timeRanges = listOf(
    Range("Mọi thời gian", null, null),
    Range("Dưới 10 phút", null, 9),
    Range("10–15 phút", 10, 15),
    Range("Trên 15 phút", 16, null)
)

@Composable
fun ManHinhQuanLyThucDon(database: MenuDatabase) {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var priceFilter by remember { mutableIntStateOf(0) }
    var timeFilter by remember { mutableIntStateOf(0) }

    var editingDish by remember { mutableStateOf<Dish?>(null) }
    var showDishForm by remember { mutableStateOf(false) }
    var deletingDish by remember { mutableStateOf<Dish?>(null) }
    var editingCategory by remember { mutableStateOf<FoodCategory?>(null) }
    var showCategoryForm by remember { mutableStateOf(false) }
    var deletingCategory by remember { mutableStateOf<FoodCategory?>(null) }

    val categories = remember(refresh) { database.LayLoaiMonAn() }
    val dishes = remember(selectedCategory, search, priceFilter, timeFilter, refresh) {
        val gia = priceRanges[priceFilter]
        val phut = timeRanges[timeFilter]
        database.TimMonAn(search.trim(), selectedCategory, gia.from, gia.to, phut.from?.toInt(), phut.to?.toInt())
    }
    val dishListState = rememberLazyListState()
    LaunchedEffect(selectedCategory, search, priceFilter, timeFilter) {
        dishListState.scrollToItem(0)
    }
    val categoryNames = categories.associate { it.code to it.name }
    val isFiltering = search.isNotBlank() || selectedCategory != null || priceFilter != 0 || timeFilter != 0

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (tab == 0 && categories.isNotEmpty()) {
                        editingDish = null
                        showDishForm = true
                    } else {
                        tab = 1
                        editingCategory = null
                        showCategoryForm = true
                    }
                },
                containerColor = Color(0xFFE65100),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                icon = { Text("＋", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                text = { Text(if (tab == 0 && categories.isNotEmpty()) "Thêm món ăn" else "Thêm loại món", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Banner
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFE65100), Color(0xFFFF9800))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "🍜 QUẢN LÝ THỰC ĐƠN",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.4f))
                    ) {
                        Text(
                            "Sinh viên: $STUDENT",
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Tab Row hiện đại
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFEEEEEE)
            ) {
                Row(Modifier.padding(4.dp)) {
                    val tabModifier = Modifier.weight(1f)
                    Surface(
                        modifier = tabModifier.clickable { tab = 0 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (tab == 0) Color.White else Color.Transparent,
                        shadowElevation = if (tab == 0) 2.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 9.dp)) {
                            Text(
                                "🍲 Món ăn (${dishes.size})",
                                fontWeight = if (tab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (tab == 0) Color(0xFFE65100) else Color(0xFF616161),
                                fontSize = 14.sp
                            )
                        }
                    }
                    Surface(
                        modifier = tabModifier.clickable { tab = 1 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (tab == 1) Color.White else Color.Transparent,
                        shadowElevation = if (tab == 1) 2.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 9.dp)) {
                            Text(
                                "📂 Loại món (${categories.size})",
                                fontWeight = if (tab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (tab == 1) Color(0xFFE65100) else Color(0xFF616161),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (tab == 0) {
                // Thanh tìm kiếm gọn gàng
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFE65100),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    ),
                    singleLine = true,
                    placeholder = { Text("🔍 Tìm tên hoặc mã món...", fontSize = 13.sp, color = Color(0xFF9E9E9E)) },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { search = "" }) {
                                Text("✕", fontWeight = FontWeight.Bold, color = Color(0xFF757575))
                            }
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))

                // Dòng loại món (cuộn ngang)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillLoaiMon(
                        text = "Tất cả",
                        isSelected = selectedCategory == null,
                        onClick = { selectedCategory = null }
                    )
                    categories.forEach { cat ->
                        PillLoaiMon(
                            text = cat.name,
                            isSelected = selectedCategory == cat.code,
                            onClick = { selectedCategory = cat.code }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // BỘ LỌC NÂNG CAO GỌN GÀNG (1 dòng duy nhất với Dropdown)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Menu chọn giá
                    DropdownFilterPill(
                        prefix = "💰 Giá",
                        label = priceRanges[priceFilter].label,
                        isActive = priceFilter != 0,
                        options = priceRanges.mapIndexed { index, range -> index to range.label },
                        onSelect = { priceFilter = it }
                    )

                    // Menu chọn thời gian
                    DropdownFilterPill(
                        prefix = "⏱️ Thời gian",
                        label = timeRanges[timeFilter].label,
                        isActive = timeFilter != 0,
                        options = timeRanges.mapIndexed { index, range -> index to range.label },
                        onSelect = { timeFilter = it }
                    )

                    // Nút Xóa tất cả bộ lọc khi đang lọc
                    if (isFiltering) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFEBEE),
                            border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                            modifier = Modifier.clickable {
                                search = ""
                                selectedCategory = null
                                priceFilter = 0
                                timeFilter = 0
                            }
                        ) {
                            Text(
                                "✕ Bỏ lọc",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD32F2F),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Danh sách món ăn
                if (dishes.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🍽️", fontSize = 42.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Không tìm thấy món ăn phù hợp",
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF757575)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = dishListState,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(dishes, key = { it.code }) { dish ->
                            TheMonAn(
                                dish = dish,
                                categoryName = categoryNames[dish.categoryCode] ?: dish.categoryCode,
                                onEdit = { editingDish = dish; showDishForm = true },
                                onDelete = { deletingDish = dish }
                            )
                        }
                    }
                }
            } else {
                // Tab Loại món
                Spacer(Modifier.height(4.dp))
                if (categories.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Chưa có loại món nào", color = Color(0xFF757575))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(categories, key = { it.code }) { category ->
                            TheLoaiMonAn(
                                category = category,
                                onEdit = { editingCategory = category; showCategoryForm = true },
                                onDelete = { deletingCategory = category },
                                onShowDishes = { selectedCategory = category.code; tab = 0 }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showDishForm) {
        BieuMauMonAn(
            initial = editingDish,
            categories = categories,
            onDismiss = { showDishForm = false },
            onSave = { dish ->
                try {
                    if (editingDish == null) {
                        if (database.MaMonAnDaTonTai(dish.code)) throw IllegalArgumentException("Mã món đã tồn tại!")
                        database.ThemMonAn(dish)
                    } else {
                        database.SuaMonAn(dish)
                    }
                    refresh++
                    showDishForm = false
                    Toast.makeText(context, "Đã lưu món ${dish.name}", Toast.LENGTH_SHORT).show()
                } catch (error: Exception) {
                    Toast.makeText(context, error.message ?: "Không lưu được món", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (showCategoryForm) {
        BieuMauLoaiMonAn(
            initial = editingCategory,
            onDismiss = { showCategoryForm = false },
            onSave = { category ->
                try {
                    if (editingCategory == null) {
                        if (database.MaLoaiMonDaTonTai(category.code)) throw IllegalArgumentException("Mã loại đã tồn tại!")
                        database.ThemLoaiMonAn(category)
                    } else {
                        database.SuaLoaiMonAn(category)
                    }
                    refresh++
                    showCategoryForm = false
                    Toast.makeText(context, "Đã lưu loại ${category.name}", Toast.LENGTH_SHORT).show()
                } catch (error: Exception) {
                    Toast.makeText(context, error.message ?: "Không lưu được loại", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    deletingDish?.let { dish ->
        AlertDialog(
            onDismissRequest = { deletingDish = null },
            shape = RoundedCornerShape(16.dp),
            title = { Text("Xác nhận xóa món") },
            text = { Text("Bạn có chắc muốn xóa món '${dish.name}' (${dish.code}) khỏi thực đơn?") },
            confirmButton = {
                Button(
                    onClick = {
                        database.XoaMonAn(dish.code)
                        deletingDish = null
                        refresh++
                        Toast.makeText(context, "Đã xóa món ăn", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("Xóa", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { deletingDish = null }) { Text("Hủy") }
            }
        )
    }

    deletingCategory?.let { category ->
        val affectedDishes = database.LayMonAn(category.code).size
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            shape = RoundedCornerShape(16.dp),
            title = { Text("Xác nhận xóa loại món") },
            text = { Text("Xóa loại '${category.name}' (${category.code}) sẽ xóa kèm $affectedDishes món ăn thuộc loại này. Bạn có chắc chắn?") },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            database.XoaLoaiMonAn(category.code)
                            if (selectedCategory == category.code) selectedCategory = null
                            refresh++
                            Toast.makeText(context, "Đã xóa loại món", Toast.LENGTH_SHORT).show()
                        } catch (error: Exception) {
                            Toast.makeText(context, error.message ?: "Không xóa được", Toast.LENGTH_LONG).show()
                        }
                        deletingCategory = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text("Xóa", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) { Text("Hủy") }
            }
        )
    }
}

// =================== CÁC THẺ GIAO DIỆN HIỆN ĐẠI ===================

@Composable
private fun TheMonAn(dish: Dish, categoryName: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnhMon(
                dish.image,
                Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    dish.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )
                Spacer(Modifier.height(3.dp))
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "${dish.code} • $categoryName",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "⏱️ ${dish.serviceMinutes} phút",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${"%,d".format(dish.price)} đ",
                    color = Color(0xFFE65100),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEBF5FF),
                    border = BorderStroke(0.5.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.clickable { onEdit() }
                ) {
                    Text(
                        "✏️ Sửa",
                        color = Color(0xFF1D4ED8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(0.5.dp, Color(0xFFFECACA)),
                    modifier = Modifier.clickable { onDelete() }
                ) {
                    Text(
                        "🗑️ Xóa",
                        color = Color(0xFFDC2626),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TheLoaiMonAn(category: FoodCategory, onEdit: () -> Unit, onDelete: () -> Unit, onShowDishes: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            category.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFFFE0B2),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                category.code,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEBF5FF),
                        border = BorderStroke(0.5.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.clickable { onEdit() }
                    ) {
                        Text(
                            "✏️ Sửa",
                            color = Color(0xFF1D4ED8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(0.5.dp, Color(0xFFFECACA)),
                        modifier = Modifier.clickable { onDelete() }
                    ) {
                        Text(
                            "🗑️ Xóa",
                            color = Color(0xFFDC2626),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                category.description,
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )

            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFF7ED),
                border = BorderStroke(1.dp, Color(0xFFFFEDD5)),
                modifier = Modifier.clickable { onShowDishes() }
            ) {
                Text(
                    "Xem món thuộc loại này →",
                    color = Color(0xFFEA580C),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

// =================== CÁC THÀNH PHẦN BỘ LỌC ===================

@Composable
private fun PillLoaiMon(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFFE65100) else Color.White,
        border = BorderStroke(1.dp, if (isSelected) Color(0xFFE65100) else Color(0xFFE0E0E0)),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF424242),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun DropdownFilterPill(
    prefix: String,
    label: String,
    isActive: Boolean,
    options: List<Pair<Int, String>>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isActive) Color(0xFFFFE0B2) else Color.White,
            border = BorderStroke(1.dp, if (isActive) Color(0xFFE65100) else Color(0xFFE0E0E0)),
            modifier = Modifier.clickable { expanded = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text(
                    text = if (isActive) "$prefix: $label" else "$prefix ▾",
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) Color(0xFFE65100) else Color(0xFF424242)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = Color.White
        ) {
            options.forEach { (index, text) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text,
                            fontSize = 13.sp,
                            fontWeight = if (text == label) FontWeight.Bold else FontWeight.Normal,
                            color = if (text == label) Color(0xFFE65100) else Color.Black
                        )
                    },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

// =================== BIỂU MẪU NHẬP LIỆU ===================

@Composable
private fun BieuMauLoaiMonAn(initial: FoodCategory?, onDismiss: () -> Unit, onSave: (FoodCategory) -> Unit) {
    var code by remember(initial) { mutableStateOf(initial?.code ?: "") }
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
    var error by remember(initial) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                if (initial == null) "📁 Thêm loại món mới" else "📁 Sửa loại món",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OThongTin("Mã loại", code, { code = it }, readOnly = initial != null)
                OThongTin("Tên loại", name, { name = it })
                OThongTin("Mô tả", description, { description = it })
                if (error.isNotEmpty()) {
                    Text(error, color = Color(0xFFD32F2F), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isBlank() || name.isBlank()) error = "Vui lòng nhập mã và tên loại."
                    else onSave(FoodCategory(code.trim(), name.trim(), description.trim()))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Lưu", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF757575)) }
        }
    )
}

@Composable
private fun BieuMauMonAn(initial: Dish?, categories: List<FoodCategory>, onDismiss: () -> Unit, onSave: (Dish) -> Unit) {
    var code by remember(initial) { mutableStateOf(initial?.code ?: "") }
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var image by remember(initial) { mutableStateOf(initial?.image ?: "") }
    var price by remember(initial) { mutableStateOf(initial?.price?.toString() ?: "") }
    var serviceMinutes by remember(initial) { mutableStateOf(initial?.serviceMinutes?.toString() ?: "") }
    var categoryCode by remember(initial) { mutableStateOf(initial?.categoryCode ?: categories.firstOrNull()?.code.orEmpty()) }
    var error by remember(initial) { mutableStateOf("") }
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val saved = luuAnhTuMay(context, uri)
            if (saved != null) image = saved else Toast.makeText(context, "Không đọc được file ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                if (initial == null) "🍲 Thêm món ăn mới" else "🍲 Sửa món ăn",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OThongTin("Mã món", code, { code = it }, readOnly = initial != null)
                OThongTin("Tên món", name, { name = it })

                // Phần tải ảnh từ máy
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8F9FA),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (image.isNotEmpty()) {
                            AnhMon(
                                image,
                                Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📷", fontSize = 22.sp)
                                    Text("Chưa có ảnh", fontSize = 10.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { picker.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (image.isEmpty()) "📁 Tải ảnh từ máy" else "📁 Đổi ảnh khác",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Text(
                                if (image.isEmpty()) "Chọn ảnh món từ thư viện máy" else "Đã chọn ảnh từ máy",
                                fontSize = 11.sp,
                                color = if (image.isEmpty()) Color(0xFF64748B) else Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                OThongTin("Giá món (VNĐ)", price, { price = it }, keyboard = KeyboardType.Number)
                OThongTin("Thời gian phục vụ (phút)", serviceMinutes, { serviceMinutes = it }, keyboard = KeyboardType.Number)
                OChon(
                    label = "Loại món",
                    selected = categories.firstOrNull { it.code == categoryCode }?.name ?: "Chọn loại món",
                    choices = categories.map { it.code to it.name }
                ) { categoryCode = it }

                if (error.isNotEmpty()) {
                    Text(error, color = Color(0xFFD32F2F), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPrice = price.toLongOrNull()
                    val parsedMinutes = serviceMinutes.toIntOrNull()
                    if (code.isBlank() || name.isBlank() || categoryCode.isBlank() || image.isBlank() || parsedPrice == null || parsedPrice < 0 || parsedMinutes == null || parsedMinutes <= 0) {
                        error = if (image.isBlank()) "Vui lòng tải ảnh món ăn từ máy lên."
                                else "Vui lòng nhập đủ thông tin (giá >= 0, phút > 0)."
                    } else {
                        onSave(Dish(code.trim(), name.trim(), image, parsedPrice, parsedMinutes, categoryCode))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Lưu món", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF757575)) }
        }
    )
}

@Composable
private fun OThongTin(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        readOnly = readOnly,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFE65100),
            unfocusedBorderColor = Color(0xFFD6D6D6)
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard)
    )
}

@Composable
private fun OChon(
    label: String,
    selected: String,
    choices: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("$label: $selected", maxLines = 1, fontSize = 12.sp, color = Color(0xFF424242))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = Color.White
        ) {
            choices.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text, fontSize = 13.sp) },
                    onClick = { onSelected(value); expanded = false }
                )
            }
        }
    }
}

// =================== XỬ LÝ ẢNH ===================

private fun anhMinhHoaMonAn(image: String): Int = when (image) {
    "dish_pho" -> R.drawable.dish_pho_new
    "dish_bun_bo" -> R.drawable.dish_bun_bo_new
    "dish_com_tam" -> R.drawable.dish_com_tam_new
    "dish_com_ga" -> R.drawable.dish_com_ga_new
    "dish_goi_cuon" -> R.drawable.dish_goi_cuon_new
    "dish_tra_dao" -> R.drawable.dish_tra_dao_new
    "dish_bun_rieu" -> R.drawable.dish_bun_rieu_new
    "dish_com_chien" -> R.drawable.dish_com_chien_new
    "dish_bo_luc_lac" -> R.drawable.dish_bo_luc_lac_new
    "dish_nem_cuon" -> R.drawable.dish_nem_cuon_new
    "dish_sinh_to_xoai" -> R.drawable.dish_sinh_to_xoai_new
    else -> R.drawable.dish_pho_new
}

@Composable
private fun AnhMon(image: String, modifier: Modifier) {
    if (image.startsWith("file:")) {
        val bitmap = remember(image) { BitmapFactory.decodeFile(image.removePrefix("file:")) }
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), "Ảnh món ăn", modifier, contentScale = ContentScale.Crop)
            return
        }
    }
    if (image.isNotEmpty()) {
        Image(painterResource(anhMinhHoaMonAn(image)), "Ảnh món ăn", modifier, contentScale = ContentScale.Crop)
    } else {
        Box(
            modifier = modifier.background(Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            Text("📷", fontSize = 24.sp)
        }
    }
}

private fun luuAnhTuMay(context: Context, uri: Uri): String? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
    val bitmap = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    }
    if (bitmap == null) null else {
        val file = File(context.filesDir, "dish_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        "file:${file.absolutePath}"
    }
} catch (e: Exception) { null }
