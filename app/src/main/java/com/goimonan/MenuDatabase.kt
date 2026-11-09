package com.goimonan

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// =========================================================================================
// PHẦN 1: ĐỊNH NGHĨA CÁC MODEL DỮ LIỆU (DATA CLASS)
// =========================================================================================

/**
 * Model đại diện cho một danh mục loại món ăn (Ví dụ: Món nước, Món cơm, Đồ uống...).
 *
 * @property code Mã loại món (Khóa chính - Primary Key, ví dụ: "L01", "L02")
 * @property name Tên hiển thị của loại món (Ví dụ: "Món nước")
 * @property description Mô tả chi tiết về loại món đó
 */
data class FoodCategory(
    val code: String,
    val name: String,
    val description: String
)

/**
 * Model đại diện cho một món ăn cụ thể trong thực đơn.
 *
 * @property code Mã món ăn (Khóa chính - Primary Key, ví dụ: "MA01", "MA02")
 * @property name Tên món ăn (Ví dụ: "Phở bò", "Trà đào")
 * @property image Tên tài nguyên ảnh Drawable (ví dụ "dish_pho") HOẶC tên file ảnh người dùng tự tải lên lưu trong máy
 * @property price Giá bán của món (đơn vị: VNĐ, kiểu Long để tránh tràn số, điều kiện >= 0)
 * @property serviceMinutes Thời gian ước tính phục vụ/chế biến món ăn (đơn vị: phút, điều kiện > 0)
 * @property categoryCode Mã loại món mà món ăn này trực thuộc (Khóa ngoại - Foreign Key trỏ tới categories.code)
 */
data class Dish(
    val code: String,
    val name: String,
    val image: String,
    val price: Long,
    val serviceMinutes: Int,
    val categoryCode: String
)

// =========================================================================================
// PHẦN 2: LỚP QUẢN TRỊ CƠ SỞ DỮ LIỆU SQLITE (SQLiteOpenHelper)
// =========================================================================================

/**
 * Lớp kế thừa từ SQLiteOpenHelper chuyên quản lý việc tạo bảng, nâng cấp phiên bản (migration),
 * cấu hình ràng buộc toàn vẹn và các hàm CRUD (Thêm, Đọc, Sửa, Xóa) cho ứng dụng Gọi Món Ăn.
 *
 * @param context Context của ứng dụng (truyền từ Activity vào để mở đường dẫn lưu file database)
 * Database name: "menu.db"
 * Database version: 3
 */
class MenuDatabase(context: Context) : SQLiteOpenHelper(context, "menu.db", null, 3) {

