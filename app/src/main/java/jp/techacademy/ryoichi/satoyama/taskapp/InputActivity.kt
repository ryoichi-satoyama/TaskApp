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
//import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_input.*
import kotlinx.android.synthetic.main.content_input.categorySpinner
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

        // ActionBarを設定する
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        if(supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        // UI部品の設定
        date_button.setOnClickListener(mOnDateClickListener)
        times_button.setOnClickListener(mOnTimeClickListener)
        done_button.setOnClickListener(mOnDoneClickListener)
        addCategoryButton.setOnClickListener(mOnCategoryClickListener)

        //MainActivityからのIntentオブジェクトを取得し、対象のタスクオブジェクトをRealmから取得
        val intent = intent
        val taskId = intent.getIntExtra(EXTRA_TASK, -1)
        val realm = Realm.getDefaultInstance()
        mTask = realm.where(Task::class.java).equalTo("id", taskId).findFirst()
        realm.close()

        //カテゴリ一覧の取得
        getCategory()


        //mTaskがnullの場合、タスクの新規作成
        //null出ない場合、タスクの編集
        if(mTask == null) {
            //現在の時刻情報を取得
            val calendar = Calendar.getInstance()
            mYear = calendar.get(Calendar.YEAR)
            mMonth = calendar.get(Calendar.MONTH)
            mDay = calendar.get(Calendar.DAY_OF_MONTH)
            mHour = calendar.get(Calendar.HOUR_OF_DAY)
            mMinute = calendar.get(Calendar.MINUTE)
        } else {
            //対象タスクのカテゴリIDを取得し、カテゴリスピナーを選択する
            val categoryId = mTask!!.category?.id
            var index = 0
            for(i in categories) {
                if(categoryId == i.id) {
                    break
                }
                index++
            }
            categorySpinner.setSelection(index)

            //対象タスクの現在の値を取得し、表示する
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

    //カテゴリ一覧を取得し、カテゴリスピナーにセット
    private fun getCategory() {
        val realm = Realm.getDefaultInstance()

        //全カテゴリを取得
        val categoryRealmResults: RealmResults<Category>
        categoryRealmResults = realm.where(Category::class.java).findAll().sort("id", Sort.ASCENDING)
        categories = realm.copyFromRealm(categoryRealmResults)

        //カテゴリをカテゴリスピナーにセット
        val adapter = ArrayAdapter<Category>(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
        realm.close()
    }

    //タスク追加・更新処理
    private fun addTask() {
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()

        //タスクが新規作成の場合、タスクオブジェクトを作成
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

        //入力された値の取得
        val title = title_edit_text.text.toString()
        val content = content_edit_text.text.toString()

        //カテゴリの取得
        val item = categorySpinner.selectedItem.toString()
        val results = realm.where(Category::class.java).equalTo("name", item).findFirst()
        val category: Category? = realm.copyFromRealm(results)

        //タスクオブジェクトに各値をセット
        mTask!!.title = title
        mTask!!.contents = content
        val calendar = GregorianCalendar(mYear, mMonth, mDay, mHour, mMinute)
        val date = calendar.time
        mTask!!.date = date
        mTask!!.category = category

        //Realmへの追加・更新
        realm.copyToRealmOrUpdate(mTask!!)
        realm.commitTransaction()
        realm.close()

        //アラームセット
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


    override fun onRestart() {
        super.onRestart()
        //画面復帰時にカテゴリスピナーを更新
        getCategory()
    }
}