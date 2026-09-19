# Hướng dẫn xuất file APK đã ký (Android Studio, Windows)

Tài liệu này ghi lại đúng các bước đã làm để xuất `MemWidget-v1.3-release.apk` cho dự án này. Làm theo là ra được file cài đặt
`app-release.apk` đã ký bằng khóa riêng `release.jks`, có thể cài lên điện thoại và cài đè lên các bản sau.

## Chuẩn bị (chỉ làm một lần)

1. Cài **Android Studio** (đã kèm JDK và Android SDK). Mở dự án bằng *File > Open* và chọn thư mục `D:\MemWidget`.
2. Có sẵn `local.properties` ở thư mục gốc với dòng `sdk.dir=...` trỏ tới thư mục SDK (Android Studio tự tạo khi mở dự án).
3. Có khóa ký ở thư mục gốc: `release.jks` và `keystore.properties` (4 dòng: `storeFile`, `storePassword`, `keyAlias`, `keyPassword`).
   Hai file này **không đưa lên GitHub** (đã có trong `.gitignore`), hãy sao lưu ra chỗ an toàn. Chưa có thì tạo bằng:

   ```powershell
   keytool -genkeypair -v -keystore release.jks -storetype PKCS12 -alias memwidget -keyalg RSA -keysize 2048 -validity 36500
   ```

   Rồi ghi mật khẩu vừa đặt vào `keystore.properties`. `app/build.gradle` sẽ tự đọc file này khi build bản `release`.

## Các bước xuất APK

1. **Đồng bộ Gradle.** Bấm *File > Sync Project with Gradle Files*. Chờ tab *Build* báo `BUILD SUCCESSFUL`.
   Nếu ô Build Variants còn trống hoặc trình soạn thảo hiện "Waiting for build to finish...", nghĩa là sync chưa xong.
2. **Chọn bản `release`.** Ở khung *Build Variants* (thanh bên trái), cột *Active Build Variant* của module `:app` đang là `debug`.
   Bấm vào đó rồi chọn `release`. Bản `debug` ký bằng khóa debug, không phải khóa của bạn.
3. **Build.** Vào *Build > Assemble Module 'MemWidget.app.main'*. Lệnh này tương đương `gradlew assembleRelease` cho variant đang chọn.
   Cách khác: *Build > Generate App Bundles or APK(s) > Generate APK(s)*.
4. **Chờ kết quả.** Tab *Build* hiện `BUILD SUCCESSFUL`. Lần build đầu của dự án này mất khoảng 11 giây (36 task chạy mới).
5. **Lấy file.** APK nằm ở:

   ```
   D:\MemWidget\app\build\outputs\apk\release\app-release.apk
   ```

   Tên `app-release.apk` (không có `-unsigned`) cho biết file đã được ký. Copy ra chỗ dễ nhớ, ví dụ `MemWidget-v1.3-release.apk`
   (đuôi `.apk` đã được `.gitignore` chặn nên không bị commit nhầm).
6. **Kiểm tra chữ ký (nên làm).** Dùng `apksigner` của Android SDK, thay `<phiên bản>` bằng thư mục có trong `build-tools`:

   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\build-tools\<phiên bản>\apksigner.bat" verify --print-certs -v D:\MemWidget\app\build\outputs\apk\release\app-release.apk
   ```

   Kết quả phải có `Verified using v2 scheme (APK Signature Scheme v2): true` và dòng `SHA-256 digest` của chứng chỉ
   trùng vân tay của `release.jks`. Xem vân tay khóa bằng:

   ```powershell
   keytool -list -v -keystore release.jks -alias memwidget
   ```

   Với khóa hiện tại của dự án, vân tay SHA-256 là
   `AD:C8:9D:D3:7F:4C:2E:D9:D3:A0:0C:D6:A1:30:C4:82:CB:F9:33:50:1F:90:6C:B5:71:24:79:23:8C:76:C6:F6`.
7. **Cài lên điện thoại.** Chép file sang máy rồi mở để cài (bật *Cài ứng dụng không rõ nguồn gốc* nếu được hỏi), hoặc cắm cáp, bật gỡ lỗi USB và chạy:

   ```powershell
   adb install -r D:\MemWidget\app\build\outputs\apk\release\app-release.apk
   ```

   Nếu máy đang cài bản ký bằng khóa khác thì phải gỡ bản cũ một lần trước. Từ đó về sau, cứ ký bằng cùng `release.jks` thì cài đè được.

## Cách làm bằng dòng lệnh (không cần mở giao diện)

```powershell
cd D:\MemWidget
.\gradlew.bat assembleRelease
```

File ra cùng đường dẫn ở bước 5. Cần đặt `JAVA_HOME` (Android Studio có JDK sẵn tại thư mục `jbr` trong thư mục cài đặt) hoặc chạy trong terminal của Android Studio.

## Lỗi thường gặp

| Hiện tượng | Nguyên nhân và cách xử lý |
|---|---|
| Ô Build Variants trống, editor treo ở "Waiting for build to finish..." | Gradle chưa sync xong. Làm bước 1 và đợi. |
| Ra file `app-debug.apk` hoặc APK ký khóa debug | Đang chọn `debug`, hoặc thiếu `keystore.properties` / `release.jks` ở thư mục gốc. Kiểm tra bước 2 và phần chuẩn bị. |
| `SDK location not found` | Thiếu dòng `sdk.dir` trong `local.properties`. |
| Android Studio báo `Unable to create '.git/index.lock': File exists` khi commit | Còn file khóa cũ do một lệnh git bị ngắt. Đóng mọi cửa sổ git rồi xóa file `.git\index.lock`. |
| Cài đè báo xung đột chữ ký | Bản cũ ký bằng khóa khác. Gỡ bản cũ một lần rồi cài lại. |
| Muốn phát hành bản mới | Tăng `versionCode` (và `versionName`) trong `app/build.gradle` rồi build lại. |

## Khi phát hành lên GitHub

Đẩy commit lên GitHub, rồi tạo tag phiên bản để workflow `.github/workflows/build.yml` tự build và đăng APK lên mục Releases:

```powershell
git tag v1.3
git push origin v1.3
```

APK do GitHub build chỉ ký bằng khóa của bạn khi bạn đã thêm các secret `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
(xem mục *Ký APK* trong README). Nếu chưa thêm thì APK đó ký bằng khóa debug.