    /**
     * Hàm cấu hình kết nối database trước khi thực thi bất kỳ thao tác nào.
     * BẬT ràng buộc khóa ngoại (Foreign Keys): Khi xóa 1 Category cha, các Dish con sẽ tự động bị xóa theo (CASCADE).
     */
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    /**
     * Được gọi khi database lần đầu tiên được tạo trên thiết bị người dùng.
     * Tạo 2 bảng:
     * 1. categories: Chứa danh mục loại món ăn
     * 2. dishes: Chứa danh sách món ăn, liên kết với categories qua category_code
     * Đồng thời chèn dữ liệu mẫu ban đầu để app mở lên có ngay món ăn hiển thị.
     */
    override fun onCreate(db: SQLiteDatabase) {
        // Tạo bảng categories (Loại món)
        db.execSQL("""CREATE TABLE categories (
            code TEXT PRIMARY KEY NOT NULL,
            name TEXT NOT NULL,
            description TEXT NOT NULL
        )""".trimIndent())

        // Tạo bảng dishes (Món ăn) kèm kiểm tra ràng buộc giá >= 0 và thời gian > 0, khóa ngoại CASCADE
        db.execSQL("""CREATE TABLE dishes (
            code TEXT PRIMARY KEY NOT NULL,
            name TEXT NOT NULL,
            image TEXT NOT NULL,
            price INTEGER NOT NULL CHECK(price >= 0),
            service_minutes INTEGER NOT NULL CHECK(service_minutes > 0),
            category_code TEXT NOT NULL,
            FOREIGN KEY(category_code) REFERENCES categories(code) ON DELETE CASCADE
        )""".trimIndent())

        // Chèn 4 loại món mẫu ban đầu
        listOf(
            FoodCategory("L01", "Món nước", "Phở, bún và các món dùng với nước dùng"),
            FoodCategory("L02", "Món cơm", "Các món ăn kèm cơm"),
            FoodCategory("L03", "Món cuốn", "Các món cuốn ăn nhẹ"),
            FoodCategory("L04", "Đồ uống", "Thức uống phục vụ tại quán")
        ).forEach { db.insertOrThrow("categories", null, it.duLieuLoaiMon()) }

        // Chèn 6 món ăn mẫu ban đầu
        listOf(
            Dish("MA01", "Phở bò", "dish_pho", 75_000, 12, "L01"),
            Dish("MA02", "Bún bò Huế", "dish_bun_bo", 120_000, 14, "L01"),
            Dish("MA03", "Cơm tấm", "dish_com_tam", 95_000, 18, "L02"),
            Dish("MA04", "Cơm gà", "dish_com_ga", 150_000, 12, "L02"),
            Dish("MA05", "Gỏi cuốn", "dish_goi_cuon", 110_000, 10, "L03"),
            Dish("MA06", "Trà đào", "dish_tra_dao", 45_000, 5, "L04")
        ).forEach { db.insertOrThrow("dishes", null, it.duLieuMonAn()) }

        // Bổ sung thêm 5 món ăn mẫu mới (ở version 3)
        themMonMauMoi(db)
    }

    /**
     * Tự động chạy khi tăng database version (từ ver cũ lên ver mới).
     * Đảm bảo dữ liệu người dùng tự nhập không bị mất khi cập nhật ứng dụng.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Nâng cấp từ version 1 lên 2: Tạo bảng dishes_new có khóa ngoại CASCADE, copy data rồi đổi tên lại
        if (oldVersion < 2) {
            db.execSQL("""CREATE TABLE dishes_new (
                code TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                image TEXT NOT NULL,
                price INTEGER NOT NULL CHECK(price >= 0),
                service_minutes INTEGER NOT NULL CHECK(service_minutes > 0),
                category_code TEXT NOT NULL,
                FOREIGN KEY(category_code) REFERENCES categories(code) ON DELETE CASCADE
            )""".trimIndent())
            db.execSQL("""INSERT INTO dishes_new
                (code, name, image, price, service_minutes, category_code)
                SELECT code, name, image, price, service_minutes, category_code FROM dishes
            """.trimIndent())
            db.execSQL("DROP TABLE dishes")
            db.execSQL("ALTER TABLE dishes_new RENAME TO dishes")
        }
        // Nâng cấp lên version 3: Chèn thêm 5 món ăn mẫu mới
        if (oldVersion < 3) themMonMauMoi(db)
    }

    /**
     * Hàm phụ: Chèn thêm 5 món ăn mẫu đa dạng mới vào database
     * Dùng CONFLICT_IGNORE để nếu món đã có rồi thì bỏ qua, không gây lỗi trùng khóa chính.
     */
    private fun themMonMauMoi(db: SQLiteDatabase) {
        val monMoi = listOf(
            Dish("MA07", "Bún riêu cua", "dish_bun_rieu", 135_000, 13, "L01"),
            Dish("MA08", "Cơm chiên hải sản", "dish_com_chien", 165_000, 17, "L02"),
            Dish("MA09", "Bò lúc lắc", "dish_bo_luc_lac", 190_000, 14, "L02"),
            Dish("MA10", "Nem cuốn", "dish_nem_cuon", 85_000, 8, "L03"),
            Dish("MA11", "Sinh tố xoài", "dish_sinh_to_xoai", 65_000, 6, "L04")
        )
        monMoi.forEach { mon ->
            // Kiểm tra xem loại món tương ứng còn tồn tại không trước khi chèn món
            val conLoai = db.rawQuery("SELECT 1 FROM categories WHERE code = ?", arrayOf(mon.categoryCode))
                .use { it.moveToFirst() }
            if (conLoai) {
                db.insertWithOnConflict("dishes", null, mon.duLieuMonAn(), SQLiteDatabase.CONFLICT_IGNORE)
            }
        }
    }

