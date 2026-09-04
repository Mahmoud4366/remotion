# ModernVPN

## معرفی برنامه
یک اپلیکیشن VPN مدرن، امن و پایدار برای کاربران. این اپلیکیشن از VpnService استاندارد اندروید استفاده می‌کند و آماده اضافه شدن پروفایل‌ها و سرورهای واقعی است.

## قابلیت‌ها
- اتصال و قطع اتصال استاندارد با VpnService
- نمایش مدت زمان اتصال
- دکمه‌های بزرگ Connect/Disconnect
- رابط کاربری مدرن با Jetpack Compose (Material 3)
- پشتیبانی از زبان‌های فارسی و انگلیسی (RTL Support)
- Foreground Service با Notification

## Screenshot placeholder
[تصویر برنامه در اینجا قرار می‌گیرد]

## نحوه Build
برای بیلد کردن برنامه کافیست دستور زیر را اجرا کنید:
`./gradlew assembleDebug`

## نحوه نصب APK
پس از پایان عملیات Build، می‌توانید فایل APK را در مسیر زیر پیدا کنید و آن را نصب کنید:
APK: artifacts/app-debug.apk

## نحوه اضافه‌کردن Profile
در حال حاضر اپلیکیشن برای اتصال شبیه‌سازی شده است. در آینده می‌توانید با تغییر پیاده‌سازی `MyVpnService.kt` اطلاعات و پیکربندی‌های سرورهای واقعی را به آن بیفزایید.

## توضیح معماری
این برنامه با استفاده از Clean Architecture، Kotlin Coroutines, StateFlow / ViewModel و Jetpack Compose ساخته شده است.

## توضیح امنیت
هیچ API Key یا Private Key درون سورس قرار ندارد. برای امنیت بیشتر هیچ لاگ حساسی ذخیره نمی‌شود.

## محدودیت‌های فعلی
- نبود سرور واقعی VPN
- پیاده‌سازی شبیه‌سازی شده تونل VPN

## مسیر APK
APK: artifacts/app-debug.apk
