package com.riva.mods.data.auth.mapper

import com.riva.mods.data.auth.dto.AuthData
import com.riva.mods.data.auth.dto.UserDto
import com.riva.mods.domain.model.User

fun UserDto.toDomain(): User = User(
    id = id,
    email = email,
    name = name,
    avatarUrl = avatarUrl,
    role = role,
    createdAt = createdAt
)

fun AuthData.toDomainUser(): User = user.toDomain()
