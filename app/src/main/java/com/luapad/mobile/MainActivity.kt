package com.luapad.mobile

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.*
import android.text.InputType

class MainActivity : Activity() {
    private lateinit var editor: EditText
    private lateinit var lineNumbers: TextView
    private lateinit var fileLabel: TextView
    private lateinit var status: TextView
    private var currentUri: Uri? = null
    private var currentName = "untitled.lua"
    private var highlighting = false
    private val bg = Color.rgb(16,17,22)
    private val panel = Color.rgb(27,29,38)
    private val muted = Color.rgb(125,132,150)
    private val fg = Color.rgb(220,224,235)
    private val keywords = setOf("and","break","do","else","elseif","end","false","for","function","goto","if","in","local","nil","not","or","repeat","return","then","true","until","while")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildUi()
        editor.setText("-- LuaPad Mobile :3\n-- Open or create a Lua script\n\nlocal function hello()\n    print(\"meow!\")\nend\n\nhello()\n")
        editor.setSelection(editor.text.length)
    }

    private fun dp(n:Int)= (n*resources.displayMetrics.density).toInt()
    private fun button(label:String, action:()->Unit)=TextView(this).apply {
        text=label; textSize=12f; setTextColor(Color.WHITE); gravity=Gravity.CENTER
        setPadding(dp(10),dp(9),dp(10),dp(9)); setBackgroundColor(panel)
        setOnClickListener { action() }
    }
    private fun buildUi() {
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding(dp(10),dp(8),dp(10),dp(6)) }
        val top=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        top.addView(TextView(this).apply { text="LuaPad"; textSize=22f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        fileLabel=TextView(this).apply { text=currentName; textSize=12f; setTextColor(muted); setPadding(dp(10),0,0,0) }
        top.addView(fileLabel,LinearLayout.LayoutParams(0,-2,1f))
        top.addView(button("OPEN",::openFile)); top.addView(View(this),LinearLayout.LayoutParams(dp(5),1))
        top.addView(button("SAVE",::saveFile)); root.addView(top)
        val tools=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(0,dp(9),0,dp(8)) }
        tools.addView(button("FIND",::findText)); tools.addView(View(this),LinearLayout.LayoutParams(dp(6),1))
        tools.addView(button("REPLACE",::replaceText)); tools.addView(View(this),LinearLayout.LayoutParams(dp(6),1))
        tools.addView(button("UNDO",::undo)); root.addView(tools)
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setBackgroundColor(Color.rgb(20,22,29)) }
        lineNumbers=TextView(this).apply { text="1"; textSize=13f; typeface=Typeface.MONOSPACE; setTextColor(muted); gravity=Gravity.TOP or Gravity.END; setPadding(dp(8),dp(12),dp(8),dp(12)) }
        editor=EditText(this).apply {
            setTextColor(fg); setHintTextColor(muted); hint="-- write Lua here"; textSize=14f
            typeface=Typeface.MONOSPACE; gravity=Gravity.TOP or Gravity.START
            setPadding(dp(8),dp(12),dp(8),dp(20)); setBackgroundColor(Color.TRANSPARENT)
            inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            isSingleLine=false; setHorizontallyScrolling(true); setTextIsSelectable(true)
        }
        row.addView(lineNumbers,LinearLayout.LayoutParams(-2,-1)); row.addView(editor,LinearLayout.LayoutParams(0,-1,1f))
        root.addView(row,LinearLayout.LayoutParams(-1,0,1f))
        status=TextView(this).apply { text="Lua • UTF-8 • Ready"; textSize=11f; setTextColor(muted); setPadding(dp(4),dp(8),dp(4),dp(2)) }
        root.addView(status); setContentView(root)
        editor.addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int) {}
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) { updateLines(); updateStatus() }
            override fun afterTextChanged(s:Editable?) { if(!highlighting) highlight() }
        })
        updateLines()
    }
    private fun updateLines() { if(!::editor.isInitialized)return; lineNumbers.text=(1..(editor.text.toString().count{it=='\n'}+1)).joinToString("\n") }
    private fun updateStatus() {
        if(!::status.isInitialized)return
        val p=editor.selectionStart.coerceAtLeast(0); val before=editor.text.substring(0,p)
        status.text="Lua • Ln ${before.count{it=='\n'}+1}, Col ${p-(before.lastIndexOf('\n')+1)+1} • ${editor.length()} chars"
    }
    private fun highlight() {
        val e=editor.text; val src=e.toString(); val a=editor.selectionStart; val b=editor.selectionEnd
        highlighting=true
        e.setSpan(ForegroundColorSpan(fg),0,e.length,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val rx=Regex("--[^\\n]*|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|\\b[A-Za-z_][A-Za-z0-9_]*\\b|\\b\\d+\\b")
        for(m in rx.findAll(src)) {
            val t=m.value
            val c=when { t.startsWith("--")->Color.rgb(106,153,85); t.startsWith("\"")||t.startsWith("'")->Color.rgb(206,145,120); t.firstOrNull()?.isDigit()==true->Color.rgb(181,206,168); t in keywords->Color.rgb(197,134,192); else->null }
            if(c!=null)e.setSpan(ForegroundColorSpan(c),m.range.first,m.range.last+1,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        highlighting=false
        if(a>=0&&b>=0&&a<=e.length&&b<=e.length) editor.setSelection(a,b)
    }
    private fun openFile() { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="*/*" },100) }
    private fun saveFile() {
        val u=currentUri
        if(u!=null) try { contentResolver.openOutputStream(u,"wt")!!.bufferedWriter().use{it.write(editor.text.toString())}; toast("Saved $currentName") }
        catch(e:Exception){toast("Save failed: ${e.message}")}
        else startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="text/plain"; putExtra(Intent.EXTRA_TITLE,currentName) },101)
    }
    @Deprecated("Deprecated")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data); if(resultCode!=RESULT_OK)return
        val u=data?.data?:return
        try {
            if(requestCode==100) {
                currentUri=u; contentResolver.takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                editor.setText(contentResolver.openInputStream(u)!!.bufferedReader().use{it.readText()})
                currentName=u.lastPathSegment?.substringAfterLast('/')?:"script.lua"; fileLabel.text=currentName
            } else if(requestCode==101) {
                currentUri=u; contentResolver.openOutputStream(u,"wt")!!.bufferedWriter().use{it.write(editor.text.toString())}
                currentName=u.lastPathSegment?.substringAfterLast('/')?:"script.lua"; fileLabel.text=currentName; toast("Saved $currentName")
            }
        } catch(e:Exception){toast("File error: ${e.message}")}
    }
    private fun findText() {
        val input=EditText(this).apply{hint="Text to find";setTextColor(fg);setHintTextColor(muted)}
        android.app.AlertDialog.Builder(this).setTitle("Find").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Find"){_,_->
            val q=input.text.toString(); if(q.isNotEmpty()){val p=editor.text.indexOf(q,(editor.selectionStart+1).coerceAtMost(editor.length()).coerceAtLeast(0)); if(p>=0)editor.setSelection(p,p+q.length) else toast("Not found")}
        }.show()
    }
    private fun replaceText() {
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(8),dp(18),0)}
        val f=EditText(this).apply{hint="Find";setTextColor(fg)}; val r=EditText(this).apply{hint="Replace with";setTextColor(fg)}
        box.addView(f);box.addView(r)
        android.app.AlertDialog.Builder(this).setTitle("Replace all").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Replace"){_,_->if(f.text.isNotEmpty())editor.setText(editor.text.toString().replace(f.text.toString(),r.text.toString()))}.show()
    }
    private fun undo() { toast("Use your keyboard's undo if available; full undo history is coming in the next update.") }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
