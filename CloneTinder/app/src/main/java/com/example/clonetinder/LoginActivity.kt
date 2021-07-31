package com.example.clonetinder

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.telecom.Call
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import javax.security.auth.callback.Callback

class LoginActivity: AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        callbackManager = CallbackManager.Factory.create()
        //콜백 매니저 초기화

        initLoginBtn()
        initSignUpBtn()
        initEmailAndPasswordEditText()
        initFacebookLoginBtn()
    }

    private fun initLoginBtn() {
        val loginBtn = findViewById<Button>(R.id.loginBtn)
        loginBtn.setOnClickListener {
            val email = getInputEmail()
            val password = getInputPasswd()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        finish()
                        //위 행동이 성공적이라면 해당 Activity 종료
                    } else {
                        Toast.makeText(this, "로그인에 실패! 이메일 또는 비밀번호 재확인 요구.",Toast.LENGTH_SHORT).show()
                    }
                } //데이터 추가에 대한 리스너
            //이메일과 비밀번호를 입력받아 파이어베이스 로그인 진행
        }
    }

    private fun initSignUpBtn() {
        val signUpBtn = findViewById<Button>(R.id.signUpBtn)
        signUpBtn.setOnClickListener {
            val email = getInputEmail()
            val password = getInputPasswd()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "회원가입에 성공!",Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this,"이미 가입한 이메일, 회원가입 실패!",Toast.LENGTH_SHORT).show()
                    }
                }
            //파이어베이스를 통한 회원가입 진행
        }
    }

    private fun initFacebookLoginBtn() {
        val facebookLoginButton = findViewById<LoginButton>(R.id.facebookLogin)

        facebookLoginButton.setPermissions("email", "public_profile")
        facebookLoginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                //로그인 성공 시
                //페이스북을 통해 로그인 액세스 토큰을 가져온 뒤 파이어베이스로 옮기는 과정
                val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this@LoginActivity) { task ->
                        if (task.isSuccessful) {
                            finish()
                        }else {
                            Toast.makeText(this@LoginActivity, "페이스북 로그인 실패!",Toast.LENGTH_SHORT).show()
                        }
                    }
            }

            override fun onCancel() {
            }

            override fun onError(error: FacebookException?) {
                Toast.makeText(this@LoginActivity, "페이스북 로그인 실패!",Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun getInputEmail() : String {
        return findViewById<EditText>(R.id.emailEditText).text.toString()
    }

    private fun getInputPasswd() : String {
        return findViewById<EditText>(R.id.passwdEditText).text.toString()
    }

    private fun initEmailAndPasswordEditText() {
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwdEditText = findViewById<EditText>(R.id.passwdEditText)
        val signUpBtn = findViewById<Button>(R.id.signUpBtn)
        val loginBtn = findViewById<Button>(R.id.loginBtn)

        emailEditText.addTextChangedListener {
            val enable = emailEditText.text.isNotEmpty() && passwdEditText.text.isNotEmpty()
            //이메일과 비밀번호 입력창이 비어있지 않을 때 true
            loginBtn.isEnabled = enable
            signUpBtn.isEnabled = enable
        } //입력되는 Text에 따라 이벤트를 설정하는 리스너

        passwdEditText.addTextChangedListener {
            val enable = emailEditText.text.isNotEmpty() && passwdEditText.text.isNotEmpty()
            //이메일과 비밀번호 입력창이 비어있지 않을 때 true
            loginBtn.isEnabled = enable
            signUpBtn.isEnabled = enable
        } //입력되는 Text에 따라 이벤트를 설정하는 리스너
    }//값이 있을 때만 입력창이 활성화

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //콜백 매니저를 이용한 결과창 반환을 위한 메소드

        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

}