package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.DotDensity
import com.example.model.WidgetBorderStyle
import com.example.model.WidgetConfig
import com.example.model.WidgetGridSize
import com.example.model.WidgetThemeMode
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "widget_instances")
data class WidgetInstanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val appWidgetId: Int = -1,
    val widgetTypeId: String,
    val customName: String,
    val themeMode: String,
    val gridSize: String,
    val cornerRadiusDp: Int,
    val backgroundOpacity: Float,
    val dotDensity: String,
    val borderStyle: String,
    val showRedAccent: Boolean,
    val showDotBackdrop: Boolean,
    val use24Hour: Boolean,
    val monochromePhoto: Boolean,
    val photoPath: String,
    val timezoneId: String,
    val customTitle: String,
    val customNote: String,
    val targetEpochMillis: Long,
    val goalValue: Int,
    val currentValue: Int,
    val bitmaskState: Int,
    val isTimerRunning: Boolean,
    val timerStartedAtMillis: Long,
    val timerDurationSec: Int,
    val updatedAtMillis: Long
) {
    fun toModel(): WidgetConfig = WidgetConfig(
        id = id,
        appWidgetId = appWidgetId,
        widgetTypeId = widgetTypeId,
        customName = customName,
        themeMode = runCatching { WidgetThemeMode.valueOf(themeMode) }.getOrDefault(WidgetThemeMode.OLED_NIGHT),
        gridSize = runCatching { WidgetGridSize.valueOf(gridSize) }.getOrDefault(WidgetGridSize.SMALL_2X2),
        cornerRadiusDp = cornerRadiusDp,
        backgroundOpacity = backgroundOpacity,
        dotDensity = runCatching { DotDensity.valueOf(dotDensity) }.getOrDefault(DotDensity.BALANCED),
        borderStyle = runCatching { WidgetBorderStyle.valueOf(borderStyle) }.getOrDefault(WidgetBorderStyle.HAIRLINE),
        showRedAccent = showRedAccent,
        showDotBackdrop = showDotBackdrop,
        use24Hour = use24Hour,
        monochromePhoto = monochromePhoto,
        photoPath = photoPath,
        timezoneId = timezoneId,
        customTitle = customTitle,
        customNote = customNote,
        targetEpochMillis = targetEpochMillis,
        goalValue = goalValue,
        currentValue = currentValue,
        bitmaskState = bitmaskState,
        isTimerRunning = isTimerRunning,
        timerStartedAtMillis = timerStartedAtMillis,
        timerDurationSec = timerDurationSec,
        updatedAtMillis = updatedAtMillis
    )

    companion object {
        fun fromModel(config: WidgetConfig): WidgetInstanceEntity = WidgetInstanceEntity(
            id = config.id,
            appWidgetId = config.appWidgetId,
            widgetTypeId = config.widgetTypeId,
            customName = config.customName,
            themeMode = config.themeMode.name,
            gridSize = config.gridSize.name,
            cornerRadiusDp = config.cornerRadiusDp,
            backgroundOpacity = config.backgroundOpacity,
            dotDensity = config.dotDensity.name,
            borderStyle = config.borderStyle.name,
            showRedAccent = config.showRedAccent,
            showDotBackdrop = config.showDotBackdrop,
            use24Hour = config.use24Hour,
            monochromePhoto = config.monochromePhoto,
            photoPath = config.photoPath,
            timezoneId = config.timezoneId,
            customTitle = config.customTitle,
            customNote = config.customNote,
            targetEpochMillis = config.targetEpochMillis,
            goalValue = config.goalValue,
            currentValue = config.currentValue,
            bitmaskState = config.bitmaskState,
            isTimerRunning = config.isTimerRunning,
            timerStartedAtMillis = config.timerStartedAtMillis,
            timerDurationSec = config.timerDurationSec,
            updatedAtMillis = config.updatedAtMillis
        )
    }
}

@Dao
interface WidgetInstanceDao {
    @Query("SELECT * FROM widget_instances ORDER BY updatedAtMillis DESC")
    fun observeAll(): Flow<List<WidgetInstanceEntity>>

    @Query("SELECT * FROM widget_instances ORDER BY updatedAtMillis DESC")
    suspend fun getAll(): List<WidgetInstanceEntity>

    @Query("SELECT * FROM widget_instances WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WidgetInstanceEntity?

    @Query("SELECT * FROM widget_instances WHERE appWidgetId = :appWidgetId LIMIT 1")
    suspend fun getByAppWidgetId(appWidgetId: Int): WidgetInstanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WidgetInstanceEntity): Long

    @Query("UPDATE widget_instances SET appWidgetId = :appWidgetId, updatedAtMillis = :now WHERE id = :id")
    suspend fun bindAppWidgetId(id: Long, appWidgetId: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE widget_instances SET appWidgetId = -1 WHERE appWidgetId = :appWidgetId")
    suspend fun unbindAppWidgetId(appWidgetId: Int)

    @Query("DELETE FROM widget_instances WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(
    entities = [WidgetInstanceEntity::class],
    version = 2,
    exportSchema = false
)
abstract class DotGridDatabase : RoomDatabase() {
    abstract fun widgetInstanceDao(): WidgetInstanceDao

    companion object {
        @Volatile
        private var INSTANCE: DotGridDatabase? = null

        fun getInstance(context: Context): DotGridDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DotGridDatabase::class.java,
                    "anywidgets_local.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
