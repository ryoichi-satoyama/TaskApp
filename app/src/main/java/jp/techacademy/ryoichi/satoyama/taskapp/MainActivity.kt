package jp.techacademy.ryoichi.satoyama.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*

const val EXTRA_TASK = "MyTASK"

class MainActivity : AppCompatActivity() {
    private lateinit var mRealm: Realm
    private lateinit var categories: MutableList<Category>
    private lateinit var mTaskAdapter: TaskAdapter

    private val mRealmListener = RealmChangeListener<Realm> { reloadListView() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            val intent = Intent(this, InputActivity::class.java)
            startActivity(intent)
        }

        //カテゴリ一覧の取得
        getCategory()

        //カテゴリスピナー選択時の処理（タスクのフィルター)
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if(position == 0) {
                    //カテゴリ未選択時の処理
                    //フィルターなし
                    reloadListView()
                } else {
                    //カテゴリ選択時の処理
                    //フィルターあり
                    val item = categorySpinner.selectedItem.toString()
                    mRealm = Realm.getDefaultInstance()
                    val results = mRealm.where(Category::class.java).equalTo("name", item).findFirst()
                    val category: Category? = mRealm.copyFromRealm(results)
                    reloadListView(category)
                }
            }
        }

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

                reloadListView()
            }

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()

            //trueはsetOnItemClickListenerを含めない
            true
        }

        reloadListView()
    }



    //タスクListViewの更新
    private fun reloadListView(category: Category? = null) {
        val taskRealmResults: RealmResults<Task>
        val taskList = mutableListOf<Task>()

        //カテゴリが選択されている場合、フィルターを行う
        if(category == null) {
            taskList.addAll(mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING))
        } else {
//            taskRealmResults = mRealm.where(Task::class.java).equalTo("category", category).findAll().sort("date", Sort.DESCENDING)
            taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)

            //カテゴリにマッチするタスクのみリストに追加
            for(task in taskRealmResults) {
                val c: Category? = task.category
                if(c?.id == category?.id) {
                    taskList.add(task)
                }
            }
        }

        mTaskAdapter.mTaskList = taskList
        listView1.adapter = mTaskAdapter
        mTaskAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.close()
    }

    //カテゴリ一覧を取得し、カテゴリスピナーにセット
    private fun getCategory() {
        val realm = Realm.getDefaultInstance()
        val categoryRealmResults: RealmResults<Category>
        categoryRealmResults = realm.where(Category::class.java).findAll().sort("id", Sort.ASCENDING)

        categories = realm.copyFromRealm(categoryRealmResults)

        //検索フィルターなし用ダミーカテゴリを追加
        val dummyCategory = Category()
        dummyCategory.name = "All"
        categories.add(0,dummyCategory)

        val adapter = ArrayAdapter<Category>(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
        realm.close()
    }

    override fun onRestart() {
        super.onRestart()
        //画面復帰時にカテゴリスピナーを更新
        getCategory()
    }

}