<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" width="96" alt="Иконка YTD-Lite">
</p>

<h1 align="center">YTD-Lite</h1>

<p align="center">
  Компактная Android-программа на базе <code>yt-dlp</code> для сохранения видео и аудио по поддерживаемым ссылкам.
</p>

<p align="center">
  <strong>Первый альфа-выпуск:</strong> <code>v0.1.0-alpha</code> · <strong>Android 8.0+</strong> · только <code>arm64-v8a</code> · Kotlin · Material 3 Expressive
</p>

| Светлая тема | Тёмная тема |
| --- | --- |
| ![Светлая тема](docs/preview.png) | ![Тёмная тема](docs/preview-dark.png) |

*Превью экранов, собранные по разметке проекта.*

## Статус

Это ранняя альфа-версия от 2 ноября 2026 года. Основные сценарии уже работают, но функции, оформление и внутренние решения могут меняться от выпуска к выпуску. Проект не является официальным клиентом `yt-dlp`.

## Возможности

### Главная и загрузка

- Вставка ссылки и получение информации о видео или аудио через встроенный движок `yt-dlp`.
- Нижний лист предпросмотра с обложкой, названием, автором и длительностью.
- Переключатель **«Видео / Аудио»**, выбор качества и кнопка **«Скачать»**.
- Прогресс загрузки с процентами и волнистым индикатором Material 3 Expressive.
- Выбор отдельных папок для видео и аудио через системный диалог Android (SAF). Разрешения `READ/WRITE_EXTERNAL_STORAGE` не требуются.
- Если папка не выбрана, файлы сохраняются в `Android/data/com.ytdlite.app/files/Movies` и `.../Music`.

### Очередь

- Задачи выполняются по одной: `PENDING → RUNNING → DONE / FAILED / CANCELED`.
- Отображаются название, тип, выбранное качество, статус и живой прогресс.
- Доступны отмена, повтор неудачной задачи и удаление завершённой из списка.
- Очередь продолжает работать при переходе между вкладками и закрытии листа предпросмотра.

### Загрузки

- История сохранённых файлов с именем, типом, размером, датой и путём или URI.
- Открытие файла во внешнем плеере через `FileProvider` или URI документа.
- Удаление записи вместе с файлом после подтверждения.
- История хранится локально в `SharedPreferences` + JSON, без Room.

### Настройки

- **Папки** — отдельные каталоги для видео и аудио, сброс к папкам по умолчанию, сообщение о потерянном доступе.
- **Язык** — переключение языка внутри приложения; на Android 13+ также доступны системные языки приложения.
- **Обновление** — проверка и обновление движка `yt-dlp`: встроенная сборка, стабильный канал или ночная сборка. Проверка обновлений APK приложения в этом разделе не выполняется.
- **О приложении** — версия, сведения о движке и история изменений.

### Оформление

- Material 3 Expressive: тулбар, нижняя навигация, кнопки, чипы, карточки, индикаторы прогресса и нижние листы.
- Светлая и тёмная темы, Dynamic Color / Material You на Android 12+.
- Adaptive-иконка, в том числе монохромная для тематических иконок Android 13+.
- Четыре основных раздела: **«Главная»**, **«Загрузки»**, **«Очередь»**, **«Настройки»**.

### Языки интерфейса

| Язык | Каталог ресурсов |
| --- | --- |
| Беларуская | `values-be` |
| Moldovenească / молдавский | `values-ro` |
| Русский | `values-ru` |
| Українська | `values-uk` |
| English, по умолчанию | `values` |

## Требования

- Android 8.0 (API 26) или новее.
- 64-битное ARM-устройство (`arm64-v8a`). Текущая конфигурация собирает один APK без универсального варианта, поэтому обычный x86-эмулятор не поддерживается.
- Доступ к интернету для разбора ссылок, загрузки файлов и проверки сборок `yt-dlp`.
- Отдельно устанавливать Python, `yt-dlp` или FFmpeg не нужно: необходимые компоненты поставляются внутри APK через `youtubedl-android`.

## Установка готового APK

