package jp.techacademy.ryoichi.satoyama.taskapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(context: Context) : BaseAdapter() {
    var mTaskList = mutableListOf<Task>()
    private val mLayoutInflater : LayoutInflater

    init {
        this.mLayoutInflater = LayoutInflater.from(context)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
//        val view: View = convertView ?: mLayoutInflater.inflate(android.R.layout.simple_list_item_2, null)
        val view: View = convertView ?: mLayoutInflater.inflate(R.layout.layout_list, null)

//        val textView1 = view.findViewById<TextView>(android.R.id.text1)
//        val textView2 = view.findViewById<TextView>(android.R.id.text2)

        val categoryTextView = view.findViewById<TextView>(R.id.categoryTextView)
        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)
        val dateTextView = view.findViewById<TextView>(R.id.dateTextView)

//        textView1.text = mTaskList[position].title
        categoryTextView.text = "カテゴリ：" + mTaskList[position].category
        titleTextView.text = "タイトル：" + mTaskList[position].title

        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.JAPANESE)
        val date = mTaskList[position].date
//        textView2.text = simpleDateFormat.format(date)
        dateTextView.text = simpleDateFormat.format(date)

        return view
    }

    override fun getItem(position: Int): Any {
        return mTaskList[position]
    }

    override fun getItemId(position: Int): Long {
        return mTaskList[position].id.toLong()
    }

    override fun getCount(): Int {
        return mTaskList.size
    }
}