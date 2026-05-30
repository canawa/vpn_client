# Кнопка «Подключить через Telegram»

Приложение открывает бота:

```
https://t.me/testingcoffffeemaniabot?start=connect
```

## Важно: Telegram не принимает `cmvpn://` в кнопках

Inline-кнопка поддерживает только **https://** (и tg://).

Поэтому в кнопку ставится **https-ссылка**, а не `cmvpn://...`.

## URL для кнопки в боте

**Рекомендуется** — короткая ссылка с token:

```
https://sub.coffemaniavpn.online/app/add?token=5WZG-z-wgAMyDsSM&connect=1
```

`5WZG-z-wgAMyDsSM` — это token из ссылки подписки  
(`https://sub.coffemaniavpn.online/5WZG-z-wgAMyDsSM`).

Вариант с полным URL:

```
https://sub.coffemaniavpn.online/app/add?url=https://sub.coffemaniavpn.online/5WZG-z-wgAMyDsSM&connect=1
```

Только подключить (подписка уже в приложении):

```
https://sub.coffemaniavpn.online/app/connect
```

## Пример для aiogram 3

```python
from aiogram.types import InlineKeyboardMarkup, InlineKeyboardButton

sub_url = user.subscription_url  # https://sub.coffemaniavpn.online/5WZG-z-wgAMyDsSM
token = sub_url.rstrip("/").split("/")[-1]

app_link = f"https://sub.coffemaniavpn.online/app/add?token={token}&connect=1"

keyboard = InlineKeyboardMarkup(inline_keyboard=[
    [InlineKeyboardButton(text="Подключить VPN", url=app_link)]
])
await message.answer("Нажмите кнопку:", reply_markup=keyboard)
```

## Если ссылка открывается в браузере

**Причина:** на сервере нет App Links (`assetlinks.json`).

**Решение:** развернуть файлы из `android/app-link/` — пошагово в **`APP_LINKS_SETUP.md`**.

Кратко:
1. Положить `app-link/.well-known/assetlinks.json` на сервер
2. Положить `app-link/add/index.html` для fallback
3. Настроить nginx (пример в `APP_LINKS_SETUP.md`)
4. **Переустановить** приложение на телефоне

После этого https-ссылка из кнопки бота откроет приложение напрямую.

Если Telegram всё равно показывает страницу — нажать **«Открыть приложение»** или ⋮ → «Открыть в браузере».

## `cmvpn://` — только не для Telegram-кнопок

Схема `cmvpn://add?url=...&connect=1` работает, если ссылку открыть **вне** inline-кнопки Telegram
(например, из браузера или другого приложения).
