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

// =========================================================================================
// PHẦN 1: CÁC HẰNG SỐ, DANH SÁCH MẪU VÀ DỮ LIỆU CẤU HÌNH BỘ LỌC
// =========================================================================================

/** Thông tin sinh viên thực hiện đồ án / bài tập */
private const val STUDENT = "K24DTCN008 - Đào Xuân Dũng"

/**
 * Danh mục ảnh mẫu có sẵn trong thư mục drawable của dự án.
 * Key: tên định danh lưu trong database (ví dụ "dish_pho")
 * Value: tên hiển thị tiếng Việt trên giao diện chọn ảnh mẫu
 */
private val imageChoices = linkedMapOf(
    "dish_pho" to "Phở bò",
    "dish_bun_bo" to "Bún bò Huế",
    "dish_com_tam" to "Cơm tấm",
    "dish_com_ga" to "Cơm gà",
    "dish_goi_cuon" to "Gỏi cuốn",
    "dish_tra_dao" to "Trà đào",
    "dish_bun_rieu" to "Bún riêu cua",
    "dish_com_chien" to "Cơm chiên hải sản",
    "dish_bo_luc_lac" to "Bò lúc lắc",
    "dish_nem_cuon" to "Nem cuốn",
    "dish_sinh_to_xoai" to "Sinh tố xoài"
)

/**
 * Class định nghĩa một khoảng giá trị dùng cho bộ lọc (Giá tiền hoặc Thời gian).
 * @param label Tên nhãn hiển thị trên nút chọn lọc (ví dụ: "Dưới 100k", "10–15 phút")
 * @param from Giá trị bắt đầu (null nếu không có giới hạn dưới)
 * @param to Giá trị kết thúc (null nếu không có giới hạn trên)
 */
private class Range(val label: String, val from: Long?, val to: Long?)

/** Danh sách các khoảng lọc theo Giá tiền (VNĐ) */
private val priceRanges = listOf(
    Range("Tất cả giá", null, null),
    Range("Dưới 100k", null, 99_999),
    Range("100k–200k", 100_000, 200_000),
    Range("Trên 200k", 200_001, null)
)

/** Danh sách các khoảng lọc theo Thời gian chế biến / phục vụ (phút) */
private val timeRanges = listOf(
    Range("Mọi thời gian", null, null),
    Range("Dưới 10 phút", null, 9),
    Range("10–15 phút", 10, 15),
    Range("Trên 15 phút", 16, null)
)

// =========================================================================================
// PHẦN 2: MÀN HÌNH CHÍNH QUẢN LÝ THỰC ĐƠN (ManHinhQuanLyThucDon)
// =========================================================================================

/**
 * Composable chính của ứng dụng Gọi Món Ăn:
 * - Điều phối hiển thị 2 Tab: "Món ăn" (tab 0) và "Loại món" (tab 1).
 * - Quản lý state tìm kiếm, bộ lọc danh mục, bộ lọc khoảng giá, bộ lọc thời gian.
 * - Quản lý các hộp thoại (Dialogs) Thêm/Sửa/Xóa Món ăn và Loại món.
 * - Tương tác trực tiếp với MenuDatabase thông qua SQLite.
 *
 * @param database Thể hiện của lớp MenuDatabase để truy vấn và cập nhật dữ liệu SQLite
 */
