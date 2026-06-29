package com.example.data.repository

import com.example.data.database.ProjectDao
import com.example.data.model.EditingProject
import com.example.data.model.TrackerPoint
import com.example.data.model.OverlayItem
import com.example.data.model.ExportedVideo
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {

    // --- Projects ---
    val allProjects: Flow<List<EditingProject>> = projectDao.getAllProjects()

    fun getProjectById(projectId: Int): Flow<EditingProject?> = 
        projectDao.getProjectById(projectId)

    suspend fun insertProject(project: EditingProject): Int {
        return projectDao.insertProject(project).toInt()
    }

    suspend fun deleteProject(project: EditingProject) {
        projectDao.deleteProject(project)
    }

    suspend fun deleteProjectById(projectId: Int) {
        projectDao.deleteProjectById(projectId)
        projectDao.deleteTrackerPointsForProject(projectId)
        projectDao.deleteOverlayItemsForProject(projectId)
    }

    // --- Tracker Points ---
    fun getTrackerPoints(projectId: Int): Flow<List<TrackerPoint>> =
        projectDao.getTrackerPointsForProject(projectId)

    suspend fun getTrackerPointsList(projectId: Int): List<TrackerPoint> =
        projectDao.getTrackerPointsForProjectList(projectId)

    suspend fun insertTrackerPoint(point: TrackerPoint) {
        projectDao.insertTrackerPoint(point)
    }

    suspend fun insertTrackerPoints(points: List<TrackerPoint>) {
        projectDao.insertTrackerPoints(points)
    }

    suspend fun deleteTrackerPointsForProject(projectId: Int) {
        projectDao.deleteTrackerPointsForProject(projectId)
    }

    suspend fun deleteTrackerPointAtFrame(projectId: Int, frameIndex: Int) {
        projectDao.deleteTrackerPointAtFrame(projectId, frameIndex)
    }

    // --- Overlay Items ---
    fun getOverlayItems(projectId: Int): Flow<List<OverlayItem>> =
        projectDao.getOverlayItemsForProject(projectId)

    suspend fun insertOverlayItem(item: OverlayItem): Int {
        return projectDao.insertOverlayItem(item).toInt()
    }

    suspend fun deleteOverlayItemById(itemId: Int) {
        projectDao.deleteOverlayItemById(itemId)
    }

    // --- Exported Videos ---
    val allExportedVideos: Flow<List<ExportedVideo>> = projectDao.getAllExportedVideos()

    suspend fun insertExportedVideo(video: ExportedVideo): Int {
        return projectDao.insertExportedVideo(video).toInt()
    }

    suspend fun deleteExportedVideoById(videoId: Int) {
        projectDao.deleteExportedVideoById(videoId)
    }
}
