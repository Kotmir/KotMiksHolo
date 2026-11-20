# KotMiksHolo

Пакетные голограммы на базе `TextDisplay` для **Paper / Purpur 1.21+** (Java 21).
Сущности в мире **не спавнятся** — всё отправляется клиентам пакетами через **ProtocolLib**.

## Возможности

- `TextDisplay` через пакеты `SPAWN_ENTITY`, `ENTITY_METADATA`, `ENTITY_TELEPORT`, `ENTITY_DESTROY`
- Мульти-блоки: у каждого блока свой `scale` и `spacing` (крупный заголовок + мелкий подзаголовок)
- Персональные плейсхолдеры PlaceholderAPI (для каждого игрока свой текст, отправляется только при изменении)
- MiniMessage + legacy (`&a`, `&l`) + HEX (`&#RRGGBB`) одновременно
- По умолчанию прозрачный фон и тень текста
- Никаких утечек: сущности уничтожаются при выходе, смене мира, респавне, выгрузке чанка, `/holo reload` и `onDisable`

## Сборка

Нужен JDK 21.

```bat
.\gradlew.bat build
```

Готовый jar: `build/libs/KotMiksHolo-1.0.0.jar`

## Установка

1. Положите `KotMiksHolo-1.0.0.jar` в `plugins/`.
2. Установите **ProtocolLib** (для самых новых версий игры берите последнюю dev-сборку).
3. (Необязательно) Установите **PlaceholderAPI**.

## Команды (`/holo`, `/kmholo`, `/hologram`) — право `kotmiksholo.admin`

| Команда | Описание |
|---|---|
| `/holo create <id> [текст]` | создать голограмму на уровне ваших глаз |
| `/holo delete <id>` | удалить |
| `/holo movehere <id>` | перенести к себе |
| `/holo addline <id> <текст>` | добавить блок (строку) |
| `/holo setline <id> <№> <текст>` | изменить блок |
| `/holo removeline <id> <№>` | удалить блок |
| `/holo scale <id> <№\|all> <число>` | масштаб блока или всей голограммы |
| `/holo billboard <id> <CENTER\|VERTICAL\|HORIZONTAL\|FIXED>` | режим поворота |
| `/holo list` | список (клик по имени = телепорт) |
| `/holo tp <id>` | телепорт к голограмме |
| `/holo reload` | перезагрузка конфигов и повторная отправка пакетов |

В тексте команд `\n` означает перенос строки внутри одного блока.

## holograms.yml

Позиция `x/y/z` — **нижняя точка** голограммы, блоки в `lines` растут вверх, первый в списке — самый верхний.

```yaml
holograms:
  shop-npc:
    world: world
    x: 0.5
    y: 66.5
    z: 0.5
    yaw: 0.0
    pitch: 0.0
    view-distance: 48        # блоков
    update-interval: 20      # тиков, 0 = статичная
    billboard: CENTER        # CENTER | VERTICAL | HORIZONTAL | FIXED
    shadow: true
    see-through: false
    background: "#00000000"  # #AARRGGBB
    alignment: CENTER        # CENTER | LEFT | RIGHT
    line-width: 200
    brightness: -1           # -1 авто, 0..15
    lines:
      - text: "<gradient:#ffaa00:#ffdd00><bold>Магазин</bold></gradient>"
        scale: 1.6           # число или [x, y, z]
        spacing: 0.05        # отступ вниз до следующего блока
      - text: "<white>ресурсов"
        scale: 1.0
```

`text` — строка (с `\n`) или список строк. Необязательный `offset-y` двигает конкретный блок по вертикали.
Отсутствующие ключи берутся из секции `defaults` в `config.yml`.
В файл заранее добавлены **отключённые** примеры (`enabled: false`): магазин над NPC, инфо-голограмма и пример с плейсхолдерами.
Включите нужный (`enabled: true`), поправьте координаты и сделайте `/holo reload`.

## Интеграция с WorldGuard (необязательно)

Если на сервере есть WorldGuard, в тексте голограмм работают встроенные плейсхолдеры
(PlaceholderAPI для них не нужен):

| Плейсхолдер | Значение |
|---|---|
| `{wg_players:<регион>}` | онлайн-игроков в регионе (ищется во всех загруженных мирах) |
| `{wg_players:<мир>:<регион>}` | то же, но в регионе указанного мира |

Имя региона сравнивается без учёта регистра. Без WorldGuard, при неизвестном регионе или мире результат — `0`.
Число обновляется по `update-interval` голограммы (должен быть > 0), пакет уходит игроку только если текст изменился.
Подсчёт кэшируется на 1 секунду, чтобы много голограмм не нагружали сервер.

Если установлен PlaceholderAPI, плагин регистрирует расширение `kotmiksholo`:
`%kotmiksholo_wg_players_<регион>%` (можно использовать и в других плагинах).

## Simple Voice Chat

`{svc_players}` — число онлайн-игроков с установленным модом Simple Voice Chat.
Считается через PlaceholderAPI: игрок учитывается, если `%vcplaceholder_installed%` возвращает `true`
(нужно расширение PlaceholderAPI `vcplaceholder`). Без PlaceholderAPI результат — `0`.
Значение кэшируется на 1 секунду; для обновления в голограмме нужен `update-interval` > 0.

## Совместимость и индексы метаданных

Индексы метаданных `TextDisplay` собраны в одном классе `packet/DisplayMeta.java` (раскладка 1.20.2 – 1.21.x:
scale=12, billboard=15, brightness=16, view-range=17, text=23, line-width=24, background=25, flags=27).
Если в какой-то будущей версии игры Mojang добавит поля в `Display`, сдвиньте все индексы разом параметром
`advanced.metadata-index-shift` в `config.yml` — перекомпилировать плагин не нужно.

`ENTITY_TELEPORT` в новых версиях изменил формат; если он не поддержан ProtocolLib, плагин автоматически
пересоздаёт сущности (`destroy` + `spawn`), поведение для игрока то же.

## Структура

```
src/main/java/ru/kotmiks/holo/
  KotMiksHoloPlugin.java
  command/   HoloCommand
  config/    PluginConfig, HologramStorage
  hologram/  Hologram, HoloLine, HologramManager, Billboard, Alignment, HologramDefaults
  packet/    HologramPackets, DisplayMeta
  listener/  PlayerListener, ChunkListener
  util/      TextUtil, Papi, ColorUtil, EntityIds
```