1. Откройте [GitHub Actions репозитория](https://github.com/Animesash/YTD-Lite/actions/workflows/build-apk.yml).
2. Выберите успешный запуск workflow **Build APK**.
3. В разделе **Artifacts** скачайте `ytd-lite-release`.
4. Разрешите установку из неизвестных источников и установите APK.

Для установки через ADB:

```bash
adb install -r app-arm64-v8a-release.apk
```

GitHub Releases пока не создавались; основной способ получить готовый файл — артефакт сборки в Actions.

## Сборка из исходников

### Что нужно

- JDK 17.
- Android SDK с платформами API 34/35 и Build Tools.
- Android Studio Ladybug или новее — при работе через IDE.
- Gradle Wrapper 8.9 уже есть в репозитории; отдельно устанавливать Gradle не обязательно.

Путь к SDK задаётся в `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

### Команды

```bash
./gradlew assembleRelease
```

Готовый файл:

```text
app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

Сборка по умолчанию не содержит FFmpeg и занимает около 20 МБ. В этом режиме `yt-dlp` загружает готовые прогрессивные файлы, как правило до 720p.

Для склейки отдельных видео- и аудиодорожек, более высокого качества и извлечения аудио соберите вариант с FFmpeg:

```bash
./gradlew assembleRelease -PwithFfmpeg=true
```

Такой APK заметно больше — ориентировочно около 57 МБ.

### Подпись

`debug` и `release` используют конфигурацию `ytdlite`. Файл ключа не публикуется в репозитории, поэтому перед сборкой создайте собственный ключ и файл `keystore.properties` в корне проекта:

```properties
storeFile=keystore/my-release.jks
storeType=JKS
storePassword=ваш_пароль
keyAlias=mykey
keyPassword=ваш_пароль
```

`keystore.properties` и каталог `keystore/` закрыты `.gitignore`. Для CI реквизиты следует передавать через GitHub Secrets.

### CI

Workflow [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml) запускается при `push`, `pull_request` и вручную. Он собирает release-APK, проверяет подпись, размер и наличие пяти локалей, после чего загружает APK как артефакт `ytd-lite-release`.

## Структура проекта

```text
app/src/main/
├── java/com/ytdlite/app/
│   ├── MainActivity.kt              # тулбар и нижняя навигация
│   ├── YtdLiteApp.kt               # Application, Dynamic Color, очередь
│   ├── data/
│   │   ├── download/               # сохранение файлов и история
│   │   ├── queue/                  # задачи и StateFlow-очередь
│   │   ├── settings/               # папки, язык, первый запуск
│   │   ├── update/                 # история изменений и сравнение версий
│   │   └── ytdlp/                  # обёртка над yt-dlp
│   └── ui/
│       ├── home/                   # ссылка, предпросмотр, запуск загрузки
│       ├── queue/                  # активные задачи
│       ├── downloads/              # история файлов
│       ├── settings/               # подэкраны настроек
│       ├── update/                 # обновление yt-dlp
│       ├── changelog/              # история изменений
│       └── about/                  # экран «О приложении»
└── res/
    ├── layout/                     # экраны, строки списков, нижний лист
    ├── drawable/ + mipmap-*/       # иконки и adaptive icon
    ├── values*/                    # темы, цвета, строки пяти языков
    ├── menu/                       # нижняя навигация
    └── xml/                        # FileProvider и localeConfig
```

## Технические параметры

| Параметр | Значение |
| --- | --- |
| `applicationId` | `com.ytdlite.app` |
| Версия | `versionCode 1`, `versionName v0.1.0-alpha` |
| SDK | `minSdk 26`, `targetSdk 34`, `compileSdk 35` |
| ABI | только `arm64-v8a` |
| Язык | Kotlin 2.0.21 |
| Сборка | AGP 8.6.1, Gradle 8.9, JDK 17, ViewBinding |
| UI | Material Components 1.14.0, Fragment, RecyclerView, Coroutines, StateFlow |
| Движок | `youtubedl-android` 0.18.1, `yt-dlp`, Python, QuickJS |
| Оптимизация | R8, `shrinkResources`, только пять локалей, Gradle build cache |

## Текущие ограничения

- Проект находится в альфе: возможны ошибки и несовместимость будущих версий.
- Собирается только APK под `arm64-v8a`.
- Без FFmpeg недоступны склейка раздельных дорожек и часть сценариев извлечения аудио.
- Очередь живёт в процессе приложения и пока не сохраняется между запусками.
- Функция проверки обновлений относится к сборкам `yt-dlp`, а не к APK YTD-Lite.

## Планы

- Уведомление о завершении загрузки и открытие файла из него.
- Сохранение очереди между запусками приложения.
- Настройки формата и качества по умолчанию.
- Дальнейшая стабилизация первой альфа-версии.

## Лицензия и сторонние компоненты

В репозитории пока нет файла `LICENSE`, поэтому лицензия исходного кода YTD-Lite не объявлена. Если проект планируется распространять как открытый код, добавьте `LICENSE`, а при необходимости — `NOTICE.md` с атрибуциями сторонних компонентов.

Проект использует:

- [yt-dlp](https://github.com/yt-dlp/yt-dlp);
- [yt-dlp nightly builds](https://github.com/yt-dlp/yt-dlp-nightly-builds);
- [youtubedl-android](https://github.com/JunkFood02/youtubedl-android);
- AndroidX, Kotlin Coroutines и [Material Components for Android](https://github.com/material-components/material-components-android).

Перед публикацией APK проверьте условия распространения включённых бинарных компонентов Python, `yt-dlp`, QuickJS и, при включённом варианте сборки, FFmpeg.

## Ссылки

- [Репозиторий](https://github.com/Animesash/YTD-Lite)
- [GitHub Actions](https://github.com/Animesash/YTD-Lite/actions)
- [Issues](https://github.com/Animesash/YTD-Lite/issues)
- [История коммитов](https://github.com/Animesash/YTD-Lite/commits/main/)
