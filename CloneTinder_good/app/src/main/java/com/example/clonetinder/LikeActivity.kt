package com.example.clonetinder

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LikeActivity: AppCompatActivity() {

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var userDB: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_like)

        userDB = Firebase.database.reference.child("Users")
        val currentUserDB = userDB.child(getCurrentUserID())
        currentUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.child("name").value==null) { //name에 대한 데이터 값이 있는가?

                    showNameInputPopup()
                    return
                }

                //유저 정보 갱신하기
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        }) //파이어베이스로부터 User 데이터 가져오기
    }

    private fun showNameInputPopup() {

        val editText = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("이름을 입력해주세요.")
            .setView(editText) //AlertDialog에서 EditText 사용 가능
            .setPositiveButton("저장") { _, _ ->
                if(editText.text.isEmpty()) {
                    showNameInputPopup()
                } else {
                    saveUserName(editText.text.toString())
                }
            } //EditText에 name을 저장했을 시 DB 등록 처리
            .setCancelable(false) //취소 불가
            .show()
    }

    private fun saveUserName(name: String) {
        val userId = getCurrentUserID()
        val currentUserDB = userDB.child(userId)
        //Users라는 데이터베이스의 userId 데이터 호출

        val user = mutableMapOf<String,Any>()
        user["userId"] = userId
        user["name"] = name
        currentUserDB.updateChildren(user)
        //user의 ID와 name을 DB에 등록
    }

    private fun getCurrentUserID() : String {

        if(auth.currentUser == null) {
            Toast.makeText(this,"로그인이 되어있지 않습니다!",Toast.LENGTH_SHORT).show()
            finish()
        }

        return auth.currentUser?.uid.orEmpty()
    } //로그인이 되어있는지를 확인하는 절차
}