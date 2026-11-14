# Изменения в форке

## Интерфейс
- Новая палитра: графитовые поверхности и один зелёный акцент (`values/colors.xml`).
- Кнопки меню главного экрана стали скруглёнными карточками с ripple-эффектом (`drawable/menu_card_background.xml`, `layout/fragment_launcher.xml`).
- Кнопка «Играть» и все `MineButton` теперь скруглённые, с акцентным цветом и отдельным состоянием disabled (`drawable/mine_button_background.xml`).
- Диалоги со скруглением 18dp, акцентный цвет темы (`values/styles.xml`).

## Устройство и производительность
- `utils/DeviceProfile.java`: определяет SoC и семейство GPU (Adreno / Mali / другое).
- При первом запуске на Mali/Immortalis с разными по частоте ядрами включается «Force big core» (только если пользователь ещё не менял эту настройку).
- Экран «Экспериментальные» показывает SoC, GPU, версию OpenGL ES, число ядер и ОЗУ.

## Подпись
- Удалены чужие ключи `mojo_release_next.jks` и `mojo_upload_next.jks`. Для отладочной сборки используется `debug.keystore`.
- Для своего релиза создайте собственный keystore и подключите его в `app_mojo/build.gradle`.

## Сборка
    .\gradlew.bat :app_mojo:assembleFullDebug
Перед сборкой на Windows сделайте симлинки `mojoexec`, `sdl`, `glfw` в `app_mojo/src/main/jni/`.

Не проверено сборкой: изменения писались без Android SDK. Если сборка выдаст ошибку, пришлите текст ошибки.
