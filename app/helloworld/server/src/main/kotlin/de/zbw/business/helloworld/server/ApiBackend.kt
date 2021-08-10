package de.zbw.business.helloworld.server

/**
 * Backend implementation of REST-API.
 *
 * Created on 05-05-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object ApiBackend {
    fun getUserInformation(userId: Int): UserInformation {
        return UserInformation(EXAMPLE_USER, userId.toLong())
    }

    const val EXAMPLE_USER = "Max Mustermann"
}