    // =========================================================================================
    // PHẦN 3: CÁC HÀM TRUY VẤN (QUERY / SELECT)
    // =========================================================================================

    /**
     * Lấy toàn bộ danh sách các Loại món ăn (sắp xếp tăng dần theo tên, không phân biệt hoa thường).
     * @return Danh sách List<FoodCategory>
     */
    fun LayLoaiMonAn(): List<FoodCategory> {
        val result = mutableListOf<FoodCategory>()
        // Truy vấn bảng categories, sắp xếp theo tên (NOCASE)
        readableDatabase.query("categories", null, null, null, null, null, "name COLLATE NOCASE ASC").use { cursor ->
            while (cursor.moveToNext()) result += FoodCategory(
                cursor.getString(cursor.getColumnIndexOrThrow("code")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getString(cursor.getColumnIndexOrThrow("description"))
            )
        }
        return result
    }

    /**
     * Lấy danh sách món ăn cơ bản theo mã loại món hoặc tùy chọn sắp xếp theo loại.
     */
    fun LayMonAn(maLoai: String? = null, sapTheoLoai: Boolean = false): List<Dish> =
        truyVanMonAn(maLoai, "", null, null, null, null, sapTheoLoai)

    /**
     * Tìm kiếm và lọc món ăn nâng cao kết hợp nhiều điều kiện:
     * - Từ khóa tìm kiếm (theo mã món hoặc tên món)
     * - Mã danh mục loại món
     * - Khoảng giá (Từ giá -> Đến giá)
     * - Khoảng thời gian chế biến (Từ phút -> Đến phút)
     * - Tùy chọn sắp xếp gom nhóm theo loại món
     */
    fun TimMonAn(
        tuKhoa: String = "",
        maLoai: String? = null,
        giaTu: Long? = null,
        giaDen: Long? = null,
        phutTu: Int? = null,
        phutDen: Int? = null,
        sapTheoLoai: Boolean = false
    ): List<Dish> = truyVanMonAn(maLoai, tuKhoa, giaTu, giaDen, phutTu, phutDen, sapTheoLoai)

    /**
     * Hàm nội bộ cốt lõi xử lý build câu truy vấn SQL động dựa trên các điều kiện lọc được truyền vào.
     * Sử dụng parameterized query (? placeholder) để chống tấn công SQL Injection.
     */
    private fun truyVanMonAn(
        categoryCode: String?,
        search: String,
        giaTu: Long?,
        giaDen: Long?,
        phutTu: Int?,
        phutDen: Int?,
        sortByCategory: Boolean
    ): List<Dish> {
        val conditions = mutableListOf<String>()
        val arguments = mutableListOf<String>()

        // Điều kiện 1: Lọc theo loại món nếu có
        if (categoryCode != null) {
            conditions += "category_code = ?"
            arguments += categoryCode
        }
        // Điều kiện 2: Tìm kiếm theo mã món hoặc tên món (LIKE không phân biệt hoa thường)
        if (search.isNotBlank()) {
            conditions += "(code LIKE ? OR name LIKE ?)"
            arguments += "%$search%"
            arguments += "%$search%"
        }
        // Điều kiện 3: Lọc theo khoảng giá tiền
        if (giaTu != null) { conditions += "price >= ?"; arguments += giaTu.toString() }
        if (giaDen != null) { conditions += "price <= ?"; arguments += giaDen.toString() }

        // Điều kiện 4: Lọc theo thời gian phục vụ (phút)
        if (phutTu != null) { conditions += "service_minutes >= ?"; arguments += phutTu.toString() }
        if (phutDen != null) { conditions += "service_minutes <= ?"; arguments += phutDen.toString() }

        val result = mutableListOf<Dish>()
        // Thực thi query với danh sách điều kiện nối nhau bằng chữ AND
        readableDatabase.query(
            "dishes", null,
            conditions.joinToString(" AND ").takeIf { it.isNotEmpty() },
            arguments.toTypedArray().takeIf { it.isNotEmpty() },
            null, null, if (sortByCategory) "category_code ASC, name COLLATE NOCASE ASC" else "name COLLATE NOCASE ASC"
        ).use { cursor ->
            // Đọc từng dòng kết quả và map vào đối tượng Dish
            while (cursor.moveToNext()) result += Dish(
                cursor.getString(cursor.getColumnIndexOrThrow("code")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getString(cursor.getColumnIndexOrThrow("image")),
                cursor.getLong(cursor.getColumnIndexOrThrow("price")),
                cursor.getInt(cursor.getColumnIndexOrThrow("service_minutes")),
                cursor.getString(cursor.getColumnIndexOrThrow("category_code"))
            )
        }
        return result
    }

    // =========================================================================================
    // PHẦN 4: CÁC THAO TÁC THÊM - SỬA - XÓA (C.U.D) VÀ KIỂM TRA RÀNG BUỘC
    // =========================================================================================

    /** Thêm một loại món ăn mới vào bảng categories */
    fun ThemLoaiMonAn(category: FoodCategory) {
        writableDatabase.insertOrThrow("categories", null, category.duLieuLoaiMon())
    }

    /** Cập nhật thông tin một loại món ăn theo mã loại (code) */
    fun SuaLoaiMonAn(category: FoodCategory) {
        writableDatabase.update("categories", category.duLieuLoaiMon(), "code = ?", arrayOf(category.code))
    }

    /** Xóa loại món ăn theo mã loại (do có CASCADE nên các món ăn thuộc loại này cũng sẽ bị xóa tự động) */
    fun XoaLoaiMonAn(code: String) {
        writableDatabase.delete("categories", "code = ?", arrayOf(code))
    }

    /** Thêm một món ăn mới vào bảng dishes */
    fun ThemMonAn(dish: Dish) {
        writableDatabase.insertOrThrow("dishes", null, dish.duLieuMonAn())
    }

    /** Cập nhật thông tin món ăn theo mã món (code) */
    fun SuaMonAn(dish: Dish) {
        writableDatabase.update("dishes", dish.duLieuMonAn(), "code = ?", arrayOf(dish.code))
    }

    /** Xóa một món ăn theo mã món (code) */
    fun XoaMonAn(code: String) {
        writableDatabase.delete("dishes", "code = ?", arrayOf(code))
    }

    /** Kiểm tra xem mã loại món đã tồn tại trong database hay chưa (để báo lỗi trùng mã khi thêm mới) */
    fun MaLoaiMonDaTonTai(code: String): Boolean = maDaTonTai("categories", code)

    /** Kiểm tra xem mã món ăn đã tồn tại trong database hay chưa */
    fun MaMonAnDaTonTai(code: String): Boolean = maDaTonTai("dishes", code)

    /** Hàm nội bộ kiểm tra tồn tại mã trong bảng bất kỳ */
    private fun maDaTonTai(table: String, code: String): Boolean = readableDatabase.rawQuery(
        "SELECT 1 FROM $table WHERE code = ?", arrayOf(code)
    ).use { it.moveToFirst() }

    // =========================================================================================
    // PHẦN 5: EXTENSION FUNCTIONS ĐỂ MAP ĐỐI TƯỢNG SANG CONTENTVALUES
    // =========================================================================================

    /** Chuyển đổi đối tượng FoodCategory sang ContentValues để lưu vào SQLite */
    private fun FoodCategory.duLieuLoaiMon() = ContentValues().apply {
        put("code", code)
        put("name", name)
        put("description", description)
    }

    /** Chuyển đổi đối tượng Dish sang ContentValues để lưu vào SQLite */
    private fun Dish.duLieuMonAn() = ContentValues().apply {
        put("code", code)
        put("name", name)
        put("image", image)
        put("price", price)
        put("service_minutes", serviceMinutes)
        put("category_code", categoryCode)
    }
}