@Composable
fun ManHinhQuanLyThucDon(database: MenuDatabase) {
    val context = LocalContext.current

    // --- State điều hướng và bộ lọc ---
    var tab by remember { mutableIntStateOf(0) }                      // Tab hiện tại: 0 = Món ăn, 1 = Loại món
    var refresh by remember { mutableIntStateOf(0) }                  // Biến đếm làm mới: tăng lên mỗi khi thêm/sửa/xóa để trigger đọc lại database
    var search by remember { mutableStateOf("") }                     // Từ khóa tìm kiếm (theo tên hoặc mã món)
    var selectedCategory by remember { mutableStateOf<String?>(null) } // Mã loại món đang được chọn để lọc (null = tất cả)
    var priceFilter by remember { mutableIntStateOf(0) }              // Index khoảng giá đang chọn trong danh sách priceRanges
    var timeFilter by remember { mutableIntStateOf(0) }               // Index khoảng thời gian đang chọn trong danh sách timeRanges

    // --- State quản lý các Dialog Thêm/Sửa/Xóa Món ăn ---
    var editingDish by remember { mutableStateOf<Dish?>(null) }       // Món ăn đang được sửa (null nếu là thêm mới)
    var showDishForm by remember { mutableStateOf(false) }            // Trạng thái bật/tắt Dialog form món ăn
    var deletingDish by remember { mutableStateOf<Dish?>(null) }      // Món ăn đang chờ xác nhận xóa trong AlertDialog

    // --- State quản lý các Dialog Thêm/Sửa/Xóa Loại món ---
    var editingCategory by remember { mutableStateOf<FoodCategory?>(null) } // Loại món đang được sửa (null nếu là thêm mới)
    var showCategoryForm by remember { mutableStateOf(false) }              // Trạng thái bật/tắt Dialog form loại món
    var deletingCategory by remember { mutableStateOf<FoodCategory?>(null) } // Loại món đang chờ xác nhận xóa

    // --- Truy vấn dữ liệu từ SQLite (tự động tính toán lại khi dependency thay đổi) ---
    // Danh sách tất cả loại món (lấy lại khi 'refresh' tăng)
    val categories = remember(refresh) { database.LayLoaiMonAn() }

    // Danh sách món ăn được lọc đa tiêu chí: theo loại, từ khóa tìm kiếm, khoảng giá, khoảng thời gian
    val dishes = remember(selectedCategory, search, priceFilter, timeFilter, refresh) {
        val gia = priceRanges[priceFilter]
        val phut = timeRanges[timeFilter]
        database.TimMonAn(search.trim(), selectedCategory, gia.from, gia.to, phut.from?.toInt(), phut.to?.toInt())
    }

    // State điều khiển vị trí cuộn của danh sách món ăn
    val dishListState = rememberLazyListState()

    // Mỗi khi thay đổi tiêu chí lọc hoặc tìm kiếm, tự động cuộn danh sách lên đầu trang
    LaunchedEffect(selectedCategory, search, priceFilter, timeFilter) {
        dishListState.scrollToItem(0)
    }

    // Map tra cứu nhanh: từ mã loại món -> tên loại món (để hiển thị tên loại trên thẻ món ăn)
    val categoryNames = categories.associate { it.code to it.name }

    // Kiểm tra xem người dùng có đang áp dụng bất kỳ bộ lọc/tìm kiếm nào hay không
    val isFiltering = search.isNotBlank() || selectedCategory != null || priceFilter != 0 || timeFilter != 0

    Scaffold(
        containerColor = Color(0xFFF8F9FA), // Màu nền tổng thể sáng nhẹ dịu mắt
        floatingActionButton = {
            // Nút bấm nổi (+) góc dưới: Tùy theo tab đang đứng sẽ mở form thêm món hoặc thêm loại món
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
                containerColor = Color(0xFFE65100), // Màu cam đậm chủ đạo
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
            // ---------------------------------------------------------
            // 1. BANNER TIÊU ĐỀ ĐẦU TRANG (Header Gradient)
            // ---------------------------------------------------------
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

            // ---------------------------------------------------------
            // 2. THANH CHUYỂN TAB HIỆN ĐẠI (Món ăn / Loại món)
            // ---------------------------------------------------------
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFEEEEEE)
            ) {
                Row(Modifier.padding(4.dp)) {
                    val tabModifier = Modifier.weight(1f)

                    // Tab 0: Danh sách món ăn
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

                    // Tab 1: Danh sách loại món
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

            // ---------------------------------------------------------
            // 3. NỘI DUNG CHÍNH THEO TỪNG TAB
            // ---------------------------------------------------------
            if (tab == 0) {
                // ==================== TAB 0: QUẢN LÝ MÓN ĂN ====================

                // Ô tìm kiếm nhanh theo tên hoặc mã món
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
                        // Nút 'X' để xóa nhanh nội dung tìm kiếm
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { search = "" }) {
                                Text("✕", fontWeight = FontWeight.Bold, color = Color(0xFF757575))
                            }
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))

                // Thanh cuộn ngang chọn loại món (Pill lọc nhanh)
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

                // Thanh bộ lọc nâng cao (Dropdown lọc theo Giá tiền và Thời gian chế biến)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Menu chọn khoảng giá
                    DropdownFilterPill(
                        prefix = "💰 Giá",
                        label = priceRanges[priceFilter].label,
                        isActive = priceFilter != 0,
                        options = priceRanges.mapIndexed { index, range -> index to range.label },
                        onSelect = { priceFilter = it }
                    )

                    // Menu chọn khoảng thời gian
                    DropdownFilterPill(
                        prefix = "⏱️ Thời gian",
                        label = timeRanges[timeFilter].label,
                        isActive = timeFilter != 0,
                        options = timeRanges.mapIndexed { index, range -> index to range.label },
                        onSelect = { timeFilter = it }
                    )

                    // Nút Xóa tất cả bộ lọc khi đang có điều kiện lọc
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

                // Danh sách các thẻ món ăn (LazyColumn)
                if (dishes.isEmpty()) {
                    // Giao diện khi không có kết quả phù hợp
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
                    // Danh sách món ăn cuộn mượt mà
                    LazyColumn(
                        state = dishListState,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp) // Chừa khoảng trống bên dưới tránh bị nút FAB che khuất
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
                // ==================== TAB 1: QUẢN LÝ LOẠI MÓN ====================
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

    // ---------------------------------------------------------
    // 4. CÁC HỘP THOẠI DIALOGS (Form & Xác nhận xóa)
    // ---------------------------------------------------------

    // Dialog Biểu mẫu Thêm/Sửa Món ăn
    if (showDishForm) {
        BieuMauMonAn(
            initial = editingDish,
            categories = categories,
            onDismiss = { showDishForm = false },
            onSave = { dish ->
                try {
                    if (editingDish == null) {
                        // Thêm mới: kiểm tra xem mã món đã bị trùng chưa
                        if (database.MaMonAnDaTonTai(dish.code)) throw IllegalArgumentException("Mã món đã tồn tại!")
                        database.ThemMonAn(dish)
                    } else {
                        // Cập nhật món đã có
                        database.SuaMonAn(dish)
                    }
                    refresh++ // Làm mới danh sách
                    showDishForm = false
                    Toast.makeText(context, "Đã lưu món ${dish.name}", Toast.LENGTH_SHORT).show()
                } catch (error: Exception) {
                    Toast.makeText(context, error.message ?: "Không lưu được món", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Dialog Biểu mẫu Thêm/Sửa Loại món
    if (showCategoryForm) {
        BieuMauLoaiMonAn(
            initial = editingCategory,
            onDismiss = { showCategoryForm = false },
            onSave = { category ->
                try {
                    if (editingCategory == null) {
                        // Thêm mới: kiểm tra mã loại món đã tồn tại chưa
                        if (database.MaLoaiMonDaTonTai(category.code)) throw IllegalArgumentException("Mã loại đã tồn tại!")
                        database.ThemLoaiMonAn(category)
                    } else {
                        // Cập nhật loại món đã có
                        database.SuaLoaiMonAn(category)
                    }
                    refresh++ // Làm mới danh sách
                    showCategoryForm = false
                    Toast.makeText(context, "Đã lưu loại ${category.name}", Toast.LENGTH_SHORT).show()
                } catch (error: Exception) {
                    Toast.makeText(context, error.message ?: "Không lưu được loại", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Hộp thoại xác nhận xóa một món ăn
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

    // Hộp thoại xác nhận xóa loại món ăn (kèm thông báo số lượng món ăn liên đới bị xóa theo CASCADE)
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

// =========================================================================================
// PHẦN 3: CÁC THẺ GIAO DIỆN HIỂN THỊ ITEM (Cards)
// =========================================================================================

/**
 * Thẻ hiển thị thông tin 1 món ăn trong danh sách:
 * - Ảnh đại diện món ăn bên trái (hỗ trợ cả ảnh nội bộ lẫn ảnh tải lên từ bộ nhớ máy)
 * - Tên món, Mã món + Tên loại món, Thời gian chế biến, Giá tiền (được format dấu phẩy)
 * - Các nút thao tác nhanh: Sửa và Xóa
 *
 * @param dish Đối tượng Dish chứa thông tin món ăn
 * @param categoryName Tên hiển thị của danh mục loại món
 * @param onEdit Callback được gọi khi bấm nút Sửa
 * @param onDelete Callback được gọi khi bấm nút Xóa
 */
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
            // Ảnh món ăn bo tròn góc
            AnhMon(
                dish.image,
                Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(12.dp))

            // Cột thông tin chi tiết món ăn
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

            // Cột các nút thao tác Sửa / Xóa
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

/**
 * Thẻ hiển thị thông tin 1 loại danh mục món ăn:
 * - Tiêu đề tên loại, Mã loại dạng badge nổi bật
 * - Mô tả danh mục
 * - Nút "Xem món thuộc loại này →" để chuyển nhanh sang tab món ăn và tự động lọc loại đó
 * - Nút Sửa và Xóa loại món
 *
 * @param category Đối tượng FoodCategory
 * @param onEdit Callback khi bấm Sửa loại
 * @param onDelete Callback khi bấm Xóa loại
 * @param onShowDishes Callback khi bấm "Xem món thuộc loại này"
 */
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

// =========================================================================================
// PHẦN 4: CÁC THÀNH PHẦN NÚT BẤM VÀ BỘ LỌC (Pills & Dropdowns)
// =========================================================================================

/**
 * Nút lọc danh mục hình con nhộng (Pill):
 * - Đổi màu cam đậm nổi bật khi được chọn (isSelected = true)
 * - Màu trắng viền xám khi không chọn
 */
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

/**
 * Nút bộ lọc dạng dropdown xổ xuống:
 * - Khi bấm vào sẽ bung danh sách các tùy chọn (ví dụ: các mức giá hoặc thời gian)
 * - Khi đang áp dụng lọc (isActive = true), nút sẽ đổi màu nền cam nhạt để người dùng nhận biết
 *
 * @param prefix Tiền tố tiêu đề (ví dụ "💰 Giá" hoặc "⏱️ Thời gian")
 * @param label Tên tùy chọn hiện đang chọn (ví dụ "Dưới 100k")
 * @param isActive Trạng thái có đang lọc khác mặc định hay không
 * @param options Danh sách các cặp (Index, Tên hiển thị)
 * @param onSelect Callback trả về Index được chọn
 */
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

// =========================================================================================
// PHẦN 5: BIỂU MẪU NHẬP LIỆU (DIALOG FORMS)
// =========================================================================================

/**
 * Hộp thoại (Dialog) nhập liệu Loại món ăn:
 * - Thêm mới hoặc chỉnh sửa (nếu initial != null)
 * - Khóa trường Mã loại khi ở chế độ chỉnh sửa (vì mã là Khóa chính không được đổi)
 * - Kiểm tra hợp lệ các trường bắt buộc trước khi lưu
 *
 * @param initial Đối tượng FoodCategory hiện tại nếu là sửa, hoặc null nếu là thêm mới
 * @param onDismiss Đóng dialog khi hủy hoặc nhấn ra ngoài
 * @param onSave Callback lưu đối tượng FoodCategory sau khi validate thành công
 */
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

/**
 * Hộp thoại (Dialog) nhập liệu Món ăn:
 * - Hỗ trợ cả Thêm món mới và Sửa món ăn hiện có.
 * - Cho phép chọn ảnh mẫu có sẵn HOẶC tải ảnh tùy ý từ bộ nhớ máy (Activity Result Launcher).
 * - Kiểm tra đầy đủ: Giá tiền >= 0, Thời gian chế biến > 0, không để trống mã/tên/loại.
 *
 * @param initial Đối tượng Dish hiện tại nếu là sửa, hoặc null nếu là thêm mới
 * @param categories Danh sách tất cả loại món hiện có để người dùng chọn danh mục
 * @param onDismiss Đóng dialog
 * @param onSave Callback lưu đối tượng Dish
 */
@Composable
private fun BieuMauMonAn(initial: Dish?, categories: List<FoodCategory>, onDismiss: () -> Unit, onSave: (Dish) -> Unit) {
    var code by remember(initial) { mutableStateOf(initial?.code ?: "") }
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var image by remember(initial) { mutableStateOf(initial?.image ?: imageChoices.keys.first()) }
    var price by remember(initial) { mutableStateOf(initial?.price?.toString() ?: "") }
    var serviceMinutes by remember(initial) { mutableStateOf(initial?.serviceMinutes?.toString() ?: "") }
    var categoryCode by remember(initial) { mutableStateOf(initial?.categoryCode ?: categories.firstOrNull()?.code.orEmpty()) }
    var error by remember(initial) { mutableStateOf("") }
    val context = LocalContext.current

    // Launcher mở trình chọn file ảnh hệ thống của Android
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

                // Khung chọn ảnh hoặc tải ảnh từ máy
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8F9FA),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Xem trước ảnh được chọn
                        AnhMon(
                            image,
                            Modifier
                                .size(74.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            // Nút mở thư viện ảnh máy
                            Button(
                                onClick = { picker.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📁 Tải ảnh từ máy", fontSize = 12.sp, color = Color.White)
                            }
                            // Dropdown chọn ảnh mẫu có sẵn
                            OChon(
                                label = "Ảnh mẫu",
                                selected = imageChoices[image] ?: "Ảnh tải lên",
                                choices = imageChoices.map { it.key to it.value }
                            ) { image = it }
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
                    if (code.isBlank() || name.isBlank() || categoryCode.isBlank() || parsedPrice == null || parsedPrice < 0 || parsedMinutes == null || parsedMinutes <= 0) {
                        error = "Vui lòng nhập đủ thông tin (giá >= 0, phút > 0)."
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

// =========================================================================================
// PHẦN 6: CÁC THÀNH PHẦN VIEW TÁI SỬ DỤNG (REUSABLE UI COMPONENTS)
// =========================================================================================

/**
 * Ô nhập liệu OutlinedTextField chuẩn hóa:
 * Có nhãn label, hỗ trợ chế độ chỉ đọc (readOnly) và kiểu bàn phím (KeyboardType).
 */
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

/**
 * Nút chọn Dropdown chuẩn hóa:
 * Hiển thị giá trị đang chọn và danh sách sổ xuống khi nhấn vào.
 */
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

// =========================================================================================
// PHẦN 7: XỬ LÝ HÌNH ẢNH (ẢNH RESOURCE VÀ ẢNH TẢI TỪ MÁY)
// =========================================================================================

/**
 * Ánh xạ tên định danh chuỗi sang ID tài nguyên Drawable tương ứng trong file res/drawable.
 * Nếu không tìm thấy sẽ trả về ảnh mặc định phở bò.
 */
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

/**
 * Composable hiển thị ảnh món ăn:
 * - Nếu chuỗi ảnh bắt đầu bằng "file:": giải mã Bitmap từ file lưu trong bộ nhớ máy (ảnh tải lên).
 * - Nếu là tên định danh ảnh mẫu: tải qua painterResource lấy từ drawable.
 */
@Composable
private fun AnhMon(image: String, modifier: Modifier) {
    if (image.startsWith("file:")) {
        val bitmap = remember(image) { BitmapFactory.decodeFile(image.removePrefix("file:")) }
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), "Ảnh món ăn", modifier, contentScale = ContentScale.Crop)
            return
        }
    }
    Image(painterResource(anhMinhHoaMonAn(image)), "Ảnh món ăn", modifier, contentScale = ContentScale.Crop)
}

/**
 * Lưu file ảnh do người dùng chọn từ thiết bị vào thư mục bộ nhớ riêng của ứng dụng (context.filesDir):
 * - Đọc kích thước ảnh trước (inJustDecodeBounds = true) để tránh tràn bộ nhớ RAM (OOM).
 * - Tính toán inSampleSize để downscale ảnh nếu chiều rộng hoặc chiều cao > 1024px.
 * - Nén ảnh sang định dạng JPEG chất lượng 88% và lưu vào file cục bộ.
 * - Trả về đường dẫn chuỗi có tiền tố "file:/..." để lưu vào database SQLite.
 *
 * @param context Context ứng dụng
 * @param uri Đường dẫn Uri của file ảnh được chọn
 * @return Chuỗi đường dẫn file có dạng "file:/path/to/dish_*.jpg", hoặc null nếu xảy ra lỗi
 */
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
