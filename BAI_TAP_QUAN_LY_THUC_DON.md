# Bài tập lớn: Quản lý thực đơn

Sinh viên: **K24DTCN008 - Đào Xuân Dũng**

## Cách chạy

Mở thư mục dự án bằng Android Studio, chọn AVD rồi chạy cấu hình `app`.
Ứng dụng có dữ liệu mẫu gồm 4 loại món và 11 món ăn.

## Chức năng

- Tab **Loại món**: xem, thêm, sửa, xóa loại món gồm mã loại, tên loại, mô tả. Khi xóa một loại, hộp thoại báo số món liên quan; các món này được xóa cùng loại sau khi xác nhận.
- Tab **Món ăn**: xem, thêm, sửa, xóa món gồm mã món, tên món, hình ảnh (tải ảnh trực tiếp từ bộ nhớ máy lên), giá đặt, thời gian phục vụ, loại món.
- Chọn một loại ở hàng bộ lọc loại món hoặc bấm **Xem món thuộc loại** để xem các món ăn theo từng danh mục.
- **Bộ lọc nâng cao gọn gàng**:
  - Lọc theo **Giá** độc lập: Tất cả, Dưới 100k, 100k–200k, Trên 200k.
  - Lọc theo **Thời gian phục vụ** độc lập: Mọi lúc, Dưới 10 phút, 10–15 phút, Trên 15 phút.
  - Có thể kết hợp lọc đồng thời cả Giá và Thời gian cùng lúc qua các nút Dropdown Pill gọn gàng (chỉ chiếm 1 dòng duy nhất). Nút **✕ Bỏ lọc** hỗ trợ xóa nhanh tất cả điều kiện lọc để quay về danh sách ban đầu.

Năm món mới là Bún riêu cua (Món nước), Cơm chiên hải sản và Bò lúc lắc (Món cơm), Nem cuốn (Món cuốn), Sinh tố xoài (Đồ uống). Mỗi món có ảnh minh họa riêng. Cơ sở dữ liệu phiên bản 3 tự thêm các món mẫu mới cho bản cài đặt đã có dữ liệu và giữ nguyên những món người dùng đã nhập.

## Cấu trúc code

- `app/src/main/java/com/goimonan/MainActivity.kt`: mở SQLite và gọi `ManHinhQuanLyThucDon()`.
- `app/src/main/java/com/goimonan/MenuScreen.kt`: màn hình, danh sách, biểu mẫu và xử lý thao tác người dùng.
- `app/src/main/java/com/goimonan/MenuDatabase.kt`: tạo bảng SQLite, dữ liệu mẫu, CRUD và truy vấn.
- `app/src/main/res/drawable/dish_*.xml`: ảnh minh họa vector cho món ăn.

Hai bảng SQLite là `categories` và `dishes`. `dishes.category_code` tham chiếu `categories.code`. Mã là khóa chính; giá không âm và thời gian phục vụ phải lớn hơn 0.

## Hai truy vấn chính

Liệt kê món theo một loại, ví dụ `L02`:

```sql
SELECT d.code, d.name, d.price, d.service_minutes, c.name AS category_name
FROM dishes AS d
JOIN categories AS c ON c.code = d.category_code
WHERE c.code = 'L02'
ORDER BY d.name COLLATE NOCASE;
```

Tìm món giá từ 100.000 đến 200.000 đồng và phục vụ dưới 15 phút:

```sql
SELECT d.code, d.name, d.price, d.service_minutes, c.name AS category_name
FROM dishes AS d
JOIN categories AS c ON c.code = d.category_code
WHERE d.price BETWEEN 100000 AND 200000
  AND d.service_minutes < 15
ORDER BY d.name COLLATE NOCASE;
```

Trong ứng dụng, `TimMonAn()` dùng tham số `?` thay cho dữ liệu người dùng khi lọc và tìm kiếm.

## Tên hàm để tìm nhanh trong Android Studio

| Việc cần xem | Hàm trong `MenuDatabase.kt` |
|---|---|
| Lấy danh sách món ăn, có thể lọc theo loại | `LayMonAn()` |
| Tìm theo mã/tên; lọc giá và thời gian phục vụ | `TimMonAn()` |
| Thêm, sửa, xóa món ăn | `ThemMonAn()`, `SuaMonAn()`, `XoaMonAn()` |
| Lấy danh sách loại món | `LayLoaiMonAn()` |
| Thêm, sửa, xóa loại món | `ThemLoaiMonAn()`, `SuaLoaiMonAn()`, `XoaLoaiMonAn()` |
| Kiểm tra mã trùng trước khi thêm | `MaMonAnDaTonTai()`, `MaLoaiMonDaTonTai()` |

Trong `MenuScreen.kt`, `ManHinhQuanLyThucDon()` hiển thị hai tab. `BieuMauMonAn()` và `BieuMauLoaiMonAn()` là các hộp thoại nhập liệu; `TheMonAn()` và `TheLoaiMonAn()` hiển thị từng dòng dữ liệu. Nút Lưu gọi các hàm `Them...` hoặc `Sua...`; nút Xóa gọi các hàm `Xoa...`. Sau khi thao tác thành công, biến `refresh` tăng để đọc lại danh sách từ SQLite.

## Ảnh chụp AVD

Ảnh màn hình nằm trong `screenshots/menu/`, gồm danh sách món, loại món, biểu mẫu, lọc theo loại và kết quả truy vấn.
