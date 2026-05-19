package com.example.mobileproject

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.mobileproject.ui.DashboardFragment
import com.example.mobileproject.ui.EditActivity
import com.example.mobileproject.ui.HomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * [MainActivity]: 하단 네비게이션바를 통한 프래그먼트 탭 전환,
 * 교수님 필수 요구 사양인 상단 옵션 메뉴 2개 바인딩 및 액티비티 화면 전환(Intent)을 통제하는 뼈대 액티비티 계층.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottom_navigation)

        // 앱 처음 실행 시 가상 메모리 스냅샷(savedInstanceState) 유무를 검증하여 디폴트 탭으로 HomeFragment 적재
        if (savedInstanceState == null) {
            switchFragment(HomeFragment(), "Home")
        }

        // 하단 탭 버튼 클릭 리스너 연결 및 분기 제어 알고리즘 수립
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

        // 최신 안드로이드 보안 규격에 맞춘 백스택 제어 전용 콜백 등록 (onBackPressed 대체)
        setupBackPressedDispatcher()
    }

    /**
     * [onCreateOptionsMenu]: 컴파일러가 최상단 툴바 자원을 전개할 때 호출되는 라이프사이클 메서드.
     * 우리가 정의한 top_menu.xml 리소스를 메모리에 인플레이트하여 상단 메뉴 2개를 동적 배치합니다.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    /**
     * [onOptionsItemSelected]: 사용자가 상단 바 메뉴 아이템을 클릭했을 때 이벤트 패킷을 가로채 분기하는 라우팅 알고리즘.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                // [화면 전환 기능 구현]: 상단 검색 아이콘 클릭 시 여행 기록 신규 작성 화면(EditActivity)으로 인텐트 전이 가동
                Toast.makeText(this, "기록 추가 및 지도 확인 화면으로 이동합니다.", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, EditActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_info -> {
                // 서브 보조 메뉴 기능 동작 서술
                Toast.makeText(this, "순천향대 컴퓨터소프트웨어공학 기말 프로젝트 v1.0", Toast.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * [switchFragment]: 프래그먼트 매니저를 통해 컴포넌트를 원자적 트랜잭션으로 교체하고 백스택 트래킹을 수행합니다.
     */
    private fun switchFragment(fragment: Fragment, tag: String) {
        val fragmentManager = supportFragmentManager
        val currentFragment = fragmentManager.findFragmentByTag(tag)
        val transaction = fragmentManager.beginTransaction()

        transaction.replace(R.id.fragment_container, currentFragment ?: fragment, tag)
        transaction.addToBackStack(tag)
        transaction.commit()
    }

    /**
     * [setupBackPressedDispatcher]: Jetpack 컴포넌트 아키텍처에 부합하는 독립형 백스택 콜백 처리 디스패처 가동 알고리즘.
     */
    private fun setupBackPressedDispatcher() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragmentManager = supportFragmentManager
                if (fragmentManager.backStackEntryCount > 1) {
                    fragmentManager.popBackStackImmediate()
                    updateBottomNavSelection()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    /**
     * [updateBottomNavSelection]: 하드웨어 뒤로가기 시 뷰포트에 렌더링된 객체를 추론하여 하단 탭 하이라이트를 동기화하는 메서드.
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