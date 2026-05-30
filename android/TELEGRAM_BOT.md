# Кнопка «Подключить через Telegram»

API не нужен. Приложение просто открывает бота:

```
https://t.me/testingcoffffeemaniabot?start=connect
```

## Что делает бот

При `/start connect` (или `connect` в deep link):

1. Проверить подписку пользователя в вашей БД.
2. Если есть — показать inline-кнопку с deep link в приложение.
3. Если нет — сообщение «оформите подписку».

## Deep link для кнопки в боте

**Добавить подписку и сразу подключить** (рекомендуется):

```
cmvpn://add?url=https://sub.coffemaniavpn.online/USER_TOKEN&connect=1
```

Короткая схема тоже работает:

```
cmv://add?url=https://sub.coffemaniavpn.online/USER_TOKEN&connect=1
```

**Только подключить** (если подписка уже была добавлена раньше):

```
cmvpn://connect
cmv://connect
```

**Только добавить подписку** (без автоподключения):

```
cmvpn://add?url=https://sub.coffemaniavpn.online/USER_TOKEN
```

URL подписки в query нужно передавать закодированным (`urllib.parse.quote`).

## Пример кнопки (aiogram 3)

```python
from urllib.parse import quote
from aiogram.types import InlineKeyboardMarkup, InlineKeyboardButton

sub_url = "https://sub.coffemaniavpn.online/USER_TOKEN"
app_link = f"cmvpn://add?url={quote(sub_url, safe='')}&connect=1"

keyboard = InlineKeyboardMarkup(inline_keyboard=[
    [InlineKeyboardButton(text="Подключить VPN", url=app_link)]
])
await message.answer("Нажмите кнопку — откроется приложение:", reply_markup=keyboard)
```
