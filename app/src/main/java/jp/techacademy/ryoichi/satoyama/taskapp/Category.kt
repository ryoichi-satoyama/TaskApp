package jp.techacademy.ryoichi.satoyama.taskapp

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.io.Serializable

open class Category : RealmObject(), Serializable {
    var name: String = ""
    @PrimaryKey
    var id: Int = 0

    override fun toString(): String {
        return name
    }
}