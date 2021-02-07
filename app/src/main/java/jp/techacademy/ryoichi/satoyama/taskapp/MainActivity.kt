package jp.techacademy.ryoichi.satoyama.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

const val EXTRA_TASK = "MyTASK"

class MainActivity : AppCompatActivity() {
    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadListView(null)
        }
    }

    private lateinit var mTaskAdapter: TaskAdapter

    //SearchViewのリスナー
    private val mSearchViewListener = object  : SearchView.OnQueryTextListener{
        override fun onQueryTextChange(newText: String?): Boolean {
            if(newText == "") {
                reloadListView(null)
            } else {
                reloadListView(newText)
            }
            return true
        }

        override fun onQueryTextSubmit(query: String?): Boolean {
            return false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            val intent = Intent(this, InputActivity::class.java)
            startActivity(intent)
        }

        //SearchViewのリスナー登録
        categorySearchView.setOnQueryTextListener(mSearchViewListener)

        //Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        mTaskAdapter = TaskAdapter(this)

        //リストのアイテム押下時の処理
        listView1.setOnItemClickListener { parent, view, position, id ->
            //入力・編集画面への遷移
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }


        //リストのアイテム長押し押下時の処理
        listView1.setOnItemLongClickListener { parent, view, position, id ->
            //タスク削除
            val task = parent.adapter.getItem(position) as Task

            val builder = AlertDialog.Builder(this)
            builder.setTitle("削除")
            builder.setMessage(task.title + "を削除しますか")

            builder.setPositiveButton("OK"){dialog, which ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()
                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)

                reloadListView(null)
            }

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()

            true
        }

//        reloadListView()
        reloadListView(null)
    }

//    private fun reloadListView() {
//        val taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)
//        mTaskAdapter.mTaskList = mRealm.copyFromRealm(taskRealmResults)
//        listView1.adapter = mTaskAdapter
//        mTaskAdapter.notifyDataSetChanged()
//    }

    private fun reloadListView(query: String?) {
        var taskRealmResults: RealmResults<Task>
        if(query == null) {
            taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)
            Log.d("TaskApp", "Filter NULL")
        } else {
            taskRealmResults = mRealm.where(Task::class.java).equalTo("category", query.toString()).findAll().sort("date", Sort.DESCENDING)
            Log.d("TaskApp", "Filter Exist")
        }
        mTaskAdapter.mTaskList = mRealm.copyFromRealm(taskRealmResults)
        listView1.adapter = mTaskAdapter
        mTaskAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.close()
    }

    private fun addTaskForTest() {
        val task = Task()
        task.title = "作業"
        task.contents = "プログラムを書いてPUSHする"
        task.date = Date()
        task.id = 0
        mRealm.beginTransaction()
        mRealm.copyToRealmOrUpdate(task)
        mRealm.commitTransaction()
    }

}