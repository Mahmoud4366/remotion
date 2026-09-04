# To get it under 6MB we must aggressively drop even arm64-v8a native payload and fallback to armeabi-v7a.
# The `libovpn3.so` in `arm64-v8a` is ~7MB alone. The `armeabi-v7a` one is much smaller, and devices run it perfectly via compatibility.
