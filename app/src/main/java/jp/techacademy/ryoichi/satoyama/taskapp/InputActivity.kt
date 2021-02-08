package jp.techacademy.ryoichi.satoyama.taskapp

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.widget.Toolbar
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_input.*
import java.util.*
import kotlin.collections.ArrayList

class InputActivity : AppCompatActivity() {
    private var mYear = 0
    private var mMonth = 0
    private var mDay = 0
    private var mHour = 0
    private var mMinute = 0
    private var mTask: Task? = null
    private lateinit var categories: List<Category>

    private val mOnDateClickListener = View.OnClickListener {
        val datePickerDialog = DatePickerDialog(this,
        DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            mYear = year
            mMonth = month
            mDay = dayOfMonth
            val dateString = mYear.toString() + "/" + String.format("%02d", mMonth + 1) + "/" + String.format("%02d", mDay)
            date_button.text = dateString
        }, mYear, mMonth, mDay)
        datePickerDialog.show()
    }

    private val mOnTimeClickListener = View.OnClickListener {
        val timePickerDialog = TimePickerDialog(this,
        TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
            mHour = hourOfDay
            mMinute = minute
            val timeString = String.format("%02d", mHour) + ":" + String.format("%02d", mMinute)
            times_button.text = timeString
        }, mHour, mMinute, false)
        timePickerDialog.show()
    }

    private val mOnDoneClickListener = View.OnClickListener {
        addTask()
        finish()
    }

    private val mOnCategoryClickListener = View.OnClickListener {
        val intent = Intent(this, CategoryActivity::class.java)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        if(supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        date_button.setOnClickListener(mOnDateClickListener)
        times_button.setOnClickListener(mOnTimeClickListener)
        done_button.setOnClickListener(mOnDoneClickListener)
        addCategoryButton.setOnClickListener(mOnCategoryClickListener)




        val intent = intent
        val taskId = intent.getIntExtra(EXTRA_TASK, -1)
        val realm = Realm.getDefaultInstance()
        mTask = realm.where(Task::class.java).equalTo("id", taskId).findFirst()
        realm.close()


        //カテゴリ取得
        getCategory()


        if(mTask == null) {
            val calendar = Calendar.getInstance()
            mYear = calendar.get(Calendar.YEAR)
            mMonth = calendar.get(Calendar.MONTH)
            mDay = calendar.get(Calendar.DAY_OF_MONTH)
            mHour = calendar.get(Calendar.HOUR_OF_DAY)
            mMinute = calendar.get(Calendar.MINUTE)
        } else {
//            カテゴリの表示
//            category_edit_text.setText(mTask!!.category)
//            val categoryName = mTask!!.category?.name
            val categoryId = mTask!!.category?.id

            var num = 0
            for(i in categories) {
                if(categoryId == i.id) {
                    num = i.id
                }
            }
            categorySpinner

            title_edit_text.setText(mTask!!.title)
            content_edit_text.setText(mTask!!.contents)

            val calendar = Calendar.getInstance()
            calendar.time = mTask!!.date
            mYear = calendar.get(Calendar.YEAR)
            mMonth = calendar.get(Calendar.MONTH)
            mDay = calendar.get(Calendar.DAY_OF_MONTH)
            mHour = calendar.get(Calendar.HOUR_OF_DAY)
            mMinute = calendar.get(Calendar.MINUTE)

            val dateString = mYear.toString() + "/" + String.format("%02d", mMonth) + "/" + String.format("%02d", mDay)
            val timeString = String.format("%02d", mHour) + ":" + String.format("%02d", mMinute)

            date_button.text = dateString
            times_button.text = timeString
        }
    }

    //カテゴリを取得し、Spinnerにセット
    private fun getCategory() {
        val realm = Realm.getDefaultInstance()
        val categoryRealmResults: RealmResults<Category>
        categoryRealmResults = realm.where(Category::class.java).findAll().sort("id", Sort.ASCENDING)
//        val categories = realm.copyFromRealm(categoryRealmResults)
        categories = realm.copyFromRealm(categoryRealmResults)
        val adapter = ArrayAdapter<Category>(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
        realm.close()
    }

    private fun addTask() {
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()

        if(mTask == null) {
            mTask = Task()

            val taskRealmResults = realm.where(Task::class.java).findAll()

            val identifier: Int =
                if(taskRealmResults.max("id") != null) {
                    taskRealmResults.max("id")!!.toInt() + 1
                } else {
                    0
                }
            mTask!!.id = identifier
        }

        //カテゴリの選択
//        val category = category_edit_text.text.toString()
        val title = title_edit_text.text.toString()
        val content = content_edit_text.text.toString()

//        mTask!!.category = category
        mTask!!.title = title
        mTask!!.contents = content
        val calendar = GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute)
        val date = calendar.time
        mTask!!.date = date

        realm.copyToRealmOrUpdate(mTask!!)
        realm.commitTransaction()

        realm.close()

        val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
        resultIntent.putExtra(EXTRA_TASK, mTask!!.id)
        val resultPendingIntent = PendingIntent.getBroadcast(
            this,
            mTask!!.id,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, resultPendingIntent)
    }

//    private fun addCategoryTest() {
//        val category = Category()
//        category.name = "abc"
//        category.id = 0
//        val realm = Realm.getDefaultInstance()
//        realm.beginTransaction()
//        realm.copyToRealmOrUpdate(category)
//        realm.commitTransaction()
//    }

    override fun onRestart() {
        super.onRestart()
        getCategory()
    }
}