# Podcast & Audio Streaming App

Đây là ứng dụng Podcast & Audio Streaming được xây dựng trên Android sử dụng **Kotlin** & **Jetpack Compose**.

## 🚀 Kiến trúc (Architecture)
Ứng dụng tuân theo mô hình **Clean Architecture** kết hợp với **MVVM**:
- **Domain Layer**: Chứa Business Logic (`Podcast`, `Episode`, `PodcastRepository`). Không phụ thuộc vào thư viện bên thứ 3.
- **Data Layer**: Xử lý logic lấy dữ liệu (Room cho Database cục bộ, Retrofit & XMLParser cho Network, WorkManager cho tải nền).
- **Presentation Layer**: Giao diện người dùng sử dụng Jetpack Compose, hiển thị dữ liệu dựa trên ViewModels.

## 🛠 Công nghệ sử dụng
- **Ngôn ngữ**: Kotlin (2.0.21)
- **UI**: Jetpack Compose (Material 3, Navigation Compose)
- **Network**: Retrofit2 (tìm kiếm qua iTunes API)
- **Cơ sở dữ liệu**: Room Database
- **Phát âm thanh**: Media3 (ExoPlayer) kèm theo `MediaSessionService` phát nhạc dưới nền.
- **Dependency Injection**: Dagger Hilt & KSP.
- **Hình ảnh**: Coil Compose.
- **Background Tasks**: WorkManager Coroutines.
- **Testing**: JUnit4 cho ViewModels.
- **CI/CD**: Tích hợp GitHub Actions để tự động build APK qua luồng `.github/workflows/android.yml`.

## 📌 Các Lớp chính
- `PodcastApplication`: Khởi tạo Hilt.
- `HomeViewModel` & `HomeScreen`: Xử lý luồng dữ liệu search và trending.
- `PodcastRepositoryImpl`: Nơi tổng hợp source of truth giữa Network và Room.
- `PodcastRssParser`: Bộ cày XML tùy biến với Coroutines để tự tải danh sách Episode.
- `PlaybackService`: Trái tim của ứng dụng, phát nhạc khi khóa màn hình.

## 📦 Thiết lập
Tất cả dependencies yêu cầu đã được khai báo chính xác trong `libs.versions.toml`. Project chạy bằng Java 11 và API 35. Bạn chỉ cần chạy:
```bash
./gradlew build
```
để biên dịch hoặc nhấn nút Run trên Android Studio.
