package com.shinsak.travle.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Attractions
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.GolfCourse
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector
import com.shinsak.travle.data.ExpCategory
import com.shinsak.travle.data.PlaceCategory

fun ExpCategory.icon(): ImageVector = when (this) {
    ExpCategory.STAY -> Icons.Rounded.Hotel
    ExpCategory.FLIGHT -> Icons.Rounded.Flight
    ExpCategory.TRANSPORT -> Icons.Rounded.DirectionsCar
    ExpCategory.FOOD -> Icons.Rounded.Restaurant
    ExpCategory.SHOPPING -> Icons.Rounded.ShoppingBag
    ExpCategory.SIGHT -> Icons.Rounded.Attractions
    ExpCategory.GOLF -> Icons.Rounded.GolfCourse
    ExpCategory.ETC -> Icons.Rounded.MoreHoriz
}

fun PlaceCategory.icon(): ImageVector = when (this) {
    PlaceCategory.SIGHT -> Icons.Rounded.Attractions
    PlaceCategory.FOOD -> Icons.Rounded.Restaurant
    PlaceCategory.CAFE -> Icons.Rounded.LocalCafe
    PlaceCategory.SHOPPING -> Icons.Rounded.ShoppingBag
    PlaceCategory.GOLF -> Icons.Rounded.GolfCourse
    PlaceCategory.STAY -> Icons.Rounded.Hotel
    PlaceCategory.TRANSPORT -> Icons.Rounded.DirectionsCar
    PlaceCategory.ETC -> Icons.Rounded.Place
}

/** 장소 카테고리 → 지출 카테고리 (장소 예상비용을 지출로 옮길 때) */
fun PlaceCategory.toExp(): ExpCategory = when (this) {
    PlaceCategory.SIGHT -> ExpCategory.SIGHT
    PlaceCategory.FOOD, PlaceCategory.CAFE -> ExpCategory.FOOD
    PlaceCategory.SHOPPING -> ExpCategory.SHOPPING
    PlaceCategory.GOLF -> ExpCategory.GOLF
    PlaceCategory.STAY -> ExpCategory.STAY
    PlaceCategory.TRANSPORT -> ExpCategory.TRANSPORT
    PlaceCategory.ETC -> ExpCategory.ETC
}
