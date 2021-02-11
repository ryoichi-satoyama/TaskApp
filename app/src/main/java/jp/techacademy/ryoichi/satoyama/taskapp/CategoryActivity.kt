package jp.techacademy.ryoichi.satoyama.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_category.*
import kotlinx.android.synthetic.main.content_input.*
import java.util.*

class CategoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        addButton.setOnClickListener {
            addCategory()
            Toast.makeText(this, "カテゴリを追加しました", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun addCategory() {
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()

        val category = Category()
        val categoryRealmResults = realm.where(Category::class.java).findAll()

        val identifier: Int =
            if (categoryRealmResults.max("id") != null) {
                categoryRealmResults.max("id")!!.toInt() + 1
            } else {
                1
            }
        category.id = identifier
        category.name = categoryEditText.text.toString()

        realm.copyToRealmOrUpdate(category)
        realm.commitTransaction()
        realm.close()
    }
}