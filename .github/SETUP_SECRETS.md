# GitHub Secrets Setup для подписания APK

Для автоматического подписания релизных APK в GitHub Actions необходимо настроить следующие секреты.

## Шаг 1: Закодировать keystore в Base64

В PowerShell выполните:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("fastmediasorter.keystore")) | Out-File -Encoding ASCII keystore_base64.txt
```

Или в Linux/macOS:

```bash
base64 -i fastmediasorter.keystore -o keystore_base64.txt
```

## Шаг 2: Добавить GitHub Secrets

Перейдите в репозиторий GitHub:
**Settings → Secrets and variables → Actions → New repository secret**

Добавьте следующие секреты:

### 1. KEYSTORE_BASE64
- **Значение:** Содержимое файла `keystore_base64.txt` (весь текст без пробелов и переносов)
- **Описание:** Keystore файл закодированный в Base64

### 2. KEYSTORE_PASSWORD
- **Значение:** `FastMedia2025!`
- **Описание:** Пароль к keystore файлу

### 3. KEY_PASSWORD
- **Значение:** `FastMedia2025!`
- **Описание:** Пароль ключа подписи

### 4. KEY_ALIAS
- **Значение:** `fastmediasorter`
- **Описание:** Алиас ключа в keystore

## Шаг 3: Проверка

После добавления секретов, GitHub Actions будет:

1. Декодировать keystore из Base64
2. Создавать `keystore.properties` с credentials
3. Собирать **подписанный** APK
4. Загружать его как релиз с именем `FastMediaSorter-v{номер}.apk`

## Безопасность

- ❌ **НЕ коммитьте** файлы:
  - `keystore_base64.txt` (после использования удалите)
  - `fastmediasorter.keystore` (уже в `.gitignore`)
  - `keystore.properties` (уже в `.gitignore`)
  
- ✅ Keystore и пароли хранятся только в GitHub Secrets
- ✅ Подписание происходит в изолированной среде CI/CD
- ✅ Подписанный APK можно публиковать в Google Play

## Проверка подписи APK

После скачивания релизного APK проверьте подпись:

```bash
keytool -printcert -jarfile FastMediaSorter-v{номер}.apk
```

Должен отобразиться владелец сертификата и отпечаток.
