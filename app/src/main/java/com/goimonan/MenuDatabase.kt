package com.goimonan

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class FoodCategory(
    val code: String,
    val name: String,
    val description: String
)

data class Dish(
    val code: String,
    val name: String,
    val image: String,
    val price: Long,
    val serviceMinutes: Int,
    val categoryCode: String
)

class MenuDatabase(context: Context) : SQLiteOpenHelper(context, "menu.db", null, 3) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE categories (
            code TEXT PRIMARY KEY NOT NULL,
            name TEXT NOT NULL,
            description TEXT NOT NULL
        )""".trimIndent())
        db.execSQL("""CREATE TABLE dishes (
            code TEXT PRIMARY KEY NOT NULL,
            name TEXT NOT NULL,
            image TEXT NOT NULL,
            price INTEGER NOT NULL CHECK(price >= 0),
            service_minutes INTEGER NOT NULL CHECK(service_minutes > 0),
            category_code TEXT NOT NULL,
            FOREIGN KEY(category_code) REFERENCES categories(code) ON DELETE CASCADE
        )""".trimIndent())

        listOf(
            FoodCategory("L01", "Món nước", "Phở, bún và các món dùng với nước dùng"),
            FoodCategory("L02", "Món cơm", "Các món ăn kèm cơm"),
            FoodCategory("L03", "Món cuốn", "Các món cuốn ăn nhẹ"),
            FoodCategory("L04", "Đồ uống", "Thức uống phục vụ tại quán")
        ).forEach { db.insertOrThrow("categories", null, it.duLieuLoaiMon()) }

        listOf(
            Dish("MA01", "Phở bò", "dish_pho", 75_000, 12, "L01"),
            Dish("MA02", "Bún bò Huế", "dish_bun_bo", 120_000, 14, "L01"),
            Dish("MA03", "Cơm tấm", "dish_com_tam", 95_000, 18, "L02"),
            Dish("MA04", "Cơm gà", "dish_com_ga", 150_000, 12, "L02"),
            Dish("MA05", "Gỏi cuốn", "dish_goi_cuon", 110_000, 10, "L03"),
            Dish("MA06", "Trà đào", "dish_tra_dao", 45_000, 5, "L04")
        ).forEach { db.insertOrThrow("dishes", null, it.duLieuMonAn()) }
        themMonMauMoi(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
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
        if (oldVersion < 3) themMonMauMoi(db)
    }

    private fun themMonMauMoi(db: SQLiteDatabase) {
        val monMoi = listOf(
            Dish("MA07", "Bún riêu cua", "dish_bun_rieu", 135_000, 13, "L01"),
            Dish("MA08", "Cơm chiên hải sản", "dish_com_chien", 165_000, 17, "L02"),
            Dish("MA09", "Bò lúc lắc", "dish_bo_luc_lac", 190_000, 14, "L02"),
            Dish("MA10", "Nem cuốn", "dish_nem_cuon", 85_000, 8, "L03"),
            Dish("MA11", "Sinh tố xoài", "dish_sinh_to_xoai", 65_000, 6, "L04")
        )
        monMoi.forEach { mon ->
            val conLoai = db.rawQuery("SELECT 1 FROM categories WHERE code = ?", arrayOf(mon.categoryCode))
                .use { it.moveToFirst() }
            if (conLoai) {
                db.insertWithOnConflict("dishes", null, mon.duLieuMonAn(), SQLiteDatabase.CONFLICT_IGNORE)
            }
        }
    }

    fun LayLoaiMonAn(): List<FoodCategory> {
        val result = mutableListOf<FoodCategory>()
        readableDatabase.query("categories", null, null, null, null, null, "name COLLATE NOCASE ASC").use { cursor ->
            while (cursor.moveToNext()) result += FoodCategory(
                cursor.getString(cursor.getColumnIndexOrThrow("code")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getString(cursor.getColumnIndexOrThrow("description"))
            )
        }
        return result
    }

    fun LayMonAn(maLoai: String? = null, sapTheoLoai: Boolean = false): List<Dish> =
        truyVanMonAn(maLoai, "", null, null, null, null, sapTheoLoai)

    fun TimMonAn(
        tuKhoa: String = "",
        maLoai: String? = null,
        giaTu: Long? = null,
        giaDen: Long? = null,
        phutTu: Int? = null,
        phutDen: Int? = null,
        sapTheoLoai: Boolean = false
    ): List<Dish> = truyVanMonAn(maLoai, tuKhoa, giaTu, giaDen, phutTu, phutDen, sapTheoLoai)

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
        if (categoryCode != null) {
            conditions += "category_code = ?"
            arguments += categoryCode
        }
        if (search.isNotBlank()) {
            conditions += "(code LIKE ? OR name LIKE ?)"
            arguments += "%$search%"
            arguments += "%$search%"
        }
        if (giaTu != null) { conditions += "price >= ?"; arguments += giaTu.toString() }
        if (giaDen != null) { conditions += "price <= ?"; arguments += giaDen.toString() }
        if (phutTu != null) { conditions += "service_minutes >= ?"; arguments += phutTu.toString() }
        if (phutDen != null) { conditions += "service_minutes <= ?"; arguments += phutDen.toString() }
        val result = mutableListOf<Dish>()
        readableDatabase.query(
            "dishes", null,
            conditions.joinToString(" AND ").takeIf { it.isNotEmpty() },
            arguments.toTypedArray().takeIf { it.isNotEmpty() },
            null, null, if (sortByCategory) "category_code ASC, name COLLATE NOCASE ASC" else "name COLLATE NOCASE ASC"
        ).use { cursor ->
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

    fun ThemLoaiMonAn(category: FoodCategory) {
        writableDatabase.insertOrThrow("categories", null, category.duLieuLoaiMon())
    }

    fun SuaLoaiMonAn(category: FoodCategory) {
        writableDatabase.update("categories", category.duLieuLoaiMon(), "code = ?", arrayOf(category.code))
    }

    fun XoaLoaiMonAn(code: String) {
        writableDatabase.delete("categories", "code = ?", arrayOf(code))
    }

    fun ThemMonAn(dish: Dish) {
        writableDatabase.insertOrThrow("dishes", null, dish.duLieuMonAn())
    }

    fun SuaMonAn(dish: Dish) {
        writableDatabase.update("dishes", dish.duLieuMonAn(), "code = ?", arrayOf(dish.code))
    }

    fun XoaMonAn(code: String) {
        writableDatabase.delete("dishes", "code = ?", arrayOf(code))
    }

    fun MaLoaiMonDaTonTai(code: String): Boolean = maDaTonTai("categories", code)
    fun MaMonAnDaTonTai(code: String): Boolean = maDaTonTai("dishes", code)

    private fun maDaTonTai(table: String, code: String): Boolean = readableDatabase.rawQuery(
        "SELECT 1 FROM $table WHERE code = ?", arrayOf(code)
    ).use { it.moveToFirst() }

    private fun FoodCategory.duLieuLoaiMon() = ContentValues().apply {
        put("code", code)
        put("name", name)
        put("description", description)
    }

    private fun Dish.duLieuMonAn() = ContentValues().apply {
        put("code", code)
        put("name", name)
        put("image", image)
        put("price", price)
        put("service_minutes", serviceMinutes)
        put("category_code", categoryCode)
    }
}
