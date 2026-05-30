# App Links — чтобы ссылка открывала приложение, а не браузер

Telegram принимает только `https://`. Чтобы **сразу открывалось приложение**, на сервере `sub.coffemaniavpn.online` нужны два файла.

## 1. Скопировать на сервер

Содержимое папки `android/app-link/`:

```
/var/www/coffemania-app-link/
├── .well-known/
│   └── assetlinks.json
└── add/
    └── index.html
```

## 2. Nginx

```nginx
# App Links — проверка Android
location = /.well-known/assetlinks.json {
    alias /var/www/coffemania-app-link/.well-known/assetlinks.json;
    default_type application/json;
    add_header Access-Control-Allow-Origin *;
}

# Fallback-страница, если App Links ещё не сработали (Telegram WebView)
location ^~ /app/add {
    alias /var/www/coffemania-app-link/add/index.html;
    default_type text/html;
}
```

После правок:

```bash
nginx -t && systemctl reload nginx
```

Проверка:

```bash
curl -I https://sub.coffemaniavpn.online/.well-known/assetlinks.json
curl -I "https://sub.coffemaniavpn.online/app/add?token=TEST"
```

Оба должны отдавать **200**, не 502/404.

## 3. Переустановить приложение

Android проверяет App Links **при установке**:

1. Удалить приложение с телефона
2. Поставить APK заново
3. Нажать кнопку в боте

После этого `https://sub.coffemaniavpn.online/app/add?token=...` должно открывать **КОФЕМАНИЯ ВПН** напрямую.

## 4. URL для кнопки в боте (без изменений)

```python
token = sub_url.rstrip("/").split("/")[-1]
app_link = f"https://sub.coffemaniavpn.online/app/add?token={token}&connect=1"
```

## Если всё равно открывается браузер

1. Убедитесь, что `assetlinks.json` отдаёт 200
2. В Telegram: ⋮ → **«Открыть в браузере»** → нажать **«Открыть приложение»**
3. Для release APK добавьте SHA256 release-ключа в `assetlinks.json`:

```bash
cd android && ./gradlew signingReport
```

Сейчас в `assetlinks.json` — отпечаток **debug** APK (`app-debug.apk`).

## Telegram WebView

Встроенный браузер Telegram **не всегда** передаёт ссылку в приложение автоматически.
App Links решают это в большинстве случаев.
Если нет — пользователь нажимает кнопку на странице `/app/add`.
