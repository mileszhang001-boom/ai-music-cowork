package com.music.localmusic

import com.music.core.api.models.MusicHints

class MusicQueryBuilder {
    
    private val conditions = mutableListOf<String>()
    private val args = mutableListOf<String>()
    private var orderBy: String? = null
    private var limit: Int? = null
    
    fun whereGenre(genre: String): MusicQueryBuilder {
        conditions.add("genre LIKE ?")
        args.add("%$genre%")
        return this
    }
    
    fun whereGenres(genres: List<String>): MusicQueryBuilder {
        if (genres.isEmpty()) return this
        
        val placeholders = genres.joinToString(" OR ") { "genre LIKE ?" }
        conditions.add("($placeholders)")
        genres.forEach { args.add("%$it%") }
        return this
    }
    
    fun whereBpmRange(minBpm: Int, maxBpm: Int): MusicQueryBuilder {
        conditions.add("bpm BETWEEN ? AND ?")
        args.add(minBpm.toString())
        args.add(maxBpm.toString())
        return this
    }
    
    fun whereEnergyRange(minEnergy: Double, maxEnergy: Double): MusicQueryBuilder {
        conditions.add("energy BETWEEN ? AND ?")
        args.add(minEnergy.toString())
        args.add(maxEnergy.toString())
        return this
    }
    
    fun whereValenceRange(minValence: Double, maxValence: Double): MusicQueryBuilder {
        conditions.add("valence BETWEEN ? AND ?")
        args.add(minValence.toString())
        args.add(maxValence.toString())
        return this
    }
    
    fun whereMoodTag(tag: String): MusicQueryBuilder {
        conditions.add("mood_tags LIKE ?")
        args.add("%$tag%")
        return this
    }
    
    fun whereMoodTags(tags: List<String>): MusicQueryBuilder {
        if (tags.isEmpty()) return this
        
        val placeholders = tags.joinToString(" OR ") { "mood_tags LIKE ?" }
        conditions.add("($placeholders)")
        tags.forEach { args.add("%$it%") }
        return this
    }
    
    fun whereSceneTag(tag: String): MusicQueryBuilder {
        conditions.add("scene_tags LIKE ?")
        args.add("%$tag%")
        return this
    }
    
    fun whereSceneTags(tags: List<String>): MusicQueryBuilder {
        if (tags.isEmpty()) return this
        
        val placeholders = tags.joinToString(" OR ") { "scene_tags LIKE ?" }
        conditions.add("($placeholders)")
        tags.forEach { args.add("%$it%") }
        return this
    }
    
    fun whereKeyword(keyword: String): MusicQueryBuilder {
        val searchTerm = "%$keyword%"
        conditions.add("(title LIKE ? OR title_pinyin LIKE ? OR artist LIKE ? OR artist_pinyin LIKE ? OR album LIKE ?)")
        repeat(5) { args.add(searchTerm) }
        return this
    }
    
    fun whereArtist(artist: String): MusicQueryBuilder {
        conditions.add("artist LIKE ?")
        args.add("%$artist%")
        return this
    }
    
    fun whereAlbum(album: String): MusicQueryBuilder {
        conditions.add("album LIKE ?")
        args.add("%$album%")
        return this
    }
    
    fun orderByRandom(): MusicQueryBuilder {
        orderBy = "RANDOM()"
        return this
    }
    
    fun orderByBpm(ascending: Boolean = true): MusicQueryBuilder {
        orderBy = if (ascending) "bpm ASC" else "bpm DESC"
        return this
    }
    
    fun orderByEnergy(ascending: Boolean = true): MusicQueryBuilder {
        orderBy = if (ascending) "energy ASC" else "energy DESC"
        return this
    }
    
    fun orderByValence(ascending: Boolean = true): MusicQueryBuilder {
        orderBy = if (ascending) "valence ASC" else "valence DESC"
        return this
    }
    
    fun orderByTitle(): MusicQueryBuilder {
        orderBy = "title COLLATE NOCASE ASC"
        return this
    }
    
    fun orderByArtist(): MusicQueryBuilder {
        orderBy = "artist COLLATE NOCASE ASC"
        return this
    }
    
    fun limit(count: Int): MusicQueryBuilder {
        this.limit = count
        return this
    }
    
    fun applyHints(hints: MusicHints): MusicQueryBuilder {
        hints.genres?.let { genres ->
            if (genres.isNotEmpty()) {
                whereGenres(genres)
            }
        }
        
        hints.tempo?.let { tempo ->
            val (minBpm, maxBpm) = when (tempo.lowercase()) {
                "slow" -> 60 to 90
                "moderate" -> 90 to 120
                "fast" -> 120 to 160
                "very_fast", "very fast" -> 160 to 200
                else -> return@let
            }
            whereBpmRange(minBpm, maxBpm)
        }
        
        return this
    }
    
    fun build(): QueryResult {
        val queryBuilder = StringBuilder("SELECT * FROM tracks")
        
        if (conditions.isNotEmpty()) {
            queryBuilder.append(" WHERE ")
            queryBuilder.append(conditions.joinToString(" AND "))
        }
        
        orderBy?.let {
            queryBuilder.append(" ORDER BY ")
            queryBuilder.append(it)
        }
        
        limit?.let {
            queryBuilder.append(" LIMIT ")
            queryBuilder.append(it)
        }
        
        return QueryResult(
            query = queryBuilder.toString(),
            args = if (args.isNotEmpty()) args.toTypedArray() else null
        )
    }
    
    fun reset(): MusicQueryBuilder {
        conditions.clear()
        args.clear()
        orderBy = null
        limit = null
        return this
    }
    
    data class QueryResult(
        val query: String,
        val args: Array<String>?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as QueryResult
            
            if (query != other.query) return false
            if (args != null) {
                if (other.args == null) return false
                if (!args.contentEquals(other.args)) return false
            } else if (other.args != null) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = query.hashCode()
            result = 31 * result + (args?.contentHashCode() ?: 0)
            return result
        }
    }
}
