package com.cherryzp.animalshelter.ui.common

import com.cherryzp.animalshelter.data.api.ApiEndpoint
import com.cherryzp.animalshelter.data.repository.ImageUrlPolicy
import com.cherryzp.animalshelter.model.Animal
import com.cherryzp.animalshelter.model.statusOn
import kotlinx.datetime.LocalDate

/**
 * [Animal] → [AnimalCardUi]. `today`는 `clock.todayInSeoul()`, `endpoint`는 `EndpointSelector.current()`
 * (Relay면 사진이 중계 `/img`를 거친다, 02-data 5.3).
 */
fun Animal.toCardUi(today: LocalDate, endpoint: ApiEndpoint, isBookmarked: Boolean): AnimalCardUi =
    AnimalCardUi(
        desertionNo = desertionNo,
        happenDate = happenDate,
        photoUrl = photos.firstOrNull()?.let { ImageUrlPolicy.displayUrl(it, endpoint) },
        kind = kind,
        status = statusOn(today).toStatusUi(),
        title = titleParts(),
        shelterLine = shelterLine(),
        traits = traitParts(),
        happenPlace = happenPlace?.trim()?.takeIf { it.isNotEmpty() },
        isBookmarked = isBookmarked
    )
