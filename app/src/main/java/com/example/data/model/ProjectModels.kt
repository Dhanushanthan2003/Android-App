package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "editing_projects")
data class EditingProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val videoType: String, // "tokyo_neon", "apex_skater", "f1_speed", "drone_skyline"
    val videoTitle: String,
    val aspectRatio: String, // "16:9", "9:16", "1:1"
    val durationMs: Long,
    val trackerEngineType: String = "CSRT", // "CSRT", "MIL", "KCF", "OpticalFlow"
    val trackerWindowSize: Int = 48,
    val searchRange: Int = 64,
    val precisionThreshold: Float = 0.75f,
    val colorSpace: String = "HSV", // "HSV", "RGB", "GRAY"
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "tracker_points")
data class TrackerPoint(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val frameIndex: Int,
    val xPercent: Float,
    val yPercent: Float,
    val widthPercent: Float,
    val heightPercent: Float,
    val confidence: Float
) : Serializable

@Entity(tableName = "overlay_items")
data class OverlayItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val type: String, // "text", "sticker"
    val content: String, // text message or sticker identifier (e.g. "🔥", "🚀", "💥", "🎯")
    val colorHex: String = "#00FFFF",
    val fontSize: Float = 16f,
    val startMs: Long = 0,
    val endMs: Long = 10000,
    val isPinnedToTracker: Boolean = true
) : Serializable

@Entity(tableName = "exported_videos")
data class ExportedVideo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val projectName: String,
    val videoType: String,
    val resolution: String, // "1080p", "4K", "720p"
    val format: String, // "MP4", "MKV", "ProRes"
    val fps: Int = 60,
    val fileSizeMb: Float,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
