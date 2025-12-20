package live.mehiz.mpvkt.domain.playbackstate.repository

import live.mehiz.mpvkt.database.entities.PlaybackStateEntity

interface PlaybackStateRepository {

  suspend fun upsert(playbackState: PlaybackStateEntity)

  suspend fun getVideoDataById(mediaId: String): PlaybackStateEntity?

  suspend fun clearAllPlaybackStates()
}
