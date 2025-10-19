# FastMediaSorter Test Guide

Этот документ описывает автоматизированные тесты для FastMediaSorter и как их запускать.

## Типы тестов

### 1. Unit Tests (Модульные тесты)
Быстрые тесты без Android framework, работают на JVM.

**Расположение:** `app/src/test/java/`

**Тесты:**
- `PreferenceManagerTest` - тестирование логики валидации настроек
- `MediaUtilsTest` - тестирование утилит для медиафайлов

**Запуск в Android Studio:**
1. Открыть `Run` → `Edit Configurations`
2. Нажать `+` → `Gradle`
3. Name: `Unit Tests`
4. Tasks: `testDebugUnitTest`
5. Нажать `OK` и запустить

**Запуск из командной строки:**
```
gradlew testDebugUnitTest
```

### 2. Instrumented Tests (Инструментальные тесты)
Тесты с UI, требуют Android устройство или эмулятор.

**Расположение:** `app/src/androidTest/java/`

**Тесты:**
- `MainActivityTest` - тестирование главного экрана
- `SettingsActivityTest` - тестирование настроек
- `LanguageTest` - тестирование переключения языков

**Запуск в Android Studio:**
1. Запустить эмулятор или подключить устройство
2. Открыть `Run` → `Edit Configurations`
3. Нажать `+` → `Android Instrumented Tests`
4. Module: `app`
5. Test: `All in Package` → `com.sza.fastmediasorter`
6. Нажать `OK` и запустить

**Запуск из командной строки:**
```
gradlew connectedDebugAndroidTest
```

## Настройка эмулятора для тестов

### Создание AVD (Android Virtual Device):

1. В Android Studio открыть `Tools` → `Device Manager`
2. Нажать `Create Device`
3. Выбрать устройство (рекомендуется Pixel 5)
4. Выбрать системный образ:
   - API Level: 30 или выше
   - ABI: x86_64
   - Target: Android 11.0+
5. Имя: `Test_Pixel_5`
6. Нажать `Finish`

### Запуск эмулятора:
```
# Через Android Studio
Tools → Device Manager → Play button

# Через командную строку
emulator -avd Test_Pixel_5
```

## Запуск всех тестов

### Gradle задачи:

```bash
# Только unit тесты
gradlew runUnitTests

# Только UI тесты (требуется эмулятор/устройство)
gradlew runInstrumentedTests

# Все тесты
gradlew runAllTests
```

## Test Coverage (Покрытие кода)

Для генерации отчёта о покрытии кода тестами:

```bash
gradlew createDebugCoverageReport
```

Отчёт будет доступен в:
`app/build/reports/coverage/debug/index.html`

## Проверка результатов

### Unit Tests:
- HTML отчёт: `app/build/reports/tests/testDebugUnitTest/index.html`
- XML результаты: `app/build/test-results/testDebugUnitTest/`

### Instrumented Tests:
- HTML отчёт: `app/build/reports/androidTests/connected/index.html`
- XML результаты: `app/build/outputs/androidTest-results/connected/`

## Написание новых тестов

### Unit Test пример:
```kotlin
package com.sza.fastmediasorter.utils

import org.junit.Test
import org.junit.Assert.*

class MyUtilTest {
    @Test
    fun `test my function`() {
        // Given
        val input = "test"
        
        // When
        val result = MyUtil.process(input)
        
        // Then
        assertEquals("expected", result)
    }
}
```

### Instrumented Test пример:
```kotlin
package com.sza.fastmediasorter.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyActivityTest {
    @get:Rule
    val activityRule = ActivityTestRule(MyActivity::class.java)

    @Test
    fun myView_isDisplayed() {
        onView(withId(R.id.myView))
            .check(matches(isDisplayed()))
    }
}
```

## Отладка тестов

### Android Studio:
1. Открыть тестовый файл
2. Кликнуть на номер строки теста (появится breakpoint)
3. ПКМ на названии теста → `Debug 'testName'`

### Логирование в тестах:
```kotlin
@Test
fun myTest() {
    android.util.Log.d("TestTag", "Debug message")
    // или
    println("Test output")
}
```

## Часто возникающие проблемы

### "No connected devices"
**Решение:** Запустить эмулятор или подключить физическое устройство

### "Test infrastructure failure"
**Решение:** 
- Перезапустить эмулятор
- Очистить кэш: `gradlew clean`
- Удалить и переустановить приложение на эмуляторе

### "Tests not found"
**Решение:**
- Убедиться что тесты в правильной папке (test/ или androidTest/)
- Синхронизировать проект: `File` → `Sync Project with Gradle Files`

## CI/CD интеграция

Для автоматического запуска тестов в CI/CD (GitHub Actions, Jenkins):

```yaml
# .github/workflows/tests.yml
name: Run Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: '11'
      - name: Run unit tests
        run: ./gradlew testDebugUnitTest
```

## Дополнительные ресурсы

- [Android Testing Guide](https://developer.android.com/training/testing)
- [Espresso Documentation](https://developer.android.com/training/testing/espresso)
- [JUnit 4 Documentation](https://junit.org/junit4/)
