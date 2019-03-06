package com.a65apps.clustering.yandex.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import com.a65apps.clustering.core.Clusters
import com.a65apps.clustering.core.LatLng
import com.a65apps.clustering.core.Marker
import com.a65apps.clustering.core.view.ClusterRenderer
import com.a65apps.clustering.yandex.extention.addPlacemark
import com.a65apps.clustering.yandex.extention.toPoint
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject

class YandexClusterRenderer(
        map: Map,
        private val imageProvider: ClusterPinProvider,
        private var withAnimation: Boolean = false)
    : ClusterRenderer {
    private val layer: MapObjectCollection = map.addMapObjectLayer(LAYER_NAME)
    private val mapObjects = mutableMapOf<Marker, PlacemarkMapObject>()

    companion object {
        const val LAYER_NAME = "CLUSTER_LAYER"
    }

    override fun updateClusters(clusters: Clusters) {
        if (clusters.actualMarkers.isEmpty()) {
            simpleUpdate(clusters)
        } else {
            if (clusters.isCollapsed) {
                for ((cluster, markers) in clusters.transitions) {
                    markersToCluster(cluster, markers)
                }
            } else {
                for ((cluster, markers) in clusters.transitions) {
                    clusterToMarkers(cluster, markers)
                }
            }
        }
    }

    override fun setMarkers(markers: Set<Marker>) {
        markers.forEach {
            createPlacemark(it)
        }
    }

    override fun animation(withAnimation: Boolean) {
        this.withAnimation = withAnimation
    }

    override fun onAdd() {
        //проброс ЖЦ
    }

    override fun onRemove() {
        //проброс ЖЦ
    }

    private fun simpleUpdate(clusters: Clusters) {
        layer.clear()
        for (marker in clusters.newMarkers) {
            createPlacemark(marker)
        }
    }

    //Вызывается для перемещения маркеров в кластер
    private fun markersToCluster(cluster: Marker, markers: Set<Marker>) {
        if (withAnimation) {
            animateMarkersToClusterWith(cluster, markers)
        } else {
            setMarkersToCluster(cluster, markers)
        }
    }

    //Перемещение маркеров в кластер с анимацией
    private fun animateMarkersToClusterWith(cluster: Marker, markers: Set<Marker>) {
        //коллекция маркеров которые будут анимироваться в кластер
        val movedMarkers = mutableListOf<PlacemarkMapObject>()
        val startCoordinates = mutableListOf<LatLng>()
        markers.forEach { marker ->
            mapObjects[marker]?.let { mapObject ->
                movedMarkers.add(mapObject)
                startCoordinates.add(marker.getGeoCoor())
            }
        }
        val clusterPoint = cluster.getGeoCoor().toPoint()
        createPlacemark(cluster)

        val animator = ValueAnimator.ofFloat(0f, 1f)
        val animatorUpdateListener = ValueAnimator.AnimatorUpdateListener {
            val factor = it.animatedValue as Float
            for (i in 0 until movedMarkers.size) {
                val mapObject = movedMarkers[i]
                val start = startCoordinates[i]

                val kx = clusterPoint.latitude - start.latitude
                val ky = clusterPoint.longitude - start.longitude

                val point = Point(start.latitude + (factor * kx),
                        start.longitude + (factor * ky))
                updateObjectGeometry(mapObject, point)
            }
        }
        animator.addUpdateListener(animatorUpdateListener)
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                removePlacemarks(markers)
                animator.removeUpdateListener(animatorUpdateListener)
                animator.removeListener(this)
            }
        })
        animator.start()
    }

    //Перемещение маркеров в кластер без анимации
    private fun setMarkersToCluster(cluster: Marker, markers: Set<Marker>) {
        createPlacemark(cluster)
        removePlacemarks(markers)
    }

    //Вызывается для перемещения маркеров из кластера
    private fun clusterToMarkers(cluster: Marker, markers: Set<Marker>) {
        if (withAnimation) {
            animateClusterToMarkers(cluster, markers)
        } else {
            setClusterToMarkers(cluster, markers)
        }
    }

    //Перемещение маркеров из кластера с анимацией
    private fun animateClusterToMarkers(cluster: Marker, markers: Set<Marker>) {
        removePlacemark(cluster)

        //коллекция маркеров которые будут анимироваться в кластер
        val movedMarkers = mutableListOf<PlacemarkMapObject>()
        val to = mutableListOf<Point>()
        val clusterPoint = cluster.getGeoCoor()
        markers.forEach {
            movedMarkers.add(createPlacemark(it, clusterPoint))
            to.add(it.getGeoCoor().toPoint())
        }

        val animator = ValueAnimator.ofFloat(0f, 1f)
        val animatorUpdateListener = ValueAnimator.AnimatorUpdateListener {
            val factor = it.animatedValue as Float
            for (i in 0 until movedMarkers.size) {
                val mapObject = movedMarkers[i]
                val point = to[i]
                val kx = point.latitude - clusterPoint.latitude
                val ky = point.longitude - clusterPoint.longitude
                val lat = clusterPoint.latitude + (factor * kx)
                val lon = clusterPoint.longitude + (factor * ky)
                updateObjectGeometry(mapObject, Point(lat, lon))
            }
        }
        animator.addUpdateListener(animatorUpdateListener)
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                animator.removeUpdateListener(animatorUpdateListener)
                animator.removeListener(this)
            }
        })
        animator.start()
    }

    //Перемещение маркеров из кластера без анимации
    private fun setClusterToMarkers(cluster: Marker, markers: Set<Marker>) {
        removePlacemark(cluster)
        markers.forEach { createPlacemark(it) }
    }

    private fun removePlacemarks(markers: Set<Marker>) {
        for (marker in markers) {
            removePlacemark(marker)
        }
    }

    private fun removePlacemark(marker: Marker) {
        mapObjects[marker]?.let {
            try {
                layer.remove(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            mapObjects.remove(marker)
        }
    }

    private fun createPlacemark(marker: Marker): PlacemarkMapObject {
        return createPlacemark(marker, marker.getGeoCoor())
    }

    private fun createPlacemark(marker: Marker, coords: LatLng): PlacemarkMapObject {
        val image = imageProvider.get(marker)
        val placemark = layer.addPlacemark(coords, image.provider(), image.style)
        mapObjects[marker] = placemark
        return placemark
    }

    private fun updateObjectGeometry(mapObject: PlacemarkMapObject, point: Point) {
        try {
            mapObject.geometry = point
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
