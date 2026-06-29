package com.example.data.database

import androidx.room.*
import com.example.data.model.EditingProject
import com.example.data.model.TrackerPoint
import com.example.data.model.OverlayItem
import com.example.data.model.ExportedVideo
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    // --- EditingProject Queries ---
    @Query("SELECT * FROM editing_projects ORDER BY lastModified DESC")
    fun getAllProjects(): Flow<List<EditingProject>>

    @Query("SELECT * FROM editing_projects WHERE id = :id")
    fun getProjectById(id: Int): Flow<EditingProject?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: EditingProject): Long

    @Delete
    suspend fun deleteProject(project: EditingProject)

    @Query("DELETE FROM editing_projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Int)

    // --- TrackerPoint Queries ---
    @Query("SELECT * FROM tracker_points WHERE projectId = :projectId ORDER BY frameIndex ASC")
    fun getTrackerPointsForProject(projectId: Int): Flow<List<TrackerPoint>>

    @Query("SELECT * FROM tracker_points WHERE projectId = :projectId ORDER BY frameIndex ASC")
    suspend fun getTrackerPointsForProjectList(projectId: Int): List<TrackerPoint>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackerPoint(point: TrackerPoint): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackerPoints(points: List<TrackerPoint>)

    @Query("DELETE FROM tracker_points WHERE projectId = :projectId")
    suspend fun deleteTrackerPointsForProject(projectId: Int)

    @Query("DELETE FROM tracker_points WHERE projectId = :projectId AND frameIndex = :frameIndex")
    suspend fun deleteTrackerPointAtFrame(projectId: Int, frameIndex: Int)

    // --- OverlayItem Queries ---
    @Query("SELECT * FROM overlay_items WHERE projectId = :projectId")
    fun getOverlayItemsForProject(projectId: Int): Flow<List<OverlayItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverlayItem(item: OverlayItem): Long

    @Query("DELETE FROM overlay_items WHERE id = :itemId")
    suspend fun deleteOverlayItemById(itemId: Int)

    @Query("DELETE FROM overlay_items WHERE projectId = :projectId")
    suspend fun deleteOverlayItemsForProject(projectId: Int)

    // --- ExportedVideo Queries ---
    @Query("SELECT * FROM exported_videos ORDER BY createdAt DESC")
    fun getAllExportedVideos(): Flow<List<ExportedVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExportedVideo(video: ExportedVideo): Long

    @Query("DELETE FROM exported_videos WHERE id = :videoId")
    suspend fun deleteExportedVideoById(videoId: Int)
}
