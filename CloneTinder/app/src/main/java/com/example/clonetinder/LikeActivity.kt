package com.example.clonetinder

import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction

class LikeActivity: AppCompatActivity(), CardStackListener {

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var userDB: DatabaseReference
    private val adapter = CardItemAdapter()
    private val cardItems = mutableListOf<CardItem>()
    //카드 아이템들을 저장할 리스트 선언
    private val manager by lazy {
        CardStackLayoutManager(this,this)
    }

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
                getUnselectedUsers()
            }
            override fun onCancelled(error: DatabaseError) {}
        }) //파이어베이스로부터 User 데이터 가져오기
        initCardStackView()
    }

    private fun initCardStackView() {
        val stackView = findViewById<CardStackView>(R.id.cardStackView)
        stackView.layoutManager = manager
        stackView.adapter = adapter
    }

    private fun getUnselectedUsers() {
        userDB.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if(snapshot.child("userId").value!= getCurrentUserID() //UserId가 본인과 같지 않도록 설정
                    && snapshot.child("likedBy").child("like").hasChild(getCurrentUserID()).not()
                    //likedBy의 like 데이터를 통해 본인에 해당하는 값이 있지 않도록 설정
                    && snapshot.child("likedBy").child("disLike").hasChild(getCurrentUserID()).not()
                    //likedBy의 disLike 데이터를 통해 본인에 해당하는 값이 있지 않도록 설정
                    //이유 : 나 자신을 평가할 수 없도록 하기 위함
                ) {
                    val userId = snapshot.child("userId").value.toString()
                    var name = "unDecided"
                    //유저의 ID와 Name 호출
                    if(snapshot.child("name").value != null) {
                        name = snapshot.child("name").value.toString()
                    }

                    cardItems.add(CardItem(userId,name))
                    //카드 아이템 목록 추가
                    adapter.submitList(cardItems)
                    //adapter에 바꾸니 카드 아이템 목록 올리기
                    adapter.notifyDataSetChanged()
                    //데이터 갱신
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                cardItems.find { it.userId == snapshot.key }?.let {
                    it.name = snapshot.child("name").value.toString() }
                adapter.submitList(cardItems)
                adapter.notifyDataSetChanged()
                //상대방의 정보가 바뀌었을 때 데이터 갱신
            }

           override fun onChildRemoved(snapshot: DataSnapshot) {}

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {}

        } )
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

        getUnselectedUsers()
    }

    private fun getCurrentUserID() : String {

        if(auth.currentUser == null) {
            Toast.makeText(this,"로그인이 되어있지 않습니다!",Toast.LENGTH_SHORT).show()
            finish()
        }

        return auth.currentUser?.uid.orEmpty()
    } //로그인이 되어있는지를 확인하는 절차

    private fun like() {
        val card = cardItems[manager.topPosition - 1]
        //인덱스 값이 0번부터 시작하므로 -1
        cardItems.removeFirst()
        //좋아요를 누른 아이템은 목록에서 삭제

        userDB.child(card.userId)
            .child("likedBy")
            .child("like")
            .child(getCurrentUserID())
            .setValue(true)
        //좋아요를 눌렀다는 데이터를 DB에 저장

        saveMatchIfOtherUserLikedMe(card.userId)

        Toast.makeText(this, "${card.name}님을 Like 하셨습니다!",Toast.LENGTH_SHORT).show()
    }

    private fun disLike() {
        val card = cardItems[manager.topPosition - 1]
        //인덱스 값이 0번부터 시작하므로 -1
        cardItems.removeFirst()
        //좋아요를 누른 아이템은 목록에서 삭제

        userDB.child(card.userId)
            .child("likedBy")
            .child("dislike")
            .child(getCurrentUserID())
            .setValue(true)
        //dislike를 눌렀다는 데이터를 DB에 저장
    }

    private fun saveMatchIfOtherUserLikedMe(otherUserId: String) {
        val isUserLikedMe = userDB.child(getCurrentUserID()).child("likedBy").child("like").child(otherUserId)
        //나를 좋아요한 유저의 ID를 가져오기 위한 변수

        isUserLikedMe.addListenerForSingleValueEvent(object :ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.value == true) { //나를 좋아요한 상대방이라면
                    userDB.child(getCurrentUserID())
                        .child("likedBy")
                        .child("match")
                        .child(otherUserId)
                        .setValue(true)

                    userDB.child(getCurrentUserID())
                        .child("likedBy")
                        .child("match")
                        .child(getCurrentUserID())
                        .setValue(true)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

    }

    override fun onCardDragging(direction: Direction?, ratio: Float) {}

    override fun onCardSwiped(direction: Direction?) {
        when(direction) {
            Direction.Right -> like()
                Direction.Left -> disLike()
            else -> {

            }
        }
    }

    override fun onCardRewound() {}

    override fun onCardCanceled() {}

    override fun onCardAppeared(view: View?, position: Int) {}

    override fun onCardDisappeared(view: View?, position: Int) {}
}