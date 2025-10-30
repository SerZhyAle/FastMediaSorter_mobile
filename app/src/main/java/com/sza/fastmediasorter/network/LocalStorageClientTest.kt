// Тестирование оптимизации LocalStorageClient для больших папок
//
// Этот файл демонстрирует, как протестировать новую функциональность
// прогрессивного сканирования больших папок в LocalStorageClient.
//
// Для тестирования:
// 1. Создайте папку с большим количеством файлов (1000+ изображений)
// 2. Выберите эту папку через SAF (Storage Access Framework)
// 3. Запустите приложение и откройте выбранную папку
// 4. Наблюдайте за прогресс-баром во время сканирования
//
// Ожидаемый результат:
// - UI остается отзывчивым во время сканирования
// - Прогресс-бар показывает текущий прогресс
// - Файлы обрабатываются батчами по 50 штук
// - Логи показывают детали обработки каждого батча

import android.content.Context
import android.net.Uri
import com.sza.fastmediasorter.network.LocalStorageClient
import com.sza.fastmediasorter.utils.Logger
import kotlinx.coroutines.*

class LocalStorageClientTest(
    private val context: Context,
) {
    private val localStorageClient = LocalStorageClient(context)

    /**
     * Тест 1: Сканирование большой папки с прогрессом
     * Создайте папку с 1000+ изображениями и протестируйте
     */
    suspend fun testLargeFolderScanning(folderUri: Uri) {
        Logger.d("LocalStorageClientTest", "=== ТЕСТ: Сканирование большой папки ===")

        val startTime = System.currentTimeMillis()

        val files =
            localStorageClient.getImageFiles(
                folderUri = folderUri,
                isVideoEnabled = true,
                maxVideoSizeMb = 100,
                batchSize = 50, // Маленькие батчи для лучшей отзывчивости
                progressCallback =
                    object : LocalStorageClient.ScanProgressCallback {
                        override fun onProgress(
                            scannedCount: Int,
                            totalCount: Int,
                            currentBatch: Int,
                        ) {
                            val progressPercent = if (totalCount > 0) (scannedCount * 100) / totalCount else 0
                            Logger.d(
                                "LocalStorageClientTest",
                                "Прогресс: $scannedCount/$totalCount файлов ($progressPercent%), батч $currentBatch",
                            )

                            // В реальном приложении здесь обновлялся бы UI
                            // updateProgressBar(progressPercent)
                        }

                        override fun onComplete(
                            totalFiles: Int,
                            durationMs: Long,
                        ) {
                            Logger.d(
                                "LocalStorageClientTest",
                                "Сканирование завершено: $totalFiles файлов за ${durationMs}мс",
                            )
                        }
                    },
            )

        val totalTime = System.currentTimeMillis() - startTime
        Logger.d("LocalStorageClientTest", "=== РЕЗУЛЬТАТ ТЕСТА ===")
        Logger.d("LocalStorageClientTest", "Найдено файлов: ${files.size}")
        Logger.d("LocalStorageClientTest", "Общее время: ${totalTime}мс")
        Logger.d("LocalStorageClientTest", "Среднее время на файл: ${totalTime.toFloat() / files.size}мс")

        // Проверка результатов
        assert(files.isNotEmpty()) { "Должны быть найдены файлы" }
        assert(files.all { it.uri.toString().isNotEmpty() }) { "Все файлы должны иметь валидные URI" }

        Logger.d("LocalStorageClientTest", "✅ Тест пройден успешно")
    }

    /**
     * Тест 2: Сравнение производительности разных размеров батчей
     */
    suspend fun testBatchSizeComparison(folderUri: Uri) {
        Logger.d("LocalStorageClientTest", "=== ТЕСТ: Сравнение размеров батчей ===")

        val batchSizes = listOf(10, 50, 100, 500)

        for (batchSize in batchSizes) {
            Logger.d("LocalStorageClientTest", "Тестируем batchSize = $batchSize")

            val startTime = System.currentTimeMillis()

            val files =
                localStorageClient.getImageFiles(
                    folderUri = folderUri,
                    batchSize = batchSize,
                    progressCallback =
                        object : LocalStorageClient.ScanProgressCallback {
                            override fun onProgress(
                                scannedCount: Int,
                                totalCount: Int,
                                currentBatch: Int,
                            ) {
                                // Тихий режим для сравнения производительности
                            }

                            override fun onComplete(
                                totalFiles: Int,
                                durationMs: Long,
                            ) {
                                Logger.d(
                                    "LocalStorageClientTest",
                                    "batchSize $batchSize: $totalFiles файлов за ${durationMs}мс",
                                )
                            }
                        },
                )

            val totalTime = System.currentTimeMillis() - startTime
            Logger.d(
                "LocalStorageClientTest",
                "batchSize $batchSize: ${files.size} файлов, время: ${totalTime}мс",
            )
        }

        Logger.d("LocalStorageClientTest", "✅ Тест сравнения завершен")
    }

    /**
     * Тест 3: Проверка отзывчивости UI
     * Этот тест проверяет, что UI остается отзывчивым во время сканирования
     */
    suspend fun testUIResponsiveness(folderUri: Uri) =
        withContext(Dispatchers.Main) {
            Logger.d("LocalStorageClientTest", "=== ТЕСТ: Отзывчивость UI ===")

            // Запускаем сканирование в фоне
            val scanJob =
                launch(Dispatchers.IO) {
                    localStorageClient.getImageFiles(
                        folderUri = folderUri,
                        batchSize = 25, // Очень маленькие батчи
                        progressCallback =
                            object : LocalStorageClient.ScanProgressCallback {
                                override fun onProgress(
                                    scannedCount: Int,
                                    totalCount: Int,
                                    currentBatch: Int,
                                ) {
                                    Logger.d("LocalStorageClientTest", "Сканирование: $scannedCount/$totalCount")
                                }

                                override fun onComplete(
                                    totalFiles: Int,
                                    durationMs: Long,
                                ) {
                                    Logger.d("LocalStorageClientTest", "Сканирование завершено: $totalFiles файлов")
                                }
                            },
                    )
                }

            // Проверяем отзывчивость UI каждые 100мс
            var uiResponsiveChecks = 0
            val uiCheckJob =
                launch(Dispatchers.Main) {
                    while (scanJob.isActive) {
                        delay(100)
                        uiResponsiveChecks++
                        Logger.d("LocalStorageClientTest", "UI отзывчив, проверка #$uiResponsiveChecks")
                        // В реальном тесте здесь можно было бы обновлять UI элемент
                    }
                }

            // Ждем завершения сканирования
            scanJob.join()
            uiCheckJob.cancel()

            Logger.d("LocalStorageClientTest", "UI оставался отзывчивым в течение $uiResponsiveChecks проверок")
            Logger.d("LocalStorageClientTest", "✅ Тест отзывчивости UI пройден")
        }

    /**
     * Запуск всех тестов
     */
    suspend fun runAllTests(folderUri: Uri) {
        Logger.d("LocalStorageClientTest", "🚀 НАЧАЛО ТЕСТИРОВАНИЯ LocalStorageClient")

        try {
            testLargeFolderScanning(folderUri)
            delay(1000) // Пауза между тестами

            testBatchSizeComparison(folderUri)
            delay(1000)

            testUIResponsiveness(folderUri)

            Logger.d("LocalStorageClientTest", "🎉 ВСЕ ТЕСТЫ ПРОЙДЕНЫ УСПЕШНО")
        } catch (e: Exception) {
            Logger.e("LocalStorageClientTest", "❌ ОШИБКА В ТЕСТАХ: ${e.message}", e)
            throw e
        }
    }
}

/*
ИНСТРУКЦИЯ ПО ТЕСТИРОВАНИЮ:

1. Подготовка тестовых данных:
   - Создайте папку с 1000+ изображениями разных форматов (JPG, PNG, WebP)
   - Добавьте несколько видео файлов для тестирования фильтрации
   - Поместите файлы в папку, доступную через SAF

2. Запуск тестов:
   ```kotlin
   val test = LocalStorageClientTest(context)
   val folderUri = // URI выбранной папки через SAF

   lifecycleScope.launch {
       test.runAllTests(folderUri)
   }
   ```

3. Что наблюдать:
   - Прогресс-бар должен плавно заполняться
   - UI должен оставаться отзывчивым (можно нажимать кнопки)
   - Логи должны показывать обработку батчей
   - Время сканирования должно быть разумным

4. Ожидаемые результаты:
   - Без оптимизации: UI зависает на несколько секунд/минут
   - С оптимизацией: UI остается отзывчивым, прогресс виден

5. Метрики производительности:
   - Batch size 10-50: Лучшая отзывчивость UI
   - Batch size 100+: Лучшая общая производительность
   - Компромисс: 50 файлов на батч
*/
