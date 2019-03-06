package com.a65apps.clustering.core

data class ClusteredMarker(private val geoCoor: LatLng, private val payload: Any? = null) : Marker {
    val rawMarkers: MutableSet<Marker> = mutableSetOf()

    override fun getGeoCoor(): LatLng = geoCoor

    override fun getPayload(): Any? = payload

    override fun isCluster(): Boolean = rawMarkers.size > 1

    override fun getChildrenCount(): Int = rawMarkers.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClusteredMarker

        if (geoCoor != other.geoCoor) return false
        if (payload != other.payload) return false
        if (rawMarkers != other.rawMarkers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = geoCoor.hashCode()
        result = 31 * result + (payload?.hashCode() ?: 0)
        result = 31 * result + rawMarkers.hashCode()
        return result
    }
}
