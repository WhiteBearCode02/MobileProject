package com.example.mobileproject

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.mobileproject.ui.DashboardFragment
import com.example.mobileproject.ui.HomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * 하단 네비게이션바를 통해 2개의 프래그먼트 탭 전환을 통제하는 메인 액티비티
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottom_navigation)

        try {
            val info = packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md = java.security.MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val keyHash = android.util.Base64.encodeToString(md.digest(), android.util.Base64.DEFAULT).trim()
                android.util.Log.d("KakaoKeyHash", "==== 내 진짜 키 해시: $keyHash ====")
            }
        } catch (e: Exception) {
            android.util.Log.e("KakaoKeyHash", "추출 실패", e)
        }

        // 앱 처음 실행 시 기본 탭을 HomeFragment로 지정 및 초기 컨텍스트 설정
        if (savedInstanceState == null) {
            switchFragment(HomeFragment(), "Home")
        }

        // 하단 탭 버튼 클릭 리스너 연결
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(HomeFragment(), "Home")
                    true
                }
                R.id.nav_dashboard -> {
                    switchFragment(DashboardFragment(), "Dashboard")
                    true
                }
                else -> false
            }
        }

        // 안드로이드 최신 규격(API 33+)에 맞춘 백스택 제어 전용 콜백 등록 (onBackPressed 대체)
        setupBackPressedDispatcher()
    }

    /**
     * [요구사항 반영] 프래그먼트를 트랜잭션으로 교체하고, 백스택 트래킹을 수행합니다.
     */
    private fun switchFragment(fragment: Fragment, tag: String) {
        val fragmentManager = supportFragmentManager

        // 스택에 이미 쌓여있는 동일 프래그먼트가 있다면 중복 생성을 방지하기 위해 꺼내옴
        val currentFragment = fragmentManager.findFragmentByTag(tag)
        val transaction = fragmentManager.beginTransaction()

        // 프래그먼트 교체 연산 실행
        transaction.replace(R.id.fragment_container, currentFragment ?: fragment, tag)

        // 교수님 필수 요구사항: 백스택 관리를 포함하여 뒤로가기 시 이전 탭으로 전이되도록 유도
        transaction.addToBackStack(tag)
        transaction.commit()
    }

    /**
     * 최신 안드로이드 컴포넌트 환경에 맞는 백스택 콜백 처리 헬퍼 메서드
     */
    private fun setupBackPressedDispatcher() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragmentManager = supportFragmentManager

                // 백스택에 프래그먼트 내역이 2개 이상 남아있다면 이전 탭으로 뒤로가기 실행
                if (fragmentManager.backStackEntryCount > 1) {
                    fragmentManager.popBackStackImmediate()
                    // 바뀐 프래그먼트 상태에 맞춰 하단 탭 아이콘 하이라이팅 동기화
                    updateBottomNavSelection()
                } else {
                    // 더 이상 돌아갈 백스택이 없다면 이 콜백을 비활성화하고 시스템 뒤로가기로 앱 종료
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        // 액티비티의 수명 주기에 링크하여 콜백을 디스패처에 등록
        onBackPressedDispatcher.addCallback(this, callback)
    }

    /**
     * 사용자가 백스택 뒤로가기를 수행했을 때 현재 화면에 맞게 하단 아이콘 불빛을 맞춰주는 메서드
     */
    private fun updateBottomNavSelection() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is HomeFragment) {
            bottomNavigation.selectedItemId = R.id.nav_home
        } else if (currentFragment is DashboardFragment) {
            bottomNavigation.selectedItemId = R.id.nav_dashboard
        }
    }
}